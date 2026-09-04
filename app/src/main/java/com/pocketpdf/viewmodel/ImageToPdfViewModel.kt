package com.pocketpdf.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpdf.data.HistoryRepository
import com.pocketpdf.engine.ImageToPdfEngine
import com.pocketpdf.model.HistoryItem
import com.pocketpdf.model.HistoryType
import com.pocketpdf.model.ImageItem
import com.pocketpdf.model.ImageQualityPreset
import com.pocketpdf.model.ImageToPdfResult
import com.pocketpdf.model.PageFitMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileInputStream

data class ImageToPdfUiState(
    val images: List<ImageItem> = emptyList(),
    val fitMode: PageFitMode = PageFitMode.FIT_A4,
    val quality: ImageQualityPreset = ImageQualityPreset.BALANCED,
    val isResolving: Boolean = false,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val currentImageIndex: Int = 0,
    val totalImages: Int = 0,
    val result: ImageToPdfResult? = null,
    val errorMessage: String? = null,
    val saveSuccessMessage: String? = null
)

class ImageToPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val historyRepository = HistoryRepository(application)

    private val _uiState = MutableStateFlow(ImageToPdfUiState())
    val uiState: StateFlow<ImageToPdfUiState> = _uiState.asStateFlow()

    private var conversionJob: Job? = null

    fun addImages(uris: List<Uri>, context: Context) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isResolving = true, errorMessage = null) }
            val resolved = ImageToPdfEngine.resolveImageItems(context.applicationContext, uris)
            _uiState.update { current ->
                current.copy(
                    images = current.images + resolved,
                    isResolving = false,
                    result = null
                )
            }
        }
    }

    fun removeImage(id: String) {
        _uiState.update { current ->
            current.copy(
                images = current.images.filterNot { it.id == id },
                result = null
            )
        }
    }

    fun moveImage(fromIndex: Int, toIndex: Int) {
        _uiState.update { current ->
            if (fromIndex !in current.images.indices || toIndex !in current.images.indices) return@update current
            val updated = current.images.toMutableList()
            val item = updated.removeAt(fromIndex)
            updated.add(toIndex, item)
            current.copy(images = updated, result = null)
        }
    }

    fun clearAll() {
        _uiState.update {
            it.copy(
                images = emptyList(),
                result = null,
                errorMessage = null
            )
        }
    }

    fun setFitMode(fitMode: PageFitMode) {
        _uiState.update { it.copy(fitMode = fitMode, result = null) }
    }

    fun setQuality(quality: ImageQualityPreset) {
        _uiState.update { it.copy(quality = quality, result = null) }
    }

    fun convertImagesToPdf(context: Context) {
        val currentImages = _uiState.value.images
        if (currentImages.isEmpty()) return

        val fitMode = _uiState.value.fitMode
        val quality = _uiState.value.quality

        conversionJob?.cancel()
        conversionJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progress = 0f,
                    currentImageIndex = 0,
                    totalImages = currentImages.size,
                    errorMessage = null,
                    result = null
                )
            }

            val result = ImageToPdfEngine.convertImagesToPdf(
                context = context.applicationContext,
                images = currentImages,
                fitMode = fitMode,
                quality = quality,
                onProgress = { current, total ->
                    _uiState.update {
                        it.copy(
                            currentImageIndex = current,
                            totalImages = total,
                            progress = current.toFloat() / total.toFloat()
                        )
                    }
                }
            )

            result.onSuccess { res ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        result = res,
                        errorMessage = null
                    )
                }

                // Save to history automatically
                historyRepository.addItem(
                    HistoryItem(
                        title = res.outputFileName,
                        filePath = res.outputFile.absolutePath,
                        type = HistoryType.IMAGES_TO_PDF,
                        originalSizeBytes = 0L,
                        resultSizeBytes = res.fileSizeBytes,
                        pageCount = res.pageCount
                    )
                )
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Creation failed: ${err.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun cancelConversion() {
        conversionJob?.cancel()
        conversionJob = null
        _uiState.update {
            it.copy(
                isProcessing = false,
                progress = 0f,
                errorMessage = "Cancelled"
            )
        }
    }

    fun saveResultFile(targetUri: Uri, context: Context) {
        val res = _uiState.value.result ?: return
        viewModelScope.launch {
            runCatching {
                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    FileInputStream(res.outputFile).use { input ->
                        input.copyTo(out)
                    }
                } ?: throw IllegalStateException("Cannot open destination")
            }.onSuccess {
                _uiState.update { it.copy(saveSuccessMessage = "Saved PDF to device!") }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Failed to save: ${error.localizedMessage}") }
            }
        }
    }

    fun createShareIntent(context: Context): Intent? {
        val res = _uiState.value.result ?: return null
        return runCatching {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                res.outputFile
            )

            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, res.outputFileName)
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
