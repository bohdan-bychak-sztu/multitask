package com.bbm.multitask.ui.l3ib

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private object L3ibDefaults {

    val LANGUAGES = listOf("Ukrainian", "English")

    val METHODS = listOf("Simple substitution cipher", "Transposition cipher", "Gamma cipher")

    val ACTIONS = listOf("Encrypt", "Decrypt")
    val FORMULAS = listOf("S = Г + О", "S = Г - О", "S = О - Г")

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun L3ib(
    viewModel: L3ibViewModel = viewModel { L3ibViewModel() }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cipher Tool", style = MaterialTheme.typography.titleLarge) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsSection(state, viewModel)

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            InputSection(state, viewModel)

            ResultSection(state.result)
        }
    }
}

@Composable
private fun SettingsSection(state: L3ibMethodState, viewModel: L3ibViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(title = "Configuration")

        SelectorItem("Language") {
            SegmentedControlSelector(
                options = L3ibDefaults.LANGUAGES,
                selectedIndex = L3ibDefaults.LANGUAGES.indexOf(state.language)
            ) { viewModel.onEvent(L3ibMethodEvent.UpdateLanguage(L3ibDefaults.LANGUAGES[it])) }
        }

        SelectorItem("Method") {
            SegmentedControlSelector(
                options = L3ibDefaults.METHODS,
                selectedIndex = L3ibDefaults.METHODS.indexOf(state.method)
            ) { viewModel.onEvent(L3ibMethodEvent.UpdateMethod(L3ibDefaults.METHODS[it])) }
        }

        if (state.method == "Gamma cipher") {
            SelectorItem("Formula") {
                SegmentedControlSelector(
                    options = L3ibDefaults.FORMULAS,
                    selectedIndex = L3ibDefaults.FORMULAS.indexOf(state.formula)
                ) {
                    viewModel.onEvent(L3ibMethodEvent.UpdateFormula(L3ibDefaults.FORMULAS[it]))
                }
            }
        }

        SelectorItem("Action") {
            SegmentedControlSelector(
                options = L3ibDefaults.ACTIONS,
                selectedIndex = L3ibDefaults.ACTIONS.indexOf(state.action)
            ) { viewModel.onEvent(L3ibMethodEvent.UpdateAction(L3ibDefaults.ACTIONS[it])) }
        }
    }
}

@Composable
private fun InputSection(state: L3ibMethodState, viewModel: L3ibViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(title = "Data Input")

        OutlinedTextField(
            value = state.keyWord,
            onValueChange = { viewModel.onEvent(L3ibMethodEvent.UpdateKeyWord(it)) },
            label = { Text("Keyword") },
            placeholder = { Text("Enter secret key...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
        )

        OutlinedTextField(
            value = state.text,
            onValueChange = { viewModel.onEvent(L3ibMethodEvent.UpdateText(it)) },
            label = { Text("Source Text") },
            placeholder = { Text("Type your message here...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            leadingIcon = { Icon(Icons.Default.Create, contentDescription = null) }
        )
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
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
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
private fun ResultSection(result: String) {
    if (result.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title = "Result")

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