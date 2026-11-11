package com.userlab.movieplus.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.userlab.movieplus.ui.theme.UlBrandGradient
import com.userlab.movieplus.ui.theme.UlButtonGradient
import com.userlab.movieplus.ui.theme.UlCyan
import com.userlab.movieplus.ui.theme.UlMidnight
import com.userlab.movieplus.ui.theme.UlOutline
import com.userlab.movieplus.ui.theme.UlSurface
import com.userlab.movieplus.ui.theme.UlSurfaceHigh
import com.userlab.movieplus.ui.theme.UlTextPrimary
import com.userlab.movieplus.ui.theme.UlTextSecondary
import com.userlab.movieplus.ui.theme.UlViolet

/**
 * Single source of truth for everything the app says about the company behind it.
 * Edit here and every screen (splash, auth, settings, about) updates.
 */
object UserLabInfo {
    const val COMPANY = "UserLab Ltd"
    const val APP_NAME = "AnyFilm"
    const val COPYRIGHT_YEAR = "2026"
    const val ABOUT_TEXT =
        "$APP_NAME is designed, built and maintained by $COMPANY, a software company " +
            "creating apps for mobile, web and desktop."

    // Optional contact details. Leave empty to hide the matching row.
    val WEBSITE: String = ""
    val SUPPORT_EMAIL: String = ""
}

// ── Background ─────────────────────────────────────────────

/** Midnight canvas with soft violet and cyan glows. */
fun Modifier.userLabBackground(): Modifier =
    this
        .background(UlMidnight)
        .drawBehind {
            val violetCenter = Offset(size.width * 0.1f, 0f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(UlViolet.copy(alpha = 0.30f), Color.Transparent),
                    center = violetCenter,
                    radius = size.width * 0.95f
                ),
                radius = size.width * 0.95f,
                center = violetCenter
            )
            val cyanCenter = Offset(size.width * 1.0f, size.height * 0.42f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(UlCyan.copy(alpha = 0.12f), Color.Transparent),
                    center = cyanCenter,
                    radius = size.width * 0.8f
                ),
                radius = size.width * 0.8f,
                center = cyanCenter
            )
        }

// ── Brand mark & company attribution ───────────────────────

/** The UserLab monogram. */
@Composable
fun UserLabMark(size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(UlBrandGradient),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "U",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.52f).sp
        )
    }
}

/** Small pill: [U] from UserLab Ltd */
@Composable
fun UserLabBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(UlSurfaceHigh.copy(alpha = 0.7f))
            .border(BorderStroke(1.dp, UlOutline), RoundedCornerShape(50))
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserLabMark(size = 22.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "from ",
            color = UlTextSecondary,
            fontSize = 12.sp
        )
        Text(
            text = UserLabInfo.COMPANY,
            color = UlTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Centered company sign-off used at the bottom of long screens. */
@Composable
fun UserLabFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UserLabMark(size = 30.dp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "${UserLabInfo.APP_NAME} is a product of",
            color = UlTextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = UserLabInfo.COMPANY,
            color = UlTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "© ${UserLabInfo.COPYRIGHT_YEAR} ${UserLabInfo.COMPANY}. All rights reserved.",
            color = UlTextSecondary.copy(alpha = 0.7f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** "About the company" card with a gradient hairline border. */
@Composable
fun UserLabAboutCard(appVersion: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(UlSurface)
            .border(BorderStroke(1.dp, UlBrandGradient), shape)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserLabMark(size = 48.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "About ${UserLabInfo.COMPANY}",
                    color = UlTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "The company behind ${UserLabInfo.APP_NAME}",
                    color = UlTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = UserLabInfo.ABOUT_TEXT,
            color = UlTextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        UserLabInfoRow(label = "Developer", value = UserLabInfo.COMPANY)
        UserLabInfoRow(label = "App", value = "${UserLabInfo.APP_NAME}  v$appVersion")
        if (UserLabInfo.WEBSITE.isNotEmpty()) UserLabInfoRow(label = "Website", value = UserLabInfo.WEBSITE)
        if (UserLabInfo.SUPPORT_EMAIL.isNotEmpty()) UserLabInfoRow(label = "Support", value = UserLabInfo.SUPPORT_EMAIL)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "© ${UserLabInfo.COPYRIGHT_YEAR} ${UserLabInfo.COMPANY}. All rights reserved.",
            color = UlTextSecondary.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun UserLabInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = UlTextSecondary, fontSize = 12.sp)
        Text(text = value, color = UlTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Inputs & buttons ───────────────────────────────────────

@Composable
fun UlTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val hidden = isPassword && !passwordVisible

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = label) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        } else null,
        visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = UlTextPrimary,
            unfocusedTextColor = UlTextPrimary,
            disabledTextColor = UlTextSecondary,
            focusedContainerColor = UlSurfaceHigh.copy(alpha = 0.55f),
            unfocusedContainerColor = UlSurfaceHigh.copy(alpha = 0.35f),
            cursorColor = UlCyan,
            focusedBorderColor = UlViolet,
            unfocusedBorderColor = UlOutline,
            focusedLabelColor = UlViolet,
            unfocusedLabelColor = UlTextSecondary,
            focusedLeadingIconColor = UlViolet,
            unfocusedLeadingIconColor = UlTextSecondary,
            focusedTrailingIconColor = UlTextSecondary,
            unfocusedTrailingIconColor = UlTextSecondary
        )
    )
}

@Composable
fun UlPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val shape = RoundedCornerShape(16.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = shape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled) UlButtonGradient else Brush.horizontalGradient(listOf(UlSurfaceHigh, UlSurfaceHigh)),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

/** Two-or-more option pill switch (e.g. Sign in / Create account). */
@Composable
fun UlSegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tags: List<String> = emptyList()
) {
    val outer = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(outer)
            .background(UlSurfaceHigh.copy(alpha = 0.5f))
            .border(BorderStroke(1.dp, UlOutline), outer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val itemModifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = if (selected) UlButtonGradient else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                )
                .clickable { onSelect(index) }
            Box(
                modifier = if (tags.size > index) itemModifier.testTag(tags[index]) else itemModifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    color = if (selected) Color.White else UlTextSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/** Thin 4-step password strength bar. */
@Composable
fun UlStrengthBar(score: Int, modifier: Modifier = Modifier) {
    val clamped = score.coerceIn(0, 4)
    val color = when (clamped) {
        0, 1 -> Color(0xFFFF5C7A)
        2 -> Color(0xFFFFB547)
        3 -> Color(0xFF22D3EE)
        else -> Color(0xFF34D399)
    }
    val label = when (clamped) {
        0 -> "Too short"
        1 -> "Weak"
        2 -> "Fair"
        3 -> "Good"
        else -> "Strong"
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            for (i in 1..4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (i <= clamped) color else UlOutline)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Password strength: $label", color = UlTextSecondary, fontSize = 11.sp)
    }
}
