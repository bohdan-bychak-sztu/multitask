package com.bbm.multitask.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel = MainScreenViewModel()
) {
    Column {
        Text(text = "Main Screen")
    }
}