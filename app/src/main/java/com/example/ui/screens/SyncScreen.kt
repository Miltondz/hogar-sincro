package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.HomeViewModel
import com.example.ui.SyncActivity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(viewModel: HomeViewModel) {
    val settings by viewModel.syncSettings.collectAsState()
    val activities by viewModel.syncActivities.collectAsState()
    val context = LocalContext.current

    var showCodeDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showWipeConfirm by remember { mutableStateOf(false) }

    // Logout confirmation dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "¿Cerrar sesión?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Se cerrará la sesión activa del hogar. Los datos locales se mantienen seguros.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        viewModel.logoutUser()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sí, cerrar sesión", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Limpiar Base de Datos",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text("¿Estás seguro de que deseas limpiar toda la base de datos local y en la nube de tu familia?\n\nEsta acción eliminará todos los gastos, despensa y listas de compras, restableciendo el hogar únicamente con los usuarios Milton y Alejandra. Esta operación es irreversible.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeConfirm = false
                        viewModel.clearFullFamilyDatabase()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Limpiar Todo", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("sync_screen"),
        contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanatory Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sincronización del Hogar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Permite que múltiples miembros del mismo hogar registren e importen datos de forma conjunta. Todos los cambios se comparten y sincronizan instantáneamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Integrated Neon Database Sincro Card with Interactive Sync Ring Indicator
        item {
            val neonStatus by viewModel.neonConnectionState.collectAsState()
            
            Card(
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Base de Datos Neon Cloud",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Status Pill Badge with Glowing Pulse Indicator
                        val badgeColor = when (neonStatus) {
                            "CONNECTED" -> Color(0xFF2E7D32)
                            "CONNECTING" -> Color(0xFFEF6C00)
                            "ERROR" -> Color(0xFFC62828)
                            else -> Color.DarkGray
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeColor.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            // Glowing Dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor)
                            )
                            Text(
                                text = if (neonStatus == "CONNECTED") "NEON ACTIVO" else neonStatus,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = badgeColor,
                                fontSize = 8.sp
                            )
                        }
                    }
                    
                    Text(
                        text = "La aplicación está vinculada directamente con Neon Serverless PostgreSQL y utiliza Neon Auth para verificar credenciales de múltiples dispositivos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Host: ep-blue-water-aco8ck54-pooler.sa-east-1.aws.neon.tech",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Auth: https://ep-blue-water-aco8ck54.neonauth.sa-east-1.aws.neon.tech",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.syncWithNeon() },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sincronizar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { viewModel.showWebPortal.value = true },
                            modifier = Modifier.weight(1.2f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Laptop, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Abrir Portal Web", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Family Membership Card (Digital membership card visual)
        item {
            val cardGradient = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            )
            Card(
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(cardGradient)
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "MEMBRESÍA DIGITAL HOGAR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Código Familiar Vinculante",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = settings.householdCode,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                letterSpacing = 2.sp
                            )
                            
                            Button(
                                onClick = { showCodeDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Vincular", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Shared Configuration
        item {
            Card(
                modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Simulated sync toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sincronización en Tiempo Real",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Conectarse al canal del hogar para recibir actualizaciones remotas inmediatas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = settings.isSyncEnabled,
                            onCheckedChange = { viewModel.changeSyncProvider(it) }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    // Current User Profile Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Miembro de Familia Activo",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "Pulsa sobre un perfil para cambiar el usuario activo de este dispositivo y registrar consumos a su nombre:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        
                        // Scrollable family member list
                        val membersList = remember(settings.members) { settings.members.split(",") }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            membersList.forEach { memberName ->
                                val isMe = memberName == "Milton"
                                val isSelected = settings.activeUser == memberName
                                val avatarColor = when (membersList.indexOf(memberName) % 5) {
                                    0 -> MaterialTheme.colorScheme.primary
                                    1 -> Color(0xFFE91E63) // Pink
                                    2 -> Color(0xFFFF9800) // Orange
                                    3 -> Color(0xFF4CAF50) // Green
                                    else -> Color(0xFF9C27B0) // Purple
                                }
                                
                                MemberProfileCard(
                                    name = memberName + if (isMe) " (Tú)" else "",
                                    role = if (isMe) "Administrador" else "Miembro",
                                    avatarColor = avatarColor,
                                    isSelected = isSelected,
                                    icon = if (isMe) Icons.Default.Face else Icons.Default.Face4,
                                    onClick = { viewModel.selectActiveUser(memberName) },
                                    modifier = Modifier.width(130.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Button to Add Member
                        Button(
                            onClick = { showAddMemberDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Añadir Integrante al Hogar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // AI Model Config Section
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Modelo de Inteligencia Artificial (Escaneo & Extracción)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = "Selecciona qué modelo inteligente se usará para procesar e importar la información de tus tickets de compra:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )

                            val selectedModel = settings.geminiModel

                            val geminiModels = listOf(
                                Triple("gemini-3.5-flash",        "3.5 Flash",        "Más inteligente (Recomendado)"),
                                Triple("gemini-3.1-flash-lite-preview", "3.1 Flash-Lite", "Eficiente y rápido")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                geminiModels.forEach { (modelId, label, sublabel) ->
                                    val isSelected = selectedModel == modelId
                                    Button(
                                        onClick = { viewModel.changeGeminiModel(modelId) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(52.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(sublabel, fontSize = 8.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // QR Code and Sharing Section
        item {
            var showInviteToast by remember { mutableStateOf(false) }
            val clipboardManager = LocalClipboardManager.current
            
            Card(
                modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Acceso Rápido Colaborativo",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Escanea el código QR de vinculación desde otro dispositivo para sincronizar los presupuestos, despensa y listas al instante.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Button(
                            onClick = {
                                clipboardManager.setText(
                                    AnnotatedString(
                                        "Hogar Sincro: Sincroniza conmigo tus gastos del hogar con el código vinculante: ${settings.householdCode}"
                                    )
                                )
                                showInviteToast = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copiar Enlace", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        MockQRCode(codeText = settings.householdCode)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = settings.householdCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            if (showInviteToast) {
                LaunchedEffect(Unit) {
                    delay(2000)
                    showInviteToast = false
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "¡Mensaje de invitación copiado al portapapeles!",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Live Synced Feed / Activity Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HistoryToggleOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Auditoría de Actividad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (settings.isSyncEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32))
                        )
                        Text("VIVO", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                    }
                }
            }
        }

        if (activities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Wifi, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Esperando sincronizaciones...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "El motor sincronizador simula actividad periódica del otro co-propietario ('Alejandra' o 'Milton') modificando despensa o confirmando listas para auditar el canal bidireccional.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
                        )
                    }
                }
            }
        } else {
            // Display as a beautiful chronological vertical timeline
            items(activities.size) { index ->
                val activity = activities[index]
                TimelineActivityLog(
                    activity = activity,
                    isFirst = index == 0,
                    isLast = index == activities.lastIndex
                )
            }
        }

        // Danger zone
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWipeConfirm = true }
                    .testTag("wipe_db_button"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Limpiar Base de Datos del Hogar",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Borra todo en local y cloud (Neon PostgreSQL)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Logout
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogoutConfirm = true }
                    .testTag("logout_button"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cerrar Sesión",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Cambiar de hogar o usuario activo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    // Code sharing dialog optimized for mobile
    if (showCodeDialog) {
        var tempCode by remember { mutableStateOf(settings.householdCode) }
        Dialog(
            onDismissRequest = { showCodeDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .imePadding(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Asociar Hogar Sincro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Ingresa el código compartido del hogar de tu pareja para vincular las bases de datos en tiempo real:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    OutlinedTextField(
                        value = tempCode,
                        onValueChange = { tempCode = it },
                        label = { Text("Código de Familia") },
                        placeholder = { Text("ej. HOGAR-1234") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCodeDialog = false }) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (tempCode.isNotBlank()) {
                                    viewModel.updateHouseholdCode(tempCode)
                                    showCodeDialog = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            enabled = tempCode.isNotBlank()
                        ) {
                            Text("Vincular")
                        }
                    }
                }
            }
        }
    }

    // Add household member dialog optimized for mobile
    if (showAddMemberDialog) {
        var memberName by remember { mutableStateOf("") }
        Dialog(
            onDismissRequest = { showAddMemberDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .imePadding(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Asociar Integrante", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Añade a un nuevo miembro a la lista compartida del hogar para que registre e identifique consumos a su nombre:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    OutlinedTextField(
                        value = memberName,
                        onValueChange = { memberName = it },
                        label = { Text("Nombre del familiar") },
                        placeholder = { Text("ej. Laura, Carlos") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddMemberDialog = false }) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (memberName.isNotBlank()) {
                                    viewModel.addHouseholdMember(memberName)
                                    showAddMemberDialog = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            enabled = memberName.isNotBlank()
                        ) {
                            Text("Integrar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineActivityLog(
    activity: SyncActivity,
    isFirst: Boolean,
    isLast: Boolean
) {
    val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    val userColor = if (activity.user == "Milton") MaterialTheme.colorScheme.primary else Color(0xFFE91E63)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Vertical Timeline Connector Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(44.dp)
        ) {
            // Top connector line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(12.dp)
                    .background(if (isFirst) Color.Transparent else Color.LightGray.copy(alpha = 0.6f))
            )
            
            // Avatar dot
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(userColor)
                    .shadow(1.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activity.user.take(1),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
            
            // Bottom connector line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(32.dp)
                    .background(if (isLast) Color.Transparent else Color.LightGray.copy(alpha = 0.6f))
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Activity Card details
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
                .shadow(1.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${activity.user}  •  ${activity.category}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = userColor
                    )
                    Text(
                        text = sdf.format(Date(activity.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MemberProfileCard(
    name: String,
    role: String,
    avatarColor: Color,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .shadow(if (isSelected) 4.dp else 1.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = role,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else Color.Gray
                )
            }
        }
    }
}

@Composable
fun MockQRCode(
    codeText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(100.dp)
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val size = size.width
                val numGrid = 15
                val blockSize = size / numGrid

                fun drawFinderPattern(x: Int, y: Int) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(x * blockSize, y * blockSize),
                        size = Size(3 * blockSize, 3 * blockSize)
                    )
                    drawRect(
                        color = Color.White,
                        topLeft = Offset((x + 0.5f) * blockSize, (y + 0.5f) * blockSize),
                        size = Size(2 * blockSize, 2 * blockSize)
                    )
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset((x + 1) * blockSize, (y + 1) * blockSize),
                        size = Size(blockSize, blockSize)
                    )
                }

                drawFinderPattern(0, 0)
                drawFinderPattern(numGrid - 3, 0)
                drawFinderPattern(0, numGrid - 3)

                val hash = codeText.hashCode()
                for (row in 0 until numGrid) {
                    for (col in 0 until numGrid) {
                        if ((row < 4 && col < 4) || (row < 4 && col >= numGrid - 4) || (row >= numGrid - 4 && col < 4)) {
                            continue
                        }
                        val bitIndex = (row * numGrid + col) % 32
                        val drawBlock = ((hash ushr bitIndex) and 1) == 1 || (row + col) % 3 == 0
                        if (drawBlock) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(col * blockSize, row * blockSize),
                                size = Size(blockSize, blockSize)
                            )
                        }
                    }
                }
            }
        }
    }
}

