package com.pocketpdf.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpdf.data.HistoryRepository
import com.pocketpdf.engine.PdfCompressorEngine
import com.pocketpdf.model.CompressionQuality
import com.pocketpdf.model.HistoryItem
import com.pocketpdf.model.HistoryType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileInputStream

class CompressorViewModel(application: Application) : AndroidViewModel(application) {

    val historyRepository = HistoryRepository(application)

    private val _uiState = MutableStateFlow(CompressorUiState())
    val uiState: StateFlow<CompressorUiState> = _uiState.asStateFlow()

    private var compressionJob: Job? = null

    fun onPdfSelected(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isInspecting = true,
                    result = null,
                    errorMessage = null
                )
            }

            PdfCompressorEngine.inspectPdf(context.applicationContext, uri)
                .onSuccess { pdf ->
                    _uiState.update {
                        it.copy(
                            selectedPdf = pdf,
                            isInspecting = false,
                            result = null,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isInspecting = false,
                            errorMessage = "Failed to open PDF: ${error.localizedMessage ?: "Unknown error"}"
                        )
                    }
                }
        }
    }

    fun onQualitySelected(quality: CompressionQuality) {
        _uiState.update {
            it.copy(
                selectedQuality = quality,
                result = null // Invalidate previous result so user can compress with the newly selected quality
            )
        }
    }

    fun startCompression(context: Context) {
        val currentPdf = _uiState.value.selectedPdf ?: return
        val currentQuality = _uiState.value.selectedQuality

        compressionJob?.cancel()
        compressionJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCompressing = true,
                    compressionProgress = 0f,
                    currentPage = 0,
                    totalPages = currentPdf.pageCount,
                    errorMessage = null,
                    result = null
                )
            }

            val result = PdfCompressorEngine.compress(
                context = context.applicationContext,
                inputUri = currentPdf.uri,
                originalName = currentPdf.name,
                quality = currentQuality,
                onProgress = { current, total ->
                    _uiState.update {
                        it.copy(
                            currentPage = current,
                            totalPages = total,
                            compressionProgress = current.toFloat() / total.toFloat()
                        )
                    }
                }
            )

            result.onSuccess { compressionResult ->
                _uiState.update {
                    it.copy(
                        isCompressing = false,
                        result = compressionResult,
                        errorMessage = null
                    )
                }

                // Save to history automatically
                historyRepository.addItem(
                    HistoryItem(
                        title = compressionResult.outputFileName,
                        filePath = compressionResult.outputFile.absolutePath,
                        type = HistoryType.COMPRESSED_PDF,
                        originalSizeBytes = compressionResult.originalSizeBytes,
                        resultSizeBytes = compressionResult.compressedSizeBytes,
                        pageCount = compressionResult.pageCount
                    )
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isCompressing = false,
                        errorMessage = "Compression failed: ${error.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun cancelCompression() {
        compressionJob?.cancel()
        compressionJob = null
        _uiState.update {
            it.copy(
                isCompressing = false,
                compressionProgress = 0f,
                errorMessage = "Compression cancelled"
            )
        }
    }

    fun saveCompressedFile(targetUri: Uri, context: Context) {
        val result = _uiState.value.result ?: return
        viewModelScope.launch {
            runCatching {
                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    FileInputStream(result.outputFile).use { input ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException("Cannot open destination")
            }.onSuccess {
                _uiState.update {
                    it.copy(saveSuccessMessage = "Saved successfully to device!")
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = "Failed to save file: ${error.localizedMessage}")
                }
            }
        }
    }

    fun createShareIntent(context: Context): Intent? {
        val result = _uiState.value.result ?: return null
        return runCatching {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                result.outputFile
            )

            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, result.outputFileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }.getOrNull()
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissSaveSuccess() {
        _uiState.update { it.copy(saveSuccessMessage = null) }
    }
}
