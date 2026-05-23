package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.HomeViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.*
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

enum class NavigationTab {
    EXPENSES, SHOPPING, INVENTORY, SCANNER, SYNC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val viewModel: HomeViewModel = viewModel()
    var currentTab by remember { mutableStateOf(NavigationTab.EXPENSES) }
    val notifications by viewModel.notifications.collectAsState()
    var showNotificationsDialog by remember { mutableStateOf(false) }

    // Synchronize current state alerts on view loaded
    LaunchedEffect(currentTab) {
        viewModel.generateSmartAlerts()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (currentTab) {
                                NavigationTab.EXPENSES -> Icons.Default.Paid
                                NavigationTab.SHOPPING -> Icons.Default.ShoppingCart
                                NavigationTab.INVENTORY -> Icons.Default.Inventory2
                                NavigationTab.SCANNER -> Icons.Default.AutoAwesome
                                NavigationTab.SYNC -> Icons.Default.CloudSync
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (currentTab) {
                                NavigationTab.EXPENSES -> "Gastos del Hogar"
                                NavigationTab.SHOPPING -> "Lista de Compras"
                                NavigationTab.INVENTORY -> "Despensa y Stock"
                                NavigationTab.SCANNER -> "Escáner Inteligente"
                                NavigationTab.SYNC -> "Sincronización Hogar"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    // Smart alert bell at top-right
                    IconButton(
                        onClick = { showNotificationsDialog = true },
                        modifier = Modifier.testTag("notification_bell_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (notifications.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = Color.White
                                    ) {
                                        Text("${notifications.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (notifications.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = "Alertas Inteligentes",
                                tint = if (notifications.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("app_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == NavigationTab.EXPENSES,
                    onClick = { currentTab = NavigationTab.EXPENSES },
                    icon = { Icon(imageVector = if (currentTab == NavigationTab.EXPENSES) Icons.Default.Paid else Icons.Outlined.Paid, contentDescription = "Gastos") },
                    label = { Text("Gastos", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("expenses_tab")
                )

                NavigationBarItem(
                    selected = currentTab == NavigationTab.SHOPPING,
                    onClick = { currentTab = NavigationTab.SHOPPING },
                    icon = { Icon(imageVector = if (currentTab == NavigationTab.SHOPPING) Icons.Default.ShoppingCart else Icons.Outlined.ShoppingCart, contentDescription = "Compras") },
                    label = { Text("Compras", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("shopping_tab")
                )

                NavigationBarItem(
                    selected = currentTab == NavigationTab.INVENTORY,
                    onClick = { currentTab = NavigationTab.INVENTORY },
                    icon = { Icon(imageVector = if (currentTab == NavigationTab.INVENTORY) Icons.Default.Inventory2 else Icons.Outlined.Inventory2, contentDescription = "Despensa") },
                    label = { Text("Despensa", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("inventory_tab")
                )

                NavigationBarItem(
                    selected = currentTab == NavigationTab.SCANNER,
                    onClick = { currentTab = NavigationTab.SCANNER },
                    icon = { Icon(imageVector = if (currentTab == NavigationTab.SCANNER) Icons.Default.AutoAwesome else Icons.Outlined.AutoAwesome, contentDescription = "Escáner") },
                    label = { Text("Escáner", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("scanner_tab")
                )

                NavigationBarItem(
                    selected = currentTab == NavigationTab.SYNC,
                    onClick = { currentTab = NavigationTab.SYNC },
                    icon = { Icon(imageVector = if (currentTab == NavigationTab.SYNC) Icons.Default.CloudSync else Icons.Outlined.CloudSync, contentDescription = "Sincro") },
                    label = { Text("Hogar", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("sync_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.EXPENSES -> ExpensesScreen(viewModel)
                NavigationTab.SHOPPING -> ShoppingScreen(viewModel)
                NavigationTab.INVENTORY -> InventoryScreen(viewModel)
                NavigationTab.SCANNER -> ScannerScreen(viewModel)
                NavigationTab.SYNC -> SyncScreen(viewModel)
            }
        }
    }

    // Modal dialogue representing the "Smart Alert Drawer" (Centro de Notificaciones Inteligentes)
    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Centro de Alertas del Hogar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    if (notifications.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "¡Al día! Todo está ordenado",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "No hay alertas de stock bajo ni vencimientos hoy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(notifications, key = { it.id }) { alert ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (alert.type) {
                                            "ALERTA" -> Color(0xFFFFEBEE)
                                            "ADVERTENCIA" -> Color(0xFFFFF3E0)
                                            "ALQUILER_SERVICIO" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                            else -> Color(0xE8E8F5E9)
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (alert.type) {
                                                        "ALERTA" -> Color(0xFFD32F2F)
                                                        "ADVERTENCIA" -> Color(0xFFFF9800)
                                                        "ALQUILER_SERVICIO" -> MaterialTheme.colorScheme.primary
                                                        else -> Color(0xFF2E7D32)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (alert.type) {
                                                    "ALERTA" -> Icons.Default.Warning
                                                    "ADVERTENCIA" -> Icons.Default.Timer
                                                    "ALQUILER_SERVICIO" -> Icons.Default.EventNote
                                                    else -> Icons.Default.SyncAlt
                                                },
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = alert.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            Text(
                                                text = alert.message,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.DarkGray,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        IconButton(
                                            onClick = { viewModel.dismissNotification(alert.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Descartar",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
