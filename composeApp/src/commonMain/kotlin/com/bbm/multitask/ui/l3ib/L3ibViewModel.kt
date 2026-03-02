package com.bbm.multitask.ui.l3ib

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.bbm.multitask.data.model.lPGraphicalMethod.Constraint
import com.bbm.multitask.data.model.lPGraphicalMethod.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


data class L3ibMethodState(
    val constraints: List<Constraint> = emptyList(),
    val objectiveFunction: Pair<Double, Double> = 0.0 to 0.0,
    val isMax: Boolean = true,
    val optimalPoint: Point? = null,
    val optimalValue: Double? = null,
    val graphScale: Float = 50f,
    val graphOffset: Offset = Offset.Zero,
)

sealed interface L3ibMethodEvent {

}

class L3ibViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(L3ibMethodState())
    val uiState: StateFlow<L3ibMethodState> = _uiState.asStateFlow()

    fun onEvent(event: L3ibMethodEvent) {
        /*when (event) {
            is L3ibMethodEvent.UpdateObjectiveFunction -> {
                _uiState.update { it.copy(objectiveFunction = Pair(event.a, event.b)) }
                _uiState.update { it.copy(isMax = event.isMax) }
            }

*/
    }
}
