package com.pocketpdf.model

import android.net.Uri
import java.io.File
import java.util.UUID

data class ImageItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val sizeBytes: Long
)

enum class PageFitMode(val title: String, val description: String) {
    FIT_A4(
        title = "A4 Page (Standard)",
        description = "Fits each image cleanly onto a standard A4 page (595 × 842 pt)"
    ),
    ORIGINAL_IMAGE_SIZE(
        title = "Fit Image Bounds",
        description = "Adjusts page dimensions to exactly match the image size"
    )
}

enum class ImageQualityPreset(
    val title: String,
    val jpegQuality: Int,
    val maxDimension: Int,
    val badge: String
) {
    COMPACT(
        title = "Compact",
        jpegQuality = 55,
        maxDimension = 1280,
        badge = "Smallest File"
    ),
    BALANCED(
        title = "Balanced",
        jpegQuality = 75,
        maxDimension = 1800,
        badge = "Recommended"
    ),
    HIGH(
        title = "High Quality",
        jpegQuality = 90,
        maxDimension = 2560,
        badge = "Sharpest"
    )
}

data class ImageToPdfResult(
    val outputFile: File,
    val outputFileName: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val durationMs: Long
) {
    val formattedSize: String
        get() = formatBytes(fileSizeBytes)
}
