package com.iboalali.basicrootchecker.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.iboalali.basicrootchecker.ui.theme.BasicRootCheckerTheme
import com.iboalali.basicrootchecker.ui.theme.RootStatusColors
import com.iboalali.basicrootchecker.ui.theme.rememberRootStatusColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * The status card's icon: a spinning arc while a check runs, which closes into a ring, floods into a
 * colored disc and then draws the result's glyph. Checking again plays it backwards.
 *
 * [onSettled] fires once a result's glyph has finished drawing, so the outcome haptic lands with the
 * visual rather than on the state change. It also fires straight away for a result that is already
 * showing when the icon first composes (a recreated activity, or animations switched off).
 *
 * Decorative for accessibility: the status text below is the live region that announces the result.
 */
@Composable
fun RootStatusIcon(
    status: RootStatus,
    onSettled: (RootStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inspection = LocalInspectionMode.current
    val currentOnSettled by rememberUpdatedState(onSettled)
    val colors = rememberRootStatusColors()
    val checkingColor = MaterialTheme.colorScheme.primary

    val checking = status == RootStatus.CHECKING
    val atRest = if (checking) 0f else 1f
    // The badge whose color and glyph are drawn. It lags behind [status] while checking, so the
    // previous result can drain away in its own color.
    var badge by remember { mutableStateOf(if (checking) RootStatus.NOT_CHECKED else status) }
    val sweep = remember { Animatable(if (checking) OPEN_SWEEP else FULL_SWEEP) }
    val rotation = remember { Animatable(0f) }
    val tint = remember { Animatable(atRest) }
    val fill = remember { Animatable(atRest) }
    val glyph = remember { Animatable(atRest) }
    val dot = remember { Animatable(atRest) }
    var composedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        val firstRun = !composedOnce
        composedOnce = true
        if (inspection || (firstRun && status != RootStatus.CHECKING)) {
            if (status.isResult) currentOnSettled(status)
            return@LaunchedEffect
        }
        if (status == RootStatus.CHECKING) {
            coroutineScope {
                launch { dot.animateTo(0f, tween(UNDRAW_MS)) }
                launch { glyph.animateTo(0f, tween(UNDRAW_MS)) }
                launch { tint.animateTo(0f, tween(DRAIN_MS, delayMillis = DRAIN_DELAY_MS)) }
                // Starts once the glyph is mostly gone, so it never shows against the card.
                fill.animateTo(
                    0f,
                    tween(DRAIN_MS, delayMillis = DRAIN_DELAY_MS, easing = FastOutSlowInEasing),
                )
                launch {
                    while (true) {
                        rotation.snapTo(rotation.value % 360f)
                        rotation.animateTo(rotation.value + 360f, tween(SPIN_MS, easing = LinearEasing))
                    }
                }
                while (true) {
                    sweep.animateTo(OPEN_SWEEP, tween(BREATHE_MS, easing = FastOutSlowInEasing))
                    sweep.animateTo(MIN_SWEEP, tween(BREATHE_MS, easing = FastOutSlowInEasing))
                }
            }
        } else {
            // A fast check can land while the previous glyph is still un-drawing.
            if (glyph.value > 0f || dot.value > 0f) {
                coroutineScope {
                    launch { dot.animateTo(0f, tween(UNDRAW_FAST_MS)) }
                    glyph.animateTo(0f, tween(UNDRAW_FAST_MS))
                }
            }
            badge = status
            // Scaled to the work left, so a result landing on a still-full disc recolors quickly
            // instead of holding the glyph back for a full close-and-fill.
            val closeMs = (CLOSE_MS * (1f - sweep.value / FULL_SWEEP)).toInt()
            val fillMs = (FILL_MS * (1f - fill.value)).toInt()
            launch { tint.animateTo(1f, tween((closeMs + fillMs).coerceAtLeast(MIN_TINT_MS))) }
            sweep.animateTo(FULL_SWEEP, tween(closeMs, easing = FastOutSlowInEasing))
            fill.animateTo(1f, tween(fillMs, easing = FastOutSlowInEasing))
            glyph.animateTo(1f, tween(DRAW_MS, easing = FastOutSlowInEasing))
            if (status.isResult) currentOnSettled(status)
            dot.animateTo(
                1f,
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
            )
        }
    }

    val shape = remember(badge) { GlyphShape.of(badge) }
    val segment = remember { Path() }
    Canvas(modifier.size(ICON_SIZE)) {
        val (badgeColor, glyphColor) = colors.of(badge)
        val ringColor = lerp(checkingColor, badgeColor, tint.value)
        val outer = RING_DIAMETER.toPx() / 2f
        val ringStroke = RING_STROKE.toPx()
        val width = ringStroke + (outer - ringStroke) * fill.value
        val radius = outer - width / 2f
        val sweepAngle = sweep.value.coerceAtMost(FULL_SWEEP)
        drawArc(
            color = ringColor,
            startAngle = rotation.value - 90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = center - Offset(radius, radius),
            size = Size(radius * 2f, radius * 2f),
            style =
                Stroke(width, cap = if (sweepAngle < FULL_SWEEP) StrokeCap.Round else StrokeCap.Butt),
        )
        if (glyph.value > 0f || dot.value > 0f) {
            drawGlyph(shape, glyph.value, dot.value, glyphColor, outer, segment)
        }
    }
}

