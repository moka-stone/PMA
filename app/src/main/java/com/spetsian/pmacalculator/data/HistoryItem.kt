package com.spetsian.pmacalculator.data

data class HistoryItem(
    val id: String = "",
    val expression: String = "",
    val result: String = "",
    val createdAt: Long = 0L
)