package com.example.startapp.data.model

import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val date: Long,
    val description: String
)
