package com.example.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HomeViewModel
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
    val isScanning by viewModel.isScanning.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()

    var selectedSample by remember { mutableStateOf("super") }
    var showExplanationDialog by remember { mutableStateOf(false) }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("scanner_screen"),
        contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanatory Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)),
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
                            text = "Toma fotos de recibos, facturas o tickets de compra. La IA de Gemini extraerá automáticamente los productos, precios e importes para añadirlos a tu despensa y gastos del mes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Viewfinder simulator panel
        if (scanResult == null && !isScanning) {
            item {
                Text(
                    text = "Cámara y Captura de Factura",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Drawing realistic stylized grid representing scanner camera viewfinder
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = when (selectedSample) {
                                    "luz" -> Icons.Outlined.Receipt
                                    "alquiler" -> Icons.Outlined.HomeWork
                                    else -> Icons.Outlined.ReceiptLong
                                },
                                contentDescription = null,
                                tint = Color.Green,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = when (selectedSample) {
                                    "luz" -> "FACTURA DE LUZ Y CONSUMO\nCompañía Eléctrica Nacional - $85.30"
                                    "alquiler" -> "RECIBO ALQUILER MENSUAL\nArrendamientos Centrales - $850.00"
                                    else -> "TICKET DE SUPERMERCADO\nSupermercado Ahorro - $37.75"
                                },
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "[ ENCUADRA EL TICKET Y TOMA LA FOTO ]",
                                color = Color.Green.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Grid overlays
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(24.dp)
                                .border(width = 2.dp, color = Color.Green, shape = RoundedCornerShape(topStart = 8.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .border(width = 2.dp, color = Color.Green, shape = RoundedCornerShape(topEnd = 8.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .size(24.dp)
                                .border(width = 2.dp, color = Color.Green, shape = RoundedCornerShape(bottomStart = 8.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .border(width = 2.dp, color = Color.Green, shape = RoundedCornerShape(bottomEnd = 8.dp))
                        )
                    }
                }
            }

            // Selector of mock samples for easy testing
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Selecciona un tipo de ticket de ejemplo para escanear:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SampleChip(
                            label = "Supermercado",
                            selected = selectedSample == "super",
                            icon = Icons.Default.ShoppingCart,
                            onClick = { selectedSample = "super" }
                        )
                        SampleChip(
                            label = "Factura de Luz",
                            selected = selectedSample == "luz",
                            icon = Icons.Default.FlashOn,
                            onClick = { selectedSample = "luz" }
                        )
                        SampleChip(
                            label = "Alquiler Hogar",
                            selected = selectedSample == "alquiler",
                            icon = Icons.Default.Home,
                            onClick = { selectedSample = "alquiler" }
                        )
                    }
                }
            }

            // Capture CTA Trigger button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.scanTicketWithGemini(null, selectedSample) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("scan_trigger_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escanear con IA de Gemini", fontWeight = FontWeight.Bold)
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

        // Analysis state loader
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
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
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

        // Output scanned result layout drawer
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
                    
                    TextButton(onClick = { viewModel.clearScanResult() }) {
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
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
                            text = "Se ha registrado un gasto de ${receipt.category} y se actualizaron los precios más convenientes de cada artículo en tu base de datos del hogar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // The receipt paper card
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
                        // Ticket header
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
                        Divider(color = Color.Black, modifier = Modifier.padding(bottom = 12.dp))

                        // Items
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
                                    Column {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.DarkGray
                                        )
                                        Text(
                                            text = "${item.quantity} raciones/u * $${String.format(Locale.US, "%.2f", item.price)}",
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

                        Divider(color = Color.Black, modifier = Modifier.padding(vertical = 12.dp))

                        // Total Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL IMP. EXTRAÍDO",
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
        }
    }

    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            title = { Text("¿Cómo funciona el scanner?") },
            text = {
                Text("Esta sección simula un sensor de cámara y envía la imagen del ticket a Gemini Flash (modelo multimodal) para extraer autónomamente:\n\n1. Nombre Comercial\n2. Consumo por categoría\n3. Lista desglosada con raciones y precios\n\nLos artículos se registran directamente en los históricos de precios habituales de la sección Despensa, y el total se archiva en tus Gastos Mensuales.")
            },
            confirmButton = {
                Button(onClick = { showExplanationDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
fun SampleChip(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(10.dp)
            )
        }
    )
}
