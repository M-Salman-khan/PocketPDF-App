package com.pocketpdf.model

enum class CompressionQuality(
    val title: String,
    val subtitle: String,
    val scale: Float,
    val jpegQuality: Int,
    val badge: String
) {
    SCREEN(
        title = "Screen",
        subtitle = "Low resolution, maximum compression (~72 DPI)",
        scale = 0.7f,
        jpegQuality = 42,
        badge = "Smallest Size"
    ),
    EBOOK(
        title = "eBook",
        subtitle = "Balanced quality for digital reading (~150 DPI)",
        scale = 1.0f,
        jpegQuality = 65,
        badge = "Recommended"
    ),
    PRINTER(
        title = "Printer",
        subtitle = "High quality, suitable for printing (~200 DPI)",
        scale = 1.35f,
        jpegQuality = 82,
        badge = "High Quality"
    ),
    PREPRESS(
        title = "Prepress",
        subtitle = "Highest quality for professional use (~300 DPI)",
        scale = 1.75f,
        jpegQuality = 92,
        badge = "Max Fidelity"
    )
}
