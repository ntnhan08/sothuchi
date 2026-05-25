package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_config")
data class BudgetConfig(
    @PrimaryKey val id: Int = 1,
    val currentFunds: Double = 0.0
)
