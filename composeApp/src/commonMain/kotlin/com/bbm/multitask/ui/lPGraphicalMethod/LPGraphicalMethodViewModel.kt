package com.bbm.multitask.ui.lPGraphicalMethod

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.lifecycle.ViewModel
import com.bbm.multitask.data.model.lPGraphicalMethod.Constraint
import com.bbm.multitask.data.model.lPGraphicalMethod.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


data class LPGraphicalMethodState(
    val constraints: List<Constraint> = emptyList(),
    val objectiveFunction: Pair<Double, Double> = 0.0 to 0.0,
    val isMax: Boolean = true,
    val optimalPoint: Point? = null,
    val optimalValue: Double? = null,
    val graphScale: Float = 50f,
    val graphOffset: Offset = Offset.Zero,
)

sealed interface LPGraphicalMethodEvent {
    data class UpdateObjectiveFunction(val a: Double, val b: Double, val isMax: Boolean = true) : LPGraphicalMethodEvent
    data class UpdateConstraint(val index: Int, val constraint: Constraint) : LPGraphicalMethodEvent
    data class GraphTransform(val pan: Offset, val zoom: Float) : LPGraphicalMethodEvent
    data class RemoveConstraint(val index: Int) : LPGraphicalMethodEvent
    data class PasteConstraints(val constrains: List<Constraint>) : LPGraphicalMethodEvent
    object AddConstraint : LPGraphicalMethodEvent
    object Solve : LPGraphicalMethodEvent
    object RemoveAllConstraints : LPGraphicalMethodEvent
    object CopyConstraints : LPGraphicalMethodEvent
}

class LPGraphicalMethodViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LPGraphicalMethodState())
    val uiState: StateFlow<LPGraphicalMethodState> = _uiState.asStateFlow()

    fun onEvent(event: LPGraphicalMethodEvent) {
        when (event) {
            is LPGraphicalMethodEvent.UpdateObjectiveFunction -> {
                _uiState.update { it.copy(objectiveFunction = Pair(event.a, event.b)) }
                _uiState.update { it.copy(isMax = event.isMax) }
            }

            is LPGraphicalMethodEvent.UpdateConstraint -> {
                val updatedConstraints = _uiState.value.constraints.toMutableList()
                if (event.index < updatedConstraints.size) {
                    updatedConstraints[event.index] = event.constraint
                    _uiState.value = _uiState.value.copy(constraints = updatedConstraints)
                }
            }

            is LPGraphicalMethodEvent.AddConstraint -> {
                val updatedConstraints = _uiState.value.constraints.toMutableList()
                updatedConstraints.add(Constraint(1.0, 1.0, 10.0))
                _uiState.value = _uiState.value.copy(constraints = updatedConstraints)
            }

            is LPGraphicalMethodEvent.GraphTransform -> {
                val newScale = _uiState.value.graphScale * event.zoom
                val newOffset = _uiState.value.graphOffset + event.pan
                _uiState.update { it.copy(graphScale = newScale, graphOffset = newOffset) }
            }

            is LPGraphicalMethodEvent.RemoveConstraint -> {
                val updatedConstraints = _uiState.value.constraints.toMutableList()
                if (event.index < updatedConstraints.size) {
                    updatedConstraints.removeAt(event.index)
                    _uiState.value = _uiState.value.copy(constraints = updatedConstraints)
                }
            }

            is LPGraphicalMethodEvent.RemoveAllConstraints -> {
                _uiState.value = _uiState.value.copy(constraints = emptyList())
            }

            is LPGraphicalMethodEvent.CopyConstraints -> {

            }

            is LPGraphicalMethodEvent.PasteConstraints -> {
                    _uiState.value = _uiState.value.copy(constraints = event.constrains)
            }

            is LPGraphicalMethodEvent.Solve -> {
            }
        }
    }
}