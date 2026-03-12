package com.bbm.multitask.ui.pvdMethod

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbm.multitask.ui.components.*
import com.bbm.multitask.utils.platformImage.ColorChannel
import com.bbm.multitask.utils.platformImage.PlatformImage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import kotlinx.coroutines.launch


private object PvdMethodDefaults {
    val ACTIONS = listOf("Encrypt", "Decrypt")
    val COLORS_TO_USE = ColorChannel.entries.map { it.name }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PvdMethod(
    viewModel: PvdMethodViewModel = PvdMethodViewModel()
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

            PvdCalculatorSection(state, viewModel)

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            if (state.action == "Encrypt")
                InputSection(state, viewModel)

            Row {
                Column {
                    SelectImageArea(
                        image = state.image,
                        onFileSelected = { platformFile ->
                            viewModel.onEvent(PvdMethodEvent.UpdateImage(platformFile))
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
                            Button(
                                onClick = { viewModel.onEvent(PvdMethodEvent.ProcessImage) },
                                enabled = state.image != null && (state.action == "Decrypt" || state.text.isNotEmpty())
                            ) {
                                Text("Process Image")
                            }

                            if (state.outputImage != null && state.action == "Encrypt")
                                Button(
                                    onClick = {
                                        scope.launch {
                                            viewModel.onEvent(PvdMethodEvent.UpdateOutputPath(saveFile(state.outputImage)))
                                        }
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Download Result")
                                }
                        }
                        if (state.outputImage != null && state.action == "Encrypt")
                            Box(
                                Modifier
                                    .size(200.dp)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                                    .clickable(
                                        enabled = !state.outputPath.isNullOrEmpty(),
                                        onClick = {
                                            openFileWithDefaultApplication(state.outputPath!!)
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
        }
    }
}

@Composable
private fun PvdCalculatorSection(state: PvdMethodState, viewModel: PvdMethodViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(title = "PVD Calculator", isCollapse = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.p1,
                    onValueChange = {
                        if (it.length <= 3 && it.all { c -> c.isDigit() }) {
                            val p1Value = it.ifEmpty { "0" }.toIntOrNull() ?: 0
                            if (p1Value in 0..255) {
                                viewModel.onEvent(PvdMethodEvent.CalculatePvd(it, state.p2, state.charToEmbed))
                            }
                        }
                    },
                    label = { Text("Pixel 1") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.p2,
                    onValueChange = {
                        if (it.length <= 3 && it.all { c -> c.isDigit() }) {
                            val p2Value = it.ifEmpty { "0" }.toIntOrNull() ?: 0
                            if (p2Value in 0..255) {
                                viewModel.onEvent(PvdMethodEvent.CalculatePvd(state.p1, it, state.charToEmbed))
                            }
                        }
                    },
                    label = { Text("Pixel 2") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.charToEmbed,
                    onValueChange = { if (it.length <= 1) viewModel.onEvent(PvdMethodEvent.CalculatePvd(state.p1, state.p2, it)) },
                    label = { Text("Char") },
                    modifier = Modifier.weight(0.8f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.onEvent(PvdMethodEvent.CalculatePvd(state.p1, state.p2, state.charToEmbed)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Embed")
                }
                Button(
                    onClick = { viewModel.onEvent(PvdMethodEvent.ExtractPvd(state.p1, state.p2)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Extract")
                }
            }
            if (state.pvdCalcResult.isNotEmpty()) {
                val isError = state.pvdCalcResult.startsWith("Error") || state.pvdCalcResult.startsWith("Cannot")
                val textColor = if (isError) MaterialTheme.colorScheme.error else LocalContentColor.current

                val annotatedString = buildAnnotatedString {
                    val binaryLineMatch = "Binary: `(.*?)`(.*)".toRegex().find(state.pvdCalcResult)
                    if (binaryLineMatch != null) {
                        val fullText = binaryLineMatch.value
                        val prefix = state.pvdCalcResult.substringBefore(fullText)
                        append(prefix)

                        val (hiddenPart, remainingPart) = binaryLineMatch.destructured
                        append("Binary: ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                            append(hiddenPart)
                        }
                        append(remainingPart)
                    } else {
                        append(state.pvdCalcResult)
                    }
                }

                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SelectImageArea(
    image: PlatformFile?,
    onFileSelected: (PlatformFile) -> Unit,
) {
    val launcher = rememberFilePickerLauncher(
        mode = FileKitMode.Single,
        type = FileKitType.File(extension = "png")
    ) { file ->
        try {
            onFileSelected(file!!)
        } catch (e: Exception) {
            null
        }
    }

    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, image) {
        value = if (image != null) {
            try {
                image.toImageBitmap()
            } catch (e: Exception) {
                TODO("Handle image loading error: ${e.message}")
            }
        } else {
            null
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap!!,
            contentDescription = "Selected Image",
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .border(BorderStroke(3.dp, Color.Black))
                .clickable(
                    onClick = {
                        launcher.launch()
                    }
                )
        )

    } else {
        Box(
            Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .border(BorderStroke(3.dp, Color.Black))
        ) {
            Button(
                onClick = {
                    launcher.launch()
                },
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text("Select Image")

            }
        }
    }
}

@Composable
private fun SettingsSection(state: PvdMethodState, viewModel: PvdMethodViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(title = "Configuration") {
            SelectorItem("Action") {
                SegmentedControlSelector(
                    options = PvdMethodDefaults.ACTIONS,
                    selectedIndex = PvdMethodDefaults.ACTIONS.indexOf(state.action)
                ) { viewModel.onEvent(PvdMethodEvent.UpdateAction(PvdMethodDefaults.ACTIONS[it])) }
            }
            SelectorItem(label = "Color Channel to Use") {
                MultiSegmentedControlSelector(
                    options = PvdMethodDefaults.COLORS_TO_USE,
                    selectedIndices = state.selectedColorChannels.toSet(),
                    onOptionToggled = { index ->
                        val newSet = state.selectedColorChannels.toMutableSet()
                        if (newSet.contains(index)) {
                            newSet.remove(index)
                        } else {
                            newSet.add(index)
                        }
                        viewModel.onEvent(PvdMethodEvent.UpdateSelectedColorChannels(newSet))
                    }
                )
            }

        }
    }
}

@Composable
private fun InputSection(state: PvdMethodState, viewModel: PvdMethodViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(title = "Data Input") {
            OutlinedTextField(
                value = state.text,
                onValueChange = { viewModel.onEvent(PvdMethodEvent.UpdateText(it)) },
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

expect suspend fun saveFile(file: PlatformImage?): String?
expect fun openFileWithDefaultApplication(path: String)