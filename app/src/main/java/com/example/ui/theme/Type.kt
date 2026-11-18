package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
)

val InterFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

fun buildResponsiveTypography(scale: Float = 1.0f): Typography {
    fun s(base: Float) = (base * scale).sp
    return Typography(
        headlineMedium = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = s(24f),
            lineHeight = s(32f)
        ),
        headlineSmall = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(20f),
            lineHeight = s(28f)
        ),
        titleLarge = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(18f),
            lineHeight = s(24f)
        ),
        titleMedium = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(15f),
            lineHeight = s(20f)
        ),
        titleSmall = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = s(13f),
            lineHeight = s(18f)
        ),
        bodyLarge = TextStyle(
            fontFamily = InterFamily,
            fontWeight = FontWeight.Normal,
            fontSize = s(15f),
            lineHeight = s(22f)
        ),
        bodyMedium = TextStyle(
            fontFamily = InterFamily,
            fontWeight = FontWeight.Normal,
            fontSize = s(13f),
            lineHeight = s(19f)
        ),
        bodySmall = TextStyle(
            fontFamily = InterFamily,
            fontWeight = FontWeight.Normal,
            fontSize = s(11.5f),
            lineHeight = s(16f)
        ),
        labelLarge = TextStyle(
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(13f),
            lineHeight = s(18f)
        ),
        labelMedium = TextStyle(
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = s(11.5f),
            lineHeight = s(16f)
        ),
        labelSmall = TextStyle(
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(10.5f),
            lineHeight = s(14f)
        )
    )
}

val AppTypography = buildResponsiveTypography(1.0f)
