package com.spendr.app.kt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.spendr.app.kt.R

fun categoryColor(hex: String): Color =
    runCatching { Color(hex.toColorInt()) }.getOrDefault(Color(0xFFA86086))

internal val MciFamily = FontFamily(Font(R.font.mci))

/** Material Community Icons glyph rendered from the bundled font, RN-exact. */
@Composable
fun MciIcon(
    name: String,
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val glyphSizeSp = with(LocalDensity.current) { size.toSp() }
    val glyph = MciGlyphs.glyphString(name) ?: MciGlyphs.glyphString("help-circle") ?: "?"
    Text(
        text = glyph,
        fontFamily = MciFamily,
        fontSize = glyphSizeSp,
        lineHeight = glyphSizeSp,
        color = tint,
        textAlign = TextAlign.Center,
        modifier = modifier.size(size),
    )
}

/**
 * Circle badge with the Material Community Icons glyph, identical to the RN
 * app's `CategoryIcon`: category color at 12% alpha background, glyph rendered
 * in the bundled MCI font at full color.
 */
@Composable
fun CategoryIconBadge(
    icon: String,
    color: String,
    size: Dp,
    modifier: Modifier = Modifier,
    glyphSize: Dp = size / 2,
) {
    val resolved = categoryColor(color)
    val glyphSizeSp = with(LocalDensity.current) { glyphSize.toSp() }
    Box(
        modifier = modifier
            .size(size)
            .background(resolved.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val glyph = MciGlyphs.glyphString(icon) ?: MciGlyphs.glyphString("tag") ?: "?"
        Text(
            text = glyph,
            fontFamily = MciFamily,
            fontSize = glyphSizeSp,
            lineHeight = glyphSizeSp,
            color = resolved,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun CategoryIconBadgeForCategory(
    category: com.spendr.app.kt.data.db.entity.CategoryEntity,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    CategoryIconBadge(
        icon = category.icon,
        color = category.color,
        size = size,
        modifier = modifier,
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (action != null) action()
    }
}
