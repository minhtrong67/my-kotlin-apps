@file:OptIn(ExperimentalTextApi::class)

package com.minhtrong67.moody.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.minhtrong67.moody.R

/** Fonts bundled in res/font (subset to Latin + Vietnamese, ~0.9 MB total). */
object Fonts {
    val ids = listOf("system", "google-sans-flex", "roboto", "inter", "be-vietnam-pro")

    private val weights = listOf(400, 500, 600, 700)

    private fun gsf(round: Int) = FontFamily(weights.map { w ->
        Font(
            R.font.google_sans_flex, FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.Setting("ROND", round.toFloat())),
        )
    })

    private val googleSansFlex by lazy { gsf(0) }
    private val googleSansFlexRound by lazy { gsf(100) }   // rounded terminals for headings, like the web app
    private val inter by lazy {
        FontFamily(weights.map { w ->
            Font(R.font.inter, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
        })
    }
    private val beVietnam by lazy {
        FontFamily(
            Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
            Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
            Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
            Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
        )
    }

    fun body(id: String): FontFamily = when (id) {
        "google-sans-flex" -> googleSansFlex
        "inter" -> inter
        "be-vietnam-pro" -> beVietnam
        "roboto" -> FontFamily.SansSerif
        else -> FontFamily.Default
    }

    fun heading(id: String): FontFamily = if (id == "google-sans-flex") googleSansFlexRound else body(id)
}

fun typographyFor(fontId: String): Typography {
    val b = Fonts.body(fontId)
    val h = Fonts.heading(fontId)
    fun TextStyle.h() = copy(fontFamily = h)
    fun TextStyle.b() = copy(fontFamily = b)
    val d = Typography()
    return Typography(
        displayLarge = d.displayLarge.h(), displayMedium = d.displayMedium.h(), displaySmall = d.displaySmall.h(),
        headlineLarge = d.headlineLarge.h(), headlineMedium = d.headlineMedium.h(), headlineSmall = d.headlineSmall.h(),
        titleLarge = d.titleLarge.h(), titleMedium = d.titleMedium.b(), titleSmall = d.titleSmall.b(),
        bodyLarge = d.bodyLarge.b(), bodyMedium = d.bodyMedium.b(), bodySmall = d.bodySmall.b(),
        labelLarge = d.labelLarge.b(), labelMedium = d.labelMedium.b(), labelSmall = d.labelSmall.b(),
    )
}
