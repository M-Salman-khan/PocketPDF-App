package com.pocketpdf.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class HistoryType(val label: String) {
    COMPRESSED_PDF("Compressed PDF"),
    IMAGES_TO_PDF("Created from Images"),
    IMAGES_TO_COMPRESSED_PDF("Created & Compressed")
}

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val filePath: String,
    val type: HistoryType,
    val originalSizeBytes: Long,
    val resultSizeBytes: Long,
    val pageCount: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    val file: File
        get() = File(filePath)

    val fileExists: Boolean
        get() = file.exists()

    val formattedDate: String
        get() = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))

    val formattedOriginalSize: String
        get() = formatBytes(originalSizeBytes)

    val formattedResultSize: String
        get() = formatBytes(resultSizeBytes)

    val savingsPercentage: Float
        get() = if (originalSizeBytes > 0 && resultSizeBytes < originalSizeBytes) {
            ((originalSizeBytes - resultSizeBytes).toFloat() / originalSizeBytes.toFloat() * 100f)
        } else {
            0f
        }
}
