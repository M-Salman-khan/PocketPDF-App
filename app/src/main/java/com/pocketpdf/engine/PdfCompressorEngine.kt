package com.pocketpdf.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.pocketpdf.model.CompressionQuality
import com.pocketpdf.model.CompressionResult
import com.pocketpdf.model.SelectedPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object PdfCompressorEngine {

    suspend fun inspectPdf(context: Context, uri: Uri): Result<SelectedPdf> = withContext(Dispatchers.IO) {
        runCatching {
            var fileName = "document.pdf"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: "document.pdf"
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw IllegalStateException("Could not open file descriptor for URI: $uri")

            if (fileSize <= 0) {
                fileSize = pfd.statSize.coerceAtLeast(0L)
            }

            val pageCount = pfd.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    renderer.pageCount
                }
            }

            SelectedPdf(
                uri = uri,
                name = fileName,
                sizeBytes = fileSize,
                pageCount = pageCount
            )
        }
    }

    suspend fun compress(
        context: Context,
        inputUri: Uri,
        originalName: String,
        quality: CompressionQuality,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<CompressionResult> = withContext(Dispatchers.IO) {
        runCatching {
            val startTime = System.currentTimeMillis()
            val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(inputUri, "r")
                ?: throw IllegalStateException("Cannot access selected file")

            val originalSize = if (pfd.statSize > 0) pfd.statSize else 0L

            val renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            if (totalPages == 0) {
                renderer.close()
                pfd.close()
                throw IllegalStateException("PDF has no pages")
            }

            val document = PdfDocument()
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

            try {
                for (pageIndex in 0 until totalPages) {
                    currentCoroutineContext().ensureActive()

                    val page = renderer.openPage(pageIndex)
                    val originalWidth = page.width
                    val originalHeight = page.height

                    val targetWidth = (originalWidth * quality.scale).toInt().coerceAtLeast(72)
                    val targetHeight = (originalHeight * quality.scale).toInt().coerceAtLeast(72)

                    val renderedBitmap = Bitmap.createBitmap(
                        targetWidth,
                        targetHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(renderedBitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(renderedBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    // Compress rendered bitmap to JPEG
                    val stream = ByteArrayOutputStream()
                    renderedBitmap.compress(Bitmap.CompressFormat.JPEG, quality.jpegQuality, stream)
                    renderedBitmap.recycle()

                    val compressedBytes = stream.toByteArray()
                    val compressedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

                    // Write page to PDF document preserving original page dimensions
                    val pageInfo = PdfDocument.PageInfo.Builder(originalWidth, originalHeight, pageIndex + 1).create()
                    val pdfPage = document.startPage(pageInfo)
                    val destRect = Rect(0, 0, originalWidth, originalHeight)
                    pdfPage.canvas.drawBitmap(compressedBitmap, null, destRect, paint)
                    document.finishPage(pdfPage)

                    compressedBitmap.recycle()

                    onProgress(pageIndex + 1, totalPages)
                }

                currentCoroutineContext().ensureActive()

                val outputDir = File(context.cacheDir, "compressed_pdfs").apply { mkdirs() }
                val cleanBaseName = originalName.substringBeforeLast(".")
                val outputFileName = "compressed_${quality.name.lowercase()}_$cleanBaseName.pdf"
                val outputFile = File(outputDir, outputFileName)

                FileOutputStream(outputFile).use { out ->
                    document.writeTo(out)
                }

                val durationMs = System.currentTimeMillis() - startTime
                val compressedSize = outputFile.length()

                CompressionResult(
                    outputFile = outputFile,
                    outputFileName = outputFileName,
                    originalSizeBytes = originalSize,
                    compressedSizeBytes = compressedSize,
                    pageCount = totalPages,
                    durationMs = durationMs,
                    quality = quality
                )
            } finally {
                document.close()
                renderer.close()
                pfd.close()
            }
        }
    }
}
