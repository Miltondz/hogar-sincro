package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: HomeViewModel) {
    var isLoginTab by remember { mutableStateOf(true) }

    // Login state
    var loginUsername by remember { mutableStateOf("") }
    var loginHouseholdCode by remember { mutableStateOf("") }
    var loginPasscode by remember { mutableStateOf("") }
    
    // Register state
    var regUsername by remember { mutableStateOf("") }
    var regHouseholdCode by remember { mutableStateOf("") }
    var regMembersCsv by remember { mutableStateOf("") }
    var regPasscode by remember { mutableStateOf("") }

    // Validation alerts
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pre-generate a code for user convince
    LaunchedEffect(isLoginTab) {
        if (!isLoginTab && regHouseholdCode.isEmpty()) {
            val randomNum = (1000..9999).random()
            regHouseholdCode = "HOGAR-$randomNum"
        }
        errorMessage = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Elegant Icon + App Logo Title
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            )

            Text(
                text = "Hogar Sincro",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Gestión Inteligente y Sincronizada del Hogar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // M3 Tab row to alternate between Login & Register
            TabRow(
                selectedTabIndex = if (isLoginTab) 0 else 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = isLoginTab,
                    onClick = { isLoginTab = true },
                    modifier = Modifier.testTag("login_tab_selector")
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        modifier = Modifier.padding(vertical = 12.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Tab(
                    selected = !isLoginTab,
                    onClick = { isLoginTab = false },
                    modifier = Modifier.testTag("register_tab_selector")
                ) {
                    Text(
                        text = "Registrar Hogar",
                        modifier = Modifier.padding(vertical = 12.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Validation warning banner
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (isLoginTab) {
                // LOGIN SCREEN FLOW
                OutlinedTextField(
                    value = loginUsername,
                    onValueChange = { loginUsername = it; errorMessage = null },
                    label = { Text("Nombre de Miembro") },
                    placeholder = { Text("Ej. Milton") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("login_username_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = loginHouseholdCode,
                    onValueChange = { loginHouseholdCode = it; errorMessage = null },
                    label = { Text("Código de Hogar Sincro (ej: HOGAR-5892)") },
                    placeholder = { Text("HOGAR-XXXX") },
                    leadingIcon = { Icon(Icons.Default.Hub, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("login_household_code_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = loginPasscode,
                    onValueChange = { loginPasscode = it },
                    label = { Text("Contraseña / Código PIN (Opcional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("login_passcode_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val userTrimmed = loginUsername.trim()
                        val codeTrimmed = loginHouseholdCode.trim()
                        if (userTrimmed.isEmpty()) {
                            errorMessage = "Por favor, ingresa tu nombre de miembro del hogar."
                        } else if (codeTrimmed.isEmpty()) {
                            errorMessage = "Se requiere el código de sincronización del hogar."
                        } else {
                            viewModel.loginUser(userTrimmed, codeTrimmed)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_login_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ingresar al Hogar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                    )
                    Text(
                        text = " o accede con cuentas demo ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Demo Account 1: Milton
                    FilledTonalButton(
                        onClick = {
                            loginUsername = "Milton"
                            loginHouseholdCode = "HOGAR-5892"
                            viewModel.loginUser("Milton", "HOGAR-5892")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("demo_login_milton"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Milton (Papá)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("HOGAR-5892", fontSize = 9.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                    }

                    // Demo Account 2: Pilar
                    FilledTonalButton(
                        onClick = {
                            loginUsername = "Pilar"
                            loginHouseholdCode = "HOGAR-5892"
                            viewModel.loginUser("Pilar", "HOGAR-5892")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("demo_login_pilar"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Pilar (Mamá)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("HOGAR-5892", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        }
                    }
                }

            } else {
                // REGISTER SCREEN FLOW
                OutlinedTextField(
                    value = regUsername,
                    onValueChange = { regUsername = it; errorMessage = null },
                    label = { Text("Tu Nombre Completo") },
                    placeholder = { Text("Ej. Milton") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("register_username_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = regHouseholdCode,
                    onValueChange = { regHouseholdCode = it; errorMessage = null },
                    label = { Text("Código Propuesto para el Hogar") },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("register_household_code_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = regMembersCsv,
                    onValueChange = { regMembersCsv = it },
                    label = { Text("Nombres de otros Miembros (separados por coma)") },
                    placeholder = { Text("Ej. Pilar, Milton Junior") },
                    leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("register_members_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = regPasscode,
                    onValueChange = { regPasscode = it },
                    label = { Text("Establecer PIN / Contraseña (Opcional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("register_passcode_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val userTrimmed = regUsername.trim()
                        val codeTrimmed = regHouseholdCode.trim()
                        if (userTrimmed.isEmpty()) {
                            errorMessage = "El nombre de administrador es requerido."
                        } else if (codeTrimmed.isEmpty()) {
                            errorMessage = "El código propuesto del hogar no puede quedar vacío."
                        } else {
                            viewModel.registerUser(userTrimmed, codeTrimmed, regMembersCsv)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_register_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.AppRegistration, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear y Guardar Hogar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
