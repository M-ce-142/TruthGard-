package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.InputStream
import kotlin.coroutines.resume

object OcrHelper {

    suspend fun extractTextFromBitmap(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val recognized = visionText.text.trim()
                    if (continuation.isActive) {
                        continuation.resume(recognized)
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        // Return empty string on recognition failure so caller can fallback gracefully
                        continuation.resume("")
                    }
                }
        } catch (e: Throwable) {
            if (continuation.isActive) {
                continuation.resume("")
            }
        }
    }

    fun decodeSampledBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 1200): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            } ?: return null

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                return null
            }

            var sampleSize = 1
            val maxOut = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
            while (maxOut / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
