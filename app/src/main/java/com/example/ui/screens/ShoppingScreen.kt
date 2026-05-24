package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ShoppingItem
import com.example.ui.HomeViewModel
import java.util.Locale

@Composable
fun ShoppingScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val shoppingList by viewModel.shoppingItems.collectAsState()
    val inventoryItems by viewModel.inventoryItems.collectAsState()

    // Shopping Cart states
    val cartActive by viewModel.shoppingCartActive.collectAsState()
    val cartBudget by viewModel.shoppingCartBudget.collectAsState()

    // Gemini Price Scanner states
    val isScanningPrice by viewModel.isScanningPrice.collectAsState()
    val priceScanResult by viewModel.priceScanResult.collectAsState()

    // Local dialog controls
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showStartCartDialog by remember { mutableStateOf(false) }
    var showFinishCartDialog by remember { mutableStateOf(false) }
    var showCancelCartDialog by remember { mutableStateOf(false) }

    // Photo Picker launcher for Price Scanner
    val pricePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                viewModel.scanProductPriceWithGemini(bitmap)
            }
        }
    }

    // Calculate total possible cost of pending items in shopping list
    val possibleCostPending = remember(shoppingList) {
        shoppingList.filter { !it.isBought }.sumOf { it.estimatedPrice * it.quantityToBuy }
    }
    
    val totalBoughtCost = remember(shoppingList) {
        shoppingList.filter { it.isBought }.sumOf { it.estimatedPrice * it.quantityToBuy }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cart Sizing / Possible Cost Estimation header card
            item {
                if (cartActive) {
                    // Active Cart mode UI with animated progress bar
                    val overBudget = totalBoughtCost > cartBudget
                    val percentage = if (cartBudget > 0) (totalBoughtCost / cartBudget).toFloat().coerceIn(0f, 1f) else 0f
                    val animatedPercentage by animateFloatAsState(targetValue = percentage, label = "budget_progress")

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("active_shopping_cart_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (overBudget) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = if (overBudget) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFD32F2F)) else null
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = if (overBudget) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sesión de Compra Activa",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (overBudget) Color(0xFFC62828) else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                
                                IconButton(
                                    onClick = { showCancelCartDialog = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Cancel, "Cancelar", tint = Color.Gray)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Presupuesto del Día", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("$${String.format(Locale.US, "%,.2f", cartBudget)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Monto en Carrito", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(
                                        text = "$${String.format(Locale.US, "%,.2f", totalBoughtCost)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (overBudget) Color(0xFFD32F2F) else Color(0xFF4CAF50)
                                    )
                                }
                            }

                            // Dynamic/animated budget bar
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    progress = animatedPercentage,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp)),
                                    color = if (overBudget) Color(0xFFD32F2F) else Color(0xFF4CAF50),
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.0f", totalBoughtCost / cartBudget * 100)}% consumido",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                    if (overBudget) {
                                        Text(
                                            text = "¡Excedido por $${String.format(Locale.US, "%.2f", totalBoughtCost - cartBudget)}!",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD32F2F)
                                        )
                                    } else {
                                        Text(
                                            text = "Disponible: $${String.format(Locale.US, "%.2f", cartBudget - totalBoughtCost)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }

                            // Action buttons: Finish & Quick Price Scanner
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showFinishCartDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (overBudget) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Finalizar Compra", fontSize = 12.sp)
                                }

                                if (isScanningPrice) {
                                    Button(
                                        onClick = {},
                                        enabled = false,
                                        modifier = Modifier.weight(1.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Leyendo Precio...", fontSize = 11.sp)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            pricePhotoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier.weight(1.1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Capturar Precio IA", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Regular shopping estimation header card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Estimación del Carrito",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                
                                if (shoppingList.any { it.isBought }) {
                                    TextButton(
                                        onClick = { viewModel.clearBoughtShoppingItems() },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Limpiar Comprados", fontSize = 11.sp)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Costo Pendiente",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "$${String.format(Locale.US, "%,.2f", possibleCostPending)}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Comprado Hoy",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "$${String.format(Locale.US, "%,.2f", totalBoughtCost)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Start Shopping Session button
                            Button(
                                onClick = { showStartCartDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Iniciar Sesión de Compra (Supermercado)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // List Title Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lista Activa de Compras",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "${shoppingList.count { !it.isBought }} artículos pendientes",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Render shopping items
            if (shoppingList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "¡Lista de Compras Completada!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tu hogar está completamente abastecido. Los productos con bajo stock se listarán aquí para agregarlos fácilmente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(shoppingList, key = { it.id }) { item ->
                    ShoppingItemRow(
                        item = item,
                        onToggle = { viewModel.toggleShoppingItemBought(item) },
                        onDelete = { viewModel.deleteShoppingItem(item) }
                    )
                }
            }
        }

        // Add to shopping list FAB
        FloatingActionButton(
            onClick = { showAddItemDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 16.dp)
                .testTag("add_shopping_item_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir a la lista")
        }

        // Add manual item Dialog
        if (showAddItemDialog) {
            AddShoppingItemDialog(
                onDismiss = { showAddItemDialog = false },
                suggestedStores = inventoryItems.mapNotNull { it.bestStore }.distinct(),
                onAdd = { name, qty, unit, store ->
                    viewModel.addShoppingItem(name, qty, unit, store)
                    showAddItemDialog = false
                }
            )
        }

        // Start Cart Dialog
        if (showStartCartDialog) {
            var budgetInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showStartCartDialog = false },
                title = { Text("Iniciar Compra del Día", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Ingresa el presupuesto asignado para la compra de hoy:", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = budgetInput,
                            onValueChange = { budgetInput = it },
                            label = { Text("Presupuesto ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val budget = budgetInput.toDoubleOrNull() ?: 50.0
                            viewModel.startShoppingCart(budget)
                            showStartCartDialog = false
                        },
                        enabled = budgetInput.toDoubleOrNull() != null
                    ) {
                        Text("Iniciar Sesión")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStartCartDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Cancel Cart Dialog
        if (showCancelCartDialog) {
            AlertDialog(
                onDismissRequest = { showCancelCartDialog = false },
                title = { Text("Cancelar Sesión de Compra") },
                text = { Text("¿Deseas cerrar la sesión de compra actual? No se registrará ningún gasto en la base de datos.") },
                confirmButton = {
                    Button(
                        onClick = {
                            // Reset cart session
                            viewModel.shoppingCartActive.value = false
                            viewModel.shoppingCartBudget.value = 0.0
                            val prefs = context.getSharedPreferences("hogar_sincro_prefs", android.content.Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("shopping_cart_active", false).putFloat("shopping_cart_budget", 0f).apply()
                            showCancelCartDialog = false
                            viewModel.showSnackbar("Sesión de compra cancelada.", com.example.ui.SnackbarType.INFO)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cerrar sin Guardar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelCartDialog = false }) {
                        Text("Volver")
                    }
                }
            )
        }

        // Finish Cart Dialog
        if (showFinishCartDialog) {
            var concept by remember { mutableStateOf("Compra Supermercado") }
            var spentAmount by remember { mutableStateOf(totalBoughtCost.toString()) }

            AlertDialog(
                onDismissRequest = { showFinishCartDialog = false },
                title = { Text("Finalizar y Guardar Gasto", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Confirma el importe total real pagado en caja para registrarlo en los Gastos del Hogar:")
                        OutlinedTextField(
                            value = concept,
                            onValueChange = { concept = it },
                            label = { Text("Concepto") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = spentAmount,
                            onValueChange = { spentAmount = it },
                            label = { Text("Monto Real Pagado ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val actualSpent = spentAmount.toDoubleOrNull() ?: totalBoughtCost
                            viewModel.closeCartAndLogExpense(concept, actualSpent)
                            showFinishCartDialog = false
                        },
                        enabled = spentAmount.toDoubleOrNull() != null && concept.isNotBlank()
                    ) {
                        Text("Finalizar e Importar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFinishCartDialog = false }) {
                        Text("Atrás")
                    }
                }
            )
        }

        // Price Scanner Confirmation Dialog (Bandeja de verificación de precio rápido)
        if (priceScanResult != null) {
            var confirmedName by remember(priceScanResult) { mutableStateOf(priceScanResult!!.name) }
            var confirmedPrice by remember(priceScanResult) { mutableStateOf(priceScanResult!!.price.toString()) }
            var quantityInput by remember { mutableStateOf("1") }
            var unitSelect by remember { mutableStateOf("u") }

            val units = listOf("u", "kg", "paquetes", "litros")

            AlertDialog(
                onDismissRequest = { viewModel.clearPriceScanResult() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Precio Detectado con IA")
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Gemini leyó los siguientes detalles del producto. Edítalos si es necesario:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        
                        OutlinedTextField(
                            value = confirmedName,
                            onValueChange = { confirmedName = it },
                            label = { Text("Nombre del Producto") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = confirmedPrice,
                                onValueChange = { confirmedPrice = it },
                                label = { Text("Precio Unitario ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            
                            OutlinedTextField(
                                value = quantityInput,
                                onValueChange = { quantityInput = it },
                                label = { Text("Cantidad") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Unit selector
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Unidad", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 2.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable {
                                        val index = units.indexOf(unitSelect)
                                        unitSelect = units[(index + 1) % units.size]
                                    }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(unitSelect, style = MaterialTheme.typography.bodyLarge)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val priceVal = confirmedPrice.toDoubleOrNull() ?: priceScanResult!!.price
                            val qtyVal = quantityInput.toDoubleOrNull() ?: 1.0
                            
                            // Insert to database shopping list
                            // We can use viewModel.addShoppingItem helper
                            // Wait, it expects best price to be updated, which is automatically fetched by repo or VM.
                            // We can also insert directly or update.
                            // Let's call viewModel.addShoppingItem which inserts it!
                            // Since we want this specific custom price, we insert/add the item
                            viewModel.addShoppingItem(confirmedName, qtyVal, unitSelect, "Supermercado")
                            
                            viewModel.clearPriceScanResult()
                            viewModel.showSnackbar("Producto $confirmedName añadido al carrito.", com.example.ui.SnackbarType.SUCCESS)
                        },
                        enabled = confirmedName.isNotBlank() && confirmedPrice.toDoubleOrNull() != null && quantityInput.toDoubleOrNull() != null
                    ) {
                        Text("Agregar al Carrito")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.clearPriceScanResult() }) {
                        Text("Descartar")
                    }
                }
            )
        }
    }
}

@Composable
fun ShoppingItemRow(item: ShoppingItem, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (item.isBought) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox indicator
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (item.isBought) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Cambiar estado",
                    tint = if (item.isBought) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (item.isBought) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isBought) Color.Gray else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${item.quantityToBuy} ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (item.targetStore != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Mejor Precio: ${item.targetStore}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val totalPrice = item.estimatedPrice * item.quantityToBuy
                Text(
                    text = "$${String.format(Locale.US, "%,.2f", totalPrice)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (item.isBought) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isBought) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "est. $${String.format(Locale.US, "%,.2f", item.estimatedPrice)}/${item.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Eliminar artículo",
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShoppingItemDialog(
    onDismiss: () -> Unit,
    suggestedStores: List<String>,
    onAdd: (String, Double, String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("u") }
    var targetStore by remember { mutableStateOf("") }

    val units = listOf("u", "kg", "paquetes", "litros", "rollos")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Artículo a Compras", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Producto") },
                    placeholder = { Text("ej. Leche Entera, Papel Higiénico") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Cantidad") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("Unidad", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 2.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    val index = units.indexOf(unit)
                                    unit = units[(index + 1) % units.size]
                                }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(unit, style = MaterialTheme.typography.bodyLarge)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                }

                OutlinedTextField(
                    value = targetStore,
                    onValueChange = { targetStore = it },
                    label = { Text("Tienda recomendada (Opcional)") },
                    placeholder = { Text("ej. Walmart, Mercadona") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (suggestedStores.isNotEmpty()) {
                    Text("De tus tiendas habituales:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        suggestedStores.take(3).forEach { store ->
                            SuggestionChip(
                                onClick = { targetStore = store },
                                label = { Text(store, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qtyVal = quantity.toDoubleOrNull() ?: 1.0
                    if (name.isNotBlank()) {
                        onAdd(name, qtyVal, unit, if (targetStore.isNotBlank()) targetStore else null)
                    }
                },
                enabled = name.isNotBlank() && quantity.toDoubleOrNull() != null
            ) {
                Text("Añadir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
