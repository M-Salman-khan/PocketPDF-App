package com.pocketpdf.model

import android.net.Uri
import java.io.File
import java.util.Locale

data class SelectedPdf(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val pageCount: Int
) {
    val formattedSize: String
        get() = formatBytes(sizeBytes)
}

data class CompressionResult(
    val outputFile: File,
    val outputFileName: String,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val pageCount: Int,
    val durationMs: Long,
    val quality: CompressionQuality
) {
    val originalFormatted: String
        get() = formatBytes(originalSizeBytes)

    val compressedFormatted: String
        get() = formatBytes(compressedSizeBytes)

    val savedBytes: Long
        get() = (originalSizeBytes - compressedSizeBytes).coerceAtLeast(0L)

    val savedFormatted: String
        get() = formatBytes(savedBytes)

    val reductionPercentage: Float
        get() = if (originalSizeBytes > 0) {
            ((originalSizeBytes - compressedSizeBytes).toFloat() / originalSizeBytes.toFloat() * 100f).coerceIn(-100f, 99.9f)
        } else {
            0f
        }

    val isSizeReduced: Boolean
        get() = compressedSizeBytes < originalSizeBytes
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.size - 1) {
        value /= 1024.0
        unitIndex++
    }
    return if (unitIndex == 0) {
        "${bytes.toInt()} B"
    } else {
        String.format(Locale.US, "%.2f %s", value, units[unitIndex])
    }
}
