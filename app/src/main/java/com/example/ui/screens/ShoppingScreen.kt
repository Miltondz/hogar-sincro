package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ShoppingItem
import com.example.ui.HomeViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
            val bitmap = decodeSampledBitmapFromUri(context, uri)
            if (bitmap != null) {
                viewModel.scanProductPriceWithGemini(bitmap)
            } else {
                viewModel.showSnackbar("No se pudo leer la imagen.", com.example.ui.SnackbarType.ERROR)
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
                        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp)).testTag("active_shopping_cart_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (overBudget) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
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
                                        tint = if (overBudget) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Compra Activa",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (overBudget) Color(0xFFC62828) else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (overBudget) Color(0xFFC62828) else MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Ppto: $${String.format(Locale.US, "%.2f", cartBudget)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "Gastado Real",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (overBudget) Color(0xFFC62828).copy(alpha = 0.8f) else Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "$${String.format(Locale.US, "%,.2f", totalBoughtCost)}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = if (overBudget) Color(0xFFC62828) else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                
                                val remaining = cartBudget - totalBoughtCost
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (overBudget) "Excedido por" else "Disponible",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (overBudget) Color(0xFFC62828).copy(alpha = 0.8f) else Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "$${String.format(Locale.US, "%,.2f", kotlin.math.abs(remaining))}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = if (overBudget) Color(0xFFC62828) else Color(0xFF2E7D32)
                                    )
                                }
                            }
                            
                            // Budget progress bar
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    progress = { animatedPercentage },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp)),
                                    color = if (overBudget) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Add button to launch Gemini price scanner
                                FilledTonalButton(
                                    onClick = { pricePhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    modifier = Modifier.weight(1.2f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isScanningPrice
                                ) {
                                    if (isScanningPrice) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Escaneando...", fontSize = 12.sp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Escanear Precio (IA)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Button(
                                    onClick = { showFinishCartDialog = true },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Terminar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                IconButton(
                                    onClick = { showCancelCartDialog = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        }
                    }
                } else {
                    // Standard budget forecasting preview card
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Costo Estimado Pendiente",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$${String.format(Locale.US, "%,.2f", possibleCostPending)}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Start Shopping Session button
                            Button(
                                onClick = { showStartCartDialog = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Iniciar Sesión de Compra", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // List Title Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Render shopping items
            if (shoppingList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(24.dp),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "¡Todo Abastecido!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tu hogar está completamente al día. Los productos con stock bajo o agregados de la despensa se mostrarán aquí.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
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

        // Add manual item Dialog (Responsive custom design)
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

        // Start Cart Dialog (Single OutlinedTextField)
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
                        enabled = budgetInput.toDoubleOrNull() != null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Iniciar Sesión")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStartCartDialog = false }) {
                        Text("Cancelar")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Cancel Cart Dialog
        if (showCancelCartDialog) {
            AlertDialog(
                onDismissRequest = { showCancelCartDialog = false },
                title = { Text("Cancelar Compra", fontWeight = FontWeight.Bold) },
                text = { Text("¿Deseas cerrar la sesión de compra actual? No se registrará ningún gasto en la base de datos.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.cancelShoppingCart()
                            showCancelCartDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cerrar sin Guardar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelCartDialog = false }) {
                        Text("Volver")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Finish Cart Dialog (Mobile Optimized, scrollable)
        if (showFinishCartDialog) {
            var concept by remember { mutableStateOf("Compra Supermercado") }
            var spentAmount by remember { mutableStateOf(totalBoughtCost.toString()) }

            Dialog(
                onDismissRequest = { showFinishCartDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(0.75f)
                        .imePadding(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        // Header
                        Text(
                            text = "Finalizar y Guardar Gasto",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Scrollable Body
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Confirma el importe total real pagado en caja para registrarlo en los Gastos del Hogar:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            
                            OutlinedTextField(
                                value = concept,
                                onValueChange = { concept = it },
                                label = { Text("Concepto") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            OutlinedTextField(
                                value = spentAmount,
                                onValueChange = { spentAmount = it },
                                label = { Text("Monto Real Pagado ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Footer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showFinishCartDialog = false }) {
                                Text("Atrás")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val actualSpent = spentAmount.toDoubleOrNull() ?: totalBoughtCost
                                    viewModel.closeCartAndLogExpense(concept, actualSpent)
                                    showFinishCartDialog = false
                                },
                                enabled = spentAmount.toDoubleOrNull() != null && concept.isNotBlank(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Finalizar e Importar")
                            }
                        }
                    }
                }
            }
        }

        // Price Scanner Confirmation Dialog (AI Price Confirmation - Mobile Optimized)
        if (priceScanResult != null) {
            var confirmedName by remember(priceScanResult) { mutableStateOf(priceScanResult!!.name) }
            var confirmedPrice by remember(priceScanResult) { mutableStateOf(priceScanResult!!.price.toString()) }
            var quantityInput by remember { mutableStateOf("1") }
            var unitSelect by remember { mutableStateOf("u") }

            val units = listOf("u", "kg", "paquetes", "litros")

            Dialog(
                onDismissRequest = { viewModel.clearPriceScanResult() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .imePadding(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Precio Detectado con IA",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Scrollable Body
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Gemini leyó los siguientes detalles del producto. Edítalos si es necesario:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            
                            OutlinedTextField(
                                value = confirmedName,
                                onValueChange = { confirmedName = it },
                                label = { Text("Nombre del Producto") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
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
                                Text(
                                    "Unidad",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
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
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(unitSelect, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Footer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { viewModel.clearPriceScanResult() }) {
                                Text("Descartar")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val priceVal = confirmedPrice.toDoubleOrNull() ?: priceScanResult!!.price
                                    val qtyVal = quantityInput.toDoubleOrNull() ?: 1.0
                                    viewModel.addShoppingItem(confirmedName, qtyVal, unitSelect, "Supermercado")
                                    viewModel.clearPriceScanResult()
                                    viewModel.showSnackbar("Producto agregado desde ticket IA.", com.example.ui.SnackbarType.SUCCESS)
                                },
                                enabled = confirmedName.isNotBlank() && confirmedPrice.toDoubleOrNull() != null && quantityInput.toDoubleOrNull() != null,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Aceptar y Añadir")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isBought) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isBought) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = item.isBought,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF2E7D32)
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.titleMedium,
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
                        text = "${
                            if (item.quantityToBuy % 1.0 == 0.0) 
                                item.quantityToBuy.toInt().toString() 
                            else 
                                String.format(Locale.US, "%.1f", item.quantityToBuy)
                        } ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.isBought) Color.Gray else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (item.targetStore != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (item.isBought) 
                                        Color.LightGray.copy(alpha = 0.2f) 
                                    else 
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = if (item.isBought) Color.Gray else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.targetStore,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = if (item.isBought) Color.Gray else MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .imePadding(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = "Añadir Artículo a Compras",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Cantidad") },
                            placeholder = { Text("1") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Unit Selector Button
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                "Unidad",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedButton(
                                onClick = {
                                    val index = units.indexOf(unit)
                                    unit = units[(index + 1) % units.size]
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(unit, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = targetStore,
                        onValueChange = { targetStore = it },
                        label = { Text("Tienda Recomendada (Opcional)") },
                        placeholder = { Text("ej. Jumbo, Walmart") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Suggested stores chips (scrollable and responsive to avoid cut-offs)
                    if (suggestedStores.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "De tus tiendas habituales:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                suggestedStores.forEach { store ->
                                    SuggestionChip(
                                        onClick = { targetStore = store },
                                        label = { Text(store, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qtyVal = quantity.toDoubleOrNull() ?: 1.0
                            if (name.isNotBlank()) {
                                onAdd(name, qtyVal, unit, if (targetStore.isNotBlank()) targetStore else null)
                            }
                        },
                        enabled = name.isNotBlank() && (quantity.isBlank() || quantity.toDoubleOrNull() != null),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Añadir")
                    }
                }
            }
        }
    }
}
