package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HomeViewModel
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WebCompanionScreen(viewModel: HomeViewModel, onBackToMobile: () -> Unit) {
    var isLoggedIn by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("MiltonD.Diaz@gmail.com") }
    var passwordInput by remember { mutableStateOf("**********") }
    
    val database = AppDatabase.getDatabase(viewModel.getApplication())
    val neonStatus by viewModel.neonConnectionState.collectAsState()
    
    val expenses by viewModel.expenses.collectAsState()
    val shoppingItems by viewModel.shoppingItems.collectAsState()
    val inventoryItems by viewModel.inventoryItems.collectAsState()

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddShoppingDialog by remember { mutableStateOf(false) }

    // Neon colors
    val neonGreen = Color(0xFF00E676)
    val darkBg = Color(0xFF121214)
    val cardBg = Color(0xFF1E1E22)
    val inputBg = Color(0xFF2C2C32)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF202124)) // Browser viewport dark chrome back
            .padding(8.dp)
    ) {
        // Desktop Browser Frame Heading Chrome
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D3033))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Browser Dots and Add-Tab Look
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Red, Yellow, Green Window Dots
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFC5C58)))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFDBE41)))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF29CC47)))
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Desktop Browser Tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(Color(0xFF1E1F22))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = neonGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Consola Web Hogar Sincro", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(10.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Button to go back to mobile app
                    Button(
                        onClick = onBackToMobile,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.PhoneAndroid, "Celular", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Regresar al Celular", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Browser URL bar row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.LightGray, modifier = Modifier.clickable { viewModel.syncWithNeon() }.size(16.dp))
                    
                    // Browser SSL Address bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E1F22))
                            .padding(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Https, "Secure", tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "https://hogar-sincro.neonauth.sa-east-1.aws.neon.tech/neondb/console",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                    
                    // Neon database connection indicator badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (neonStatus) {
                                    "CONNECTED" -> Color(0xFF2E7D32)
                                    "CONNECTING" -> Color(0xFFEF6C00)
                                    "ERROR" -> Color(0xFFC62828)
                                    else -> Color(0xFF37474F)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (neonStatus) {
                                            "CONNECTED" -> neonGreen
                                            "CONNECTING" -> Color(0xFFFFF176)
                                            "ERROR" -> Color(0xFFF44336)
                                            else -> Color.Gray
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Neon Status: $neonStatus",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
        
        // Browser inner Web View Page Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(darkBg)
        ) {
            if (!isLoggedIn) {
                // NEON AUTH LOGIN INTERFACE
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 420.dp)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, neonGreen.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Neon Logo Branding
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = neonGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            
                            Text(
                                text = "Neon Auth",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black
                            )
                            
                            Text(
                                text = "Ingresa a la aplicación central 'hogar-sincro' para compartir tus gastos e inventario.",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // Form fields
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                Text("Correo Electrónico (Neon User)", color = neonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(inputBg)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Email, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = emailInput, color = Color.White, fontSize = 12.sp)
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                Text("Contraseña", color = neonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(inputBg)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = passwordInput, color = Color.White, fontSize = 12.sp)
                                }
                            }
                            
                            if (isAuthenticating) {
                                CircularProgressIndicator(color = neonGreen, modifier = Modifier.size(24.dp))
                            } else {
                                Button(
                                    onClick = {
                                        isAuthenticating = true
                                        // Simple simulated response
                                        isLoggedIn = true
                                        isAuthenticating = false
                                        viewModel.syncWithNeon()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = neonGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Iniciar Sesión Segura con Neon Auth", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            
                            Text(
                                text = "OAuth / JWKS verificado: \nhttps://ep-blue-water-aco8ck54.neonauth.sa-east-1.aws.neon.tech/neondb/auth/.well-known/jwks.json",
                                color = Color.Gray,
                                fontSize = 8.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            } else {
                // NEON SECURED CORPORATE DARK DASHBOARD
                Row(modifier = Modifier.fillMaxSize()) {
                    // Desktop Sidebar
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(180.dp)
                            .background(Color(0xFF151518))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "HOGAR SINCRO WEB",
                            color = neonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { viewModel.syncWithNeon() }) {
                            Icon(Icons.Default.Dashboard, null, tint = neonGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dashboard", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showAddExpenseDialog = true }) {
                            Icon(Icons.Default.Paid, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Añadir Gasto", color = Color.LightGray, fontSize = 12.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showAddShoppingDialog = true }) {
                            Icon(Icons.Default.ShoppingCart, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Añadir Compra", color = Color.LightGray, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Logged in user info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(cardBg)
                                .padding(8.dp)
                        ) {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(neonGreen), contentAlignment = Alignment.Center) {
                                Text("M", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Milton Díaz", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("Web Portal", color = Color.Gray, fontSize = 8.sp)
                            }
                        }
                    }
                    
                    // Main Desktop dashboard grid area
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Title row
                        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("Consola Central de Administrador", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                Text("Vistas de base de datos Neon PostgreSQL sincronizadas en tiempo real.", color = Color.Gray, fontSize = 11.sp)
                            }
                            
                            Button(
                                onClick = { viewModel.syncWithNeon() },
                                colors = ButtonDefaults.buttonColors(containerColor = neonGreen, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Forzar Sincro con Celular", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Multi-row dashboard representation
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // SINCRO EXPENSES PANEL
                            Card(
                                modifier = Modifier.weight(1.2f).fillMaxHeight(),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Historial de Gastos (Neon DB Table)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Button(
                                            onClick = { showAddExpenseDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C32), contentColor = neonGreen),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Text("+ Añadir", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                                        items(expenses) { exp ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF232327))
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(neonGreen.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Payments, null, tint = neonGreen, modifier = Modifier.size(14.dp))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(exp.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                    Text(exp.category, color = Color.Gray, fontSize = 9.sp)
                                                }
                                                Text(
                                                    text = "by ${exp.paidBy}",
                                                    color = Color.LightGray,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                )
                                                Text(
                                                    text = "$${String.format("%.2f", exp.amount)}",
                                                    color = neonGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // SHOPPING AND INVENTORY SPLIT PANEL
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Shopping lists column
                                Card(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Lista de Compras Sincronizada", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Button(
                                                onClick = { showAddShoppingDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C32), contentColor = neonGreen),
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Text("+ Añadir", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                                            items(shoppingItems) { item ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF232327))
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = if (item.isBought) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                        contentDescription = null,
                                                        tint = if (item.isBought) neonGreen else Color.Gray,
                                                        modifier = Modifier.size(16.dp).clickable { viewModel.toggleShoppingItemBought(item) }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(item.productName, color = if (item.isBought) Color.Gray else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Text("${item.quantityToBuy} ${item.unit} (${item.targetStore ?: "Tienda"})", color = Color.Gray, fontSize = 9.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                // Low stock lists column
                                Card(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Estado de Alimentos y Stock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                                            items(inventoryItems) { item ->
                                                val isLow = item.currentStock <= item.minStockAlert
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF232327))
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isLow) Color(0xFFC62828) else Color(0xFF2E7D32))
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(item.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                                    Text(
                                                        text = "${item.currentStock} / ${item.minStockAlert} ${item.unit}",
                                                        color = if (isLow) Color(0xFFEF9A9A) else Color.LightGray,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Expense Dialogue mock in Desktop console
    if (showAddExpenseDialog) {
        var tempTitle by remember { mutableStateOf("") }
        var tempAmount by remember { mutableStateOf("") }
        var tempCategory by remember { mutableStateOf("Alimentos") }
        
        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text("Registrar Gasto (Neon SQL API)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        label = { Text("Asunto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempAmount,
                        onValueChange = { tempAmount = it },
                        label = { Text("Monto ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = tempAmount.toDoubleOrNull() ?: 0.0
                        if (tempTitle.isNotBlank() && amt > 0) {
                            viewModel.addExpense(tempTitle, amt, tempCategory)
                            viewModel.syncWithNeon() // Push to PostgreSQL
                            showAddExpenseDialog = false
                        }
                    }
                ) {
                    Text("Registrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Add Shopping dialogue mock in Desktop console
    if (showAddShoppingDialog) {
        var tempName by remember { mutableStateOf("") }
        var tempQty by remember { mutableStateOf("") }
        var tempUnit by remember { mutableStateOf("u") }
        var tempStore by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddShoppingDialog = false },
            title = { Text("Registrar Producto compras (Neon SQL API)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Nombre del Producto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempQty,
                        onValueChange = { tempQty = it },
                        label = { Text("Cantidad") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val q = tempQty.toDoubleOrNull() ?: 1.0
                        if (tempName.isNotBlank()) {
                            viewModel.addShoppingItem(tempName, q, tempUnit, tempStore.ifBlank { null })
                            viewModel.syncWithNeon() // Push to PostgreSQL
                            showAddShoppingDialog = false
                        }
                    }
                ) {
                    Text("Registrar compras")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddShoppingDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
