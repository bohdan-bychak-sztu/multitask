package com.bbm.multitask.ui.lsbMethod

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.decodeToImageBitmap
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun LsbMethod(
    viewModel: LsbMethodViewModel = LsbMethodViewModel()
) {
    var showTargetBorder by remember { mutableStateOf(false) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var targetText by remember { mutableStateOf("Drop Here") }
    val coroutineScope = rememberCoroutineScope()
    val dragAndDropTarget = remember {
        object : DragAndDropTarget {

            // Highlights the border of a potential drop target
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
                        imageBitmap = file.inputStream().readAllBytes().decodeToImageBitmap()
                        dropSucceeded = true
                    } else {
                        targetText = "File path not found"
                    }
                } else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    targetText = transferable.getTransferData(DataFlavor.stringFlavor) as String
                    dropSucceeded = true
                } else {
                    targetText = transferable.transferDataFlavors.firstOrNull()?.humanPresentableName ?: "Unknown data"
                }

                coroutineScope.launch {
                    delay(2000)
                    targetText = "Drop here"
                }
                return dropSucceeded
            }
        }
    }

    Row {
        Column {
            Box(
                Modifier
                    .size(200.dp)
                    .background(Color.LightGray)
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
                val currentImage = imageBitmap
                if (currentImage != null) {
                    Image(
                        painter = BitmapPainter(currentImage),
                        contentDescription = "Dropped Image",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    Text(targetText, Modifier.align(Alignment.Center))
                }
            }
        }

        Column {
            Text(text = "LSB Method realized!")
        }
    }
}