private fun DrawScope.drawGlyph(
    shape: GlyphShape,
    progress: Float,
    dotScale: Float,
    color: Color,
    outer: Float,
    segment: Path,
) {
    val unit = outer * 2f / VIEWPORT
    translate(center.x - outer, center.y - outer) {
        scale(unit, unit, pivot = Offset.Zero) {
            val share = 1f / shape.strokes.size
            shape.strokes.forEachIndexed { index, measure ->
                val local = ((progress - index * share) / share).coerceIn(0f, 1f)
                val length = measure.length
                // A round cap turns a zero-length segment into a dot, which would linger at the
                // start of a draw and the end of an un-draw.
                if (local * length < MIN_SEGMENT) return@forEachIndexed
                val (start, stop) =
                    if (shape.fromCenter) length * (1f - local) / 2f to length * (1f + local) / 2f
                    else 0f to length * local
                segment.reset()
                measure.getSegment(start, stop, segment, startWithMoveTo = true)
                drawPath(
                    segment,
                    color,
                    style = Stroke(GLYPH_STROKE, cap = StrokeCap.Round),
                )
            }
            shape.dot?.let { dot ->
                if (dotScale > 0f) drawCircle(color, DOT_RADIUS * dotScale, dot)
            }
        }
    }
}

private fun RootStatusColors.of(status: RootStatus): Pair<Color, Color> =
    when (status) {
        RootStatus.ROOTED -> success to onSuccess
        RootStatus.NOT_ROOTED -> failure to onFailure
        RootStatus.NOT_GRANTED -> warning to onWarning
        RootStatus.UNKNOWN,
        RootStatus.NOT_CHECKED,
        RootStatus.CHECKING -> neutral to onNeutral
    }

private val RootStatus.isResult: Boolean
    get() = this != RootStatus.NOT_CHECKED && this != RootStatus.CHECKING

/** Stroke paths on a 24-unit grid, drawn one after another, plus an optional dot that pops last. */
private class GlyphShape(
    paths: List<String>,
    val dot: Offset? = null,
    val fromCenter: Boolean = false,
) {
    val strokes: List<PathMeasure> =
        paths.map { data ->
            PathMeasure().apply { setPath(PathParser().parsePathString(data).toPath(), false) }
        }

    companion object {
        private val check by lazy { GlyphShape(listOf("M6.8,12.4 L10.4,16 L17.2,8.8")) }
        private val cross by lazy {
            GlyphShape(listOf("M8.2,8.2 L15.8,15.8", "M15.8,8.2 L8.2,15.8"))
        }
        private val question by lazy {
            GlyphShape(
                listOf(
                    "M9,9.2 C9,7.4 10.4,6.2 12,6.2 C13.7,6.2 15,7.4 15,9 " +
                        "C15,10.4 14.1,11 13.1,11.6 C12.4,12.1 12,12.6 12,13.6 L12,14"
                ),
                dot = Offset(12f, 17.6f),
            )
        }
        private val exclamation by lazy {
            GlyphShape(listOf("M12,6.6 L12,13.4"), dot = Offset(12f, 17.4f))
        }
        private val dash by lazy { GlyphShape(listOf("M7.5,12 L16.5,12"), fromCenter = true) }

        fun of(status: RootStatus): GlyphShape =
            when (status) {
                RootStatus.ROOTED -> check
                RootStatus.NOT_ROOTED -> cross
                RootStatus.UNKNOWN -> question
                RootStatus.NOT_GRANTED -> exclamation
                RootStatus.NOT_CHECKED,
                RootStatus.CHECKING -> dash
            }
    }
}

private val ICON_SIZE = 96.dp
private val RING_DIAMETER = 88.dp
private val RING_STROKE = 6.dp
private const val VIEWPORT = 24f
private const val GLYPH_STROKE = 2.2f
private const val DOT_RADIUS = 1.35f
private const val MIN_SEGMENT = 0.3f

private const val FULL_SWEEP = 360f
private const val OPEN_SWEEP = 270f
private const val MIN_SWEEP = 30f

private const val SPIN_MS = 1100
private const val BREATHE_MS = 700
private const val CLOSE_MS = 250
private const val FILL_MS = 200
private const val DRAW_MS = 300
private const val UNDRAW_MS = 120
private const val UNDRAW_FAST_MS = 80
private const val DRAIN_MS = 220
private const val DRAIN_DELAY_MS = 70
private const val MIN_TINT_MS = 150

@PreviewLightDark
@Composable
private fun RootStatusIconPreview() {
    BasicRootCheckerTheme {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RootStatus.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { RootStatusIcon(status = it, onSettled = {}) }
                    }
                }
            }
        }
    }
}
