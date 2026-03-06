package com.bbm.multitask.ui.lsbMethod

import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.WifiPassword
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbm.multitask.ui.components.fadingEdge
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.png.PngDirectory
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.decodeToImageBitmap
import java.awt.datatransfer.DataFlavor
import java.io.File

private object LsbMethodDefaults {
    val NUMBER_OF_BITS_TO_HIDE = listOf("1", "2", "3", "4", "5", "6", "7", "8")

    val METHODS = listOf("Simple substitution cipher", "Transposition cipher", "Gamma cipher")
    val ACTIONS = listOf("Encrypt", "Decrypt")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LsbMethod(
    viewModel: LsbMethodViewModel = LsbMethodViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
                .fadingEdge(scrollState, 32.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsSection(state, viewModel)

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            if (state.action == "Encrypt")
                InputSection(state, viewModel)

            Row {
                Column {
                    dragAndDrop(
                        pathToImage = state.pathToImage,
                        onFileDrop = { path ->
                            viewModel.onEvent(LsbMethodEvent.UpdatePathToImage(path))
                        }
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Button(onClick = { viewModel.onEvent(LsbMethodEvent.ProcessImage) }, enabled = state.pathToImage.isNotEmpty() && (state.action == "Decrypt" || state.text.isNotEmpty())) {
                                Text("Process Image")
                            }

                            if (state.outputImage != null)
                                Button(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val file =
                                                    FileKit.openFileSaver(suggestedName = "result", extension = "png")
                                                file?.write(state.outputImage!!.getBytes())
                                                viewModel.onEvent(
                                                    LsbMethodEvent.UpdateOutputPath(
                                                        file?.absolutePath() ?: ""
                                                    )
                                                )
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Image saved successfully!",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                println("Помилка збереження: ${e.message}")
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Download Result")
                                }
                        }
                        if (state.outputImage != null)
                            Box(
                                Modifier
                                    .size(200.dp)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                                    .clickable(
                                        enabled = state.outputPath.isNotEmpty(),
                                        onClick = {
                                            FileKit.openFileWithDefaultApplication(PlatformFile(state.outputPath))
                                        }
                                    )
                            ) {
                                Image(
                                    bitmap = state.outputImage!!.toImageBitmap(),
                                    contentDescription = "Output Image",
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                    }

                    if (state.result.isNotEmpty()) {
                        ResultSection(state.result, snackbarHostState)
                    }
                }
            }
            if (state.pathToImage.isNotEmpty()) {
                var imageWidth: Int? = null
                var imageHeight: Int? = null
                try {
                    val metadata = ImageMetadataReader.readMetadata(File(state.pathToImage))
                    val exifDirectory = metadata.getFirstDirectoryOfType(PngDirectory::class.java)
                    imageWidth = exifDirectory?.getInt(PngDirectory.TAG_IMAGE_WIDTH)
                    imageHeight = exifDirectory?.getInt(PngDirectory.TAG_IMAGE_HEIGHT)
                } catch (exception: Exception) {
                    Text("Failed to read metadata: ${exception.message}", color = Color.Red)
                    return@Column
                }

                Column {
                    Text("Metadata:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Path: ${state.pathToImage}")
                    Text("Width: ${imageWidth ?: "N/A"}")
                    Text("Height: ${imageHeight ?: "N/A"}")
                    Text("Number of pixels: ${if (imageWidth != null && imageHeight != null) imageWidth * imageHeight else "N/A"}")
                    Text("Number of bits available for hiding: ${if (imageWidth != null && imageHeight != null) imageWidth * imageHeight * 3 * state.numberOfBitsToHide else "N/A"}")
                }
            }

        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun dragAndDrop(
    pathToImage: String,
    onFileDrop: (String) -> Unit,
) {
    var showTargetBorder by remember { mutableStateOf(false) }
    var targetText by remember { mutableStateOf("Drop Here") }
    val coroutineScope = rememberCoroutineScope()

    val imageBitmap = remember(pathToImage) {
        if (pathToImage.isNotEmpty()) {
            try {
                File(pathToImage).inputStream().readAllBytes().decodeToImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                showTargetBorder = true
            }

            override fun onEnded(event: DragAndDropEvent) {
                showTargetBorder = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val transferable = event.awtTransferable
                var dropSucceeded = false

                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    val files =
                        transferable.getTransferData(DataFlavor.javaFileListFlavor) as java.util.List<java.io.File>
                    val file = files.firstOrNull()
                    if (file != null) {
                        onFileDrop(file.absolutePath)
                        dropSucceeded = true
                    } else {
                        targetText = "File path not found"
                    }
                } else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    targetText = transferable.getTransferData(DataFlavor.stringFlavor) as String
                    dropSucceeded = true
                } else {
                    targetText =
                        transferable.transferDataFlavors.firstOrNull()?.humanPresentableName ?: "Unknown data"
                }

                coroutineScope.launch {
                    delay(2000)
                    targetText = "Drop here"
                }
                return dropSucceeded
            }
        }
    }

    Box(
        Modifier
            .size(200.dp)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .then(
                if (showTargetBorder)
                    Modifier.border(BorderStroke(3.dp, Color.Black))
                else
                    Modifier
            )
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = dragAndDropTarget
            )
    ) {
        if (imageBitmap != null) {
            Image(
                painter = BitmapPainter(imageBitmap),
                contentDescription = "Dropped Image",
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Text(targetText, Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun SettingsSection(state: LsbMethodState, viewModel: LsbMethodViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(title = "Configuration") {

            SelectorItem("Number of bits to hide") {
                SegmentedControlSelector(
                    options = LsbMethodDefaults.NUMBER_OF_BITS_TO_HIDE,
                    selectedIndex = LsbMethodDefaults.NUMBER_OF_BITS_TO_HIDE.indexOf(state.numberOfBitsToHide.toString())
                ) { viewModel.onEvent(LsbMethodEvent.UpdateNumberOfBitsToHide(LsbMethodDefaults.NUMBER_OF_BITS_TO_HIDE[it])) }
            }


            SelectorItem("Action") {
                SegmentedControlSelector(
                    options = LsbMethodDefaults.ACTIONS,
                    selectedIndex = LsbMethodDefaults.ACTIONS.indexOf(state.action)
                ) { viewModel.onEvent(LsbMethodEvent.UpdateAction(LsbMethodDefaults.ACTIONS[it])) }
            }

            OutlinedTextField(
                value = state.secretWord ?: "",
                onValueChange = { viewModel.onEvent(LsbMethodEvent.UpdateSecretWord(it)) },
                label = { Text("Secret Key") },
                placeholder = { Text("Type secret key here...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) }
            )

        }
    }
}


@Composable
private fun SelectorItem(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun SectionTitle(title: String, isCollapse: Boolean = true, content: @Composable () -> Unit) {
    var isExpanded by remember { mutableStateOf(true) }

    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = {
            isExpanded = !isExpanded
        }) {
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Hide")
        }
    }

    if (isExpanded)
        content()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedControlSelector(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                onClick = { onOptionSelected(index) },
                selected = index == selectedIndex
            ) {
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun InputSection(state: LsbMethodState, viewModel: LsbMethodViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(title = "Data Input") {
            OutlinedTextField(
                value = state.text,
                onValueChange = { viewModel.onEvent(LsbMethodEvent.UpdateText(it)) },
                label = { Text("Source Text") },
                placeholder = { Text("Type your message here...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                leadingIcon = { Icon(Icons.Default.Create, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun ResultSection(result: String, snackbarHostState: SnackbarHostState? = null) {
    val scope = rememberCoroutineScope()

    if (!result.isEmpty())
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(title = "Result") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )

                        val clipboard = LocalClipboardManager.current

                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(result))
                            if (snackbarHostState != null)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Result copied to clipboard!",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                        }) {
                            Icon(
                                imageVector = Icons.Default.CopyAll,
                                contentDescription = "Copy"
                            )
                        }
                    }
                }
            }
        }
}
