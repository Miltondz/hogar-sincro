package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import com.example.ui.HomeViewModel
import com.example.ui.theme.BudgetOver
import com.example.ui.theme.StockOk
import java.util.Locale

@Composable
fun FutureScreen(viewModel: HomeViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val syncSettings by viewModel.syncSettings.collectAsState()
    val membersList = remember(syncSettings.members) { syncSettings.members.split(",").map { it.trim() }.filter { it.isNotBlank() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Header
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(
                        text = "Saldos & Mejoras Futuras",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Visualiza el balance del hogar y futuras características.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            // Balance Card (Saldos y división)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("future_household_balance_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Saldos y División del Hogar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = "Porcentaje equitativo de aportaciones por cada participante del hogar (división equitativa):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )

                        // Compute spent totals for each member dynamically
                        val memberSpends = remember(expenses, membersList) {
                            membersList.associateWith { member ->
                                expenses.filter { it.paidBy.equals(member, ignoreCase = true) }.sumOf { it.amount }
                            }
                        }

                        val totalSum = memberSpends.values.sum()
                        val idealShare = if (membersList.isNotEmpty()) totalSum / membersList.size else 0.0

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            membersList.forEachIndexed { index, member ->
                                val spent = memberSpends[member] ?: 0.0
                                val color = when (index % 5) {
                                    0 -> MaterialTheme.colorScheme.primary
                                    1 -> Color(0xFFE91E63)
                                    2 -> Color(0xFFFF9800)
                                    3 -> Color(0xFF4CAF50)
                                    else -> Color(0xFF9C27B0)
                                }

                                val balance = spent - idealShare

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.8f))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(color),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = member.take(1).uppercase(),
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = member + if (member == syncSettings.activeUser) " (Tú)" else "",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            Text(
                                                text = "Aportó: $${String.format(Locale.US, "%.2f", spent)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    // Balance message
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (balance >= -0.01) Color(0xE8E8F5E9) else Color(0xFFFFEBEE)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (balance >= -0.01) {
                                                "Favor: +$${String.format(Locale.US, "%.2f", balance)}"
                                            } else {
                                                "Debe: -$${String.format(Locale.US, "%.2f", Math.abs(balance))}"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (balance >= -0.01) StockOk else BudgetOver
                                        )
                                    }
                                }
                            }
                        }

                        // Settlement status box
                        if (totalSum > 0 && membersList.size > 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cuota justa por persona: $${String.format(Locale.US, "%.2f", idealShare)}. Quienes tengan saldo 'Debe' pueden transferir directamente a quienes están en 'Favor' para saldar cuentas.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Future Improvements Section
            item {
                Text(
                    text = "Próximas Mejoras del Sistema",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val improvements = listOf(
                ImprovementItem(
                    title = "Gráficos de Consumo Avanzados",
                    description = "Visualiza el consumo del hogar por mes con diagramas circulares y líneas de tendencia predictiva.",
                    icon = Icons.Outlined.Analytics,
                    status = "En Planificación"
                ),
                ImprovementItem(
                    title = "Integración de Pagos Móviles",
                    description = "Permite saldar las deudas del hogar mediante links de pago directo (Yape, Plin, Bizum, etc.).",
                    icon = Icons.Outlined.QrCodeScanner,
                    status = "Propuesto"
                ),
                ImprovementItem(
                    title = "Planificación Inteligente de Recetas",
                    description = "AI sugiere recetas basadas en lo que tienes en tu despensa actual para minimizar desperdicio.",
                    icon = Icons.Outlined.MenuBook,
                    status = "Propuesto"
                ),
                ImprovementItem(
                    title = "Alertas de Geolocalización",
                    description = "Te avisa si pasas cerca de un supermercado que tiene el mejor precio registrado para un artículo de tu lista.",
                    icon = Icons.Outlined.Map,
                    status = "En Planificación"
                )
            )

            items(improvements) { improvement ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = improvement.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = improvement.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = improvement.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = improvement.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ImprovementItem(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val status: String
)
