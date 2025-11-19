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
    var threshold: Float = 0.5f,
    var numThreads: Int = 2,
    var maxResults: Int = 3,
    var currentDelegate: Int = DELEGATE_CPU,
    val context: Context,
    val objectDetectorListener: DetectorListener?
) {
    private var interpreter: Interpreter? = null
    private var inputImageWidth: Int = 0
    private var inputImageHeight: Int = 0
    private var outputShape: IntArray = intArrayOf()

    // Label sesuai urutan di data.yaml kamu (3 kelas)
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
            DELEGATE_CPU -> { /* Default */ }
            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    options.addDelegate(GpuDelegate())
                } else {
                    objectDetectorListener?.onError("GPU tidak didukung, fallback ke CPU")
                }
            }
        }

        try {
            // Load Model Manual
            val modelFile = "best_float32.tflite"
            val assetFileDescriptor = context.assets.openFd(modelFile)
            val fileInputStream = java.io.FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            interpreter = Interpreter(modelBuffer, options)

            val inputTensor = interpreter?.getInputTensor(0)
            val outputTensor = interpreter?.getOutputTensor(0)

            inputImageWidth = inputTensor?.shape()?.get(1) ?: 640
            inputImageHeight = inputTensor?.shape()?.get(2) ?: 640
            // Default shape YOLOv11 biasanya [1, 4+Labels, Anchors]
            outputShape = outputTensor?.shape() ?: intArrayOf(1, 7, 8400)

        } catch (e: Exception) {
            objectDetectorListener?.onError("Gagal init TFLite: ${e.message}")
        }
    }

    fun detect(image: Bitmap, imageRotation: Int) {
        if (interpreter == null) {
            setupObjectDetector()
        }

        var inferenceTime = SystemClock.uptimeMillis()

        // 1. Preprocess
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .build()

        var tensorImage = TensorImage(org.tensorflow.lite.DataType.FLOAT32)
        tensorImage.load(image)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. Output Buffer
        val outputBuffer = ByteBuffer.allocateDirect(4 * outputShape[1] * outputShape[2])
        outputBuffer.order(ByteOrder.nativeOrder())

        val outputs = mutableMapOf<Int, Any>()
        outputs[0] = outputBuffer

        // 3. Inference
        interpreter?.runForMultipleInputsOutputs(arrayOf(tensorImage.buffer), outputs)

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        // 4. Post-Process
        val detections = parseYoloOutput(outputBuffer, outputShape[1], outputShape[2], image.width, image.height)

        objectDetectorListener?.onResults(
            detections,
            inferenceTime,
            image.height,
            image.width
        )
    }

    private fun parseYoloOutput(byteBuffer: ByteBuffer, rows: Int, cols: Int, imgW: Int, imgH: Int): MutableList<Detection> {
        byteBuffer.rewind()
        val floatBuffer = byteBuffer.asFloatBuffer()
        val allDetections = ArrayList<Detection>()

        for (c in 0 until cols) {
            var maxScore = 0f
            var maxClassIndex = -1

            // Loop score tiap kelas (mulai index 4)
            for (r in 4 until rows) {
                val score = floatBuffer.get(r * cols + c)
                if (score > maxScore) {
                    maxScore = score
                    maxClassIndex = r - 4
                }
            }

            if (maxScore > threshold) {
                val cx = floatBuffer.get(0 * cols + c)
                val cy = floatBuffer.get(1 * cols + c)
                val w = floatBuffer.get(2 * cols + c)
                val h = floatBuffer.get(3 * cols + c)

                val x1 = (cx - w / 2) * imgW
                val y1 = (cy - h / 2) * imgH
                val x2 = (cx + w / 2) * imgW
                val y2 = (cy + h / 2) * imgH

                val rect = RectF(x1, y1, x2, y2)
                val label = if (maxClassIndex in labels.indices) labels[maxClassIndex] else "Unknown"

                // Gunakan Category buatan sendiri
                val category = Category(label, maxScore, maxClassIndex)

                // Gunakan Detection buatan sendiri
                allDetections.add(Detection(rect, listOf(category)))
            }
        }

        return nms(allDetections)
    }

    private fun nms(detections: ArrayList<Detection>, nmsThreshold: Float = 0.5f): MutableList<Detection> {
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
// CLASS DATA BUATAN SENDIRI (DI BAWAH FILE)
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