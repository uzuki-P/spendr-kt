package com.spendr.app.kt.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import com.spendr.app.kt.ui.components.AnimatedAmount
import com.spendr.app.kt.ui.components.BouncyButton
import com.spendr.app.kt.ui.components.EmphasizedDecelerate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendr.app.kt.LocalVibrate
import com.spendr.app.kt.domain.formatRupiah
import com.spendr.app.kt.ui.components.MciIcon
import com.spendr.app.kt.ui.home.HomeViewModel.PaceData
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MILLION = 1_000_000L

/**
 * Month-to-date cumulative paidAmount against the 3-month average: one tonal
 * primaryContainer hero with a counting headline, a compact "Report" button,
 * and the scrubbable chart (gradient area, dashed average, haptic scrub) that
 * draws itself in from the left when it first appears.
 */
@Composable
fun SpendingPaceCard(
    pace: PaceData,
    onOpenReport: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val vibrate = LocalVibrate.current
    var scrubIndex by remember(pace) { mutableIntStateOf(-1) }

    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val onContainerMuted = onContainer.copy(alpha = 0.72f)
    val onContainerFaint = onContainer.copy(alpha = 0.5f)

    val hasData = pace.thisMonth.isNotEmpty()
    val todayIndex = max(0, pace.thisMonth.size - 1)
    val idx = if (scrubIndex < 0) todayIndex else min(scrubIndex, pace.daysInMonth - 1)

    val avgShown = if (pace.hasRealAverage) pace.average else emptyList()
    val clampedAvgIdx = min(idx, max(0, avgShown.size - 1))

    val headerLabel = when {
        !hasData -> "Spending this month · ${pace.monthLabel}"
        idx == todayIndex -> "Through today · ${pace.monthLabel}"
        idx > todayIndex -> "Day ${idx + 1} · ${pace.monthLabel}"
        else -> "Through day ${idx + 1} · ${pace.monthLabel}"
    }
    val headerAvg = if (pace.hasRealAverage) formatRupiah(avgShown.getOrElse(clampedAvgIdx) { 0 }) else null

    val reveal = remember { Animatable(0f) }
    LaunchedEffect(hasData) {
        if (hasData) reveal.animateTo(1f, tween(1100, easing = EmphasizedDecelerate))
    }

    Surface(
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = onContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(top = 16.dp, bottom = 12.dp)) {
            // Label + Report on one line so the amount gets the full width
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    headerLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = onContainerMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                BouncyButton(
                    onClick = onOpenReport,
                    height = ButtonDefaults.ExtraSmallContainerHeight,
                ) {
                    MciIcon("chart-line", 16.dp, MaterialTheme.colorScheme.onPrimary)
                    Text("Report", modifier = Modifier.padding(start = 6.dp))
                }
            }
            val valueStyle = MaterialTheme.typography.displaySmallEmphasized
            val valueModifier = Modifier.padding(horizontal = 20.dp)
            when {
                !hasData || idx > todayIndex -> Text("—", style = valueStyle, maxLines = 1, modifier = valueModifier)
                idx == todayIndex -> AnimatedAmount(
                    amount = pace.thisMonth[idx],
                    style = valueStyle,
                    color = onContainer,
                    modifier = valueModifier,
                )
                else -> Text(formatRupiah(pace.thisMonth[idx]), style = valueStyle, maxLines = 1, modifier = valueModifier)
            }
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .width(14.dp)
                        .height(3.dp)
                        .background(onContainerFaint, RoundedCornerShape(50)),
                )
                Text(
                    "3-mo avg",
                    style = MaterialTheme.typography.labelMedium,
                    color = onContainerMuted,
                )
                Text(
                    headerAvg ?: "Not enough history",
                    style = MaterialTheme.typography.labelMediumEmphasized,
                    color = if (pace.hasRealAverage) onContainer else onContainerFaint,
                    maxLines = 1,
                )
            }

            PaceChart(
                pace = pace,
                avgShown = avgShown,
                scrubIndex = idx,
                todayIndex = todayIndex,
                reveal = reveal.value,
                onScrub = { newIdx ->
                    if (newIdx != idx) {
                        scrubIndex = newIdx
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        vibrate()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(132.dp)
                    .semantics { contentDescription = "Spending pace chart" },
            )
        }
    }
}

