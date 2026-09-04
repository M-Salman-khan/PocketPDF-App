package com.pocketpdf.viewmodel

import com.pocketpdf.model.CompressionQuality
import com.pocketpdf.model.CompressionResult
import com.pocketpdf.model.SelectedPdf

data class CompressorUiState(
    val selectedPdf: SelectedPdf? = null,
    val selectedQuality: CompressionQuality = CompressionQuality.EBOOK,
    val isInspecting: Boolean = false,
    val isCompressing: Boolean = false,
    val compressionProgress: Float = 0f,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val result: CompressionResult? = null,
    val errorMessage: String? = null,
    val saveSuccessMessage: String? = null
)
