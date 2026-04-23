package com.bbm.multitask.ui.rsaAlgo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bbm.multitask.ui.components.SectionTitle
import com.bbm.multitask.ui.components.fadingEdge
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RsaAlgo(
    viewModel: RsaAlgoViewModel = RsaAlgoViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
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
            Row {
                Column(
                    modifier = Modifier.padding(start = 16.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Button(
                        onClick = { viewModel.onEvent(RsaAlgoEvent.GenerateKeyPair) },
                    ) {
                        Text("Generate Keys")
                    }

                    Button(
                        onClick = { viewModel.onEvent(RsaAlgoEvent.EncryptMessage) },
                    ) {
                        Text("Encrypt")
                    }

                    Button(
                        onClick = { viewModel.onEvent(RsaAlgoEvent.DecryptMessage) },
                    ) {
                        Text("Decrypt")
                    }
                }

                Column(
                    modifier = Modifier.weight(2f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InputSection(state, viewModel)
                    ResultSection(state.encryptedMessage, snackbarHostState, "Encrypted Message")
                    ResultSection(state.decryptedMessage, snackbarHostState, "Decrypted Message")
                    ResultSection(if (state.keyPair != null) state.keyPair?.publicKey.toString() else "", snackbarHostState, "Public Key")
                    ResultSection(if (state.keyPair != null) state.keyPair?.privateKey.toString() else "", snackbarHostState, "Private Key")
                }
            }
        }
    }
}

@Composable
private fun InputSection(state: RsaAlgoState, viewModel: RsaAlgoViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(title = "Data Input") {
            OutlinedTextField(
                value = state.message,
                onValueChange = { viewModel.onEvent(RsaAlgoEvent.UpdateMessage(it)) },
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
private fun ResultSection(result: String, snackbarHostState: SnackbarHostState? = null, title : String = "Result") {
    val scope = rememberCoroutineScope()

    if (!result.isEmpty())
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(title = title) {
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