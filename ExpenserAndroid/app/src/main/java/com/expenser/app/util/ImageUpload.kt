package com.expenser.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Base64
import com.expenser.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ImageUploadException(val code: String) : Exception(code) // noKey | notImage | tooLarge | failed

/** Uploads a picked image to imgbb (same service as the web app) and returns its public URL. */
object ImageUploader {
    private const val ENDPOINT = "https://api.imgbb.com/1/upload"
    private const val MAX_INPUT_BYTES = 12L * 1024 * 1024
    private const val MAX_SIDE = 1280
    val configured: Boolean get() = BuildConfig.IMGBB_API_KEY.isNotBlank()

    suspend fun upload(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val key = BuildConfig.IMGBB_API_KEY
        if (key.isBlank()) throw ImageUploadException("noKey")
        val resolver = context.contentResolver
        val size = runCatching { resolver.openFileDescriptor(uri, "r")?.use { it.statSize } }.getOrNull() ?: -1L
        if (size > MAX_INPUT_BYTES) throw ImageUploadException("tooLarge")

        // Downscale + JPEG so phone photos (5-10 MB) upload quickly.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw ImageUploadException("notImage")
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            ?: throw ImageUploadException("notImage")
        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height))
        val bmp = if (scale < 1f) Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1), (decoded.height * scale).toInt().coerceAtLeast(1), true) else decoded
        val flat = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888).also {
            val c = android.graphics.Canvas(it); c.drawColor(Color.WHITE); c.drawBitmap(bmp, 0f, 0f, null)
        }
        val out = ByteArrayOutputStream()
        flat.compress(Bitmap.CompressFormat.JPEG, 85, out)
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)

        try {
            val conn = (URL("$ENDPOINT?key=" + URLEncoder.encode(key, "UTF-8")).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; doOutput = true; connectTimeout = 20_000; readTimeout = 90_000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            conn.outputStream.use { it.write(("image=" + URLEncoder.encode(b64, "UTF-8")).toByteArray()) }
            val text = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            val data = JSONObject(text).optJSONObject("data")
            val url = data?.optString("url").orEmpty().ifBlank { data?.optString("display_url").orEmpty() }
            if (conn.responseCode !in 200..299 || url.isBlank()) throw ImageUploadException("failed")
            url
        } catch (e: ImageUploadException) { throw e } catch (e: Exception) { throw ImageUploadException("failed") }
    }
}
