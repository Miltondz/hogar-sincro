package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String, // "Alquiler", "Servicio", "Alimentos", "Diverso"
    val timestamp: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val recurringDueDate: String? = null, // e.g. "Día 05 de cada mes"
    val paidBy: String = "Milton" // Who paid / logged this expense
)

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val currentStock: Double,      // Quantity currently in stock
    val minStockAlert: Double,     // Alert threshold
    val unit: String,              // "u", "kg", "paquetes", "litros"
    val depletionRatePerDay: Double, // Approx. consumption per day
    val bestStore: String? = null,
    val bestPrice: Double? = null,
    val secondBestStore: String? = null,
    val secondBestPrice: Double? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
) {
    // Calculated field: estimates how many days till depletion.
    val daysUntilDepletion: Int
        get() = if (depletionRatePerDay > 0) {
            (currentStock / depletionRatePerDay).toInt()
        } else {
            -1 // Infinite or unknown
        }
}

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productName: String,
    val quantityToBuy: Double,
    val unit: String = "u",
    val estimatedPrice: Double = 0.0,
    val isBought: Boolean = false,
    val targetStore: String? = null
)

@Entity(tableName = "sync_settings")
data class SyncSettings(
    @PrimaryKey val id: Int = 1, // Single row configuration
    val activeUser: String = "Milton", // "Milton" or "Alejandra"
    val householdCode: String = "HOGAR-5892",
    val isSyncEnabled: Boolean = true,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val members: String = "Milton,Alejandra", // Comma-separated list of members
    val geminiModel: String = "gemini-2.5-flash"
)


