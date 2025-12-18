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
    var numThreads: Int = 2,
    var maxResults: Int = 20,
    // UBAH DEFAULT KE GPU AGAR "DIPAKSA"
    var currentDelegate: Int = DELEGATE_GPU,
    val context: Context,
    val objectDetectorListener: DetectorListener?
) {
    private var interpreter: Interpreter? = null
    private var inputImageWidth: Int = 640
    private var inputImageHeight: Int = 640

    private var imageProcessor: ImageProcessor? = null
    private var tensorImage: TensorImage? = null
    private var outputBuffer: ByteBuffer? = null
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
                    // BERITAHU JIKA GPU GAGAL (STRICT MODE)
                    objectDetectorListener?.onError("GPU Error: Perangkat tidak support GPU TFLite. Coba ganti ke CPU.")
                    // Fallback otomatis dimatikan agar kita tahu ini berjalan di GPU atau tidak
                    // throw RuntimeException("GPU Wajib") // Uncomment jika ingin crash saat tidak ada GPU
                }
            }
        }

        try {
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

            // --- INISIALISASI OBJEK SEKALI SAJA ---

            // 1. Siapkan Image Processor Dasar (Resize Stretch & Normalize)
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()

            // 2. Siapkan Tensor Image
            tensorImage = TensorImage(org.tensorflow.lite.DataType.FLOAT32)

            // 3. Siapkan Output Buffer
            val outputTensor = interpreter?.getOutputTensor(0)
            val outputShape = outputTensor?.shape() ?: intArrayOf(1, 7, 8400)

            // Alokasi memori buffer HANYA SEKALI
            outputBuffer = ByteBuffer.allocateDirect(4 * outputShape[1] * outputShape[2])
            outputBuffer?.order(ByteOrder.nativeOrder())
            outputBuffer?.let { outputMap[0] = it }

        } catch (e: Exception) {
            objectDetectorListener?.onError("Gagal load model: ${e.message}")
        }
    }

    fun detect(image: Bitmap, imageRotation: Int) {
        if (interpreter == null) setupObjectDetector()

        var inferenceTime = SystemClock.uptimeMillis()

        // --- PROSES DETEKSI CEPAT (OPTIMIZED) ---

        // 1. Load Gambar ke Container yang sudah ada
        tensorImage?.load(image)

        // 2. Rotasi & Resize
        // Gunakan Rot90Op jika ada rotasi, lalu Resize (Stretch)
        val dynamicProcessor = if (imageRotation != 0) {
            ImageProcessor.Builder()
                .add(Rot90Op(-imageRotation / 90))
                .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()
        } else {
            imageProcessor // Pakai yang dicache kalau tidak ada rotasi
        }

        val processedImage = dynamicProcessor?.process(tensorImage) ?: return

        // 3. Inference (Tanpa alokasi memori baru)
        outputBuffer?.rewind()
        interpreter?.runForMultipleInputsOutputs(arrayOf(processedImage.buffer), outputMap)

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        // 4. Parsing (Menggunakan Logic "Stretch" yang Presisi)
        // Kita perlu dimensi efektif setelah rotasi untuk scaling balik yang benar
        val isRotated = imageRotation == 90 || imageRotation == 270
        val finalW = if (isRotated) image.height else image.width
        val finalH = if (isRotated) image.width else image.height

        val outputTensor = interpreter?.getOutputTensor(0)
        val outputShape = outputTensor?.shape() ?: intArrayOf(1, 7, 8400)

        outputBuffer?.let { buffer ->
            val detections = smartParseYoloOutput(buffer, outputShape, finalW, finalH)

            objectDetectorListener?.onResults(
                detections,
                inferenceTime,
                finalH,
                finalW
            )
        }
    }

    // --- LOGIC PARSING TETAP SAMA (Hanya optimasi loop) ---
    private fun smartParseYoloOutput(byteBuffer: ByteBuffer, shape: IntArray, imgW: Int, imgH: Int): MutableList<Detection> {
        byteBuffer.rewind()
        val floatBuffer = byteBuffer.asFloatBuffer()
        val allDetections = ArrayList<Detection>()

        val dim1 = shape[1]
        val dim2 = shape[2]
        var isChannelFirst = false
        if (dim2 > dim1) {
            isChannelFirst = true
        }

        val anchors = if (isChannelFirst) dim2 else dim1
        val channels = if (isChannelFirst) dim1 else dim2

        // Loop Optimized
        for (i in 0 until anchors) {

            var maxScore = 0f
            var maxClassIndex = -1

            // Cari score tertinggi
            for (c in 4 until channels) {
                val score = if (isChannelFirst) floatBuffer.get(c * dim2 + i) else floatBuffer.get(i * dim2 + c)
                if (score > maxScore) {
                    maxScore = score
                    maxClassIndex = c - 4
                }
            }

            if (maxScore > threshold) {
                var cx = if (isChannelFirst) floatBuffer.get(0 * dim2 + i) else floatBuffer.get(i * dim2 + 0)
                var cy = if (isChannelFirst) floatBuffer.get(1 * dim2 + i) else floatBuffer.get(i * dim2 + 1)
                var w = if (isChannelFirst) floatBuffer.get(2 * dim2 + i) else floatBuffer.get(i * dim2 + 2)
                var h = if (isChannelFirst) floatBuffer.get(3 * dim2 + i) else floatBuffer.get(i * dim2 + 3)

                // LOGIC FIX NORMALISASI
                if (cx > 1.0f || cy > 1.0f || w > 1.0f) {
                    cx /= inputImageWidth
                    cy /= inputImageHeight
                    w /= inputImageWidth
                    h /= inputImageHeight
                }

                // Convert ke ukuran layar HP (Logic Stretch)
                val x1 = (cx - w / 2) * imgW
                val y1 = (cy - h / 2) * imgH
                val x2 = (cx + w / 2) * imgW
                val y2 = (cy + h / 2) * imgH

                val rect = RectF(x1, y1, x2, y2)
                val label = if (maxClassIndex in labels.indices) labels[maxClassIndex] else "Unknown"

                allDetections.add(Detection(rect, listOf(Category(label, maxScore, maxClassIndex))))
            }
        }

        return nms(allDetections)
    }

    private fun nms(detections: ArrayList<Detection>, nmsThreshold: Float = 0.45f): MutableList<Detection> {
        if (detections.isEmpty()) return mutableListOf()
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