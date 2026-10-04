package com.bolo101.dermavision.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class Classifier(private val context: Context) {

    companion object {
        private const val MODEL_FILE      = "dermavision.tflite"
        private const val IMG_SIZE        = 224
        const val        THRESHOLD        = 0.35f
        private const val NUM_CHANNELS    = 3
        private const val BYTES_PER_FLOAT = 4
    }

    private val interpreter: Interpreter by lazy { loadModel() }

    private fun loadModel(): Interpreter {
        val afd = context.assets.openFd(MODEL_FILE)
        val fileChannel = FileInputStream(afd.fileDescriptor).channel
        val modelBuffer = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            afd.startOffset,
            afd.declaredLength
        )
        return Interpreter(modelBuffer, Interpreter.Options().apply {
            setNumThreads(4)
        })
    }

    fun classify(uri: Uri): ClassificationResult {
        val bitmap  = loadBitmapFromUri(uri)
        val scaled  = Bitmap.createScaledBitmap(bitmap, IMG_SIZE, IMG_SIZE, true)
        val resized = scaled.copy(Bitmap.Config.ARGB_8888, false)

        android.util.Log.d("DermaVision", "Bitmap config: ${resized.config} ${resized.width}x${resized.height}")

        val input = bitmapToByteBuffer(resized)

        android.util.Log.d("DermaVision", "Buffer position avant run: ${input.position()} remaining: ${input.remaining()}")

        // Lecture absolue — vérifie les données sans bouger la position
        val r0 = input.getFloat(0)
        val g0 = input.getFloat(4)
        val b0 = input.getFloat(8)
        android.util.Log.d("DermaVision", "Premier pixel RGB: R=$r0 G=$g0 B=$b0")

        val output = Array(1) { FloatArray(1) }
        interpreter.run(input, output)

        val score = output[0][0]
        android.util.Log.d("DermaVision", "Score brut: $score")

        return ClassificationResult(score, score >= THRESHOLD)
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return if (uri.scheme == "file") {
            BitmapFactory.decodeFile(uri.path, options)
                ?: throw IllegalArgumentException("Image illisible: ${uri.path}")
        } else {
            context.contentResolver.openInputStream(uri).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
                    ?: throw IllegalArgumentException("Image illisible: $uri")
            }
        }
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val bufferSize = 1 * IMG_SIZE * IMG_SIZE * NUM_CHANNELS * BYTES_PER_FLOAT
        val buffer = ByteBuffer
            .allocateDirect(bufferSize)
            .also { it.order(ByteOrder.nativeOrder()) }

        val pixels = IntArray(IMG_SIZE * IMG_SIZE)
        bitmap.getPixels(pixels, 0, IMG_SIZE, 0, 0, IMG_SIZE, IMG_SIZE)

        val nonZero = pixels.count { it != 0 }
        android.util.Log.d("DermaVision", "Pixels non-nuls: $nonZero / ${pixels.size}")

        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF).toFloat())
            buffer.putFloat(((pixel shr  8) and 0xFF).toFloat())
            buffer.putFloat(( pixel         and 0xFF).toFloat())
        }

        buffer.rewind() //flush image buffer

        android.util.Log.d("DermaVision", "Buffer après rewind: position=${buffer.position()} remaining=${buffer.remaining()} attendu=$bufferSize")

        return buffer
    }

    fun close() = interpreter.close()
}