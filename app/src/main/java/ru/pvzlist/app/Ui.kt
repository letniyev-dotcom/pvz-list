@file:OptIn(ExperimentalTextApi::class)

package ru.pvzlist.app

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

val LocalOnest = staticCompositionLocalOf<FontFamily> { FontFamily.SansSerif }

/** Onest (variable) из assets/fonts/onest.ttf; если файла нет — системный шрифт. */
fun loadOnest(context: Context): FontFamily {
    return try {
        val am = context.assets
        val head = ByteArray(4)
        am.open("fonts/onest.ttf").use { it.read(head) }
        val isTtf = head[0] == 0.toByte() && head[1] == 1.toByte()
        if (!isTtf) return FontFamily.SansSerif
        FontFamily(
            listOf(400, 500, 600).map { w ->
                Font(
                    am,
                    "fonts/onest.ttf",
                    FontWeight(w),
                    FontStyle.Normal,
                    FontVariation.Settings(FontVariation.weight(w))
                )
            }
        )
    } catch (e: Exception) {
        FontFamily.SansSerif
    }
}

@Composable
fun Txt(
    text: AnnotatedString,
    size: Int,
    lh: Int,
    weight: Int = 400,
    color: Color = C.Ink,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    ellipsis: Boolean = false,
    tnum: Boolean = false,
    strike: Boolean = false,
    spacing: Float = 0f
) {
    val style = TextStyle(
        color = color,
        fontSize = size.sp,
        fontWeight = FontWeight(weight),
        fontFamily = LocalOnest.current,
        fontFeatureSettings = if (tnum) "tnum" else null,
        letterSpacing = if (spacing != 0f) spacing.sp else TextUnit.Unspecified,
        textDecoration = if (strike) TextDecoration.LineThrough else null,
        textAlign = align,
        lineHeight = lh.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )
    BasicText(
        text = text,
        modifier = modifier,
        style = style,
        overflow = if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
        maxLines = maxLines
    )
}

@Composable
fun Txt(
    text: String,
    size: Int,
    lh: Int,
    weight: Int = 400,
    color: Color = C.Ink,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    ellipsis: Boolean = false,
    tnum: Boolean = false,
    strike: Boolean = false,
    spacing: Float = 0f
) {
    Txt(
        AnnotatedString(text), size, lh, weight, color, modifier,
        align, maxLines, ellipsis, tnum, strike, spacing
    )
}

/** Иконка: SVG 24x24, stroke, round caps/joins — как в HTML */
@Composable
fun Ico(
    name: String,
    size: Dp,
    color: Color,
    sw: Float = 1.75f,
    modifier: Modifier = Modifier
) {
    val paths = remember(name) { IconPaths.get(name) }
    Canvas(modifier.size(size).clipToBounds()) {
        val k = this.size.width / 24f
        scale(k, k, pivot = Offset.Zero) {
            paths.forEach { p ->
                drawPath(
                    p,
                    color,
                    style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

/** Клик без ripple — в HTML нет эффектов нажатия */
fun Modifier.tap(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}
