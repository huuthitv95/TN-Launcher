package vn.tn.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Design tokens (xem docs/design-tokens.md). Không dùng blur thời gian thực: Android 10 không có RenderEffect. */
data class Tokens(
    val wallTop: Color,
    val wallBottom: Color,
    val glass: Color,
    val glassStroke: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val isDark: Boolean,
)

val LightTokens = Tokens(
    wallTop = Color(0xFFDCEBFA), wallBottom = Color(0xFFB9D4F0),
    glass = Color(0xFFFFFFFF).copy(alpha = 0.45f), glassStroke = Color(0xFFFFFFFF).copy(alpha = 0.70f),
    card = Color(0xFFFFFFFF).copy(alpha = 0.62f),
    textPrimary = Color(0xFF0F1B2D), textSecondary = Color(0xFF0F1B2D).copy(alpha = 0.60f),
    accent = Color(0xFF0A84FF), isDark = false,
)

val DarkTokens = Tokens(
    wallTop = Color(0xFF14214A), wallBottom = Color(0xFF070D20),
    glass = Color(0xFF1A2748).copy(alpha = 0.55f), glassStroke = Color(0xFFFFFFFF).copy(alpha = 0.14f),
    card = Color(0xFF1A2748).copy(alpha = 0.78f),
    textPrimary = Color(0xFFF2F6FF), textSecondary = Color(0xFFF2F6FF).copy(alpha = 0.62f),
    accent = Color(0xFF3B82F6), isDark = true,
)

val LocalTokens = staticCompositionLocalOf { LightTokens }

fun Modifier.glass(t: Tokens, shape: Shape) =
    this.background(t.glass, shape).border(1.dp, t.glassStroke, shape)

fun Modifier.card(t: Tokens, shape: Shape) =
    this.background(t.card, shape).border(1.dp, t.glassStroke, shape)
