package mx.uaemex.anden.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Fotos de los activos: se guardan reducidas en la memoria de la app y, para que
 * los demas integrantes las vean aunque Cloud Storage no este disponible, se
 * genera una miniatura JPEG en Base64 que viaja dentro del documento de Firestore.
 */
object PhotoUtils {

    private const val MAX_SIDE = 1280
    private const val THUMB_SIDE = 640

    /** Copia la foto elegida a filesDir/activos ya reducida y con la orientacion correcta. */
    fun saveReduced(context: Context, source: Uri, fileName: String): File? = runCatching {
        val bitmap = decode(context, source) ?: return null
        val scaled = scale(bitmap, MAX_SIDE)
        val dir = File(context.filesDir, "activos").apply { mkdirs() }
        val target = File(dir, fileName)
        target.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 82, it) }
        target
    }.getOrNull()

    /** Miniatura en Base64 (~40-90 KB) para el campo fotoMiniatura de Firestore. */
    fun thumbnailBase64(file: File): String = runCatching {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return ""
        val thumb = scale(bitmap, THUMB_SIDE)
        val out = ByteArrayOutputStream()
        thumb.compress(Bitmap.CompressFormat.JPEG, 65, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }.getOrDefault("")

    /** Escribe la miniatura recibida de Firestore como archivo local y devuelve su file:// */
    fun writeThumbnail(context: Context, base64: String, fileName: String): String? = runCatching {
        val dir = File(context.filesDir, "activos").apply { mkdirs() }
        val target = File(dir, fileName)
        if (!target.exists()) target.writeBytes(Base64.decode(base64, Base64.NO_WRAP))
        Uri.fromFile(target).toString()
    }.getOrNull()

    private fun decode(context: Context, source: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder aplica la rotacion EXIF de la camara.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, source)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val side = maxOf(info.size.width, info.size.height)
                if (side > MAX_SIDE * 2) decoder.setTargetSampleSize(side / (MAX_SIDE * 2) + 1)
            }
        } else {
            context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it) }
        }

    private fun scale(bitmap: Bitmap, maxSide: Int): Bitmap {
        val side = maxOf(bitmap.width, bitmap.height)
        if (side <= maxSide) return bitmap
        val ratio = maxSide.toFloat() / side
        return Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true
        )
    }
}
