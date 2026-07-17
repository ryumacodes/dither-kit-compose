package com.ditherkit.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal val Bayer4 =
    arrayOf(
            floatArrayOf(0f, 8f, 2f, 10f),
            floatArrayOf(12f, 4f, 14f, 6f),
            floatArrayOf(3f, 11f, 1f, 9f),
            floatArrayOf(15f, 7f, 13f, 5f),
        )
        .map { row -> row.map { (it + .5f) / 16f }.toFloatArray() }
        .toTypedArray()

internal fun clamp01(value: Float): Float = value.coerceIn(0f, 1f)

internal fun easeOutCubic(value: Float): Float = 1f - (1f - value) * (1f - value) * (1f - value)

internal fun easeInOutCubic(value: Float): Float =
    if (value < .5f) 4f * value * value * value
    else 1f - (-2f * value + 2f).let { it * it * it } / 2f

internal fun fnv1a(value: String): UInt {
    var hash = 0x811c9dc5u
    value.forEach { char ->
        hash = hash xor char.code.toUInt()
        hash *= 0x01000193u
    }
    return hash
}

internal class XorShift32(seed: UInt) {
    private var state = if (seed == 0u) 0x9e3779b9u else seed

    fun nextFloat(): Float {
        var x = state
        x = x xor (x shl 13)
        x = x xor (x shr 17)
        x = x xor (x shl 5)
        state = x
        return x.toDouble().div(UInt.MAX_VALUE.toDouble() + 1.0).toFloat()
    }
}

internal data class DitherGrid(
    val columns: Int,
    val rows: Int,
    val cellWidth: Float,
    val cellHeight: Float,
)

internal fun gridFor(width: Float, height: Float, cellSize: Float = 2f): DitherGrid {
    val columns = max(1, min(520, (width / cellSize).toInt()))
    val rows = max(1, min(200, (height / cellSize).toInt()))
    return DitherGrid(columns, rows, width / columns, height / rows)
}

internal fun DrawScope.ditherCell(
    column: Int,
    row: Int,
    grid: DitherGrid,
    color: Color,
    density: Float,
    variant: DitherVariant,
    intensity: Float = 0f,
    bloom: Bloom = Bloom.Off,
    origin: Offset = Offset.Zero,
) {
    if (variant == DitherVariant.Hatched && (column + row) % 4 > 1) return
    val threshold = Bayer4[row and 3][column and 3]
    val bias = if (variant == DitherVariant.Dotted) .12f else 0f
    val lit = variant == DitherVariant.Solid || density > threshold - intensity * .1f - bias
    if (!lit && variant == DitherVariant.Dotted) return

    val alpha =
        if (lit) clamp01((.3f + density * .7f) * (1f + intensity * .22f)) else .12f * density
    if (alpha <= .004f) return
    val topLeft = origin + Offset(column * grid.cellWidth, row * grid.cellHeight)
    val size = Size(ceil(grid.cellWidth), ceil(grid.cellHeight))
    if (bloom.opacity > 0f && lit) {
        val spread = bloom.blurRadius.coerceAtMost(12f)
        drawRect(
            color = color.copy(alpha = alpha * bloom.opacity * .32f),
            topLeft = topLeft - Offset(spread, spread),
            size = Size(size.width + spread * 2f, size.height + spread * 2f),
        )
    }
    drawRect(color.copy(alpha = alpha), topLeft, size)
}

internal fun DrawScope.ditherBand(
    grid: DitherGrid,
    color: Color,
    variant: DitherVariant,
    topAt: (Int) -> Float,
    bottomAt: (Int) -> Float,
    reveal: Float,
    intensityAt: (Int) -> Float = { 0f },
    alpha: Float = 1f,
    bloom: Bloom = Bloom.Off,
) {
    val visibleColumns = floor(grid.columns * reveal).toInt().coerceIn(0, grid.columns)
    for (column in 0 until visibleColumns) {
        val top = min(topAt(column), bottomAt(column)).coerceIn(0f, grid.rows.toFloat())
        val bottom = max(topAt(column), bottomAt(column)).coerceIn(0f, grid.rows.toFloat())
        val start = floor(top).toInt()
        val end = ceil(bottom).toInt().coerceAtMost(grid.rows)
        val depth = max(1f, bottom - top)
        for (row in start until end) {
            val density =
                when (variant) {
                    DitherVariant.Gradient -> .22f + .78f * ((row - top) / depth)
                    DitherVariant.Dotted -> .5f
                    DitherVariant.Hatched -> .75f
                    DitherVariant.Solid -> 1f
                }
            ditherCell(
                column,
                row,
                grid,
                color.copy(alpha = alpha),
                density,
                variant,
                intensityAt(column),
                bloom,
            )
        }
        if (start in 0 until grid.rows) {
            drawRect(
                color.copy(alpha = .72f * alpha),
                Offset(column * grid.cellWidth, start * grid.cellHeight),
                Size(ceil(grid.cellWidth), ceil(grid.cellHeight)),
            )
        }
    }
}
