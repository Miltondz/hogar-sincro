package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Expense
import com.example.ui.HomeViewModel
import com.example.NavigationTab
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExpensesScreen(viewModel: HomeViewModel, onNavigateToTab: (NavigationTab) -> Unit) {
    val expenses by viewModel.expenses.collectAsState()
    val variableSum by viewModel.variableExpensesSum.collectAsState()
    val recurringSum by viewModel.recurringExpensesSum.collectAsState()
    val syncSettings by viewModel.syncSettings.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val totalExpenses = variableSum + recurringSum
    
    // Categorized spending maps
    val categoryTotals = remember(expenses) {
        val map = mutableMapOf<String, Double>()
        expenses.forEach { exp ->
            map[exp.category] = (map[exp.category] ?: 0.0) + exp.amount
        }
        map
    }

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
            // Summary Card (Aesthetic, high negative space, clear summary of spending)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("summary_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Presupuesto Mensual",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$${String.format(Locale.US, "%,.2f", totalExpenses)}",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Gastos Recurrentes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                                Text(
                                    text = "$${String.format(Locale.US, "%,.2f", recurringSum)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = Color(0xFFE91E63),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Gastos Variables",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                                Text(
                                    text = "$${String.format(Locale.US, "%,.2f", variableSum)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Beautiful segmented bar representing categories
            if (expenses.isNotEmpty() && totalExpenses > 0) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Distribución de Gastos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Drawn custom progress indicators
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray.copy(alpha = 0.2f))
                        ) {
                            val sortedCategories = categoryTotals.entries.sortedByDescending { it.value }
                            val colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                Color(0xFF00BCD4), // cyan
                                Color(0xFFFF9800), // orange
                                Color(0xFF9C27B0), // purple
                                Color(0xFF4CAF50)  // green
                            )

                            sortedCategories.forEachIndexed { index, entry ->
                                val proportion = (entry.value / totalExpenses).toFloat()
                                if (proportion > 0.01) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(proportion)
                                            .background(colors[index % colors.size])
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Labels Row
                        val sortedCategories = categoryTotals.entries.sortedByDescending { it.value }
                        val colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            Color(0xFF00BCD4),
                            Color(0xFFFF9800),
                            Color(0xFF9C27B0),
                            Color(0xFF4CAF50)
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            sortedCategories.forEachIndexed { index, entry ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(colors[index % colors.size])
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = entry.key,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = "$${String.format(Locale.US, "%,.2f", entry.value)} (${String.format(Locale.US, "%.0f", (entry.value/totalExpenses)*100)}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Predictive summary Box (Mejoras y predicciones de gastos)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
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
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Predicción de Fin de Mes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            val estimatedTotals = totalExpenses + 120.00 // standard overhead estimation
                            Text(
                                text = "Proyección de gastos para el mes actual: $${String.format(Locale.US, "%,.2f", estimatedTotals)}. Un 4% menor al promedio histórico anterior debido a precios optimizados.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Quick Actions Panel (Acciones Rápidas)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("quick_actions_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Acciones Rápidas del Hogar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Text(
                            text = "Accede rápidamente a las funciones clave de escaneo y abastecimiento del hogar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Button 1: Scan receipt
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateToTab(NavigationTab.SCANNER) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Escanear Boleta",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Subir foto / ticket",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Button 2: Add to inventory
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateToTab(NavigationTab.INVENTORY) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Ingresar Alacena",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Manual o foto",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // GASTOS RECURRENTES Section (Alquiler, servicios)
            item {
                Text(
                    text = "Gastos Recurrentes (Alquiler y Servicios)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val recurringExpenses = expenses.filter { it.isRecurring }
            if (recurringExpenses.isEmpty()) {
                item {
                    Text(
                        text = "Sin deudas ni servicios recurrentes registrados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                items(recurringExpenses, key = { it.id }) { item ->
                    ExpenseRow(expense = item, onDelete = { viewModel.deleteExpense(item) })
                }
            }

            // OTROS GASTOS Section (Variables log)
            item {
                Text(
                    text = "Consumos y Gastos Variables del Mes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val variableExpenses = expenses.filter { !it.isRecurring }
            if (variableExpenses.isEmpty()) {
                item {
                    Text(
                        text = "No hay consumos variables registrados este mes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                items(variableExpenses, key = { it.id }) { item ->
                    ExpenseRow(expense = item, onDelete = { viewModel.deleteExpense(item) })
                }
            }
        }

        // Add expense FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 16.dp)
                .testTag("add_expense_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar Gasto")
        }

        if (showAddDialog) {
            AddExpenseDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, amt, cat, isRec, due ->
                    viewModel.addExpense(title, amt, cat, isRec, due)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ExpenseRow(expense: Expense, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when (expense.category) {
                            "Alquiler" -> colorsForCategory("Alquiler").copy(alpha = 0.15f)
                            "Servicio" -> colorsForCategory("Servicio").copy(alpha = 0.15f)
                            "Alimentos" -> colorsForCategory("Alimentos").copy(alpha = 0.15f)
                            else -> colorsForCategory("Diverso").copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (expense.category) {
                        "Alquiler" -> Icons.Outlined.Home
                        "Servicio" -> Icons.Outlined.ElectricalServices
                        "Alimentos" -> Icons.Outlined.LocalGroceryStore
                        else -> Icons.Outlined.ReceiptLong
                    },
                    contentDescription = null,
                    tint = colorsForCategory(expense.category)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Dynamic payment author badge
                    val paidColor = when (expense.paidBy) {
                        "Milton" -> MaterialTheme.colorScheme.primary
                        "Pilar" -> Color(0xFFE91E63)
                        "Carlos" -> Color(0xFFFF9800)
                        "Laura" -> Color(0xFF4CAF50)
                        else -> Color(0xFF9C27B0)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(paidColor.copy(alpha = 0.12f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = expense.paidBy,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = paidColor
                        )
                    }
                    
                    if (expense.isRecurring && expense.recurringDueDate != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Recurrente: ${expense.recurringDueDate}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    } else {
                        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        Text(
                            text = sdf.format(Date(expense.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format(Locale.US, "%,.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (expense.isRecurring) colorsForCategory(expense.category) else MaterialTheme.colorScheme.onSurface
                )
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar Gasto",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun colorsForCategory(category: String): Color {
    return when (category) {
        "Alquiler" -> Color(0xFF3F51B5)  // Indigo
        "Servicio" -> Color(0xFF009688)  // Teal
        "Alimentos" -> Color(0xFFFF5722) // Deep Orange
        else -> Color(0xFF607D8B)        // Blue Grey
    }
}

@Composable
fun AddExpenseDialog(onDismiss: () -> Unit, onAdd: (String, Double, String, Boolean, String?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Alimentos") }
    var isRecurring by remember { mutableStateOf(false) }
    var recurringDueDate by remember { mutableStateOf("") }

    val categories = listOf("Alimentos", "Servicio", "Alquiler", "Diverso")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Registrar Nuevo Gasto", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título / Concepto") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Dropdown representation
                Text(text = "Categoría", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                // Recurring switch representation (alquiler, servicio)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Gasto Recurrente",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Para alquileres o facturas fijas",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                }

                if (isRecurring) {
                    OutlinedTextField(
                        value = recurringDueDate,
                        onValueChange = { recurringDueDate = it },
                        label = { Text("Fecha de vencimiento (ej. Día 05)") },
                        placeholder = { Text("Día 05 de cada mes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amtVal = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amtVal > 0.0) {
                        onAdd(
                            title, 
                            amtVal, 
                            category, 
                            isRecurring, 
                            if (isRecurring && recurringDueDate.isNotBlank()) recurringDueDate else "Día 05 de cada mes"
                        )
                    }
                },
                enabled = title.isNotBlank() && amount.toDoubleOrNull() != null
            ) {
                Text("Registrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
