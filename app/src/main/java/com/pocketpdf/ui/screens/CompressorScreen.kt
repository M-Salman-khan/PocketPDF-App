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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketpdf.model.CompressionQuality
import com.pocketpdf.model.CompressionResult
import com.pocketpdf.model.SelectedPdf
import com.pocketpdf.ui.components.FilePickerCard
import com.pocketpdf.ui.components.ProgressCard
import com.pocketpdf.ui.components.QualitySelectorCard
import com.pocketpdf.ui.components.ResultCard

@Composable
fun CompressorContent(
    selectedPdf: SelectedPdf?,
    selectedQuality: CompressionQuality,
    isInspecting: Boolean,
    isCompressing: Boolean,
    progress: Float,
    currentPage: Int,
    totalPages: Int,
    result: CompressionResult?,
    onPickPdfClicked: () -> Unit,
    onQualitySelected: (CompressionQuality) -> Unit,
    onCompressClicked: () -> Unit,
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

        // File selection
        FilePickerCard(
            selectedPdf = selectedPdf,
            isCompressing = isCompressing,
            onPickPdfClicked = onPickPdfClicked
        )

        // Loading indicator when inspecting PDF
        if (isInspecting) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Quality options
        if (selectedPdf != null) {
            QualitySelectorCard(
                selectedQuality = selectedQuality,
                enabled = !isCompressing,
                onQualitySelected = onQualitySelected
            )

            // Main Compress CTA Button
            if (!isCompressing) {
                Button(
                    onClick = onCompressClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (result != null) "Re-compress (${selectedQuality.title})" else "Compress PDF",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        // Compression progress
        if (isCompressing) {
            ProgressCard(
                progress = progress,
                currentPage = currentPage,
                totalPages = totalPages,
                onCancelClicked = onCancelClicked
            )
        }

        // Compression result
        if (result != null) {
            ResultCard(
                result = result,
                onOpenClicked = onOpenClicked,
                onSaveClicked = onSaveClicked,
                onShareClicked = onShareClicked
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