@Composable
private fun PaceChart(
    pace: PaceData,
    avgShown: List<Long>,
    scrubIndex: Int,
    todayIndex: Int,
    reveal: Float,
    onScrub: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val tertiaryColor = onContainer.copy(alpha = 0.55f)
    val borderColor = onContainer.copy(alpha = 0.12f)
    val overlayColor = onContainer.copy(alpha = 0.2f)
    val surfaceColor = MaterialTheme.colorScheme.primaryContainer
    val density = LocalDensity.current
    val labelPx = with(density) { 10.sp.toPx() }

    val n = max(pace.daysInMonth, 1)
    val maxValue = max(
        pace.thisMonth.maxOrNull() ?: 0L,
        avgShown.maxOrNull() ?: 0L,
    ).coerceAtLeast(0)
    val steps = max(1, ceil(maxValue / MILLION.toDouble()).toInt())
    val yMax = steps * MILLION

    // x tick days: 1, 5..n-5 step 5, n
    val labelDays = remember(n) {
        buildSet {
            add(1)
            var d = 5
            while (d <= n - 5) {
                add(d)
                d += 5
            }
            add(n)
        }.toSortedSet()
    }

    val hasData = pace.thisMonth.isNotEmpty()

    Canvas(
        modifier = modifier.pointerInput(pace, n) {
            fun indexAt(x: Float): Int {
                val clamped = x.coerceIn(0f, size.width.toFloat())
                return (clamped / size.width * (n - 1).coerceAtLeast(1)).roundToInt().coerceIn(0, n - 1)
            }
            detectTapGestures(
                onPress = { offset ->
                    onScrub(indexAt(offset.x))
                    tryAwaitRelease()
                },
            )
        }.pointerInput(pace, n) {
            detectHorizontalDragGestures { change, _ ->
                change.consume()
                val clamped = change.position.x.coerceIn(0f, size.width.toFloat())
                onScrub(
                    (clamped / size.width * (n - 1).coerceAtLeast(1)).roundToInt().coerceIn(0, n - 1),
                )
            }
        },
    ) {
        val plotWidth = size.width
        val plotHeight = size.height - labelPx
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (tertiaryColor.alpha * 255).toInt(),
                (tertiaryColor.red * 255).toInt(),
                (tertiaryColor.green * 255).toInt(),
                (tertiaryColor.blue * 255).toInt(),
            )
            textSize = labelPx
            isAntiAlias = true
        }

        // Gridlines + y labels at each 1M step
        for (k in 0..steps) {
            val y = (k / steps.toFloat()) * plotHeight
            drawLine(borderColor, Offset(0f, y), Offset(plotWidth, y), 1f)
            val value = (steps - k) * MILLION
            val label = if (value == 0L) "0" else "${value / MILLION}m"
            drawContext.canvas.nativeCanvas.drawText(label, 0f, y + labelPx / 3, paint)
        }

        fun xFor(index: Int) = if (n <= 1) 0f else index / (n - 1f) * plotWidth
        fun yFor(value: Long): Float =
            plotHeight - (if (yMax > 0) value / yMax.toFloat() else 0f) * plotHeight

        // "today" line hidden while scrubbing today
        if (scrubIndex != todayIndex && todayIndex < n) {
            drawLine(overlayColor, Offset(xFor(todayIndex), 0f), Offset(xFor(todayIndex), plotHeight), 1f)
        }

        // Area fill with vertical gradient, revealed left to right on entry
        if (pace.thisMonth.size >= 2) clipRect(right = plotWidth * reveal) {
            val path = monotonePath(pace.thisMonth, ::xFor, ::yFor)
            val area = Path().apply {
                addPath(path)
                lineTo(xFor(pace.thisMonth.size - 1), plotHeight)
                lineTo(xFor(0), plotHeight)
                close()
            }
            drawPath(
                area,
                brush = Brush.verticalGradient(
                    0f to primaryColor.copy(alpha = 0.32f),
                    1f to primaryColor.copy(alpha = 0.02f),
                    startY = 0f,
                    endY = plotHeight,
                ),
            )
            // Average (dashed 5 4 dp, 1.5dp, textTertiary)
            if (avgShown.size >= 2) {
                drawPath(
                    monotonePath(avgShown, ::xFor, ::yFor),
                    color = tertiaryColor,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(5.dp.toPx(), 4.dp.toPx()),
                        ),
                    ),
                )
            }
            drawPath(
                path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // Scrub indicators
        if (hasData && reveal >= 1f && scrubIndex in 0 until n) {
            val x = xFor(scrubIndex)
            // RN scrub guideline: 1.5dp, primary at opacity 0.4
            drawLine(primaryColor.copy(alpha = 0.4f), Offset(x, 0f), Offset(x, plotHeight), 1.5f)
            val avgValue = avgShown.getOrNull(min(scrubIndex, avgShown.size - 1))
            if (avgValue != null && pace.hasRealAverage) {
                drawCircle(surfaceColor, 4.dp.toPx(), Offset(x, yFor(avgValue)))
                drawCircle(tertiaryColor, 4.dp.toPx(), Offset(x, yFor(avgValue)), style = Stroke(2f))
            }
            val primaryValue = pace.thisMonth.getOrNull(min(scrubIndex, pace.thisMonth.size - 1))
            if (primaryValue != null) {
                // RN scrubDot: 12dp circle, 2.5dp surface border, primary fill
                drawCircle(surfaceColor, 7.dp.toPx(), Offset(x, yFor(primaryValue)))
                drawCircle(primaryColor, 4.5.dp.toPx(), Offset(x, yFor(primaryValue)))
            }
        }

        // X tick labels
        val textHeight = with(density) { 10.sp.toPx() }
        for (d in labelDays) {
            drawContext.canvas.nativeCanvas.drawText(
                d.toString(),
                (xFor(d - 1) - textHeight / 2).coerceIn(0f, plotWidth - textHeight),
                plotHeight + textHeight * 1.4f,
                paint,
            )
        }
    }
}

