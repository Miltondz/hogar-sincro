package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.HomeViewModel
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay

val SCANNER_FUNNY_MESSAGES = listOf(
    "🤖 Sobornando a la IA con galletas virtuales...",
    "🔍 Analizando el ticket... ¿De verdad compraste tanto?",
    "🧠 Traduciendo la letra del cajero a lenguaje binario...",
    "💸 Contabilizando la tragedia financiera de esta semana...",
    "🛒 Negociando con la BD para que no te juzgue...",
    "⚡ Gemini está reñando al servidor por la lentitud...",
    "🥛 Confirmando si la leche de almendras es alimento o estilo de vida...",
    "🧾 Descifrando jeróglifos modernos en la sección de totales...",
    "📦 Auditando la despensa... ¿De verdad necesitas 5 latas de atún?",
)

@Composable
fun ScannerScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanning.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val pendingReceipt by viewModel.pendingReceipt.collectAsState()

    // State for selected image — reset every new scan session
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showExplanationDialog by remember { mutableStateOf(false) }

    // Create a stable temp URI for camera capture (once per composition)
    val cameraPhotoUri = remember {
        val dir = File(context.cacheDir, "camera_photos").also { it.mkdirs() }
        val file = File(dir, "ticket_capture.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // --- Gallery launcher ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bmp = decodeSampledBitmapFromUri(context, uri)
            if (bmp != null) selectedBitmap = bmp
            else viewModel.showSnackbar("No se pudo leer la imagen.", com.example.ui.SnackbarType.ERROR)
        }
    }

    // --- Camera launcher ---
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            val file = File(context.cacheDir, "camera_photos/ticket_capture.jpg")
            selectedBitmap = decodeSampledBitmapFromFile(file.absolutePath)
        }
    }

    // --- Camera permission launcher ---
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(cameraPhotoUri)
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(cameraPhotoUri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Rotating funny loading message
    var funnyMessage by remember { mutableStateOf("") }
    LaunchedEffect(isScanning) {
        if (isScanning) {
            while (true) {
                funnyMessage = SCANNER_FUNNY_MESSAGES.random()
                delay(2500)
            }
        } else {
            funnyMessage = ""
        }
    }

    // Receipt import confirmation dialog
    if (pendingReceipt != null) {
        val receipt = pendingReceipt!!
        AlertDialog(
            onDismissRequest = { viewModel.clearPendingReceipt() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Confirmar Importación", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(receipt.storeName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(receipt.category, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Text(
                            "$${String.format(java.util.Locale.US, "%.2f", receipt.totalAmount)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${receipt.items.size} artículos detectados — se marcarán como en stock:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(receipt.items) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.name, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "$${String.format(java.util.Locale.US, "%.2f", item.price)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmReceiptImport(receipt) },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Importar y Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearPendingReceipt() }) { Text("Cancelar") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Camera/Gallery chooser dialog
    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = {
                Text("Seleccionar imagen", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("¿Cómo quieres obtener la foto del ticket?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSourceDialog = false
                        launchCamera()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cámara")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showSourceDialog = false
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Galería")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("scanner_screen"),
        contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Escáner Inteligente de Tickets con IA",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Toma una foto con la cámara o selecciona desde la galería. Gemini extraerá productos, precios e importes automáticamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Image picker / preview panel (only when NOT scanning and NO result)
        if (!isScanning && scanResult == null) {
            item {
                Text(
                    text = "Foto del Ticket o Factura",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Image viewfinder card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clickable { showSourceDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedBitmap != null) Color.Black
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (selectedBitmap != null) {
                            Image(
                                bitmap = selectedBitmap!!.asImageBitmap(),
                                contentDescription = "Imagen seleccionada",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                            )
                            // "Change" badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Cambiar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            // "Ready" badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Imagen lista • Pulsa Escanear", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        } else {
                            // Empty state
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Toca para seleccionar una foto",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Usa la cámara o elige desde la galería",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                // Source buttons inline
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { launchCamera() },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Cámara", fontSize = 13.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            galleryLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Galería", fontSize = 13.sp)
                                    }
                                }
                            }
                            // Corner frame brackets
                            Box(modifier = Modifier.align(Alignment.TopStart).padding(12.dp).size(24.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 8.dp)))
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(24.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topEnd = 8.dp)))
                            Box(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp).size(24.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomStart = 8.dp)))
                            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(24.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomEnd = 8.dp)))
                        }
                    }
                }
            }

            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (selectedBitmap != null) {
                                viewModel.scanTicketWithGemini(selectedBitmap, null)
                            } else {
                                showSourceDialog = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("scan_trigger_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedBitmap != null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (selectedBitmap != null)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = if (selectedBitmap != null) Icons.Default.AutoAwesome else Icons.Default.AddPhotoAlternate,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedBitmap != null) "Escanear con Gemini IA" else "Seleccionar Foto",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { showExplanationDialog = true },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Ayuda")
                    }
                }
            }
        }

        // Loading state
        if (isScanning) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Analizando con Gemini Flash",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AnimatedContent(
                            targetState = funnyMessage,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "funny_message"
                        ) { msg ->
                            Text(
                                text = msg.ifEmpty { "Enviando imagen a Gemini..." },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Result section
        if (scanResult != null && !isScanning) {
            val receipt = scanResult!!
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Resultado del Análisis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        viewModel.clearScanResult()
                        selectedBitmap = null
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Escanear otro")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "¡Factura Importada con Éxito!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Se registró un gasto de ${receipt.category} y se actualizaron los precios en la despensa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Receipt paper card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = receipt.storeName.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                        Text(
                            text = "CATEGORÍA: ${receipt.category.uppercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.Black, modifier = Modifier.padding(bottom = 12.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            receipt.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.DarkGray
                                        )
                                        Text(
                                            text = "${item.quantity} u × $${String.format(Locale.US, "%.2f", item.price)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                    }
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", item.price * item.quantity)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color.Black, modifier = Modifier.padding(vertical = 12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL EXTRAÍDO",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                            Text(
                                text = "$${String.format(Locale.US, "%,.2f", receipt.totalAmount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            // Scan another button at the bottom
            item {
                Button(
                    onClick = {
                        viewModel.clearScanResult()
                        selectedBitmap = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.DocumentScanner, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Escanear Otro Ticket", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Help dialog
    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            title = { Text("¿Cómo funciona el escáner?") },
            text = {
                Text(
                    "Toma una foto con la cámara de tu celular o selecciona una imagen de la galería.\n\n" +
                    "Gemini Flash analizará el ticket y extraerá:\n\n" +
                    "1. Nombre del comercio\n" +
                    "2. Categoría del gasto\n" +
                    "3. Lista de productos con cantidades y precios\n\n" +
                    "Los productos se registran en tu despensa y el total se archiva en Gastos Mensuales."
                )
            },
            confirmButton = {
                Button(
                    onClick = { showExplanationDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Entendido") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
