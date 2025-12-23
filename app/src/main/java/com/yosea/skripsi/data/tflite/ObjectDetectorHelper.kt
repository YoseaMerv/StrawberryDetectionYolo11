package com.yosea.skripsi.data.tflite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.PriorityQueue
import kotlin.math.max
import kotlin.math.min

class ObjectDetectorHelper(
    var threshold: Float = 0.5f,
    var numThreads: Int = 4,
    var maxResults: Int = 20,
    var currentDelegate: Int = DELEGATE_GPU,
    val context: Context
) {
    var objectDetectorListener: DetectorListener? = null

    private var interpreter: Interpreter? = null

    private var inputImageWidth: Int = 640
    private var inputImageHeight: Int = 640

    private var imageProcessor: ImageProcessor? = null
    private var tensorImage: TensorImage? = null

    // Optimasi Memori: Alokasi sekali saja agar GC tidak bekerja keras
    private var outputBuffer: ByteBuffer? = null
    private var outputArray: FloatArray? = null

    private val outputMap = mutableMapOf<Int, Any>()

    private val labels = listOf("Healthy", "Leaf Spot", "Powdery Mildew")

    init {
        setupObjectDetector()
    }

    fun clearObjectDetector() {
        interpreter?.close()
        interpreter = null
        imageProcessor = null
        tensorImage = null
        outputBuffer = null
        outputArray = null
        outputMap.clear()
    }

    fun setupObjectDetector() {
        val options = Interpreter.Options()
        options.numThreads = numThreads

        when (currentDelegate) {
            DELEGATE_CPU -> { }
            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    options.addDelegate(GpuDelegate())
                    Log.d("ObjectDetector", "Sukses: Menggunakan GPU Delegate")
                } else {
                    objectDetectorListener?.onError("GPU Error: Perangkat tidak support GPU TFLite. Fallback ke CPU.")
                }
            }
        }

        try {
            // Pastikan nama file sesuai dengan yang ada di assets
            val modelFile = "best_float32.tflite"
            val assetFileDescriptor = context.assets.openFd(modelFile)
            val fileInputStream = java.io.FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            interpreter = Interpreter(modelBuffer, options)

            val inputTensor = interpreter?.getInputTensor(0)
            inputImageWidth = inputTensor?.shape()?.get(1) ?: 640
            inputImageHeight = inputTensor?.shape()?.get(2) ?: 640

            // --- INISIALISASI OBJEK SEKALI SAJA (Optimasi) ---

            // 1. Siapkan Image Processor Dasar
            // Optimasi: Gunakan NEAREST_NEIGHBOR (Lebih cepat render)
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .add(NormalizeOp(0f, 255f))
                .build()

            // 2. Siapkan Tensor Image
            tensorImage = TensorImage(org.tensorflow.lite.DataType.FLOAT32)

            // 3. Siapkan Output Buffer & Array
            val outputTensor = interpreter?.getOutputTensor(0)
            val outputShape = outputTensor?.shape() ?: intArrayOf(1, 7, 8400) // Default YOLOv8/11 output

            // Hitung ukuran total elemen (misal 1 * 7 * 8400)
            val totalElements = outputShape[1] * outputShape[2]

            // A. Buffer Native (Wajib untuk TFLite)
            outputBuffer = ByteBuffer.allocateDirect(4 * totalElements)
            outputBuffer?.order(ByteOrder.nativeOrder())
            outputBuffer?.let { outputMap[0] = it }

            // B. Array Kotlin (Optimasi Baca Cepat) - Dibuat sekali saja
            outputArray = FloatArray(totalElements)

        } catch (e: Exception) {
            objectDetectorListener?.onError("Gagal load model: ${e.message}")
        }
    }

    fun detect(image: Bitmap, imageRotation: Int) {
        if (interpreter == null) setupObjectDetector()

        var inferenceTime = SystemClock.uptimeMillis()

        // 1. Load Gambar
        tensorImage?.load(image)

        // 2. Rotasi & Resize (Optimasi Dynamic Processor)
        // Jika ada rotasi, buat processor baru. Jika tidak, pakai yang dicache.
        val dynamicProcessor = if (imageRotation != 0) {
            ImageProcessor.Builder()
                .add(Rot90Op(-imageRotation / 90))
                .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .add(NormalizeOp(0f, 255f))
                .build()
        } else {
            imageProcessor
        }

        val processedImage = dynamicProcessor?.process(tensorImage) ?: return

        // 3. Inference
        outputBuffer?.rewind()
        interpreter?.runForMultipleInputsOutputs(arrayOf(processedImage.buffer), outputMap)

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        // 4. Parsing Output
        val isRotated = imageRotation == 90 || imageRotation == 270
        val finalW = if (isRotated) image.height else image.width
        val finalH = if (isRotated) image.width else image.height

        val outputTensor = interpreter?.getOutputTensor(0)
        val outputShape = outputTensor?.shape() ?: intArrayOf(1, 7, 8400)

        // Pastikan buffer dan array tidak null
        if (outputBuffer != null && outputArray != null) {
            val detections = smartParseYoloOutput(outputBuffer!!, outputArray!!, outputShape, finalW, finalH)

            objectDetectorListener?.onResults(
                detections,
                inferenceTime,
                finalH,
                finalW
            )
        }
    }

    /**
     * Logic Parsing Super Cepat
     * Menggunakan FloatArray yang sudah dialokasikan sebelumnya.
     */
    private fun smartParseYoloOutput(
        byteBuffer: ByteBuffer,
        targetArray: FloatArray, // Array penampung yang disiapkan di init
        shape: IntArray,
        imgW: Int,
        imgH: Int
    ): MutableList<Detection> {

        // 1. Salin data dari Native Memory (ByteBuffer) ke Java Heap (FloatArray) SEKALIGUS.
        // Ini jauh lebih cepat daripada memanggil .get() ribuan kali di dalam loop.
        byteBuffer.rewind()
        byteBuffer.asFloatBuffer().get(targetArray)

        val allDetections = ArrayList<Detection>()

        val dim1 = shape[1] // Misal: 7 (4 box + 3 class)
        val dim2 = shape[2] // Misal: 8400 (anchors)

        // Cek struktur output (Channels Last vs Channels First)
        val isChannelFirst = dim2 > dim1
        val anchors = if (isChannelFirst) dim2 else dim1
        val numClasses = labels.size

        // Loop Optimized
        for (i in 0 until anchors) {

            var maxScore = 0f
            var maxClassIndex = -1

            // --- Tahap 1: Cari Score Tertinggi Dulu (Hemat CPU) ---
            for (c in 0 until numClasses) {
                // Hitung index flat array secara manual
                // Struktur data biasanya: [x, y, w, h, score1, score2, ...]
                val rawIndex = if (isChannelFirst) {
                    (4 + c) * dim2 + i // [channel][anchor]
                } else {
                    i * (4 + numClasses) + (4 + c) // [anchor][channel]
                }

                // Akses array langsung (O(1) access time)
                val score = targetArray[rawIndex]

                if (score > maxScore) {
                    maxScore = score
                    maxClassIndex = c
                }
            }

            // --- Tahap 2: Ambil Koordinat HANYA JIKA Score > Threshold ---
            if (maxScore > threshold) {
                val idxCx = if (isChannelFirst) 0 * dim2 + i else i * (4 + numClasses) + 0
                val idxCy = if (isChannelFirst) 1 * dim2 + i else i * (4 + numClasses) + 1
                val idxW  = if (isChannelFirst) 2 * dim2 + i else i * (4 + numClasses) + 2
                val idxH  = if (isChannelFirst) 3 * dim2 + i else i * (4 + numClasses) + 3

                var cx = targetArray[idxCx]
                var cy = targetArray[idxCy]
                var w  = targetArray[idxW]
                var h  = targetArray[idxH]

                // Normalisasi jika output model > 1.0 (misal koordinat pixel 0-640)
                if (cx > 1.0f || cy > 1.0f || w > 1.0f) {
                    cx /= inputImageWidth
                    cy /= inputImageHeight
                    w /= inputImageWidth
                    h /= inputImageHeight
                }

                // Konversi ke koordinat Layar (Stretch Logic)
                val x1 = (cx - w / 2) * imgW
                val y1 = (cy - h / 2) * imgH
                val x2 = (cx + w / 2) * imgW
                val y2 = (cy + h / 2) * imgH

                val rect = RectF(x1, y1, x2, y2)
                val label = labels.getOrElse(maxClassIndex) { "Unknown" }

                allDetections.add(Detection(rect, listOf(Category(label, maxScore, maxClassIndex))))
            }
        }

        return nms(allDetections)
    }

    private fun nms(detections: ArrayList<Detection>, nmsThreshold: Float = 0.45f): MutableList<Detection> {
        if (detections.isEmpty()) return mutableListOf()

        // PriorityQueue efisien untuk sorting score
        val pq = PriorityQueue<Detection> { o1, o2 ->
            o2.categories[0].score.compareTo(o1.categories[0].score)
        }
        pq.addAll(detections)

        val finalDetections = ArrayList<Detection>()
        while (pq.isNotEmpty()) {
            val best = pq.poll()
            finalDetections.add(best)
            val iterator = pq.iterator()
            while (iterator.hasNext()) {
                val other = iterator.next()
                if (calculateIoU(best.boundingBox, other.boundingBox) > nmsThreshold) {
                    iterator.remove()
                }
            }
            if (finalDetections.size >= maxResults) break
        }
        return finalDetections
    }

    private fun calculateIoU(boxA: RectF, boxB: RectF): Float {
        val interLeft = max(boxA.left, boxB.left)
        val interTop = max(boxA.top, boxB.top)
        val interRight = min(boxA.right, boxB.right)
        val interBottom = min(boxA.bottom, boxB.bottom)

        if (interLeft < interRight && interTop < interBottom) {
            val interArea = (interRight - interLeft) * (interBottom - interTop)
            val boxAArea = boxA.width() * boxA.height()
            val boxBArea = boxB.width() * boxB.height()
            return interArea / (boxAArea + boxBArea - interArea)
        }
        return 0f
    }

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(results: MutableList<Detection>?, inferenceTime: Long, imageHeight: Int, imageWidth: Int)
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
    }
}

data class Detection(val boundingBox: RectF, val categories: List<Category>)
data class Category(val label: String, val score: Float, val index: Int = 0)