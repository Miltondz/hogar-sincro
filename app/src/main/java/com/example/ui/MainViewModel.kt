package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = Repository(database)

    // Current screen state: "dashboard", "expenses", "inventory", "shopping", "sync"
    private val _currentTab = MutableStateFlow("dashboard")
    val currentTab: StateFlow<String> = _currentTab

    val allExpenses = repository.allExpenses
    val allInventoryItems = repository.allInventoryItems
    val lowStockItems = repository.lowStockItems
    val allShoppingItems = repository.allShoppingItems
    val syncSettings = repository.syncSettings

    // Precomputed values derived from StateFlows
    val monthlyVariableExpensesSum: StateFlow<Double> = repository.variableExpensesSum
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyRecurringExpensesSum: StateFlow<Double> = repository.recurringExpensesSum
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Combined total monthly expenditures
    val totalSpendThisMonth: StateFlow<Double> = combine(monthlyVariableExpensesSum, monthlyRecurringExpensesSum) { varSum, recSum ->
        varSum + recSum
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // EOM Prediction: (variable expenses spent / days elapsed * 30 days) + fixed recurring expenses
    val predictedEndSpending: StateFlow<Double> = combine(monthlyVariableExpensesSum, monthlyRecurringExpensesSum) { varSum, recSum ->
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val dailyAvg = if (dayOfMonth > 0) varSum / dayOfMonth else varSum
        (dailyAvg * 30.0) + recSum
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Estimated total cost of current shopping list items
    val shoppingListEstimatedTotal: StateFlow<Double> = allShoppingItems
        .map { list -> list.filter { !it.isBought }.sumOf { it.quantityToBuy * it.estimatedPrice } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Store savings estimation: Costco vs Walmart logic
    val bestStoreDealMessage: StateFlow<String> = allShoppingItems
        .map { items ->
            val unbought = items.filter { !it.isBought }
            if (unbought.isEmpty()) {
                "Lista vacía. Añade artículos para buscar mejores precios."
            } else {
                val sumEst = unbought.sumOf { it.quantityToBuy * it.estimatedPrice }
                val bestStore = unbought.mapNotNull { it.targetStore }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "Walmart"
                val savings = sumEst * 0.12 // Assume 12% potential saving on deals
                String.format(Locale.US, "Ahorra $%.2f comprando en %s tu lista de esta semana.", savings, bestStore)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Cargando ofertas recomendadas...")

    // Simulated alerts / smart notification stack
    private val _notifications = MutableStateFlow<List<String>>(emptyList())
    val notifications: StateFlow<List<String>> = _notifications

    // Scan/Camera simulator variables
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _scanProgress = MutableStateFlow<String?>(null)
    val scanProgress: StateFlow<String?> = _scanProgress

    private val _scannedResultText = MutableStateFlow<String?>(null)
    val scannedResultText: StateFlow<String?> = _scannedResultText

    // Active synchronization log
    private val _syncLogs = MutableStateFlow<List<String>>(listOf("Household sincronizado exitosamente."))
    val syncLogs: StateFlow<List<String>> = _syncLogs

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    init {
        // Pre-populate Database and generate smart alerts on start
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
            checkInventoryThresholds()
        }
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    // Checking stock triggers and updating notification streams
    private suspend fun checkInventoryThresholds() {
        allInventoryItems.take(2).collectLatest { items ->
            val warningLogs = mutableListOf<String>()
            items.forEach { item ->
                if (item.currentStock <= item.minStockAlert) {
                    warningLogs.add("⚠️ Stock Bajo: '${item.name}' tiene solo ${item.currentStock} ${item.unit}. ¡Se agotará pronto!")
                }
                if (item.daysUntilDepletion in 0..3) {
                    warningLogs.add("⏰ Alerta: ${item.name} se agotará en aprox. ${item.daysUntilDepletion} días.")
                }
            }
            if (warningLogs.isEmpty()) {
                warningLogs.add("✅ Todos tus artículos en stock están en niveles óptimos.")
            }
            _notifications.value = warningLogs
        }
    }

    fun addNotification(message: String) {
        _notifications.value = listOf(message) + _notifications.value
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    // Expense Mutators
    fun saveExpense(title: String, amount: Double, category: String, isRecurring: Boolean = false, recurringDueDate: String? = null) {
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    title = title,
                    amount = amount,
                    category = category,
                    isRecurring = isRecurring,
                    recurringDueDate = if (isRecurring) (recurringDueDate ?: "Día 05 de cada mes") else null
                )
            )
            triggerBackgroundSyncSim("Gasto agregado: '$title' por $${amount}")
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            triggerBackgroundSyncSim("Gasto eliminado: '${expense.title}'")
        }
    }

    // Inventory Mutators
    fun saveInventoryItem(
        name: String,
        stock: Double,
        minAlert: Double,
        unit: String,
        depletionPerDay: Double,
        bestStore: String? = null,
        bestPrice: Double? = null,
        secondBestStore: String? = null,
        secondBestPrice: Double? = null
    ) {
        viewModelScope.launch {
            repository.insertInventoryItem(
                InventoryItem(
                    name = name,
                    currentStock = stock,
                    minStockAlert = minAlert,
                    unit = unit,
                    depletionRatePerDay = depletionPerDay,
                    bestStore = bestStore,
                    bestPrice = bestPrice,
                    secondBestStore = secondBestStore,
                    secondBestPrice = secondBestPrice
                )
            )
            checkInventoryThresholds()
            triggerBackgroundSyncSim("Artículo de inventario guardado: '$name'")
        }
    }

    fun updateInventoryStock(item: InventoryItem, newStock: Double) {
        viewModelScope.launch {
            repository.updateInventoryItem(item.copy(currentStock = newStock, lastUpdated = System.currentTimeMillis()))
            checkInventoryThresholds()
            triggerBackgroundSyncSim("Stock actualizado para '${item.name}': ${newStock} ${item.unit}")
        }
    }

    fun deleteInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteInventoryItem(item)
            checkInventoryThresholds()
            triggerBackgroundSyncSim("Artículo de inventario eliminado: '${item.name}'")
        }
    }

    // Shopping List Mutators
    fun saveShoppingItem(productName: String, quantity: Double, unit: String, estimatedPrice: Double, targetStore: String?) {
        viewModelScope.launch {
            repository.insertShoppingItem(
                ShoppingItem(
                    productName = productName,
                    quantityToBuy = quantity,
                    unit = unit,
                    estimatedPrice = estimatedPrice,
                    targetStore = targetStore,
                    isBought = false
                )
            )
            triggerBackgroundSyncSim("Añadido a compras: '$productName'")
        }
    }

    fun toggleShoppingItemBought(item: ShoppingItem) {
        viewModelScope.launch {
            val updated = item.copy(isBought = !item.isBought)
            repository.updateShoppingItem(updated)

            // If checked as bought, we can optionally auto-update/replenish our inventory if it already exists there!
            if (updated.isBought) {
                val inventory = repository.allInventoryItems.first()
                val existing = inventory.find { it.name.equals(updated.productName, ignoreCase = true) }
                if (existing != null) {
                    val replenishedStock = existing.currentStock + updated.quantityToBuy
                    repository.updateInventoryItem(existing.copy(currentStock = replenishedStock, lastUpdated = System.currentTimeMillis()))
                    addNotification("📦 Inventario auto-actualizado: Se añadieron ${updated.quantityToBuy} ${updated.unit} a '${existing.name}'")
                }
            }
            triggerBackgroundSyncSim("Estado de compra cambiado: '${item.productName}'")
        }
    }

    fun deleteShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.deleteShoppingItem(item)
            triggerBackgroundSyncSim("Eliminado de compras: '${item.productName}'")
        }
    }

    fun deleteBoughtShoppingItems() {
        viewModelScope.launch {
            repository.deleteBoughtShoppingItems()
            triggerBackgroundSyncSim("Eliminados artículos comprados")
        }
    }

    fun clearAllShoppingItems() {
        viewModelScope.launch {
            repository.clearAllShoppingItems()
            triggerBackgroundSyncSim("Lista de compras reiniciada")
        }
    }

    // Household Synced actions: toggle user simulation (Milton or Pilar)
    fun switchActiveUser(userName: String) {
        viewModelScope.launch {
            val current = syncSettings.first() ?: SyncSettings()
            repository.saveSyncSettings(current.copy(activeUser = userName))
            addNotification("👤 Cambiaste al perfil de: $userName")
            triggerBackgroundSyncSim("Usuario activo es ahora $userName")
        }
    }

    fun refreshSyncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncLogs.value = listOf("🔄 Sincronizando household con la nube...") + _syncLogs.value
            kotlinx.coroutines.delay(1200)

            val current = syncSettings.first() ?: SyncSettings()
            repository.saveSyncSettings(current.copy(lastSyncTimestamp = System.currentTimeMillis()))
            _isSyncing.value = false
            _syncLogs.value = listOf("✅ Sincronización en tiempo real completada. ¡Ambos usuarios al día!") + _syncLogs.value
            _currentTab.value = "dashboard"
            addNotification("🟢 Sincronizado hace un momento en el household.")
        }
    }

    private fun triggerBackgroundSyncSim(actionName: String) {
        viewModelScope.launch {
            val user = syncSettings.first()?.activeUser ?: "Milton"
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            _syncLogs.value = listOf("[$timestamp] $user sincronizó: $actionName") + _syncLogs.value
        }
    }

    // Simulated Receipt Intelligent Parser flow
    fun startTicketPhotoScan() {
        _isScanning.value = true
        _scanProgress.value = "Iniciando captura..."
        _scannedResultText.value = null
    }

    fun cancelScan() {
        _isScanning.value = false
        _scanProgress.value = null
        _scannedResultText.value = null
    }

    fun executeReceiptAiParse(rawTicketText: String) {
        viewModelScope.launch {
            _scanProgress.value = "Procesando ticket con inteligencia artificial de Gemini..."
            try {
                // Call our Gemini API helper
                val parsed = GeminiClient.parseReceiptWithAi(rawTicketText)

                // 1. Add to variable expenses list
                repository.insertExpense(
                    Expense(
                        title = "Ticket: ${parsed.store}",
                        amount = parsed.totalAmount,
                        category = parsed.category,
                        isRecurring = false
                    )
                )

                // 2. Add individual items to shopping list (marked as bought) and update stock or add direct
                parsed.items.forEach { item ->
                    // Add to shopping items list as purchased
                    repository.insertShoppingItem(
                        ShoppingItem(
                            productName = item.name,
                            quantityToBuy = item.quantity,
                            unit = item.unit,
                            estimatedPrice = item.price,
                            isBought = true,
                            targetStore = parsed.store
                        )
                    )

                    // Auto-generate or replenish inventory item
                    val inventory = repository.allInventoryItems.first()
                    val existing = inventory.find { it.name.equals(item.name, ignoreCase = true) }
                    if (existing != null) {
                        repository.updateInventoryItem(
                            existing.copy(
                                currentStock = existing.currentStock + item.quantity,
                                bestPrice = if (existing.bestPrice == null || item.price < existing.bestPrice) item.price else existing.bestPrice,
                                bestStore = if (existing.bestPrice == null || item.price < existing.bestPrice) parsed.store else existing.bestStore,
                                secondBestPrice = existing.bestPrice,
                                secondBestStore = existing.bestStore,
                                lastUpdated = System.currentTimeMillis()
                            )
                        )
                    } else {
                        // Create item in inventory with realistic parameters matching item category
                        val depletionRate = when(item.unit) {
                            "litros" -> 0.4
                            "kg" -> 0.15
                            "unidades" -> 1.5
                            "paquetes" -> 0.1
                            else -> 0.2
                        }
                        repository.insertInventoryItem(
                            InventoryItem(
                                name = item.name,
                                currentStock = item.quantity,
                                minStockAlert = item.quantity / 2.0,
                                unit = item.unit,
                                depletionRatePerDay = depletionRate,
                                bestStore = parsed.store,
                                bestPrice = item.price,
                                lastUpdated = System.currentTimeMillis()
                            )
                        )
                    }
                }

                _scanProgress.value = "¡Éxito! Gasto por $${parsed.totalAmount} cargado y ${parsed.items.size} productos incorporados a inventario."
                checkInventoryThresholds()
                addNotification("📸 Ticket '${parsed.store}' escaneado automáticamente. Se añadieron ${parsed.items.size} artículos.")
                _isScanning.value = false
            } catch (e: Exception) {
                _scanProgress.value = "Error al escanear: ${e.message}"
            }
        }
    }
}
