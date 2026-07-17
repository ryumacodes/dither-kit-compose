package com.ditherkit.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
public fun Sparkline(
    data: List<Number>,
    color: DitherColor,
    modifier: Modifier = Modifier,
    variant: DitherVariant = DitherVariant.Gradient,
    markerIndex: Int? = null,
    hovered: Boolean = false,
    bloom: Bloom = Bloom.Off,
    bloomOnHover: Boolean = false,
    animate: Boolean = false,
) {
    val rows =
        remember(data) {
            data.mapIndexed { index, value ->
                ChartDatum(index.toString(), mapOf("value" to value.toDouble()))
            }
        }
    AreaChart(
        rows,
        modifier,
        bloom = bloom,
        animate = animate,
        interactive = false,
        markerIndex = markerIndex,
        hovered = hovered,
        bloomOnHover = bloomOnHover,
        margins = Margins(0f, 0f, 0f, 0f),
    ) {
        area("value", color = color, variant = variant)
    }
}

@Composable
public fun DitherAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    hue: Float? = null,
    mirror: AvatarMirror = AvatarMirror.Auto,
    bloom: Bloom = Bloom.Off,
    animate: Boolean = true,
    animationDurationMillis: Int = 600,
    replayToken: Int = 0,
) {
    val avatar = remember(name, hue, mirror, replayToken) { avatarData(name, hue, mirror) }
    val revealAnimation =
        remember(name, hue, mirror, replayToken) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(name, hue, mirror, replayToken, animate, animationDurationMillis) {
        if (animate) {
            revealAnimation.snapTo(0f)
            revealAnimation.animateTo(1f, tween(animationDurationMillis.coerceAtLeast(0)))
        } else {
            revealAnimation.snapTo(1f)
        }
    }
    val reveal = revealAnimation.value
    Canvas(modifier.size(size).semantics { contentDescription = "$name avatar" }) {
        val grid = DitherGrid(32, 32, this.size.width / 32f, this.size.height / 32f)
        for (cellY in 0 until 8) for (cellX in 0 until 8) {
            val cellIndex = cellY * 8 + cellX
            if (!avatar.pattern[cellIndex]) continue
            val density = avatar.densities[cellIndex]
            for (subY in 0 until 4) for (subX in 0 until 4) {
                if (reveal < Bayer4[subY][subX]) continue
                val alpha =
                    if (density > Bayer4[subY][subX]) .35f + .65f * density
                    else (.35f + .65f * density) * .35f
                ditherCell(
                    cellX * 4 + subX,
                    cellY * 4 + subY,
                    grid,
                    avatar.color.copy(alpha = alpha),
                    1f,
                    DitherVariant.Solid,
                    bloom = bloom,
                )
            }
        }
    }
}

private data class AvatarData(
    val pattern: BooleanArray,
    val densities: FloatArray,
    val color: Color,
)

private fun avatarData(
    name: String,
    hueOverride: Float?,
    mirrorOverride: AvatarMirror,
): AvatarData {
    val random = XorShift32(fnv1a(name))
    val bits = BooleanArray(32) { random.nextFloat() < .5f }
    val automaticMirror =
        if (random.nextFloat() < .5f) AvatarMirror.Vertical else AvatarMirror.Horizontal
    val generatedHue = (random.nextFloat() * 180f).toInt() * 2f
    val freeDensities = FloatArray(32) { .55f + random.nextFloat() * .45f }
    val mirror = if (mirrorOverride == AvatarMirror.Auto) automaticMirror else mirrorOverride
    val pattern = BooleanArray(64)
    val densities = FloatArray(64)
    for (y in 0 until 8) for (x in 0 until 8) {
        val freeIndex =
            if (mirror == AvatarMirror.Horizontal) y * 4 + minOf(x, 7 - x)
            else minOf(y, 7 - y) * 8 + x
        pattern[y * 8 + x] = bits[freeIndex]
        densities[y * 8 + x] = freeDensities[freeIndex]
    }
    return AvatarData(pattern, densities, hueColor(hueOverride ?: generatedHue))
}

@Composable
public fun DitherButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: DitherColor = DitherColor.Blue,
    hue: Float? = null,
    variant: DitherVariant = DitherVariant.Gradient,
    bloom: Bloom = Bloom.Off,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val hovered by interactions.collectIsHoveredAsState()
    val target =
        when {
            pressed -> 1.5f
            hovered -> 1f
            else -> 0f
        }
    val intensity by animateFloatAsState(target, label = "button-intensity")
    val fill = hue?.let(::hueColor) ?: color.fill
    Box(
        modifier
            .defaultMinSize(minWidth = 80.dp, minHeight = 38.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = interactions,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val grid = gridFor(size.width, size.height, 2f)
            for (row in 0 until grid.rows) for (column in 0 until grid.columns) {
                val density =
                    when (variant) {
                        DitherVariant.Gradient -> .25f + .75f * row / max(1f, grid.rows - 1f)
                        DitherVariant.Dotted -> .5f
                        DitherVariant.Hatched -> .75f
                        DitherVariant.Solid -> 1f
                    }
                ditherCell(
                    column,
                    row,
                    grid,
                    fill.copy(alpha = if (enabled) 1f else .4f),
                    density,
                    variant,
                    intensity,
                    bloom,
                )
            }
        }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), content = content)
    }
}

@Composable
public fun DitherGradient(
    from: DitherColor? = null,
    modifier: Modifier = Modifier,
    to: DitherColor? = null,
    fromHue: Float? = null,
    toHue: Float? = null,
    direction: GradientDirection = GradientDirection.Up,
    cellSize: Dp = 3.dp,
    opacity: Float = 1f,
    bloom: Bloom = Bloom.Off,
) {
    val fromFill = fromHue?.let(::hueColor) ?: (from ?: DitherColor.Blue).fill
    val toFill = toHue?.let(::hueColor) ?: to?.fill
    Canvas(modifier) {
        val grid = gridFor(size.width, size.height, cellSize.toPx())
        for (row in 0 until grid.rows) for (column in 0 until grid.columns) {
            val t =
                when (direction) {
                    GradientDirection.Up -> 1f - row.toFloat() / max(1, grid.rows - 1)
                    GradientDirection.Down -> row.toFloat() / max(1, grid.rows - 1)
                    GradientDirection.Left -> 1f - column.toFloat() / max(1, grid.columns - 1)
                    GradientDirection.Right -> column.toFloat() / max(1, grid.columns - 1)
                }
            val threshold = Bayer4[row and 3][column and 3]
            val fromCell = (1f - t) > threshold
            val color = if (fromCell || toFill == null) fromFill else toFill
            val alpha =
                if (toFill == null) {
                    if (fromCell) (.35f + .65f * (1f - t)) * opacity else .12f * (1f - t) * opacity
                } else opacity
            if (alpha > .004f)
                ditherCell(
                    column,
                    row,
                    grid,
                    color.copy(alpha = alpha),
                    1f,
                    DitherVariant.Solid,
                    bloom = bloom,
                )
        }
    }
}
