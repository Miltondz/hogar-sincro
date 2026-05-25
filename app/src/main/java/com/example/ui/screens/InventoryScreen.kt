package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.ExtractedLarderItem
import com.example.data.InventoryItem
import com.example.ui.HomeViewModel
import com.example.ui.theme.StockDepleted
import com.example.ui.theme.StockOk
import java.io.File
import java.util.Locale

@Composable
fun InventoryScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val items by viewModel.inventoryItems.collectAsState()
    val depletedItems by viewModel.lowStockItems.collectAsState() // stock = 0

    val isScanningLarder by viewModel.isScanningLarder.collectAsState()
    val larderScanResult by viewModel.larderScanResult.collectAsState()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<InventoryItem?>(null) }
    var itemToDelete by remember { mutableStateOf<InventoryItem?>(null) }
    var showLarderSourceDialog by remember { mutableStateOf(false) }

    // Checkbox selection state: itemId -> quantity to buy
    val selectedForShopping = remember { mutableStateMapOf<Int, Double>() }
    val hasSelection = selectedForShopping.isNotEmpty()

    // Stable camera URI for larder scanner
    val cameraPhotoUri = remember {
        val dir = File(context.cacheDir, "camera_photos").also { it.mkdirs() }
        val file = File(dir, "larder_capture.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // Gallery launcher for larder
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bmp = decodeSampledBitmapFromUri(context, uri)
            if (bmp != null) viewModel.scanLarderWithGemini(bmp)
            else viewModel.showSnackbar("No se pudo leer la imagen.", com.example.ui.SnackbarType.ERROR)
        }
    }

    // Camera launcher for larder
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            val bmp = decodeSampledBitmapFromFile(
                File(context.cacheDir, "camera_photos/larder_capture.jpg").absolutePath
            )
            if (bmp != null) viewModel.scanLarderWithGemini(bmp)
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(cameraPhotoUri)
        else viewModel.showSnackbar("Permiso de cámara requerido.", com.example.ui.SnackbarType.ERROR)
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(cameraPhotoUri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera/Gallery chooser dialog for larder scanner
    if (showLarderSourceDialog) {
        AlertDialog(
            onDismissRequest = { showLarderSourceDialog = false },
            title = { Text("Escanear Alacena", fontWeight = FontWeight.Bold) },
            text = { Text("¿Cómo quieres tomar la foto de tu despensa?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLarderSourceDialog = false
                        launchCamera()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cámara")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showLarderSourceDialog = false
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Galería")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = if (hasSelection) 140.dp else 88.dp,
                start = 16.dp, end = 16.dp, top = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Depleted alert (stock = 0 only) ──────────────────────────────
            if (depletedItems.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${depletedItems.size} artículo(s) agotado(s)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    depletedItems.joinToString(", ") { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // ── Action buttons row: Scan + Add ────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showLarderSourceDialog = true },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isScanningLarder
                    ) {
                        if (isScanningLarder) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("Analizando...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Escanear Alacena", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { showAddItemDialog = true },
                        modifier = Modifier.weight(1f).height(50.dp).testTag("add_inventory_item_fab"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Agregar Manual", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Section header ────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Despensa (${items.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (hasSelection) {
                        Text(
                            "${selectedForShopping.size} seleccionados",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Empty state ───────────────────────────────────────────────────
            if (items.isEmpty()) {
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
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Inventory2, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary, 
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Despensa Vacía", 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "¿Tu alacena está vacía? Escanea tu despensa con Inteligencia Artificial para cargarla en segundos o añade productos manualmente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            } else {
                // ── Compact inventory list ────────────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column {
                            items.forEachIndexed { index, item ->
                                CompactInventoryRow(
                                    item = item,
                                    isChecked = selectedForShopping.containsKey(item.id),
                                    checkedQty = selectedForShopping[item.id] ?: 1.0,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedForShopping[item.id] = 1.0
                                        else selectedForShopping.remove(item.id)
                                    },
                                    onQtyChange = { qty -> selectedForShopping[item.id] = qty },
                                    onStockDecrease = {
                                        val newStock = maxOf(0.0, item.currentStock - 1.0)
                                        viewModel.updateInventoryStock(item, newStock)
                                    },
                                    onStockIncrease = {
                                        viewModel.updateInventoryStock(item, item.currentStock + 1.0)
                                    },
                                    onEditClick = { itemToEdit = item },
                                    onArchiveClick = { viewModel.archiveInventoryItem(item) },
                                    onDeleteClick = { itemToDelete = item }
                                )
                                if (index < items.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Sticky bottom bar when items are selected ─────────────────────────
        AnimatedVisibility(
            visible = hasSelection,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().shadow(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedForShopping.size} artículo(s) seleccionado(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { selectedForShopping.clear() }
                    ) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val selections = selectedForShopping.mapNotNull { (id, qty) ->
                                items.find { it.id == id }?.let { it to qty }
                            }
                            viewModel.addInventoryItemsToShoppingList(selections)
                            selectedForShopping.clear()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Añadir a Compras", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Dialogs ───────────────────────────────────────────────────────────
        if (showAddItemDialog) {
            AddInventoryItemDialog(
                onDismiss = { showAddItemDialog = false },
                onAdd = { name, stock, minStock, unit, depletion, store, price ->
                    viewModel.addInventoryItem(name, stock, minStock, unit, depletion, store, price)
                    showAddItemDialog = false
                }
            )
        }

        if (itemToEdit != null) {
            EditInventoryItemDialog(
                item = itemToEdit!!,
                onDismiss = { itemToEdit = null },
                onConfirm = { name, stock, minStock, unit, depletion, store, price ->
                    viewModel.editInventoryItem(itemToEdit!!, name, stock, minStock, unit, depletion, store, price)
                    itemToEdit = null
                }
            )
        }

        if (itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Eliminar Producto", fontWeight = FontWeight.Bold) },
                text = { Text("¿Eliminar permanentemente '${itemToDelete!!.name}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteInventoryItem(itemToDelete!!)
                            itemToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) { Text("Cancelar") }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Larder scan verification tray
        if (larderScanResult != null) {
            val allItems = larderScanResult ?: emptyList()
            var checkedNames by remember(larderScanResult) {
                mutableStateOf(allItems.map { it.name }.toSet())
            }
            var displayList by remember(larderScanResult) {
                mutableStateOf(allItems)
            }
            AlertDialog(
                onDismissRequest = { viewModel.clearLarderScanResult() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Alimentos Detectados", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                        Text(
                            "Marca los productos que tenés en stock.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(displayList.size) { index ->
                                val scanItem = displayList[index]
                                val isChecked = scanItem.name in checkedNames
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isChecked) StockOk.copy(alpha = 0.08f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                        .clickable {
                                            checkedNames = if (isChecked) checkedNames - scanItem.name
                                            else checkedNames + scanItem.name
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            checkedNames = if (isChecked) checkedNames - scanItem.name
                                            else checkedNames + scanItem.name
                                        },
                                        modifier = Modifier.size(24.dp),
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = StockOk
                                        )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        scanItem.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f),
                                        color = if (isChecked) MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = {
                                            displayList = displayList.toMutableList().also { it.removeAt(index) }
                                            checkedNames = checkedNames - scanItem.name
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.commitLarderScanItems(displayList.filter { it.name in checkedNames })
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = checkedNames.isNotEmpty()
                    ) { Text("Importar ${checkedNames.size}") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.clearLarderScanResult() }) { Text("Descartar") }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Compact single-row inventory item with stock status visuals
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CompactInventoryRow(
    item: InventoryItem,
    isChecked: Boolean,
    checkedQty: Double,
    onCheckedChange: (Boolean) -> Unit,
    onQtyChange: (Double) -> Unit,
    onStockDecrease: () -> Unit,
    onStockIncrease: () -> Unit,
    onEditClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isDepleted = item.currentStock == 0.0
    val stockColor = if (isDepleted) StockDepleted else StockOk
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
    ) {
        // Status strip (left edge)
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(stockColor.copy(alpha = 0.7f))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.size(24.dp),
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(8.dp))

                // Name + badges
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isDepleted) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StockDepleted)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("AGOTADO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    if (item.bestStore != null && item.bestPrice != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${item.bestStore}  •  $${String.format(Locale.US, "%.2f", item.bestPrice)}/${item.unit}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))

                // Binary stock toggle: "TENGO" ↔ "AGOTADO"
                OutlinedButton(
                    onClick = if (isDepleted) onStockIncrease else onStockDecrease,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = stockColor),
                    border = BorderStroke(1.dp, stockColor),
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (isDepleted) "TENGO" else "AGOTADO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Context menu (⋮)
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Editar") } },
                            onClick = { showMenu = false; onEditClick() }
                        )
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Archive, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Archivar") } },
                            onClick = { showMenu = false; onArchiveClick() }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(8.dp)); Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
                            onClick = { showMenu = false; onDeleteClick() }
                        )
                    }
                }
            }

            // Quantity selector when checked
            AnimatedVisibility(visible = isChecked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Cantidad a comprar:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { if (checkedQty > 1) onQtyChange(checkedQty - 1) }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Remove, null, modifier = Modifier.size(12.dp))
                    }
                    Text("${checkedQty.toInt()} ${item.unit}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { onQtyChange(checkedQty + 1) }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp))
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
    var unit by remember { mutableStateOf("u") }
    var bestStore by remember { mutableStateOf("") }
    var bestPrice by remember { mutableStateOf("") }

    val units = listOf("u", "kg", "g", "paquetes", "litros", "ml", "rollos")

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
                // Header Fijo
                Text(
                    text = "Nuevo Producto en Despensa",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Cuerpo Scrollable
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
                        placeholder = { Text("ej. Leche Entera, Arroz Integral") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = stock,
                            onValueChange = { stock = it },
                            label = { Text("Stock Actual") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Unit cycle button
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = "Unidad",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedButton(
                                onClick = {
                                    val i = units.indexOf(unit)
                                    unit = units[(i + 1) % units.size]
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Text(
                        text = "Detalles de Compra (Opcional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = bestStore,
                            onValueChange = { bestStore = it },
                            label = { Text("Tienda Habitual") },
                            placeholder = { Text("ej. Jumbo") },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = bestPrice,
                            onValueChange = { bestPrice = it },
                            label = { Text("Precio ($)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Fijo
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
                            val stockVal = stock.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank() && stockVal >= 0) {
                                onAdd(
                                    name.trim(), stockVal, 0.0, unit, 0.0,
                                    bestStore.ifBlank { null },
                                    bestPrice.toDoubleOrNull()
                                )
                            }
                        },
                        enabled = name.isNotBlank() && (stock.isBlank() || stock.toDoubleOrNull() != null),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Agregar")
                    }
                }
            }
        }
    }
}

@Composable
fun EditInventoryItemDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, String, Double, String?, Double?) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var stock by remember { mutableStateOf(item.currentStock.toString()) }
    var unit by remember { mutableStateOf(item.unit) }
    var bestStore by remember { mutableStateOf(item.bestStore ?: "") }
    var bestPrice by remember { mutableStateOf(item.bestPrice?.toString() ?: "") }

    val units = listOf("u", "kg", "g", "paquetes", "litros", "ml", "rollos")

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
                // Header Fijo
                Text(
                    text = "Editar Producto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Cuerpo Scrollable
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = stock,
                            onValueChange = { stock = it },
                            label = { Text("Stock Actual") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Unit cycle button
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = "Unidad",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedButton(
                                onClick = {
                                    val i = units.indexOf(unit)
                                    unit = units[(i + 1) % units.size]
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Text(
                        text = "Detalles de Compra (Opcional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = bestStore,
                            onValueChange = { bestStore = it },
                            label = { Text("Tienda Habitual") },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = bestPrice,
                            onValueChange = { bestPrice = it },
                            label = { Text("Precio ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Fijo
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
                            onConfirm(
                                name.trim(),
                                stock.toDoubleOrNull() ?: item.currentStock,
                                item.minStockAlert, unit,
                                item.depletionRatePerDay,
                                bestStore.ifBlank { null },
                                bestPrice.toDoubleOrNull()
                            )
                        },
                        enabled = name.isNotBlank() && stock.toDoubleOrNull() != null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}
