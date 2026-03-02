package com.bbm.multitask.ui.lPGraphicalMethod

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bbm.multitask.data.model.lPGraphicalMethod.*
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private object LPGraphDefaults {
    const val Epsilon = 1e-9
    const val PointRadius = 8f
    const val LineStrokeWidth = 3f
    const val FeasibleRegionStrokeWidth = 4f
    const val OptimalLineStrokeWidth = 2f
    val OptimalLinePathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

    data class GraphColors(
        val background: Color = Color.White,
        val grid: Color = Color.LightGray.copy(alpha = 0.5f),
        val axis: Color = Color.Black,
        val label: Color = Color.DarkGray,
        val constraintLine: Color = Color.Red,
        val constraintHalfPlane: Color = Color.Red.copy(alpha = 0.1f),
        val feasibleRegionFill: Color = Color(0xFF4CAF50).copy(alpha = 0.4f),
        val feasibleRegionStroke: Color = Color(0xFF2E7D32),
        val optimalPoint: Color = Color(0xFFE91E63),
        val optimalLine: Color = Color.Blue.copy(alpha = 0.6f),
        val textBackground: Color = Color.White.copy(alpha = 0.7f),
        val infoText: Color = Color.Black
    )

    val graphColors = GraphColors()
}

@Composable
fun LPGraphicalMethod(
    viewModel: LPGraphicalMethodViewModel = viewModel { LPGraphicalMethodViewModel() }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Row {
        Column {
            ObjectiveFunctionInput(
                state.objectiveFunction,
                state.isMax,
                { a, b, isMax -> viewModel.onEvent(LPGraphicalMethodEvent.UpdateObjectiveFunction(a, b, isMax)) })

            Row {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(
                            width = 2.dp,
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        .padding(16.dp)
                ) {
                    val clipboard = LocalClipboardManager.current

                    SmallFloatingActionButton(
                        onClick = { viewModel.onEvent(LPGraphicalMethodEvent.AddConstraint) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                    ) {
                        ConstraintAddContextMenu(
                            onDeleteAll = { viewModel.onEvent(LPGraphicalMethodEvent.RemoveAllConstraints) },
                            onCopy = {
                                clipboard.setText(AnnotatedString(Json.encodeToString(state.constraints)))
                            },
                            onPaste = {
                                try {
                                    viewModel.onEvent(
                                        LPGraphicalMethodEvent.PasteConstraints(
                                            Json.decodeFromString<List<Constraint>>(
                                                clipboard.getText()?.text ?: "[]"
                                            )
                                        )
                                    )
                                } catch (e: Exception) {
                                    // Ignore invalid clipboard content
                                }
                            }
                        ) {

                            Icon(Icons.Filled.Add, contentDescription = "Add Constraint")
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(end = 64.dp)
                    ) {
                        if (state.constraints.isEmpty())
                            Text(text = "No constraints", color = MaterialTheme.colorScheme.onSecondaryContainer)
                        state.constraints.forEachIndexed { index, constraint ->
                            ConstraintInput(
                                constraint,
                                { constraint ->
                                    viewModel.onEvent(
                                        LPGraphicalMethodEvent.UpdateConstraint(
                                            index,
                                            constraint
                                        )
                                    )
                                },
                                onDelete = {
                                    viewModel.onEvent(LPGraphicalMethodEvent.RemoveConstraint(index))
                                })
                        }
                    }
                }
            }
        }

        Box {
            LinearGraphic(
                state.constraints,
                { pan, zoom -> viewModel.onEvent(LPGraphicalMethodEvent.GraphTransform(pan, zoom)) },
                objFunction = state.objectiveFunction,
                offset = state.graphOffset,
                scale = state.graphScale,
                isMax = state.isMax,
            )
        }
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LinearGraphic(
    constraints: List<Constraint>,
    onTransform: (Offset, Float) -> Unit,
    objFunction: Pair<Double, Double> = 0.0 to 0.0,
    isMax: Boolean,
    offset: Offset = Offset.Zero,
    scale: Float = 50f
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = LPGraphDefaults.graphColors.label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Light
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp)
            .background(LPGraphDefaults.graphColors.background)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onTransform(pan, zoom)
                }
            }
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val delta = event.changes.first().scrollDelta
                val zoomFactor = if (delta.y > 0) 0.9f else 1.1f
                onTransform(Offset.Zero, zoomFactor)
            }
            .clipToBounds()
    ) {
        val canvasCenter = size.center + offset

        fun toScreen(p: Point): Offset = Offset(canvasCenter.x + p.x * scale, canvasCenter.y - p.y * scale)
        fun toMath(o: Offset): Point = Point((o.x - canvasCenter.x) / scale, (canvasCenter.y - o.y) / scale)

        val mathLeft = toMath(Offset(0f, 0f)).x
        val mathRight = toMath(Offset(size.width, 0f)).x
        val mathTop = toMath(Offset(0f, 0f)).y
        val mathBottom = toMath(Offset(0f, size.height)).y

        val viewportConstraints = listOf(
            Constraint(1.0, 0.0, mathLeft.toDouble(), ">="),
            Constraint(1.0, 0.0, mathRight.toDouble(), "<="),
            Constraint(0.0, 1.0, mathBottom.toDouble(), ">="),
            Constraint(0.0, 1.0, mathTop.toDouble(), "<=")
        )

        drawGrid(canvasCenter, scale)
        drawAxes(canvasCenter, scale, textMeasurer, labelStyle)

        constraints.forEach { constraint ->
            drawConstraintHalfPlane(constraint, mathLeft, mathRight, mathTop, mathBottom, ::toScreen)
            drawConstraintLine(constraint, mathLeft, mathRight, mathTop, mathBottom, ::toScreen)
        }

        val feasiblePoints = findFeasibleRegion(constraints, viewportConstraints)

        if (constraints.isNotEmpty() && feasiblePoints.isEmpty()) {
            drawInfoMessage("No feasible solution", textMeasurer)
        } else {
            drawFeasibleRegion(feasiblePoints, ::toScreen)

            val optimalSolution = findOptimalPoint(feasiblePoints, objFunction, isMax)
            optimalSolution?.let { (optPoint, optValue) ->
                val isUnbounded = optPoint.x <= mathLeft + 0.1 || optPoint.x >= mathRight - 0.1 ||
                        optPoint.y <= mathBottom + 0.1 || optPoint.y >= mathTop - 0.1

                if (isUnbounded && (objFunction.first != 0.0 || objFunction.second != 0.0)) {
                    drawInfoMessage("The objective function is unbounded", textMeasurer)
                } else {
                    drawOptimalSolution(
                        optPoint = optPoint,
                        optValue = optValue,
                        objFunction = objFunction,
                        isMax = isMax,
                        toScreen = ::toScreen,
                        viewportConstraints = viewportConstraints,
                        mathLeft = mathLeft,
                        mathRight = mathRight,
                        mathTop = mathTop,
                        mathBottom = mathBottom,
                        textMeasurer = textMeasurer
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawGrid(canvasCenter: Offset, scale: Float) {
    val xStart = (-(canvasCenter.x) / scale).toInt() - 1
    val xEnd = ((size.width - canvasCenter.x) / scale).toInt() + 1
    for (i in xStart..xEnd) {
        val xPos = canvasCenter.x + i * scale
        drawLine(LPGraphDefaults.graphColors.grid, Offset(xPos, 0f), Offset(xPos, size.height), 1f)
    }

    val yStart = (-(size.height + canvasCenter.y) / scale).toInt() - 1
    val yEnd = ((canvasCenter.y) / scale).toInt() + 1
    for (i in yStart..yEnd) {
        val yPos = canvasCenter.y + i * scale
        drawLine(LPGraphDefaults.graphColors.grid, Offset(0f, yPos), Offset(size.width, yPos), 1f)
    }
}

private fun DrawScope.drawAxes(
    canvasCenter: Offset,
    scale: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    labelStyle: TextStyle
) {
    if (canvasCenter.y in 0f..size.height) {
        drawLine(
            LPGraphDefaults.graphColors.axis,
            Offset(0f, canvasCenter.y),
            Offset(size.width, canvasCenter.y),
            LPGraphDefaults.LineStrokeWidth
        )
    }
    if (canvasCenter.x in 0f..size.width) {
        drawLine(
            LPGraphDefaults.graphColors.axis,
            Offset(canvasCenter.x, 0f),
            Offset(canvasCenter.x, size.height),
            LPGraphDefaults.LineStrokeWidth
        )
    }

    val stepValue = when {
        scale > 80f -> 1
        scale > 30f -> 2
        scale > 10f -> 5
        else -> 10
    }
    val visualStep = stepValue * scale

    val xStartIdx = (-(canvasCenter.x) / visualStep).toInt() - 1
    val xEndIdx = ((size.width - canvasCenter.x) / visualStep).toInt() + 1
    for (i in xStartIdx..xEndIdx) {
        if (i == 0) continue
        val xPos = canvasCenter.x + i * visualStep
        if (xPos < 0 || xPos > size.width) continue

        val text = (i * stepValue).toString()
        val textLayout = textMeasurer.measure(text, labelStyle)

        val yPosForText = max(0f, min(size.height - textLayout.size.height, canvasCenter.y + 4f))
        val yPosForTickStart = max(0f, min(size.height, canvasCenter.y - 5f))
        val yPosForTickEnd = max(0f, min(size.height, canvasCenter.y + 5f))

        drawText(textLayoutResult = textLayout, topLeft = Offset(xPos - textLayout.size.width / 2, yPosForText))
        drawLine(LPGraphDefaults.graphColors.axis, Offset(xPos, yPosForTickStart), Offset(xPos, yPosForTickEnd), 2f)
    }

    val yStartIdx = (-(size.height - canvasCenter.y) / visualStep).toInt() - 1
    val yEndIdx = ((canvasCenter.y) / visualStep).toInt() + 1
    for (i in yStartIdx..yEndIdx) {
        if (i == 0) continue
        val yPos = canvasCenter.y - i * visualStep
        if (yPos < 0 || yPos > size.height) continue

        val text = (i * stepValue).toString()
        val textLayout = textMeasurer.measure(text, labelStyle)

        val xPosForText = max(0f, min(size.width - textLayout.size.width, canvasCenter.x - textLayout.size.width - 8f))
        val xPosForTickStart = max(0f, min(size.width, canvasCenter.x - 5f))
        val xPosForTickEnd = max(0f, min(size.width, canvasCenter.x + 5f))

        drawText(textLayoutResult = textLayout, topLeft = Offset(xPosForText, yPos - textLayout.size.height / 2))
        drawLine(LPGraphDefaults.graphColors.axis, Offset(xPosForTickStart, yPos), Offset(xPosForTickEnd, yPos), 2f)
    }

    val zeroLayout = textMeasurer.measure("0", labelStyle)
    val xPos = max(0f, min(size.width - zeroLayout.size.width, canvasCenter.x - zeroLayout.size.width - 4f))
    val yPos = max(0f, min(size.height - zeroLayout.size.height, canvasCenter.y + 4f))
    drawText(textLayoutResult = zeroLayout, topLeft = Offset(xPos, yPos))
}

private fun DrawScope.drawConstraintLine(
    constraint: Constraint,
    mathLeft: Float,
    mathRight: Float,
    mathTop: Float,
    mathBottom: Float,
    toScreen: (Point) -> Offset
) {
    val a = constraint.a
    val b = constraint.b
    val c = constraint.c

    val startPoint: Point
    val endPoint: Point

    if (abs(b) > LPGraphDefaults.Epsilon) {
        val y1 = (c - a * mathLeft) / b
        val y2 = (c - a * mathRight) / b
        startPoint = Point(mathLeft, y1.toFloat())
        endPoint = Point(mathRight, y2.toFloat())
    } else {
        val x = (c / a).toFloat()
        startPoint = Point(x, mathTop)
        endPoint = Point(x, mathBottom)
    }

    drawLine(
        color = LPGraphDefaults.graphColors.constraintLine,
        start = toScreen(startPoint),
        end = toScreen(endPoint),
        strokeWidth = LPGraphDefaults.LineStrokeWidth
    )
}

private fun DrawScope.drawConstraintHalfPlane(
    constraint: Constraint,
    mathLeft: Float,
    mathRight: Float,
    mathTop: Float,
    mathBottom: Float,
    toScreen: (Point) -> Offset
) {
    if (constraint.operator == "=") return

    val screenCorners = listOf(
        Point(mathLeft, mathTop), Point(mathRight, mathTop),
        Point(mathRight, mathBottom), Point(mathLeft, mathBottom)
    )

    val polygonPoints = mutableListOf<Point>()
    screenCorners.forEach { corner ->
        if (constraint.isSatisfied(corner)) {
            polygonPoints.add(corner)
        }
    }

    val lineIntersections = mutableListOf<Point>()
    val viewportEdges = listOf(
        Constraint(1.0, 0.0, mathLeft.toDouble(), "="),
        Constraint(1.0, 0.0, mathRight.toDouble(), "="),
        Constraint(0.0, 1.0, mathTop.toDouble(), "="),
        Constraint(0.0, 1.0, mathBottom.toDouble(), "=")
    )

    viewportEdges.forEach { edge ->
        findIntersection(constraint, edge)?.let { p ->
            if (p.x in mathLeft..mathRight && p.y in mathBottom..mathTop) {
                lineIntersections.add(p)
            }
        }
    }

    polygonPoints.addAll(lineIntersections.distinctBy {
        Point(
            (it.x * 1000).roundToInt().toFloat(),
            (it.y * 1000).roundToInt().toFloat()
        )
    })

    if (polygonPoints.size >= 3) {
        val centerX = polygonPoints.map { it.x }.average().toFloat()
        val centerY = polygonPoints.map { it.y }.average().toFloat()
        val sortedPoints =
            polygonPoints.sortedBy { kotlin.math.atan2((it.y - centerY).toDouble(), (it.x - centerX).toDouble()) }

        val path = Path().apply {
            moveTo(toScreen(sortedPoints.first()).x, toScreen(sortedPoints.first()).y)
            for (i in 1 until sortedPoints.size) {
                lineTo(toScreen(sortedPoints[i]).x, toScreen(sortedPoints[i]).y)
            }
            close()
        }
        drawPath(path, LPGraphDefaults.graphColors.constraintHalfPlane)
    } else if (screenCorners.all { constraint.isSatisfied(it) }) {
        drawRect(LPGraphDefaults.graphColors.constraintHalfPlane)
    }
}

private fun DrawScope.drawFeasibleRegion(
    feasiblePoints: List<Point>,
    toScreen: (Point) -> Offset
) {
    if (feasiblePoints.size < 3) return

    val feasiblePath = Path().apply {
        moveTo(toScreen(feasiblePoints.first()).x, toScreen(feasiblePoints.first()).y)
        for (i in 1 until feasiblePoints.size) {
            lineTo(toScreen(feasiblePoints[i]).x, toScreen(feasiblePoints[i]).y)
        }
        close()
    }

    drawPath(
        path = feasiblePath,
        color = LPGraphDefaults.graphColors.feasibleRegionFill
    )
    drawPath(
        path = feasiblePath,
        color = LPGraphDefaults.graphColors.feasibleRegionStroke,
        style = Stroke(width = LPGraphDefaults.FeasibleRegionStrokeWidth)
    )
}

private fun DrawScope.drawOptimalSolution(
    optPoint: Point,
    optValue: Double,
    objFunction: Pair<Double, Double>,
    isMax: Boolean,
    toScreen: (Point) -> Offset,
    viewportConstraints: List<Constraint>,
    mathLeft: Float,
    mathRight: Float,
    mathTop: Float,
    mathBottom: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val screenPoint = toScreen(optPoint)

    drawCircle(
        color = LPGraphDefaults.graphColors.optimalPoint,
        radius = LPGraphDefaults.PointRadius,
        center = screenPoint
    )

    val (objA, objB) = objFunction
    if (objA != 0.0 || objB != 0.0) {
        val optimalLine = Constraint(objA, objB, optValue)
        val linePoints = viewportConstraints
            .mapNotNull { findIntersection(optimalLine, it) }
            .filter { it.x in mathLeft..mathRight && it.y in mathBottom..mathTop }

        if (linePoints.size >= 2) {
            drawLine(
                color = LPGraphDefaults.graphColors.optimalLine,
                start = toScreen(linePoints[0]),
                end = toScreen(linePoints[1]),
                strokeWidth = LPGraphDefaults.OptimalLineStrokeWidth,
                pathEffect = LPGraphDefaults.OptimalLinePathEffect
            )
        }
    }

    val roundedValue = (optValue * 100.0).roundToInt() / 100.0
    val roundedX = (optPoint.x * 100.0).roundToInt() / 100.0
    val roundedY = (optPoint.y * 100.0).roundToInt() / 100.0
    val resultText = "${if (isMax) "max" else "min"} F = $roundedValue at ($roundedX, $roundedY)"
    val textLayout = textMeasurer.measure(
        resultText,
        TextStyle(
            fontWeight = FontWeight.Bold,
            background = LPGraphDefaults.graphColors.textBackground
        )
    )
    drawText(textLayoutResult = textLayout, topLeft = screenPoint + Offset(15f, -15f))
}

private fun DrawScope.drawInfoMessage(
    message: String,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val textLayout = textMeasurer.measure(
        message,
        TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = LPGraphDefaults.graphColors.infoText,
            background = LPGraphDefaults.graphColors.textBackground
        )
    )
    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(
            x = center.x - textLayout.size.width / 2,
            y = center.y - textLayout.size.height / 2
        )
    )
}

@Composable
expect fun ConstraintAddContextMenu(
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onDeleteAll: () -> Unit,
    content: @Composable (() -> Unit),
)