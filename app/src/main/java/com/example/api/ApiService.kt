package com.example.api

import com.example.data.Expense
import com.example.data.InventoryItem
import com.example.data.ShoppingItem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class TableSyncRequest<T>(
    val deletedIds: List<Int>,
    val localItems: List<T>
)

data class SyncRequest(
    val expenses: TableSyncRequest<Expense>,
    val inventory: TableSyncRequest<InventoryItem>,
    val shopping: TableSyncRequest<ShoppingItem>
)

data class SyncResponse(
    val success: Boolean,
    val error: String? = null,
    val expenses: List<Expense> = emptyList(),
    val inventory: List<InventoryItem> = emptyList(),
    val shopping: List<ShoppingItem> = emptyList()
)

interface ApiService {
    @POST("api/sync")
    suspend fun syncData(@Body request: SyncRequest): Response<SyncResponse>

    @POST("api/clear-db")
    suspend fun clearCloudDatabase(): Response<Void>
}
