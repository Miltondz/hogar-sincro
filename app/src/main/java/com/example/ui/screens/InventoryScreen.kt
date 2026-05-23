package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InventoryItem
import com.example.ui.HomeViewModel
import java.util.Locale

@Composable
fun InventoryScreen(viewModel: HomeViewModel) {
    val items by viewModel.inventoryItems.collectAsState()
    val lowStockItems by viewModel.lowStockItems.collectAsState()

    var showAddItemDialog by remember { mutableStateOf(false) }

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
            // Low Stock Overview Card
            if (lowStockItems.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), // Soft red
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Alertas de Stock Bajo (${lowStockItems.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Hay artículos de primera necesidad que han caído por debajo del margen mínimo establecido. Agrégalos a tu lista de compras para asegurar reabastecimiento.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD32F2F).copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Inventario y Despensa Inteligente",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "${items.size} Artículos en total",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Products list
            if (items.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Inventario Vacío", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Agrega los productos habituales de tu despensa para llevar el registro del consumo diario automatizado.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            } else {
                items(items, key = { it.id }) { item ->
                    InventoryItemRow(
                        item = item,
                        onUpdateStock = { newAmt -> viewModel.updateInventoryStock(item, newAmt) },
                        onAddToShoppingList = { qty -> viewModel.addShoppingItem(item.name, qty, item.unit) },
                        onDelete = { viewModel.deleteInventoryItem(item) }
                    )
                }
            }
        }

        // Add to inventory FAB
        FloatingActionButton(
            onClick = { showAddItemDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 16.dp)
                .testTag("add_inventory_item_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir Producto")
        }

        if (showAddItemDialog) {
            AddInventoryItemDialog(
                onDismiss = { showAddItemDialog = false },
                onAdd = { name, stock, minStock, unit, depletion, store, price ->
                    viewModel.addInventoryItem(name, stock, minStock, unit, depletion, store, price)
                    showAddItemDialog = false
                }
            )
        }
    }
}

@Composable
fun InventoryItemRow(
    item: InventoryItem,
    onUpdateStock: (Double) -> Unit,
    onAddToShoppingList: (Double) -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = item.currentStock <= item.minStockAlert
    val isSoonDepleting = item.daysUntilDepletion in 1..4

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isLowStock) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFFCDD2))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "STOCK BAJO",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = "Consumo: ${item.depletionRatePerDay} ${item.unit}/día",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Interactive Stock Controller
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { if (item.currentStock >= 0.1) onUpdateStock(Math.max(0.0, String.format(Locale.US, "%.2f", item.currentStock - 0.2).toDouble())) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Menos stock", modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = "${item.currentStock} ${item.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    IconButton(
                        onClick = { onUpdateStock(String.format(Locale.US, "%.2f", item.currentStock + 0.2).toDouble()) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Más stock", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Depletion Gauge Indicator
            val percentage = if (item.currentStock > 0 && item.minStockAlert > 0) {
                val ratio = (item.currentStock / (item.minStockAlert * 3)).toFloat()
                ratio.coerceIn(0f, 1f)
            } else 0f

            val gaugeColor = when {
                isLowStock -> Color(0xFFD32F2F)      // Red
                isSoonDepleting -> Color(0xFFFF9800) // Orange
                else -> Color(0xFF4CAF50)            // Green
            }

            LinearProgressIndicator(
                progress = percentage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = gaugeColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Depletion Counter Info & Prices comparative row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Depletion estimation statement
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = gaugeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            item.daysUntilDepletion > 30 -> "Abastecido (~${item.daysUntilDepletion} días)"
                            item.daysUntilDepletion > 0 -> "Se agota en aprox. ${item.daysUntilDepletion} días"
                            item.currentStock == 0.0 -> "¡AGOTADO!"
                            else -> "Uso ocasional"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = gaugeColor
                    )
                }

                if (isLowStock) {
                    TextButton(
                        onClick = { onAddToShoppingList(item.minStockAlert * 3 - item.currentStock) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Añadir a Compras", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // BEST STORES CATALOGUE DISPLAY
            if (item.bestStore != null && item.bestPrice != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "🏷️ Historial de Precios Habituales:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.bestStore}: ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", item.bestPrice)}/${item.unit} (Mejor)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (item.secondBestStore != null && item.secondBestPrice != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${item.secondBestStore}: ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", item.secondBestPrice)}/${item.unit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddInventoryItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, Double, String, Double, String?, Double?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var minStock by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("u") }
    var depletionRate by remember { mutableStateOf("") }
    var bestStore by remember { mutableStateOf("") }
    var bestPrice by remember { mutableStateOf("") }

    val units = listOf("u", "kg", "paquetes", "litros", "rollos")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Producto en Despensa", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del Producto") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = stock,
                            onValueChange = { stock = it },
                            label = { Text("Stock Actual") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = minStock,
                            onValueChange = { minStock = it },
                            label = { Text("Stock Mínimo") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Unit representation selection
                        Column(modifier = Modifier.weight(1f)) {
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

                        OutlinedTextField(
                            value = depletionRate,
                            onValueChange = { depletionRate = it },
                            label = { Text("Uso Diario Promedio") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = bestStore,
                        onValueChange = { bestStore = it },
                        label = { Text("Tienda con mejor precio") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = bestPrice,
                        onValueChange = { bestPrice = it },
                        label = { Text("Precio Unitario ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stockVal = stock.toDoubleOrNull() ?: 0.0
                    val minVal = minStock.toDoubleOrNull() ?: 1.0
                    val rateVal = depletionRate.toDoubleOrNull() ?: 0.1
                    val priceVal = bestPrice.toDoubleOrNull()

                    if (name.isNotBlank()) {
                        onAdd(
                            name, 
                            stockVal, 
                            minVal, 
                            unit, 
                            rateVal, 
                            if (bestStore.isNotBlank()) bestStore else null, 
                            priceVal
                        )
                    }
                },
                enabled = name.isNotBlank() && stock.toDoubleOrNull() != null
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
