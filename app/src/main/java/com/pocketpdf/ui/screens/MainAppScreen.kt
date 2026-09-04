package com.pocketpdf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpdf.model.CompressionQuality
import com.pocketpdf.model.HistoryItem
import com.pocketpdf.model.ImageQualityPreset
import com.pocketpdf.model.PageFitMode
import com.pocketpdf.viewmodel.CompressorUiState
import com.pocketpdf.viewmodel.ImageToPdfUiState
import kotlinx.coroutines.launch

enum class AppScreen(val title: String, val subtitle: String, val icon: ImageVector) {
    COMPRESS("Compress PDF", "Reduce PDF file size", Icons.Default.Compress),
    IMAGES_TO_PDF("Images to PDF", "Merge photos into single PDF", Icons.Default.Collections),
    CREATE_AND_COMPRESS("Scan & Compress", "Images to compressed PDF in one go", Icons.Default.Bolt),
    HISTORY("History", "Recent documents & viewer", Icons.Default.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    compressorState: CompressorUiState,
    imageToPdfState: ImageToPdfUiState,
    historyItems: List<HistoryItem>,
    // Compressor actions
    onPickPdfClicked: () -> Unit,
    onQualitySelected: (CompressionQuality) -> Unit,
    onCompressClicked: () -> Unit,
    onCancelCompressClicked: () -> Unit,
    onOpenCompressedClicked: () -> Unit,
    onSaveCompressedClicked: () -> Unit,
    onShareCompressedClicked: () -> Unit,
    onDismissCompressorError: () -> Unit,
    onDismissCompressorSaveSuccess: () -> Unit,
    // Images to PDF actions
    onPickImagesClicked: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onMoveImage: (fromIndex: Int, toIndex: Int) -> Unit,
    onClearAllImages: () -> Unit,
    onFitModeSelected: (PageFitMode) -> Unit,
    onImageQualitySelected: (ImageQualityPreset) -> Unit,
    onCompressInOneGoChanged: (Boolean) -> Unit,
    onImageCompressionQualitySelected: (CompressionQuality) -> Unit,
    onConvertImagesClicked: () -> Unit,
    onCancelConvertImagesClicked: () -> Unit,
    onOpenImagesPdfClicked: () -> Unit,
    onSaveImagesPdfClicked: () -> Unit,
    onShareImagesPdfClicked: () -> Unit,
    onDismissImageToPdfError: () -> Unit,
    onDismissImageToPdfSaveSuccess: () -> Unit,
    // History actions
    onOpenHistoryItem: (HistoryItem) -> Unit,
    onShareHistoryItem: (HistoryItem) -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.COMPRESS) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenuOverflow by remember { mutableStateOf(false) }

    // Compressor messages
    LaunchedEffect(compressorState.errorMessage) {
        compressorState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissCompressorError()
        }
    }
    LaunchedEffect(compressorState.saveSuccessMessage) {
        compressorState.saveSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissCompressorSaveSuccess()
        }
    }

    // ImageToPdf messages
    LaunchedEffect(imageToPdfState.errorMessage) {
        imageToPdfState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissImageToPdfError()
        }
    }
    LaunchedEffect(imageToPdfState.saveSuccessMessage) {
        imageToPdfState.saveSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissImageToPdfSaveSuccess()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Pocket PDF",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your Mobile PDF Manager",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Items
                AppScreen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Column {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = screen.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        badge = {
                            if (screen == AppScreen.HISTORY && historyItems.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "${historyItems.size}",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        selected = isSelected,
                        onClick = {
                            currentScreen = screen
                            if (screen == AppScreen.CREATE_AND_COMPRESS) {
                                onCompressInOneGoChanged(true)
                            } else if (screen == AppScreen.IMAGES_TO_PDF) {
                                onCompressInOneGoChanged(false)
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = currentScreen.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Menu",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showMenuOverflow = !showMenuOverflow }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenuOverflow,
                            onDismissRequest = { showMenuOverflow = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Compress PDF") },
                                onClick = {
                                    currentScreen = AppScreen.COMPRESS
                                    showMenuOverflow = false
                                },
                                leadingIcon = { Icon(Icons.Default.Compress, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Images to PDF") },
                                onClick = {
                                    currentScreen = AppScreen.IMAGES_TO_PDF
                                    onCompressInOneGoChanged(false)
                                    showMenuOverflow = false
                                },
                                leadingIcon = { Icon(Icons.Default.Collections, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Scan & Compress") },
                                onClick = {
                                    currentScreen = AppScreen.CREATE_AND_COMPRESS
                                    onCompressInOneGoChanged(true)
                                    showMenuOverflow = false
                                },
                                leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("View History") },
                                onClick = {
                                    currentScreen = AppScreen.HISTORY
                                    showMenuOverflow = false
                                },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Show ONLY the active task on the front page
                when (currentScreen) {
                    AppScreen.COMPRESS -> CompressorContent(
                        selectedPdf = compressorState.selectedPdf,
                        selectedQuality = compressorState.selectedQuality,
                        isInspecting = compressorState.isInspecting,
                        isCompressing = compressorState.isCompressing,
                        progress = compressorState.compressionProgress,
                        currentPage = compressorState.currentPage,
                        totalPages = compressorState.totalPages,
                        result = compressorState.result,
                        onPickPdfClicked = onPickPdfClicked,
                        onQualitySelected = onQualitySelected,
                        onCompressClicked = onCompressClicked,
                        onCancelClicked = onCancelCompressClicked,
                        onOpenClicked = onOpenCompressedClicked,
                        onSaveClicked = onSaveCompressedClicked,
                        onShareClicked = onShareCompressedClicked,
                        modifier = Modifier.fillMaxSize()
                    )
                    AppScreen.IMAGES_TO_PDF, AppScreen.CREATE_AND_COMPRESS -> ImageToPdfScreen(
                        images = imageToPdfState.images,
                        fitMode = imageToPdfState.fitMode,
                        quality = imageToPdfState.quality,
                        compressInOneGo = imageToPdfState.compressInOneGo,
                        compressionQuality = imageToPdfState.compressionQuality,
                        isResolving = imageToPdfState.isResolving,
                        isProcessing = imageToPdfState.isProcessing,
                        progress = imageToPdfState.progress,
                        currentImageIndex = imageToPdfState.currentImageIndex,
                        totalImages = imageToPdfState.totalImages,
                        result = imageToPdfState.result,
                        onPickImagesClicked = onPickImagesClicked,
                        onRemoveImage = onRemoveImage,
                        onMoveImage = onMoveImage,
                        onClearAll = onClearAllImages,
                        onFitModeSelected = onFitModeSelected,
                        onQualitySelected = onImageQualitySelected,
                        onCompressInOneGoChanged = onCompressInOneGoChanged,
                        onCompressionQualitySelected = onImageCompressionQualitySelected,
                        onConvertClicked = onConvertImagesClicked,
                        onCancelClicked = onCancelConvertImagesClicked,
                        onOpenClicked = onOpenImagesPdfClicked,
                        onSaveClicked = onSaveImagesPdfClicked,
                        onShareClicked = onShareImagesPdfClicked,
                        modifier = Modifier.fillMaxSize()
                    )
                    AppScreen.HISTORY -> HistoryScreen(
                        items = historyItems,
                        onOpenItem = onOpenHistoryItem,
                        onShareItem = onShareHistoryItem,
                        onDeleteItem = onDeleteHistoryItem,
                        onClearAll = onClearHistory,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
