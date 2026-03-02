package com.bbm.multitask.data.model.lPGraphicalMethod

import kotlinx.serialization.Serializable

@Serializable
data class Constraint(
    val a: Double,
    val b: Double,
    val c: Double,
    val operator: String = "<=" // "<=", ">=", "="
) {
    fun isSatisfied(p: Point, epsilon: Float = 1e-3f): Boolean {
        val value = a * p.x + b * p.y
        return when (operator) {
            "<=" -> value <= c + epsilon
            ">=" -> value >= c - epsilon
            "=" -> kotlin.math.abs(value - c) < epsilon
            else -> true
        }
    }
}

fun findIntersection(l1: Constraint, l2: Constraint): Point? {
    val d = l1.a * l2.b - l2.a * l1.b

    if (kotlin.math.abs(d) < 1e-9) return null // Using a small epsilon for floating point comparison

    val dx = l1.c * l2.b - l2.c * l1.b
    val dy = l1.a * l2.c - l2.a * l1.c

    return Point((dx / d).toFloat(), (dy / d).toFloat())
}

fun findFeasibleRegion(constraints: List<Constraint>, viewportBounds: List<Constraint>): List<Point> {
    val allConstraints = constraints + viewportBounds
    val points = mutableListOf<Point>()

    for (i in allConstraints.indices) {
        for (j in i + 1 until allConstraints.size) {
            val intersection = findIntersection(allConstraints[i], allConstraints[j])
            if (intersection != null && allConstraints.all { it.isSatisfied(intersection, 1e-4f) }) {
                points.add(intersection)
            }
        }
    }

    if (points.isEmpty()) return emptyList()

    val centerX = points.map { it.x }.average().toFloat()
    val centerY = points.map { it.y }.average().toFloat()

    return points.distinctBy {
        val xRounded = (it.x * 10_000).toInt()
        val yRounded = (it.y * 10_000).toInt()
        xRounded to yRounded
    }.sortedBy {
        kotlin.math.atan2((it.y - centerY).toDouble(), (it.x - centerX).toDouble())
    }
}

fun findOptimalPoint(
    vertices: List<Point>,
    objFunc: Pair<Double, Double>,
    isMax: Boolean
): Pair<Point, Double>? {
    if (vertices.isEmpty()) return null

    val (a, b) = objFunc
    var optimalPoint = vertices[0]
    var optimalValue = a * vertices[0].x + b * vertices[0].y

    for (i in 1 until vertices.size) {
        val currentValue = a * vertices[i].x + b * vertices[i].y
        if (isMax) {
            if (currentValue > optimalValue) {
                optimalValue = currentValue
                optimalPoint = vertices[i]
            }
        } else {
            if (currentValue < optimalValue) {
                optimalValue = currentValue
                optimalPoint = vertices[i]
            }
        }
    }
    return optimalPoint to optimalValue
}