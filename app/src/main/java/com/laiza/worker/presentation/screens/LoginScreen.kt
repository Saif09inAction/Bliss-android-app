package com.laiza.worker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.laiza.worker.BuildConfig
import com.laiza.worker.core.navigation.Screen
import com.laiza.worker.domain.models.Role
import com.laiza.worker.presentation.components.ErrorDialog
import com.laiza.worker.presentation.components.LoadingDialog
import com.laiza.worker.presentation.uiState.AuthUiState
import com.laiza.worker.core.theme.BlissBlack
import com.laiza.worker.core.theme.BlissCream
import com.laiza.worker.core.theme.BlissGold
import com.laiza.worker.core.theme.BlissLime
import com.laiza.worker.presentation.components.BlissLogoImage
import com.laiza.worker.presentation.viewmodels.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.authUiState.collectAsState()
    val screenState by viewModel.loginScreenState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState) {
        val successState = uiState
        if (successState is AuthUiState.Success) {
            val destination = viewModel.homeRouteForRole(successState.session.role)
            navController.navigate(destination) {
                popUpTo(Screen.AuthGraph.route) { inclusive = true }
            }
            viewModel.resetUiState()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BlissCream
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // White header block with BB monogram
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BlissLogoImage(size = 100.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "BLISS BOMBAY",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = BlissBlack,
                        letterSpacing = 3.sp
                    )
                }
            }

            // Dark form container matching screenshot design
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                BlissBlack,
                                Color(0xFF151A10)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
                    )
                    .padding(28.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Tab selector (Staff / Kaariger)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .background(Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(100.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(Role.STAFF to "Staff", Role.KAARIGER to "Kaariger").forEach { (role, label) ->
                            val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            val pressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "tab_scale")
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .graphicsLayer(scaleX = scale, scaleY = scale)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(
                                        color = if (screenState.selectedRole == role) Color(0xFF15803D) else Color.Transparent
                                    )
                                    .clickable(interactionSource = interactionSource, indication = null) {
                                        viewModel.onRoleChange(role)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (screenState.selectedRole == role) Color(0xFF0A0A0A) else Color(0xFF15803D).copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // Mobile input field
                    OutlinedTextField(
                        value = screenState.employeeId,
                        onValueChange = { viewModel.onEmployeeIdChange(it) },
                        label = { Text("Mobile Number") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Mobile Number",
                                tint = Color(0xFF15803D)
                            )
                        },
                        isError = screenState.employeeIdError != null,
                        supportingText = screenState.employeeIdError?.let { { Text(it, color = Color(0xFFEF4444)) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF15803D),
                            unfocusedBorderColor = Color(0xFF15803D).copy(alpha = 0.4f),
                            focusedLabelColor = Color(0xFF15803D),
                            unfocusedLabelColor = Color(0xFF15803D).copy(alpha = 0.6f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLeadingIconColor = Color(0xFF15803D),
                            unfocusedLeadingIconColor = Color(0xFF15803D).copy(alpha = 0.6f),
                            errorBorderColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Password input field
                    var passwordVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = screenState.password,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password",
                                tint = Color(0xFF15803D)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFF15803D)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = screenState.passwordError != null,
                        supportingText = screenState.passwordError?.let { { Text(it, color = Color(0xFFEF4444)) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (screenState.isLoginButtonEnabled) {
                                    viewModel.login()
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF15803D),
                            unfocusedBorderColor = Color(0xFF15803D).copy(alpha = 0.4f),
                            focusedLabelColor = Color(0xFF15803D),
                            unfocusedLabelColor = Color(0xFF15803D).copy(alpha = 0.6f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLeadingIconColor = Color(0xFF15803D),
                            unfocusedLeadingIconColor = Color(0xFF15803D).copy(alpha = 0.6f),
                            errorBorderColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Options Row: Remember Me & Forgot Password
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = screenState.rememberMe,
                                onCheckedChange = { viewModel.onRememberMeChange(it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF15803D),
                                    checkmarkColor = Color(0xFF0A0A0A),
                                    uncheckedColor = Color(0xFF15803D).copy(alpha = 0.6f)
                                )
                            )
                            Text(
                                text = "Remember Me",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF15803D).copy(alpha = 0.8f)
                            )
                        }

                        TextButton(onClick = { /* Forgot Password action */ }) {
                            Text(
                                text = "Forgot Password?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Login Button (Premium styling matching screens)
                    val buttonInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val buttonPressed by buttonInteractionSource.collectIsPressedAsState()
                    val buttonScale by animateFloatAsState(if (buttonPressed) 0.96f else 1f, label = "login_button_scale")

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.login()
                        },
                        enabled = screenState.isLoginButtonEnabled,
                        interactionSource = buttonInteractionSource,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF15803D),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF15803D).copy(alpha = 0.25f),
                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale)
                    ) {
                        Text(
                            text = "Login as Employee",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Version Indicator
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF15803D).copy(alpha = 0.4f)
                    )
                }
            }
        }
    }

    // Modal state dialogs
    if (uiState is AuthUiState.Loading) {
        LoadingDialog()
    }

    if (uiState is AuthUiState.Error) {
        val errorMsg = (uiState as AuthUiState.Error).message
        ErrorDialog(
            title = "Login Failed",
            message = errorMsg,
            onConfirm = { viewModel.resetUiState() }
        )
    }
}
