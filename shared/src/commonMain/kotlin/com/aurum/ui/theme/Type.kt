package com.aurum.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aurum.Res
import com.aurum.oxanium_bold
import com.aurum.oxanium_extralight
import com.aurum.oxanium_regular
import org.jetbrains.compose.resources.Font

/**
 * Aurum typography — Oxanium font using W200 (ExtraLight) weight app-wide.
 */
@Composable
fun rememberAurumTypography(): Typography {
    val oxaniumFont = FontFamily(
        Font(Res.font.oxanium_extralight, weight = FontWeight.W200),
        Font(Res.font.oxanium_regular, weight = FontWeight.W400),
        Font(Res.font.oxanium_bold, weight = FontWeight.W700)
    )

    return remember(oxaniumFont) {
        Typography(
            displayLarge = TextStyle(
                fontFamily = oxaniumFont,
                fontWeight = FontWeight.W200,
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = (-0.25).sp,
            ),
            displayMedium = TextStyle(
                fontFamily = oxaniumFont,
                fontWeight = FontWeight.W200,
                fontSize = 45.sp,
                lineHeight = 52.sp,
                letterSpacing = 0.sp,
            ),
            displaySmall = TextStyle(
                fontFamily = oxaniumFont,
                fontWeight = FontWeight.W200,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = 0.sp,
            ),
            headlineLarge = TextStyle(
                fontFamily = oxaniumFont,
                fontWeight = FontWeight.W200,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.5.sp,
            ),
            headlineMedium = TextStyle(
                fontFamily = oxaniumFont,
                fontWeight = FontWeight.W200,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.5.sp,
            ),
            headlineSmall = TextStyle(
                fontFamily = oxaniumFont,
                fontWeight = FontWeight.W200,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.5.sp,
            ),
            titleLarge = TextStyle(
                fontFamily = oxaniumFont,
                fontWeight = FontWeight.W200,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.5.sp,
            ),
            titleMedium = TextStyle(
                fontFamily = oxaniumFont,
                fontWeight = FontWeight.W200,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
            ),
            titleSmall = TextStyle(
                fontFamily = oxaniumFont,
                fontWeight = FontWeight.W200,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
            bodyLarge = TextStyle(
                fontFamily = oxaniumFont,
                fontWeight = FontWeight.W200,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
            bodyMedium = TextStyle(
                fontFamily = oxaniumFont,
                fontWeight = FontWeight.W200,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
            ),
            bodySmall = TextStyle(
                fontFamily = oxaniumFont,
                fontWeight = FontWeight.W200,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),
            labelLarge = TextStyle(
                fontFamily = oxaniumFont,
                fontWeight = FontWeight.W200,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
            labelMedium = TextStyle(
                fontFamily = oxaniumFont,
                fontWeight = FontWeight.W200,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
            labelSmall = TextStyle(
                fontFamily = oxaniumFont,
                fontWeight = FontWeight.W200,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        )
    }
}
