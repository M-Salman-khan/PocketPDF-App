package com.pocketpdf.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketpdf.model.CompressionQuality
import com.pocketpdf.model.ImageItem
import com.pocketpdf.model.ImageQualityPreset
import com.pocketpdf.model.ImageToPdfResult
import com.pocketpdf.model.PageFitMode
import com.pocketpdf.ui.components.ImageGridCard
import com.pocketpdf.ui.components.ImageToPdfOptionsCard
import com.pocketpdf.ui.components.ImageToPdfResultCard
import com.pocketpdf.ui.components.ProgressCard

@Composable
fun ImageToPdfScreen(
    images: List<ImageItem>,
    fitMode: PageFitMode,
    quality: ImageQualityPreset,
    compressInOneGo: Boolean,
    compressionQuality: CompressionQuality,
    isResolving: Boolean,
    isProcessing: Boolean,
    progress: Float,
    currentImageIndex: Int,
    totalImages: Int,
    result: ImageToPdfResult?,
    onPickImagesClicked: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onMoveImage: (fromIndex: Int, toIndex: Int) -> Unit,
    onClearAll: () -> Unit,
    onFitModeSelected: (PageFitMode) -> Unit,
    onQualitySelected: (ImageQualityPreset) -> Unit,
    onCompressInOneGoChanged: (Boolean) -> Unit,
    onCompressionQualitySelected: (CompressionQuality) -> Unit,
    onConvertClicked: () -> Unit,
    onCancelClicked: () -> Unit,
    onOpenClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    onShareClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Selected images grid
        ImageGridCard(
            images = images,
            isProcessing = isProcessing,
            onPickImagesClicked = onPickImagesClicked,
            onRemoveImage = onRemoveImage,
            onMoveImage = onMoveImage,
            onClearAll = onClearAll
        )

        if (isResolving) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Layout and quality options
        if (images.isNotEmpty()) {
            ImageToPdfOptionsCard(
                fitMode = fitMode,
                quality = quality,
                compressInOneGo = compressInOneGo,
                compressionQuality = compressionQuality,
                enabled = !isProcessing,
                onFitModeSelected = onFitModeSelected,
                onQualitySelected = onQualitySelected,
                onCompressInOneGoChanged = onCompressInOneGoChanged,
                onCompressionQualitySelected = onCompressionQualitySelected
            )

            // Convert button
            if (!isProcessing && result == null) {
                Button(
                    onClick = onConvertClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (compressInOneGo) Icons.Default.Bolt else Icons.Default.PictureAsPdf,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (compressInOneGo) {
                            "⚡ Create & Compress (${images.size} ${if (images.size == 1) "Page" else "Pages"})"
                        } else {
                            "Create PDF (${images.size} ${if (images.size == 1) "Page" else "Pages"})"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Processing progress
        if (isProcessing) {
            ProgressCard(
                progress = progress,
                currentPage = currentImageIndex,
                totalPages = totalImages,
                onCancelClicked = onCancelClicked
            )
        }

        // Completed result
        if (result != null) {
            ImageToPdfResultCard(
                result = result,
                onOpenClicked = onOpenClicked,
                onSaveClicked = onSaveClicked,
                onShareClicked = onShareClicked
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
