package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = Repository(database)

    private val prefs = application.getSharedPreferences("hogar_sincro_prefs", Context.MODE_PRIVATE)
    val isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))

    // Data streams from database
    val expenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryItems: StateFlow<List<InventoryItem>> = repository.allInventoryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockItems: StateFlow<List<InventoryItem>> = repository.lowStockItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shoppingItems: StateFlow<List<ShoppingItem>> = repository.allShoppingItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncSettings: StateFlow<SyncSettings> = repository.syncSettings
        .map { it ?: SyncSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncSettings())

    val variableExpensesSum: StateFlow<Double> = repository.variableExpensesSum
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val recurringExpensesSum: StateFlow<Double> = repository.recurringExpensesSum
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Scanner UI states
    var isScanning = MutableStateFlow(false)
    var scanResult = MutableStateFlow<ExtractedReceipt?>(null)

    // Live smart alerts / notifications lists
    private val _notifications = MutableStateFlow<List<SmartNotification>>(emptyList())
    val notifications: StateFlow<List<SmartNotification>> = _notifications.asStateFlow()

    // Recent real-time sync activities
    private val _syncActivities = MutableStateFlow<List<SyncActivity>>(emptyList())
    val syncActivities: StateFlow<List<SyncActivity>> = _syncActivities.asStateFlow()

    // Neon PostgreSQL Connection status: "DISCONNECTED", "CONNECTING", "CONNECTED", "ERROR"
    val neonConnectionState = MutableStateFlow("DISCONNECTED")

    // Global Web Portal simulated view toggle
    val showWebPortal = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            // Seeding default data on startup
            repository.prepopulateIfEmpty()
            generateSmartAlerts()
            startRealTimeSyncSimulation()
            // Pull/push with Neon Postgres on startup
            syncWithNeon()
        }
    }

    // SQL Over Neon Postgres Cloud Sincro Action
    fun syncWithNeon() {
        viewModelScope.launch {
            neonConnectionState.value = "CONNECTING"
            val connected = NeonDatabaseHelper.testConnection()
            if (connected) {
                val tablesCreated = NeonDatabaseHelper.createTables()
                if (tablesCreated) {
                    val syncExpensesOk = NeonDatabaseHelper.syncExpenses(getApplication(), repository.expenseDao)
                    val syncInventoryOk = NeonDatabaseHelper.syncInventory(getApplication(), repository.inventoryDao)
                    val syncShoppingOk = NeonDatabaseHelper.syncShopping(getApplication(), repository.shoppingDao)
                    
                    if (syncExpensesOk && syncInventoryOk && syncShoppingOk) {
                        neonConnectionState.value = "CONNECTED"
                        addSyncActivity("SISTEMA", "Base de datos Neon PostgreSQL sincronizada exitosamente.", "SISTEMA")
                        addToastNotification("Sincronización Neon", "Datos del hogar actualizados con Neon Cloud.")
                    } else {
                        neonConnectionState.value = "ERROR"
                        addSyncActivity("SISTEMA", "Fallo parcial de sincronización en Neon Postgres.", "SISTEMA")
                    }
                } else {
                    neonConnectionState.value = "ERROR"
                    addSyncActivity("SISTEMA", "No se pudieron comprobar o crear las tablas en Neon.", "SISTEMA")
                }
            } else {
                neonConnectionState.value = "ERROR"
                addSyncActivity("SISTEMA", "No se pudo conectar a Neon PostgreSQL. Modo offline activo.", "SISTEMA")
            }
            generateSmartAlerts()
        }
    }

    // Generate proactive notifications based on database state
    fun generateSmartAlerts() {
        viewModelScope.launch {
            val alerts = mutableListOf<SmartNotification>()
            val items = inventoryItems.first()
            val bills = expenses.first()

            // 1. Check for low stock or soon depleting items
            items.forEach { item ->
                if (item.currentStock <= item.minStockAlert) {
                    alerts.add(
                        SmartNotification(
                            id = "low_${item.id}",
                            title = "Stock Bajo: ${item.name}",
                            message = "${item.name} se encuentra en ${item.currentStock} ${item.unit} (Mínimo: ${item.minStockAlert}). ¡Agrégalo a compras!",
                            type = "ALERTA",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } else if (item.daysUntilDepletion in 1..4) {
                    alerts.add(
                        SmartNotification(
                            id = "deplete_${item.id}",
                            title = "Agotamiento Pronto: ${item.name}",
                            message = "Quedan aprox. ${item.daysUntilDepletion} días de inventario de ${item.name}.",
                            type = "ADVERTENCIA",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }

            // 2. Add notifications for upcoming bills
            bills.forEach { bill ->
                if (bill.isRecurring && bill.recurringDueDate != null) {
                    alerts.add(
                        SmartNotification(
                            id = "bill_${bill.id}",
                            title = "Gasto Recurrente: ${bill.title}",
                            message = "Vence el ${bill.recurringDueDate}. Costo mensual recurrente de $${String.format("%.2f", bill.amount)}.",
                            type = "ALQUILER_SERVICIO",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }

            // Set current alarms
            _notifications.value = alerts
        }
    }

    // SIMULATED REAL-TIME SYNCHRONIZATION ENGINE FOR MULTI-USER ACCESS
    private fun startRealTimeSyncSimulation() {
        viewModelScope.launch {
            val random = Random()
            // Wait first
            delay(20000)

            while (true) {
                val settings = syncSettings.value
                if (settings.isSyncEnabled) {
                    val membersList = settings.members.split(",")
                    val otherUsers = membersList.filter { it != settings.activeUser }
                    val otherUser = if (otherUsers.isNotEmpty()) otherUsers[random.nextInt(otherUsers.size)] else "Pilar"
                    
                    // Choose a random sync scenario from the other user
                    when (random.nextInt(4)) {
                        0 -> {
                            // Sync Scenario 0: Pilar/otherUser consumes stock of Rice
                            val rice = inventoryItems.value.find { it.name.contains("Arroz", ignoreCase = true) }
                            if (rice != null && rice.currentStock > 0.2) {
                                val updated = rice.copy(
                                    currentStock = Math.max(0.1, rice.currentStock - 0.2),
                                    lastUpdated = System.currentTimeMillis()
                                )
                                repository.updateInventoryItem(updated)
                                addSyncActivity(otherUser, "Consumió 0.2 kg de Arroz. Inventario actualizado.", "INVENTARIO")
                                addToastNotification("Inventario", "$otherUser consumió Arroz. ¡Stock bajo!")
                            }
                        }
                        1 -> {
                            // Sync Scenario 1: Pilar/otherUser adds shopping item
                            val itemName = "Frutas frescas"
                            val exists = shoppingItems.value.any { it.productName.equals(itemName, ignoreCase = true) }
                            if (!exists) {
                                repository.insertShoppingItem(ShoppingItem(productName = itemName, quantityToBuy = 1.0, unit = "bolsa", estimatedPrice = 5.50, isBought = false, targetStore = "Mercado Central"))
                                addSyncActivity(otherUser, "Agregó 'Frutas frescas' a la lista de compras.", "COMPRAS")
                                addToastNotification("Lista de Compras", "$otherUser agregó 'Frutas frescas' a la lista.")
                            }
                        }
                        2 -> {
                            // Sync Scenario 2: Pilar/otherUser buys an item
                            val pendingItem = shoppingItems.value.find { !it.isBought }
                            if (pendingItem != null) {
                                val updated = pendingItem.copy(isBought = true)
                                repository.updateShoppingItem(updated)
                                // Add to expenses too
                                repository.insertExpense(Expense(
                                    title = "${pendingItem.productName} (Por $otherUser)",
                                    amount = pendingItem.estimatedPrice,
                                    category = "Alimentos",
                                    paidBy = otherUser
                                ))
                                addSyncActivity(otherUser, "Compró '${pendingItem.productName}' por $${pendingItem.estimatedPrice}.", "Gasto")
                                addToastNotification("Compras", "$otherUser compró ${pendingItem.productName} y registró el gasto.")
                            }
                        }
                        3 -> {
                            // Sync Scenario 3: Pilar/otherUser adds a service expense
                            repository.insertExpense(Expense(
                                title = "Gas de Cocina - Recarga",
                                amount = 22.00,
                                category = "Servicio",
                                paidBy = otherUser
                            ))
                            addSyncActivity(otherUser, "Registró un gasto de $22.00 para 'Gas de Cocina'.", "GASTO")
                            addToastNotification("Gastos", "$otherUser registró un gasto de $22.00.")
                        }
                    }
                    generateSmartAlerts()
                    repository.saveSyncSettings(settings.copy(lastSyncTimestamp = System.currentTimeMillis()))
                }
                // Sync pulse runs every 35 seconds to simulate ambient activity in background
                delay(35000)
            }
        }
    }

    private fun addSyncActivity(user: String, description: String, category: String) {
        val newList = _syncActivities.value.toMutableList()
        newList.add(0, SyncActivity(user, description, category, System.currentTimeMillis()))
        if (newList.size > 8) newList.removeAt(newList.size - 1)
        _syncActivities.value = newList
    }

    private fun addToastNotification(title: String, message: String) {
        val current = _notifications.value.toMutableList()
        current.add(0, SmartNotification(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            type = "SINCRONIZACION",
            timestamp = System.currentTimeMillis()
        ))
        _notifications.value = current
    }

    // Expense Actions
    fun addExpense(title: String, amount: Double, category: String, isRecurring: Boolean = false, dueDate: String? = null) {
        viewModelScope.launch {
            val activeUser = syncSettings.value.activeUser
            val expense = Expense(
                title = title,
                amount = amount,
                category = category,
                isRecurring = isRecurring,
                recurringDueDate = dueDate,
                paidBy = activeUser
            )
            repository.insertExpense(expense)
            generateSmartAlerts()
            
            // Add sync log
            addSyncActivity(activeUser, "Agregó gasto de $$amount: $title", "GASTOS")
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            DeletionTracker.trackExpenseDeletion(getApplication(), expense.id)
            repository.deleteExpense(expense)
            generateSmartAlerts()
            
            val activeUser = syncSettings.value.activeUser
            addSyncActivity(activeUser, "Eliminó el gasto: ${expense.title}", "GASTOS")
        }
    }

    // Shopping list Actions
    fun addShoppingItem(name: String, qty: Double, unit: String, targetStore: String? = null) {
        viewModelScope.launch {
            // Find estimated price from known inventory items (best price)
            val bestKnownPrice = inventoryItems.value.find { it.name.equals(name, ignoreCase = true) }?.bestPrice ?: 1.50
            
            val item = ShoppingItem(
                productName = name,
                quantityToBuy = qty,
                unit = unit,
                estimatedPrice = bestKnownPrice,
                targetStore = targetStore ?: inventoryItems.value.find { it.name.equals(name, ignoreCase = true) }?.bestStore
            )
            repository.insertShoppingItem(item)
            
            val activeUser = syncSettings.value.activeUser
            addSyncActivity(activeUser, "Agregó '${name}' (${qty} ${unit}) a compras", "COMPRAS")
        }
    }

    fun toggleShoppingItemBought(item: ShoppingItem) {
        viewModelScope.launch {
            val updated = item.copy(isBought = !item.isBought)
            repository.updateShoppingItem(updated)
            
            val activeUser = syncSettings.value.activeUser
            addSyncActivity(activeUser, "${if (updated.isBought) "Compró" else "Desmarcó"} '${item.productName}'", "COMPRAS")

            if (updated.isBought) {
                // Instantly register this as a monthly variable expense!
                repository.insertExpense(
                    Expense(
                        title = "${item.productName} (${item.targetStore ?: "Compras"})",
                        amount = item.estimatedPrice * item.quantityToBuy,
                        category = "Alimentos",
                        paidBy = activeUser
                    )
                )

                // Refill the corresponding inventory item if exists!
                val matchingInv = inventoryItems.value.find { it.name.equals(item.productName, ignoreCase = true) }
                if (matchingInv != null) {
                    val newStock = matchingInv.currentStock + item.quantityToBuy
                    repository.updateInventoryItem(matchingInv.copy(
                        currentStock = newStock,
                        lastUpdated = System.currentTimeMillis()
                    ))
                }
                generateSmartAlerts()
            }
        }
    }

    fun deleteShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            DeletionTracker.trackShoppingDeletion(getApplication(), item.id)
            repository.deleteShoppingItem(item)
        }
    }

    fun clearBoughtShoppingItems() {
        viewModelScope.launch {
            shoppingItems.value.filter { it.isBought }.forEach { boughtItem ->
                DeletionTracker.trackShoppingDeletion(getApplication(), boughtItem.id)
            }
            repository.deleteBoughtShoppingItems()
        }
    }

    // Inventory Actions
    fun addInventoryItem(name: String, stock: Double, limit: Double, unit: String, depletionRate: Double, store: String?, price: Double?) {
        viewModelScope.launch {
            val item = InventoryItem(
                name = name,
                currentStock = stock,
                minStockAlert = limit,
                unit = unit,
                depletionRatePerDay = depletionRate,
                bestStore = store,
                bestPrice = price,
                lastUpdated = System.currentTimeMillis()
            )
            repository.insertInventoryItem(item)
            generateSmartAlerts()
            
            val activeUser = syncSettings.value.activeUser
            addSyncActivity(activeUser, "Añadió producto al catálogo de inventario: ${name}", "INVENTARIO")
        }
    }

    fun updateInventoryStock(item: InventoryItem, newStock: Double) {
        viewModelScope.launch {
            val updated = item.copy(
                currentStock = newStock,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateInventoryItem(updated)
            generateSmartAlerts()
            
            val activeUser = syncSettings.value.activeUser
            addSyncActivity(activeUser, "Actualizó stock de '${item.name}' a $newStock ${item.unit}", "INVENTARIO")
        }
    }

    fun deleteInventoryItem(item: InventoryItem) {
         viewModelScope.launch {
              DeletionTracker.trackInventoryDeletion(getApplication(), item.id)
              repository.deleteInventoryItem(item)
              generateSmartAlerts()
         }
    }



    // Household Sync Settings Actions
    fun changeSyncProvider(isEnabled: Boolean) {
        viewModelScope.launch {
            val updated = syncSettings.value.copy(isSyncEnabled = isEnabled)
            repository.saveSyncSettings(updated)
        }
    }

    fun toggleSyncUser() {
        viewModelScope.launch {
            val current = syncSettings.value
            val membersList = current.members.split(",")
            val currentIndex = membersList.indexOf(current.activeUser)
            val nextIndex = if (currentIndex != -1) (currentIndex + 1) % membersList.size else 0
            val nextUser = membersList[nextIndex]
            val updated = current.copy(activeUser = nextUser)
            repository.saveSyncSettings(updated)
            
            addSyncActivity(nextUser, "Cambió al perfil de usuario $nextUser.", "SISTEMA")
        }
    }

    fun selectActiveUser(user: String) {
        viewModelScope.launch {
            val current = syncSettings.value
            val updated = current.copy(activeUser = user)
            repository.saveSyncSettings(updated)
            
            addSyncActivity(user, "Cambió al perfil de usuario $user.", "SISTEMA")
        }
    }

    fun addHouseholdMember(name: String) {
        viewModelScope.launch {
            val current = syncSettings.value
            val cleanedName = name.trim()
            if (cleanedName.isNotBlank()) {
                val currentMembers = current.members.split(",").map { it.trim() }.toMutableList()
                if (!currentMembers.contains(cleanedName)) {
                    currentMembers.add(cleanedName)
                    val updated = current.copy(members = currentMembers.joinToString(","))
                    repository.saveSyncSettings(updated)
                    addSyncActivity(current.activeUser, "Añadió a '$cleanedName' como miembro del hogar.", "SISTEMA")
                    addToastNotification("Miembro Añadido", "${current.activeUser} unió a $cleanedName al hogar.")
                }
            }
        }
    }

    fun updateHouseholdCode(code: String) {
        viewModelScope.launch {
            val updated = syncSettings.value.copy(householdCode = code.uppercase())
            repository.saveSyncSettings(updated)
            
            addSyncActivity(syncSettings.value.activeUser, "Actualizó código del hogar a $code", "SISTEMA")
        }
    }

    // Ticket automatic OCR Scanning Action
    fun scanTicketWithGemini(bitmap: Bitmap?, sampleType: String?) {
        viewModelScope.launch {
            isScanning.value = true
            try {
                val receipt = GeminiScannerService.scanReceipt(bitmap, sampleType)
                scanResult.value = receipt

                // Automatically import products & expenses from Receipt!
                // 1. Add as Variable Expense
                repository.insertExpense(
                    Expense(
                        title = "Compra en ${receipt.storeName}",
                        amount = receipt.totalAmount,
                        category = receipt.category
                    )
                )

                // 2. Insert items into inventory or shopping items
                receipt.items.forEach { item ->
                    // Check if exists in inventory to improve price/store data
                    val matchingInv = inventoryItems.value.find { it.name.equals(item.name, ignoreCase = true) }
                    if (matchingInv != null) {
                        // Update best price and best store if cheapest
                        val isBestPrice = matchingInv.bestPrice == null || item.price < matchingInv.bestPrice
                        val updatedInv = if (isBestPrice) {
                            matchingInv.copy(
                                secondBestPrice = matchingInv.bestPrice,
                                secondBestStore = matchingInv.bestStore,
                                bestPrice = item.price,
                                bestStore = receipt.storeName,
                                currentStock = matchingInv.currentStock + item.quantity,
                                lastUpdated = System.currentTimeMillis()
                            )
                        } else {
                            matchingInv.copy(
                                currentStock = matchingInv.currentStock + item.quantity,
                                lastUpdated = System.currentTimeMillis()
                            )
                        }
                        repository.updateInventoryItem(updatedInv)
                    } else {
                        // Create as new inventory item with current stock
                        repository.insertInventoryItem(
                            InventoryItem(
                                name = item.name,
                                currentStock = item.quantity,
                                minStockAlert = 1.0,
                                unit = "u",
                                depletionRatePerDay = 0.1, // Default slow depletion
                                bestStore = receipt.storeName,
                                bestPrice = item.price
                            )
                        )
                    }
                }

                generateSmartAlerts()
                
                val activeUser = syncSettings.value.activeUser
                addSyncActivity(activeUser, "Escaneó ticket de compras de '${receipt.storeName}' por $${receipt.totalAmount}.", "SCANNER")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Scanner exception during processing: ${e.message}", e)
            } finally {
                isScanning.value = false
            }
        }
    }

    fun clearScanResult() {
        scanResult.value = null
    }

    fun dismissNotification(id: String) {
        val current = _notifications.value.toMutableList()
        current.removeAll { it.id == id }
        _notifications.value = current
    }

    // Session Management logic for elegant Login/Register screen
    fun loginUser(activeUser: String, householdCode: String) {
        viewModelScope.launch {
            val currentSettings = syncSettings.value
            val currentMembers = currentSettings.members.split(",").map { it.trim() }.toMutableSet()
            currentMembers.add(activeUser)
            val updatedSetting = currentSettings.copy(
                activeUser = activeUser,
                householdCode = householdCode.uppercase(),
                members = currentMembers.joinToString(",")
            )
            repository.saveSyncSettings(updatedSetting)
            
            prefs.edit().putBoolean("is_logged_in", true).apply()
            isLoggedIn.value = true
            
            addSyncActivity(activeUser, "Inició sesión en el hogar $householdCode.", "SISTEMA")
            syncWithNeon()
        }
    }

    fun registerUser(activeUser: String, householdCode: String, membersCsv: String) {
        viewModelScope.launch {
            val membersList = membersCsv.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toMutableSet()
            membersList.add(activeUser)
            
            val updatedSetting = SyncSettings(
                activeUser = activeUser,
                householdCode = householdCode.uppercase(),
                members = membersList.joinToString(",")
            )
            repository.saveSyncSettings(updatedSetting)
            
            prefs.edit().putBoolean("is_logged_in", true).apply()
            isLoggedIn.value = true
            
            addSyncActivity(activeUser, "Registró un nuevo hogar con código $householdCode.", "SISTEMA")
            syncWithNeon()
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            prefs.edit().putBoolean("is_logged_in", false).apply()
            isLoggedIn.value = false
        }
    }
}

// Support classes for beautiful smart status notifications and activity log
data class SmartNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: String, // "ALERTA", "ADVERTENCIA", "ALQUILER_SERVICIO", "SINCRONIZACION"
    val timestamp: Long
)

data class SyncActivity(
    val user: String,
    val description: String,
    val category: String, // "COMPRAS", "INVENTARIO", "GASTOS", "SISTEMA", "SCANNER"
    val timestamp: Long
)
