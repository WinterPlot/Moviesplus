package com.userlab.movieplus.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.userlab.movieplus.R
import com.userlab.movieplus.ui.components.UlPrimaryButton
import com.userlab.movieplus.ui.components.UlSegmentedToggle
import com.userlab.movieplus.ui.components.UlStrengthBar
import com.userlab.movieplus.ui.components.UlTextField
import com.userlab.movieplus.ui.components.UserLabAboutCard
import com.userlab.movieplus.ui.components.UserLabBadge
import com.userlab.movieplus.ui.components.UserLabFooter
import com.userlab.movieplus.ui.components.UserLabInfo
import com.userlab.movieplus.ui.components.userLabBackground
import com.userlab.movieplus.ui.theme.*
import com.userlab.movieplus.ui.viewmodel.MovieViewModel

/** 0 = too short … 4 = strong. Purely visual guidance; the server stays the authority. */
private fun passwordScore(password: String): Int {
    if (password.length < 8) return 0
    var score = 1
    if (password.any { it.isDigit() }) score++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
    if (password.any { !it.isLetterOrDigit() } || password.length >= 12) score++
    return score
}

@Composable
fun AuthSheet(
    viewModel: MovieViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var displayName by remember(viewModel.isUserLabLoggedIn) { mutableStateOf("") }
    var email by remember(viewModel.isUserLabLoggedIn) { mutableStateOf("") }
    var password by remember(viewModel.isUserLabLoggedIn) { mutableStateOf("") }
    var confirmPassword by remember(viewModel.isUserLabLoggedIn) { mutableStateOf("") }
    var isRegisterMode by remember(viewModel.isUserLabLoggedIn) { mutableStateOf(false) }
    var errorMessage by remember(viewModel.isUserLabLoggedIn) { mutableStateOf("") }
    var statusMessage by remember(viewModel.isUserLabLoggedIn) { mutableStateOf("") }

    val appVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun switchMode(register: Boolean) {
        isRegisterMode = register
        errorMessage = ""
        statusMessage = ""
    }

    fun submit() {
        if (email.isBlank() || password.isEmpty()) {
            errorMessage = "Email and Password cannot be empty"
            return
        }
        if (!email.contains("@") || !email.contains(".")) {
            errorMessage = "Please enter a valid email address"
            return
        }
        statusMessage = ""
        if (isRegisterMode) {
            if (displayName.trim().length < 2) {
                errorMessage = "Please enter your display name (at least 2 characters)"
                return
            }
            if (password.length < 8) {
                errorMessage = "Password must be at least 8 characters"
                return
            }
            if (password != confirmPassword) {
                errorMessage = "Passwords do not match"
                return
            }
            errorMessage = ""
            viewModel.userLabRegister(displayName, email, password, onSuccess = {
                Toast.makeText(context, "Welcome to ${UserLabInfo.APP_NAME}!", Toast.LENGTH_SHORT).show()
                onDismiss()
            }, onError = { err ->
                errorMessage = err
            })
        } else {
            errorMessage = ""
            viewModel.userLabLogin(email, password, onSuccess = {
                Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                onDismiss()
            }, onError = { err ->
                errorMessage = err
            })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .userLabBackground()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        // Top bar: close + company badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(UlSurfaceHigh.copy(alpha = 0.7f))
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = UlTextPrimary)
            }
            UserLabBadge()
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_popup_logo),
                contentDescription = "${UserLabInfo.APP_NAME} logo",
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (isRegisterMode) "Create your account" else "Welcome back",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = UlTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isRegisterMode) {
                    "Create your free UserLab account to keep your profile on every device."
                } else {
                    "Sign in with your UserLab account to pick up right where you left off."
                },
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = UlTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        UlSegmentedToggle(
            options = listOf("Sign In", "Sign Up"),
            selectedIndex = if (isRegisterMode) 1 else 0,
            onSelect = { switchMode(it == 1) },
            tags = listOf("auth_tab_signin", "auth_tab_signup")
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Form card
        val cardShape = RoundedCornerShape(24.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(UlSurface.copy(alpha = 0.85f))
                .border(BorderStroke(1.dp, UlOutline), cardShape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (errorMessage.isNotEmpty()) {
                MessageBanner(text = errorMessage, color = UlError, isError = true)
            }
            if (statusMessage.isNotEmpty()) {
                MessageBanner(text = statusMessage, color = UlSuccess, isError = false)
            }

            // Display name (registration only)
            if (isRegisterMode) {
                UlTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = "Display Name",
                    icon = Icons.Filled.Person,
                    modifier = Modifier.testTag("name_input")
                )
            }

            UlTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                icon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email,
                modifier = Modifier.testTag("email_input")
            )

            UlTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                icon = Icons.Filled.Lock,
                isPassword = true,
                imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done,
                onDone = {
                    focusManager.clearFocus()
                    submit()
                },
                modifier = Modifier.testTag("password_input")
            )

            if (isRegisterMode && password.isNotEmpty()) {
                UlStrengthBar(score = passwordScore(password))
            }

            if (isRegisterMode) {
                UlTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm Password",
                    icon = Icons.Filled.Lock,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    onDone = {
                        focusManager.clearFocus()
                        submit()
                    },
                    modifier = Modifier.testTag("confirm_password_input")
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            UlPrimaryButton(
                text = if (isRegisterMode) "Create Account" else "Sign In",
                onClick = { submit() },
                loading = viewModel.isAuthLoading,
                enabled = !viewModel.isAuthLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_submit_button")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = UlTextSecondary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Protected by ${UserLabInfo.COMPANY}. Passwords are never stored in plain text.",
                color = UlTextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Secondary toggle link (kept for accessibility / existing UI tests)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRegisterMode) "Already have an account?" else "New to ${UserLabInfo.APP_NAME}?",
                color = UlTextSecondary,
                fontSize = 14.sp
            )
            TextButton(
                onClick = { switchMode(!isRegisterMode) },
                modifier = Modifier.testTag("toggle_auth_mode")
            ) {
                Text(
                    text = if (isRegisterMode) "Sign In" else "Create an account",
                    color = UlViolet,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Who is behind the app
        UserLabAboutCard(appVersion = appVersion)

        Spacer(modifier = Modifier.height(26.dp))

        UserLabFooter()

        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun MessageBanner(text: String, color: Color, isError: Boolean) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.4f)), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
