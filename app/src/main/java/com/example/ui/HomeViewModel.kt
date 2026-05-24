package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.api.RetrofitClient
import kotlinx.coroutines.channels.Channel
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

    // Larder scan state flows
    val isScanningLarder = MutableStateFlow(false)
    val larderScanResult = MutableStateFlow<List<ExtractedLarderItem>?>(null)

    // Price scan state flows
    val isScanningPrice = MutableStateFlow(false)
    val priceScanResult = MutableStateFlow<ExtractedProductPrice?>(null)

    // Shopping Cart state flows
    val shoppingCartActive = MutableStateFlow(prefs.getBoolean("shopping_cart_active", false))
    val shoppingCartBudget = MutableStateFlow(prefs.getFloat("shopping_cart_budget", 0f).toDouble())

    init {
        viewModelScope.launch {
            // Clean state and configure members on startup
            repository.prepopulateIfEmpty()
            generateSmartAlerts()
            // Pull/push with Neon Postgres on startup
            syncWithNeon()
        }
    }

    // SQL Over Neon Postgres Cloud Sincro Action
    fun syncWithNeon() {
        viewModelScope.launch {
            neonConnectionState.value = "CONNECTING"
            val syncOk = ApiSyncService.syncAll(
                getApplication(),
                repository.expenseDao,
                repository.inventoryDao,
                repository.shoppingDao
            )
            
            if (syncOk) {
                neonConnectionState.value = "CONNECTED"
                addSyncActivity("SISTEMA", "Sincronización bidireccional completada exitosamente.", "SISTEMA")
                addToastNotification("Sincronización Cloud", "Datos del hogar actualizados con la API REST de Neon.")
                showSnackbar("Sincronización con Neon completada ✓", SnackbarType.SUCCESS)
            } else {
                neonConnectionState.value = "ERROR"
                addSyncActivity("SISTEMA", "Fallo de comunicación con el servidor API de sincronización.", "SISTEMA")
                showSnackbar("Sin conexión a Neon. Modo offline activo.", SnackbarType.ERROR)
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

            // 1. Alert only when item is completely depleted (stock = 0)
            items.forEach { item ->
                if (item.currentStock == 0.0) {
                    alerts.add(
                        SmartNotification(
                            id = "depleted_${item.id}",
                            title = "Agotado: ${item.name}",
                            message = "${item.name} está completamente agotado. Agrégalo a tu lista de compras.",
                            type = "ALERTA",
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

    // Real-time sync simulation DISABLED — data is only created by the user or via Gemini AI scanning
    // No mock data is generated automatically

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

    // Snackbar feedback system
    private val _snackbarEvent = Channel<SnackbarEvent>(Channel.BUFFERED)
    val snackbarEvents = _snackbarEvent.receiveAsFlow()

    fun showSnackbar(message: String, type: SnackbarType = SnackbarType.INFO) {
        viewModelScope.launch {
            _snackbarEvent.send(SnackbarEvent(message, type))
        }
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
            showSnackbar("Gasto registrado correctamente", SnackbarType.SUCCESS)
            addSyncActivity(activeUser, "Agregó gasto de $$amount: $title", "GASTOS")
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            DeletionTracker.trackExpenseDeletion(getApplication(), expense.id)
            repository.deleteExpense(expense)
            generateSmartAlerts()
            showSnackbar("Gasto eliminado", SnackbarType.WARNING)
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
            showSnackbar("$name añadido a la lista de compras", SnackbarType.SUCCESS)
            val activeUser = syncSettings.value.activeUser
            addSyncActivity(activeUser, "Agregó '${name}' (${qty} ${unit}) a compras", "COMPRAS")
        }
    }

    // Bulk add from pantry selection (checkbox list)
    fun addInventoryItemsToShoppingList(selections: List<Pair<InventoryItem, Double>>) {
        viewModelScope.launch {
            val activeUser = syncSettings.value.activeUser
            var count = 0
            selections.forEach { (invItem, qty) ->
                if (qty > 0) {
                    val item = ShoppingItem(
                        productName = invItem.name,
                        quantityToBuy = qty,
                        unit = invItem.unit,
                        estimatedPrice = invItem.bestPrice ?: 1.50,
                        targetStore = invItem.bestStore
                    )
                    repository.insertShoppingItem(item)
                    count++
                }
            }
            if (count > 0) {
                showSnackbar("$count artículo(s) añadidos a la lista de compras ✓", SnackbarType.SUCCESS)
                addSyncActivity(activeUser, "Añadió $count artículos de despensa a lista de compras", "COMPRAS")
            }
        }
    }

    fun toggleShoppingItemBought(item: ShoppingItem) {
        viewModelScope.launch {
            val updated = item.copy(isBought = !item.isBought)
            repository.updateShoppingItem(updated)
            
            if (updated.isBought) {
                showSnackbar("${item.productName} marcado como comprado ✓", SnackbarType.SUCCESS)
            } else {
                showSnackbar("${item.productName} desmarcado", SnackbarType.INFO)
            }

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
            showSnackbar("Artículo removido de la lista", SnackbarType.WARNING)
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
            showSnackbar("$name añadido al inventario", SnackbarType.SUCCESS)
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
              showSnackbar("${item.name} eliminado del inventario", SnackbarType.WARNING)
         }
    }

    fun quickConsumeItem(item: InventoryItem) {
        viewModelScope.launch {
            val newStock = Math.max(0.0, item.currentStock - 1.0)
            val updated = item.copy(currentStock = newStock, lastUpdated = System.currentTimeMillis())
            repository.updateInventoryItem(updated)
            generateSmartAlerts()
            showSnackbar("Consumo registrado para ${item.name} (-1 ${item.unit})", SnackbarType.SUCCESS)
            val activeUser = syncSettings.value.activeUser
            addSyncActivity(activeUser, "Consumió 1 ${item.unit} de '${item.name}' (Stock actual: $newStock)", "INVENTARIO")
        }
    }

    fun archiveInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            val updated = item.copy(isArchived = true, lastUpdated = System.currentTimeMillis())
            repository.updateInventoryItem(updated)
            generateSmartAlerts()
            showSnackbar("${item.name} archivado correctamente", SnackbarType.INFO)
            val activeUser = syncSettings.value.activeUser
            addSyncActivity(activeUser, "Archivó el producto '${item.name}' de la despensa", "INVENTARIO")
        }
    }

    fun editInventoryItem(
        item: InventoryItem,
        name: String,
        stock: Double,
        limit: Double,
        unit: String,
        depletionRate: Double,
        store: String?,
        price: Double?
    ) {
        viewModelScope.launch {
            val updated = item.copy(
                name = name,
                currentStock = stock,
                minStockAlert = limit,
                unit = unit,
                depletionRatePerDay = depletionRate,
                bestStore = store,
                bestPrice = price,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateInventoryItem(updated)
            generateSmartAlerts()
            showSnackbar("Producto '${name}' actualizado", SnackbarType.SUCCESS)
            val activeUser = syncSettings.value.activeUser
            addSyncActivity(activeUser, "Editó el producto '${name}' en la despensa", "INVENTARIO")
        }
    }

    fun scanLarderWithGemini(bitmap: Bitmap) {
        viewModelScope.launch {
            isScanningLarder.value = true
            try {
                val activeModel = syncSettings.value.geminiModel
                val result = GeminiScannerService.scanLarder(bitmap, activeModel)
                larderScanResult.value = result.items
                showSnackbar("Alacena analizada. Confirma los productos.", SnackbarType.SUCCESS)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error scanning larder", e)
                showSnackbar("Error al escanear la alacena.", SnackbarType.ERROR)
            } finally {
                isScanningLarder.value = false
            }
        }
    }

    fun clearLarderScanResult() {
        larderScanResult.value = null
    }

    fun commitLarderScanItems(items: List<ExtractedLarderItem>) {
        viewModelScope.launch {
            val existing = repository.allInventoryItems.first()
            items.forEach { scanItem ->
                val match = existing.find { it.name.equals(scanItem.name, ignoreCase = true) }
                if (match != null) {
                    val updated = match.copy(
                        currentStock = match.currentStock + scanItem.quantity,
                        lastUpdated = System.currentTimeMillis()
                    )
                    repository.updateInventoryItem(updated)
                } else {
                    val newItem = InventoryItem(
                        name = scanItem.name,
                        currentStock = scanItem.quantity,
                        minStockAlert = 1.0,
                        unit = scanItem.unit,
                        depletionRatePerDay = 0.1,
                        lastUpdated = System.currentTimeMillis()
                    )
                    repository.insertInventoryItem(newItem)
                }
            }
            clearLarderScanResult()
            generateSmartAlerts()
            showSnackbar("Productos incorporados a la despensa.", SnackbarType.SUCCESS)
            val activeUser = syncSettings.value.activeUser
            addSyncActivity(activeUser, "Escaneó e incorporó productos a la despensa mediante foto de alacena.", "INVENTARIO")
        }
    }

    fun scanProductPriceWithGemini(bitmap: Bitmap) {
        viewModelScope.launch {
            isScanningPrice.value = true
            try {
                val activeModel = syncSettings.value.geminiModel
                val result = GeminiScannerService.scanProductPrice(bitmap, activeModel)
                priceScanResult.value = result
                showSnackbar("Precio de producto extraído con éxito.", SnackbarType.SUCCESS)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error scanning product price", e)
                showSnackbar("Error al extraer precio del producto.", SnackbarType.ERROR)
            } finally {
                isScanningPrice.value = false
            }
        }
    }

    fun clearPriceScanResult() {
        priceScanResult.value = null
    }

    fun startShoppingCart(budget: Double) {
        prefs.edit().apply {
            putBoolean("shopping_cart_active", true)
            putFloat("shopping_cart_budget", budget.toFloat())
            apply()
        }
        shoppingCartActive.value = true
        shoppingCartBudget.value = budget
        showSnackbar("Sesión de Carrito iniciada con $$budget", SnackbarType.SUCCESS)
    }

    fun cancelShoppingCart() {
        prefs.edit().apply {
            putBoolean("shopping_cart_active", false)
            putFloat("shopping_cart_budget", 0f)
            apply()
        }
        shoppingCartActive.value = false
        shoppingCartBudget.value = 0.0
        showSnackbar("Sesión de compra cancelada.", SnackbarType.INFO)
    }

    fun closeCartAndLogExpense(concept: String, actualSpent: Double) {
        viewModelScope.launch {
            val activeUser = syncSettings.value.activeUser
            val exp = Expense(
                title = concept,
                amount = actualSpent,
                category = "Alimentos",
                timestamp = System.currentTimeMillis(),
                paidBy = activeUser
            )
            repository.insertExpense(exp)
            repository.deleteBoughtShoppingItems()

            prefs.edit().apply {
                putBoolean("shopping_cart_active", false)
                putFloat("shopping_cart_budget", 0f)
                apply()
            }
            shoppingCartActive.value = false
            shoppingCartBudget.value = 0.0

            generateSmartAlerts()
            showSnackbar("Compra finalizada e incorporada a Gastos ($$actualSpent).", SnackbarType.SUCCESS)
            addSyncActivity(activeUser, "Finalizó compra del supermercado: $concept por $$actualSpent", "COMPRAS")
        }
    }

    fun clearFullFamilyDatabase() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.clearCloudDatabase()
                if (response.isSuccessful) {
                    Log.i("HomeViewModel", "Neon cloud database wiped successfully via API.")
                } else {
                    Log.e("HomeViewModel", "Neon cloud database wipe failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error connecting to wipe API", e)
            }

            repository.clearAllLocalData()

            val resetSettings = SyncSettings(
                activeUser = "Milton",
                members = "Milton,Alejandra",
                householdCode = "HOGAR-5892"
            )
            repository.saveSyncSettings(resetSettings)
            syncWithNeon()
            
            generateSmartAlerts()
            showSnackbar("Toda la base de datos del hogar ha sido limpiada.", SnackbarType.WARNING)
            addSyncActivity("SISTEMA", "Limpió por completo la base de datos del hogar.", "SISTEMA")
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

    fun changeGeminiModel(modelName: String) {
        viewModelScope.launch {
            val updated = syncSettings.value.copy(geminiModel = modelName)
            repository.saveSyncSettings(updated)
            addSyncActivity(syncSettings.value.activeUser, "Cambió modelo de IA a $modelName", "CONFIGURACIÓN")
        }
    }

    // Ticket automatic OCR Scanning Action
    fun scanTicketWithGemini(bitmap: Bitmap?, sampleType: String?) {
        if (bitmap == null) {
            showSnackbar("No se pudo cargar la imagen. Intenta de nuevo.", SnackbarType.ERROR)
            return
        }
        // Capture bitmap strongly before launching coroutine to prevent GC
        val capturedBitmap = bitmap
        viewModelScope.launch {
            // Clear any previous result FIRST so UI resets to loading state
            scanResult.value = null
            isScanning.value = true
            try {
                val modelToUse = syncSettings.value.geminiModel
                val receipt = GeminiScannerService.scanReceipt(capturedBitmap, modelName = modelToUse, sampleType = sampleType)
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
                showSnackbar("Ticket de ${receipt.storeName} importado ✓ — $${String.format("%.2f", receipt.totalAmount)}", SnackbarType.SUCCESS)
                val activeUser = syncSettings.value.activeUser
                addSyncActivity(activeUser, "Escaneó ticket de compras de '${receipt.storeName}' por $${receipt.totalAmount}.", "SCANNER")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Scanner exception during processing: ${e.message}", e)
                showSnackbar("Error al escanear: ${e.message?.take(60) ?: "Error desconocido"}", SnackbarType.ERROR)
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
            showSnackbar("¡Bienvenido, $activeUser!", SnackbarType.SUCCESS)
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
            showSnackbar("Hogar ${householdCode.uppercase()} registrado correctamente", SnackbarType.SUCCESS)
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

    fun scanTicketFeedback(storeName: String, total: Double) {
        showSnackbar("Ticket de $storeName ($${String.format("%.2f", total)}) escaneado ✓", SnackbarType.SUCCESS)
    }

    fun scanErrorFeedback(errorMsg: String) {
        showSnackbar("Error al escanear: $errorMsg", SnackbarType.ERROR)
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

enum class SnackbarType { SUCCESS, ERROR, WARNING, INFO }

data class SnackbarEvent(
    val message: String,
    val type: SnackbarType = SnackbarType.INFO
)
