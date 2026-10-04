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
        private const val MODEL_FILE      = "mycoVision.tflite"
        private const val LABELS_FILE     = "labels.txt"
        private const val IMG_SIZE        = 224
        private const val NUM_CHANNELS    = 3
        private const val BYTES_PER_FLOAT = 4
        private const val MIN_CONFIDENCE  = 0.40f  // seuil de confiance minimum

        // Noms français par genre
        private val FRENCH_NAMES = mapOf(
            "Agaricus"    to "Agaric",
            "Amanita"     to "Amanite",
            "Boletus"     to "Bolet",
            "Cantharellus" to "Girolle / Chanterelle",
            "Cortinarius" to "Cortinaire",
            "Entoloma"    to "Entolome",
            "Hygrocybe"   to "Hygrocybe",
            "Lactarius"   to "Lactaire",
            "Russula"     to "Russule"
        )

        // Comestibilité par genre — par sécurité on privilégie la prudence
        private val EDIBILITY = mapOf(
            "Agaricus"     to Pair(true,  "Comestible avec précaution"),
            "Amanita"      to Pair(false, "Potentiellement mortel"),
            "Boletus"      to Pair(true,  "Comestible"),
            "Cantharellus" to Pair(true,  "Comestible"),
            "Cortinarius"  to Pair(false, "Toxique"),
            "Entoloma"     to Pair(false, "Toxique"),
            "Hygrocybe"    to Pair(false, "Non recommandé"),
            "Lactarius"    to Pair(true,  "Comestible avec précaution"),
            "Russula"      to Pair(false, "Non recommandé")
        )
    }

    // Labels chargés depuis assets/labels.txt
    private val labels: List<String> by lazy { loadLabels() }
    private val interpreter: Interpreter by lazy { loadModel() }

    private fun loadLabels(): List<String> {
        return context.assets.open(LABELS_FILE)
            .bufferedReader()
            .readLines()
            .filter { it.isNotBlank() }
    }

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
        val input   = bitmapToByteBuffer(resized)

        // Sortie : 1 × NUM_CLASSES probabilités
        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(input, output)

        val scores     = output[0]
        val maxIndex   = scores.indices.maxByOrNull { scores[it] } ?: 0
        val confidence = scores[maxIndex]
        val genus      = if (maxIndex < labels.size) labels[maxIndex] else "Inconnu"

        android.util.Log.d("MycoVision", "Genre prédit : $genus (${(confidence*100).toInt()}%)")

        // Si confiance trop faible → résultat incertain
        if (confidence < MIN_CONFIDENCE) {
            return ClassificationResult(
                genusLabel     = "Inconnu",
                frenchName     = "Champignon non identifié",
                isEdible       = false,
                edibilityLabel = "Impossible à déterminer",
                confidence     = confidence
            )
        }

        val frenchName     = FRENCH_NAMES[genus] ?: genus
        val edibilityPair  = EDIBILITY[genus] ?: Pair(false, "Non recommandé")

        return ClassificationResult(
            genusLabel     = genus,
            frenchName     = frenchName,
            isEdible       = edibilityPair.first,
            edibilityLabel = edibilityPair.second,
            confidence     = confidence
        )
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return if (uri.scheme == "file") {
            BitmapFactory.decodeFile(uri.path, options)
                ?: throw IllegalArgumentException("Image illisible : ${uri.path}")
        } else {
            context.contentResolver.openInputStream(uri).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
                    ?: throw IllegalArgumentException("Image illisible : $uri")
            }
        }
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val bufferSize = 1 * IMG_SIZE * IMG_SIZE * NUM_CHANNELS * BYTES_PER_FLOAT
        val buffer = ByteBuffer.allocateDirect(bufferSize)
            .also { it.order(ByteOrder.nativeOrder()) }

        val pixels = IntArray(IMG_SIZE * IMG_SIZE)
        bitmap.getPixels(pixels, 0, IMG_SIZE, 0, 0, IMG_SIZE, IMG_SIZE)

        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF).toFloat())
            buffer.putFloat(((pixel shr  8) and 0xFF).toFloat())
            buffer.putFloat(( pixel         and 0xFF).toFloat())
        }

        buffer.rewind()
        return buffer
    }

    fun close() = interpreter.close()
}