/** Fritsch–Carlson monotone cubic interpolation, ported from RN `smoothPath`. */
private fun monotonePath(values: List<Long>, xFor: (Int) -> Float, yFor: (Long) -> Float): Path {
    val path = Path()
    if (values.size < 2) return path
    val n = values.size
    val xs = (0 until n).map { xFor(it) }
    val ys = values.map { yFor(it) }

    val slopes = FloatArray(n - 1)
    for (i in 0 until n - 1) {
        slopes[i] = if (xs[i + 1] == xs[i]) 0f else (ys[i + 1] - ys[i]) / (xs[i + 1] - xs[i])
    }
    val tangents = FloatArray(n)
    tangents[0] = slopes[0]
    tangents[n - 1] = slopes[n - 2]
    for (i in 1 until n - 1) {
        tangents[i] = if (slopes[i - 1] * slopes[i] <= 0f) {
            0f
        } else {
            2f * slopes[i - 1] * slopes[i] / (slopes[i - 1] + slopes[i])
        }
    }

    path.moveTo(xs[0], ys[0])
    for (i in 0 until n - 1) {
        val dx = (xs[i + 1] - xs[i]) / 3f
        path.cubicTo(
            xs[i] + dx, ys[i] + tangents[i] * dx,
            xs[i + 1] - dx, ys[i + 1] - tangents[i + 1] * dx,
            xs[i + 1], ys[i + 1],
        )
    }
    return path
}
