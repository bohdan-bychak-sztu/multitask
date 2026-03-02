package com.bbm.multitask.ui.lPGraphicalMethod

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bbm.multitask.data.model.lPGraphicalMethod.Constraint

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ObjectiveFunctionInput(
    objectiveFunction: Pair<Double, Double>,
    isMax: Boolean,
    onObjectiveFunctionChange: (Double, Double, Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(text = "F(x₁, x₂) = ")
        BasicTextField(
            modifier = Modifier
                .width(IntrinsicSize.Min),
            value = objectiveFunction.first.toString(),
            onValueChange = {
                val newValue = it.toDoubleOrNull()
                if (newValue != null)
                    onObjectiveFunctionChange(newValue, objectiveFunction.second, isMax)
            }
        )
        Text(text = "x₁ + ")
        BasicTextField(
            modifier = Modifier
                .width(IntrinsicSize.Min),
            value = objectiveFunction.second.toString(),
            onValueChange = {
                val newValue = it.toDoubleOrNull()
                if (newValue != null)
                    onObjectiveFunctionChange(objectiveFunction.first, newValue, isMax)
            }
        )
        Text(text = "x₂ → ")
        Text(
            text = if (isMax) "max" else "min",
            modifier = Modifier
                .onClick(onClick = {
                    onObjectiveFunctionChange(
                        objectiveFunction.first,
                        objectiveFunction.second,
                        !isMax
                    )
                })
        )


    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConstraintInput(
    constraint: Constraint,
    onConstraintChange: (Constraint) -> Unit,
    onDelete: () -> Unit
) {
    ConstraintInputContextMenu (onDelete = onDelete) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            BasicTextField(
                modifier = Modifier
                    .width(IntrinsicSize.Min),
                value = constraint.a.toString(),
                onValueChange = {
                    val newValue = it.toDoubleOrNull()
                    if (newValue != null)
                        onConstraintChange(constraint.copy(a = newValue))
                }
            )
            Text(text = "x₁ + ")
            BasicTextField(
                modifier = Modifier
                    .width(IntrinsicSize.Min),
                value = constraint.b.toString(),
                onValueChange = {
                    val newValue = it.toDoubleOrNull()
                    if (newValue != null)
                        onConstraintChange(constraint.copy(b = newValue))
                }
            )
            Text(text = "x₂")
            Text(
                text = " ${constraint.operator} ",
                modifier = Modifier
                    .onClick(onClick = {
                        onConstraintChange(
                            constraint.copy(
                                operator = when (constraint.operator) {
                                    "<=" -> ">="
                                    ">=" -> "="
                                    else -> "<="
                                }
                            )
                        )
                    })
            )

            BasicTextField(
                modifier = Modifier
                    .width(IntrinsicSize.Min),
                value = constraint.c.toString(),
                onValueChange = {
                    val newValue = it.toDoubleOrNull()
                    if (newValue != null)
                        onConstraintChange(constraint.copy(c = newValue))
                }
            )
        }
    }
}

@Composable
expect fun ConstraintInputContextMenu(
    onDelete: () -> Unit,
    content: @Composable (() -> Unit),
)