package com.pocketpdf

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.pocketpdf.model.HistoryItem
import com.pocketpdf.ui.screens.MainAppScreen
import com.pocketpdf.ui.theme.PocketPDFTheme
import com.pocketpdf.viewmodel.CompressorViewModel
import com.pocketpdf.viewmodel.ImageToPdfViewModel
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private val compressorViewModel: CompressorViewModel by viewModels()
    private val imageToPdfViewModel: ImageToPdfViewModel by viewModels()

    // Activity Result Launchers for PDF Compressor
    private val pickPdfLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { compressorViewModel.onPdfSelected(it, this) }
    }

    private val saveCompressedPdfLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { compressorViewModel.saveCompressedFile(it, this) }
    }

    // Activity Result Launchers for Image to PDF
    private val pickImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            imageToPdfViewModel.addImages(uris, this)
        }
    }

    private val saveImagesPdfLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { imageToPdfViewModel.saveResultFile(it, this) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PocketPDFTheme {
                val compressorState by compressorViewModel.uiState.collectAsState()
                val imageToPdfState by imageToPdfViewModel.uiState.collectAsState()
                val historyItems by compressorViewModel.historyRepository.historyItems.collectAsState()

                MainAppScreen(
                    compressorState = compressorState,
                    imageToPdfState = imageToPdfState,
                    historyItems = historyItems,
                    // Compressor handlers
                    onPickPdfClicked = { pickPdfLauncher.launch("application/pdf") },
                    onQualitySelected = compressorViewModel::onQualitySelected,
                    onCompressClicked = { compressorViewModel.startCompression(this) },
                    onCancelCompressClicked = compressorViewModel::cancelCompression,
                    onOpenCompressedClicked = {
                        compressorState.result?.outputFile?.let(::openDirectFile)
                    },
                    onSaveCompressedClicked = {
                        val fileName = compressorState.result?.outputFileName ?: "compressed.pdf"
                        saveCompressedPdfLauncher.launch(fileName)
                    },
                    onShareCompressedClicked = {
                        compressorViewModel.createShareIntent(this)?.let { startActivity(it) }
                    },
                    onDismissCompressorError = compressorViewModel::dismissError,
                    onDismissCompressorSaveSuccess = compressorViewModel::dismissSaveSuccess,
                    // Images to PDF handlers
                    onPickImagesClicked = { pickImagesLauncher.launch("image/*") },
                    onRemoveImage = imageToPdfViewModel::removeImage,
                    onMoveImage = imageToPdfViewModel::moveImage,
                    onClearAllImages = imageToPdfViewModel::clearAll,
                    onFitModeSelected = imageToPdfViewModel::setFitMode,
                    onImageQualitySelected = imageToPdfViewModel::setQuality,
                    onCompressInOneGoChanged = imageToPdfViewModel::setCompressInOneGo,
                    onImageCompressionQualitySelected = imageToPdfViewModel::setCompressionQuality,
                    onConvertImagesClicked = { imageToPdfViewModel.convertImagesToPdf(this) },
                    onCancelConvertImagesClicked = imageToPdfViewModel::cancelConversion,
                    onOpenImagesPdfClicked = {
                        imageToPdfState.result?.outputFile?.let(::openDirectFile)
                    },
                    onSaveImagesPdfClicked = {
                        val fileName = imageToPdfState.result?.outputFileName ?: "images_document.pdf"
                        saveImagesPdfLauncher.launch(fileName)
                    },
                    onShareImagesPdfClicked = {
                        imageToPdfViewModel.createShareIntent(this)?.let { startActivity(it) }
                    },
                    onDismissImageToPdfError = imageToPdfViewModel::dismissError,
                    onDismissImageToPdfSaveSuccess = imageToPdfViewModel::dismissSaveSuccess,
                    // History handlers
                    onOpenHistoryItem = { item -> openDirectFile(item.file) },
                    onShareHistoryItem = ::sharePdfFile,
                    onDeleteHistoryItem = { id ->
                        lifecycleScope.launch { compressorViewModel.historyRepository.deleteItem(id) }
                    },
                    onClearHistory = {
                        lifecycleScope.launch { compressorViewModel.historyRepository.clearAll() }
                    }
                )
            }
        }
    }

    private fun openDirectFile(file: File) {
        if (!file.exists()) {
            Toast.makeText(this, "File is no longer available in cache", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open PDF"))
        }.onFailure {
            Toast.makeText(this, "No app found to open PDF files", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePdfFile(item: HistoryItem) {
        if (!item.fileExists) {
            Toast.makeText(this, "File is no longer available in cache", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", item.file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, item.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share PDF"))
        }.onFailure { error ->
            Toast.makeText(this, "Failed to share: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
