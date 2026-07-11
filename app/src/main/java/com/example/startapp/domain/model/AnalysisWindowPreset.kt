package com.example.startapp.domain.model

enum class AnalysisWindowPreset(
    val label: String,
    val days: Int?
) {
    DAYS_7("7D", 7),
    DAYS_30("30D", 30),
    DAYS_90("90D", 90),
    DAYS_180("180D", 180),
    DAYS_365("365D", 365),
    CUSTOM("Custom", null)
}
