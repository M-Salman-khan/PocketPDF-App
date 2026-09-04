package com.pocketpdf.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.pocketpdf.model.CompressionQuality
import com.pocketpdf.model.ImageItem
import com.pocketpdf.model.ImageQualityPreset
import com.pocketpdf.model.ImageToPdfResult
import com.pocketpdf.model.PageFitMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object ImageToPdfEngine {

    suspend fun resolveImageItems(context: Context, uris: List<Uri>): List<ImageItem> = withContext(Dispatchers.IO) {
        uris.map { uri ->
            var name = "image_${System.currentTimeMillis()}.jpg"
            var size = 0L

            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) name = cursor.getString(nameIdx) ?: name
                        if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                    }
                }
            }

            if (size <= 0) {
                runCatching {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        size = pfd.statSize.coerceAtLeast(0L)
                    }
                }
            }

            ImageItem(uri = uri, name = name, sizeBytes = size)
        }
    }

    suspend fun convertImagesToPdf(
        context: Context,
        images: List<ImageItem>,
        fitMode: PageFitMode,
        quality: ImageQualityPreset,
        compressInOneGo: Boolean = false,
        compressionQuality: CompressionQuality = CompressionQuality.EBOOK,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<ImageToPdfResult> = withContext(Dispatchers.IO) {
        runCatching {
            if (images.isEmpty()) {
                throw IllegalArgumentException("No images selected")
            }

            val totalInputSizeBytes = images.sumOf { it.sizeBytes }
            val startTime = System.currentTimeMillis()
            val document = PdfDocument()
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

            val a4Width = 595
            val a4Height = 842
            val margin = 24

            try {
                images.forEachIndexed { index, imageItem ->
                    currentCoroutineContext().ensureActive()

                    val bitmap = decodeAndProcessBitmap(
                        context = context,
                        uri = imageItem.uri,
                        quality = quality,
                        compressInOneGo = compressInOneGo,
                        compressionQuality = compressionQuality
                    )
                    try {
                        val (pageWidth, pageHeight, destRect) = when (fitMode) {
                            PageFitMode.FIT_A4 -> {
                                val isLandscape = bitmap.width > bitmap.height
                                val pWidth = if (isLandscape) a4Height else a4Width
                                val pHeight = if (isLandscape) a4Width else a4Height

                                val availableWidth = pWidth - (margin * 2)
                                val availableHeight = pHeight - (margin * 2)

                                val scale = min(
                                    availableWidth.toFloat() / bitmap.width.toFloat(),
                                    availableHeight.toFloat() / bitmap.height.toFloat()
                                )

                                val drawWidth = (bitmap.width * scale).toInt()
                                val drawHeight = (bitmap.height * scale).toInt()

                                val left = margin + (availableWidth - drawWidth) / 2
                                val top = margin + (availableHeight - drawHeight) / 2

                                Triple(pWidth, pHeight, Rect(left, top, left + drawWidth, top + drawHeight))
                            }
                            PageFitMode.ORIGINAL_IMAGE_SIZE -> {
                                val pWidth = (bitmap.width * 72f / 150f).toInt().coerceAtLeast(100)
                                val pHeight = (bitmap.height * 72f / 150f).toInt().coerceAtLeast(100)
                                Triple(pWidth, pHeight, Rect(0, 0, pWidth, pHeight))
                            }
                        }

                        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                        val page = document.startPage(pageInfo)

                        // Clean white background
                        page.canvas.drawColor(Color.WHITE)
                        page.canvas.drawBitmap(bitmap, null, destRect, paint)

                        document.finishPage(page)
                    } finally {
                        bitmap.recycle()
                    }

                    onProgress(index + 1, images.size)
                }

                currentCoroutineContext().ensureActive()

                val outputDir = File(context.cacheDir, "images_to_pdf").apply { mkdirs() }
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val prefix = if (compressInOneGo) "compressed_${compressionQuality.name.lowercase()}" else "document"
                val outputFileName = "${prefix}_$timeStamp.pdf"
                val outputFile = File(outputDir, outputFileName)

                FileOutputStream(outputFile).use { out ->
                    document.writeTo(out)
                }

                val duration = System.currentTimeMillis() - startTime
                val fileSize = outputFile.length()

                ImageToPdfResult(
                    outputFile = outputFile,
                    outputFileName = outputFileName,
                    fileSizeBytes = fileSize,
                    originalImagesSizeBytes = totalInputSizeBytes,
                    isCompressed = compressInOneGo,
                    compressionQuality = if (compressInOneGo) compressionQuality else null,
                    pageCount = images.size,
                    durationMs = duration
                )
            } finally {
                document.close()
            }
        }
    }

    private fun decodeAndProcessBitmap(
        context: Context,
        uri: Uri,
        quality: ImageQualityPreset,
        compressInOneGo: Boolean = false,
        compressionQuality: CompressionQuality = CompressionQuality.EBOOK
    ): Bitmap {
        // 1. Read EXIF Orientation
        val rotationDegrees = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        }.getOrNull() ?: 0f

        // 2. Determine target maximum dimension and JPEG quality
        val targetMaxDimension = if (compressInOneGo) {
            when (compressionQuality) {
                CompressionQuality.SCREEN -> 1000
                CompressionQuality.EBOOK -> 1500
                CompressionQuality.PRINTER -> 2100
                CompressionQuality.PREPRESS -> 2800
            }
        } else {
            quality.maxDimension
        }

        val targetJpegQuality = if (compressInOneGo) {
            compressionQuality.jpegQuality
        } else {
            quality.jpegQuality
        }

        // 3. Decode bounds to calculate inSampleSize
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        var sampleSize = 1
        val rawMaxDim = max(boundsOptions.outWidth, boundsOptions.outHeight)
        while (rawMaxDim / (sampleSize * 2) >= targetMaxDimension) {
            sampleSize *= 2
        }

        // 4. Decode scaled bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw IllegalStateException("Failed to decode image from URI: $uri")

        // 5. Rotate if needed
        val orientedBitmap = if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            if (rotated != rawBitmap) {
                rawBitmap.recycle()
            }
            rotated
        } else {
            rawBitmap
        }

        // 6. Fine scale if still exceeding targetMaxDimension
        val currentMax = max(orientedBitmap.width, orientedBitmap.height)
        val finalScaledBitmap = if (currentMax > targetMaxDimension) {
            val scaleFactor = targetMaxDimension.toFloat() / currentMax.toFloat()
            val newWidth = (orientedBitmap.width * scaleFactor).toInt().coerceAtLeast(1)
            val newHeight = (orientedBitmap.height * scaleFactor).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(orientedBitmap, newWidth, newHeight, true)
            if (scaled != orientedBitmap) {
                orientedBitmap.recycle()
            }
            scaled
        } else {
            orientedBitmap
        }

        // 7. Apply JPEG compression quality preset
        val compressedStream = ByteArrayOutputStream()
        finalScaledBitmap.compress(Bitmap.CompressFormat.JPEG, targetJpegQuality, compressedStream)
        finalScaledBitmap.recycle()

        val compressedBytes = compressedStream.toByteArray()
        return BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
    }
}
