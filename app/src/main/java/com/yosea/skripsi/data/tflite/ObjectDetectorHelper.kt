package com.yosea.skripsi.data.tflite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.SystemClock
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.PriorityQueue
import kotlin.math.max
import kotlin.math.min

class ObjectDetectorHelper(
    var threshold: Float = 0.4f,
    var numThreads: Int = 2,
    var maxResults: Int = 20,
    var currentDelegate: Int = DELEGATE_CPU,
    val context: Context,
    val objectDetectorListener: DetectorListener?
) {
    private var interpreter: Interpreter? = null
    private var inputImageWidth: Int = 640
    private var inputImageHeight: Int = 640

    // Label (Pastikan urutan sama dengan data.yaml)
    private val labels = listOf("Healthy", "Leaf Spot", "Powdery Mildew")

    init {
        setupObjectDetector()
    }

    fun clearObjectDetector() {
        interpreter?.close()
        interpreter = null
    }

    fun setupObjectDetector() {
        val options = Interpreter.Options()
        options.numThreads = numThreads

        when (currentDelegate) {
            DELEGATE_CPU -> { }

            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    options.addDelegate(GpuDelegate())
                } else {
                    objectDetectorListener?.onError("GPU tidak support, pakai CPU")
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

            // Baca Ukuran Input dari Model
            val inputTensor = interpreter?.getInputTensor(0)
            inputImageWidth = inputTensor?.shape()?.get(1) ?: 640
            inputImageHeight = inputTensor?.shape()?.get(2) ?: 640

        } catch (e: Exception) {
            objectDetectorListener?.onError("Gagal load model: ${e.message}")
        }
    }

    fun detect(image: Bitmap, imageRotation: Int) {
        if (interpreter == null) setupObjectDetector()

        var inferenceTime = SystemClock.uptimeMillis()

        // 1. Resize & Normalize Gambar
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f)) // Input Float32 (0-1)
            .build()

        var tensorImage = TensorImage(org.tensorflow.lite.DataType.FLOAT32)
        tensorImage.load(image)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. Siapkan Output Buffer
        val outputTensor = interpreter?.getOutputTensor(0)
        val outputShape = outputTensor?.shape() ?: intArrayOf(1, 7, 8400)

        // Output buffer (Flat Array)
        val outputBuffer = ByteBuffer.allocateDirect(4 * outputShape[1] * outputShape[2])
        outputBuffer.order(ByteOrder.nativeOrder())

        val outputs = mutableMapOf<Int, Any>()
        outputs[0] = outputBuffer

        // 3. Jalankan Deteksi
        interpreter?.runForMultipleInputsOutputs(arrayOf(tensorImage.buffer), outputs)

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        // 4. Parsing Pintar (Auto-Detect Shape)
        val detections = smartParseYoloOutput(outputBuffer, outputShape, image.width, image.height)

        objectDetectorListener?.onResults(
            detections,
            inferenceTime,
            image.height,
            image.width
        )
    }

    private fun smartParseYoloOutput(byteBuffer: ByteBuffer, shape: IntArray, imgW: Int, imgH: Int): MutableList<Detection> {
        byteBuffer.rewind()
        val floatBuffer = byteBuffer.asFloatBuffer()
        val allDetections = ArrayList<Detection>()

        // Logika Deteksi Bentuk Output (Transpose atau Tidak)
        // Format A: [1, Channels(7), Anchors(8400)] -> Channels First (Standard YOLOv8/11)
        // Format B: [1, Anchors(8400), Channels(7)] -> Anchors First

        val dim1 = shape[1] // Bisa 7 atau 8400
        val dim2 = shape[2] // Bisa 8400 atau 7

        var isChannelFirst = false

        // Kita asumsikan jumlah Anchors (8400) pasti lebih besar dari Channels (7)
        if (dim2 > dim1) {
            isChannelFirst = true // Format [1, 7, 8400]
        }

        val anchors = if (isChannelFirst) dim2 else dim1
        val channels = if (isChannelFirst) dim1 else dim2

        // Loop setiap Anchor (Kotak Prediksi)
        for (i in 0 until anchors) {
            // Fungsi helper untuk ambil data (otomatis handle transpose)
            fun getVal(row: Int): Float {
                return if (isChannelFirst) {
                    // Data loncat per kolom (dim2)
                    floatBuffer.get(row * dim2 + i)
                } else {
                    // Data urut per baris
                    floatBuffer.get(i * dim2 + row)
                }
            }

            // Cari Score Tertinggi di kelas (mulai index 4 sampai channel terakhir)
            var maxScore = 0f
            var maxClassIndex = -1

            for (c in 4 until channels) {
                val score = getVal(c)
                if (score > maxScore) {
                    maxScore = score
                    maxClassIndex = c - 4
                }
            }

            if (maxScore > threshold) {
                // Ambil Koordinat Mentah
                var cx = getVal(0)
                var cy = getVal(1)
                var w = getVal(2)
                var h = getVal(3)

                // LOGIC FIX NORMALISASI OTOMATIS
                // Jika koordinat > 1.0, berarti format Pixel (0-640). Kita bagi dengan 640.
                // Jika koordinat < 1.0, berarti sudah Normalized (0-1). Jangan dibagi lagi.
                if (cx > 1.0f || cy > 1.0f || w > 1.0f) {
                    cx /= inputImageWidth
                    cy /= inputImageHeight
                    w /= inputImageWidth
                    h /= inputImageHeight
                }

                // Convert ke ukuran layar HP (imgW, imgH)
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

    // NMS (Non-Maximum Suppression) untuk hapus kotak tumpang tindih
    private fun nms(detections: ArrayList<Detection>, nmsThreshold: Float = 0.45f): MutableList<Detection> {
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

// ==========================================
// DATA CLASS
// ==========================================
data class Detection(
    val boundingBox: RectF,
    val categories: List<Category>
)

data class Category(
    val label: String,
    val score: Float,
    val index: Int = 0
)