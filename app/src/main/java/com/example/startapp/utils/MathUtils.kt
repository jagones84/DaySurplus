package com.example.startapp.utils

import kotlin.math.pow
import kotlin.math.sqrt

fun calculateStdDev(values: List<Double>): Double {
    if (values.size < 2) return 0.0
    val avg = values.average()
    val sumSqDiff = values.sumOf { (it - avg).pow(2) }
    return sqrt(sumSqDiff / (values.size - 1))
}
