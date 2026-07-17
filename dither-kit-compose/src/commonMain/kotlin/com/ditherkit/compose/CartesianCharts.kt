package com.ditherkit.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private enum class CartesianKind {
    Area,
    Line,
    Bar,
}

@Composable
public fun AreaChart(
    data: List<ChartDatum>,
    modifier: Modifier = Modifier,
    stackType: StackType = StackType.Default,
    bloom: Bloom = Bloom.Off,
    animate: Boolean = true,
    animationDurationMillis: Int = 900,
    replayToken: Int = 0,
    interactive: Boolean = true,
    markerIndex: Int? = null,
    hovered: Boolean = false,
    bloomOnHover: Boolean = false,
    margins: Margins = Margins(),
    onHoverChange: (Int?) -> Unit = {},
    defaultSelectedDataKey: String? = null,
    onSelectionChange: (String?) -> Unit = {},
    contentDescription: String? = null,
    content: CartesianChartScope.() -> Unit,
) {
    CartesianChart(
        CartesianKind.Area,
        data,
        modifier,
        stackType,
        bloom,
        animate,
        animationDurationMillis,
        replayToken,
        interactive,
        markerIndex,
        hovered,
        bloomOnHover,
        margins,
        onHoverChange,
        defaultSelectedDataKey,
        onSelectionChange,
        contentDescription,
        content,
    )
}

@Composable
public fun LineChart(
    data: List<ChartDatum>,
    modifier: Modifier = Modifier,
    stackType: StackType = StackType.Default,
    bloom: Bloom = Bloom.Off,
    animate: Boolean = true,
    animationDurationMillis: Int = 900,
    replayToken: Int = 0,
    interactive: Boolean = true,
    markerIndex: Int? = null,
    hovered: Boolean = false,
    bloomOnHover: Boolean = false,
    margins: Margins = Margins(),
    onHoverChange: (Int?) -> Unit = {},
    defaultSelectedDataKey: String? = null,
    onSelectionChange: (String?) -> Unit = {},
    contentDescription: String? = null,
    content: CartesianChartScope.() -> Unit,
) {
    CartesianChart(
        CartesianKind.Line,
        data,
        modifier,
        stackType,
        bloom,
        animate,
        animationDurationMillis,
        replayToken,
        interactive,
        markerIndex,
        hovered,
        bloomOnHover,
        margins,
        onHoverChange,
        defaultSelectedDataKey,
        onSelectionChange,
        contentDescription,
        content,
    )
}

@Composable
public fun BarChart(
    data: List<ChartDatum>,
    modifier: Modifier = Modifier,
    stackType: StackType = StackType.Default,
    bloom: Bloom = Bloom.Off,
    animate: Boolean = true,
    animationDurationMillis: Int = 900,
    replayToken: Int = 0,
    interactive: Boolean = true,
    markerIndex: Int? = null,
    hovered: Boolean = false,
    bloomOnHover: Boolean = false,
    margins: Margins = Margins(),
    onHoverChange: (Int?) -> Unit = {},
    defaultSelectedDataKey: String? = null,
    onSelectionChange: (String?) -> Unit = {},
    contentDescription: String? = null,
    content: CartesianChartScope.() -> Unit,
) {
    CartesianChart(
        CartesianKind.Bar,
        data,
        modifier,
        stackType,
        bloom,
        animate,
        animationDurationMillis,
        replayToken,
        interactive,
        markerIndex,
        hovered,
        bloomOnHover,
        margins,
        onHoverChange,
        defaultSelectedDataKey,
        onSelectionChange,
        contentDescription,
        content,
    )
}

@Composable
private fun CartesianChart(
    kind: CartesianKind,
    data: List<ChartDatum>,
    modifier: Modifier,
    stackType: StackType,
    bloom: Bloom,
    animate: Boolean,
    animationDurationMillis: Int,
    replayToken: Int,
    interactive: Boolean,
    controlledMarkerIndex: Int?,
    externallyHovered: Boolean,
    bloomOnHover: Boolean,
    margins: Margins,
    onHoverChange: (Int?) -> Unit,
    defaultSelectedDataKey: String?,
    onSelectionChange: (String?) -> Unit,
    contentDescription: String?,
    content: CartesianChartScope.() -> Unit,
) {
    val parts = remember(content) { CartesianParts().also { CartesianChartScope(it).content() } }
    var hoverIndex by remember { mutableStateOf<Int?>(null) }
    var selectedKey by remember(defaultSelectedDataKey) { mutableStateOf(defaultSelectedDataKey) }
    var focusedKey by remember { mutableStateOf<String?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val reveal = remember(data, content, replayToken) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(data, animate, animationDurationMillis, replayToken) {
        if (animate) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(animationDurationMillis.coerceAtLeast(0)))
        } else {
            reveal.snapTo(1f)
        }
    }
    val progress = reveal.value
    val sparkleTransition = rememberInfiniteTransition(label = "dither-sparkles")
    val sparklePhase by
        sparkleTransition.animateFloat(
            initialValue = 0f,
            targetValue = (kotlin.math.PI * 2).toFloat(),
            animationSpec =
                infiniteRepeatable(tween(3_600, easing = LinearEasing), RepeatMode.Restart),
            label = "sparkle-phase",
        )
    val markerIndex = hoverIndex ?: controlledMarkerIndex
    val isHovered = externallyHovered || hoverIndex != null
    val activeBloom = if (bloomOnHover && !isHovered) Bloom.Off else bloom
    val textMeasurer = rememberTextMeasurer()
    val foreground = Color(0xffd4d4d8)
    val muted = Color(0xff71717a)
    val border = Color(0xff3f3f46)
    val textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = muted)

    val accessibleDescription =
        contentDescription
            ?: "${kind.name} chart, ${data.size} data points, ${parts.series.size} series"

    Column(modifier.semantics { this.contentDescription = accessibleDescription }) {
        if (parts.legend) {
            ChartLegend(
                parts.series,
                selectedKey,
                parts.legendClickable,
                parts.legendAlignment,
                onFocusChange = { focusedKey = it },
            ) { key ->
                selectedKey = if (selectedKey == key) null else key
                onSelectionChange(selectedKey)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                Modifier.fillMaxSize()
                    .onSizeChanged { canvasSize = it }
                    .then(
                        if (interactive)
                            Modifier.pointerInput(data, margins) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.type == PointerEventType.Exit) {
                                            if (hoverIndex != null) {
                                                hoverIndex = null
                                                onHoverChange(null)
                                            }
                                            continue
                                        }
                                        val point =
                                            event.changes.firstOrNull()?.position ?: continue
                                        if (data.isNotEmpty()) {
                                            val left = if (parts.yAxis) margins.start else 4f
                                            val right = margins.end
                                            val plotWidth =
                                                (size.width - left - right).coerceAtLeast(1f)
                                            val relative =
                                                ((point.x - left) / plotWidth).coerceIn(0f, 1f)
                                            val next =
                                                if (kind == CartesianKind.Bar)
                                                    (relative * data.size)
                                                        .toInt()
                                                        .coerceIn(0, data.lastIndex)
                                                else
                                                    (relative * max(1, data.lastIndex))
                                                        .toInt()
                                                        .coerceIn(0, data.lastIndex)
                                            if (next != hoverIndex) {
                                                hoverIndex = next
                                                onHoverChange(next)
                                            }
                                            if (event.changes.any { it.changedToUp() }) {
                                                closestClickableSeries(
                                                        point = point,
                                                        index = next,
                                                        width = size.width.toFloat(),
                                                        height = size.height.toFloat(),
                                                        kind = kind,
                                                        data = data,
                                                        series = parts.series,
                                                        stackType = stackType,
                                                        margins = margins,
                                                    )
                                                    ?.let { key ->
                                                        selectedKey =
                                                            if (selectedKey == key) null else key
                                                        onSelectionChange(selectedKey)
                                                    }
                                            }
                                        }
                                    }
                                }
                            }
                        else Modifier
                    )
            ) {
                if (data.isEmpty() || parts.series.isEmpty()) return@Canvas
                val left = if (parts.yAxis) margins.start else 4f
                val bottom = if (parts.xAxis) margins.bottom else 4f
                val top = margins.top
                val right = margins.end
                val plotWidth = (size.width - left - right).coerceAtLeast(1f)
                val plotHeight = (size.height - top - bottom).coerceAtLeast(1f)
                val values = data.flatMap { row -> parts.series.map { row[it.dataKey] } }
                val stackedMax =
                    if (stackType != StackType.Default)
                        data.maxOfOrNull { row -> parts.series.sumOf { max(0.0, row[it.dataKey]) } }
                            ?: 0.0
                    else 0.0
                val stackedMin =
                    if (stackType != StackType.Default)
                        data.minOfOrNull { row -> parts.series.sumOf { min(0.0, row[it.dataKey]) } }
                            ?: 0.0
                    else 0.0
                var domainMin = min(0.0, min(values.minOrNull() ?: 0.0, stackedMin))
                var domainMax = max(0.0, max(values.maxOrNull() ?: 0.0, stackedMax))
                if (stackType == StackType.Percent) {
                    domainMin = 0.0
                    domainMax = 1.0
                }
                if (abs(domainMax - domainMin) < 1e-9) domainMax = domainMin + 1.0
                fun y(value: Double): Float =
                    top + ((domainMax - value) / (domainMax - domainMin)).toFloat() * plotHeight

                if (parts.grid) {
                    val gridEffect =
                        if (parts.gridStrokeVariant == StrokeVariant.Dashed)
                            PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
                        else null
                    if (parts.gridHorizontal)
                        repeat(5) { tick ->
                            val yy = top + plotHeight * tick / 4f
                            drawLine(
                                border.copy(alpha = .45f),
                                Offset(left, yy),
                                Offset(left + plotWidth, yy),
                                1f,
                                pathEffect = gridEffect,
                            )
                        }
                    if (parts.gridVertical)
                        data.indices.forEach { index ->
                            val xx =
                                left +
                                    if (kind == CartesianKind.Bar)
                                        plotWidth * (index + .5f) / data.size
                                    else plotWidth * index / max(1, data.lastIndex)
                            drawLine(
                                border.copy(alpha = .45f),
                                Offset(xx, top),
                                Offset(xx, top + plotHeight),
                                1f,
                                pathEffect = gridEffect,
                            )
                        }
                }
                parts.referenceLines.forEach { line ->
                    val yy = y(line.value)
                    val effect =
                        if (line.strokeVariant == StrokeVariant.Dashed)
                            PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        else null
                    drawLine(
                        muted,
                        Offset(left, yy),
                        Offset(left + plotWidth, yy),
                        1f,
                        pathEffect = effect,
                    )
                    line.label?.let { label ->
                        drawText(
                            textMeasurer,
                            label,
                            Offset(left + plotWidth - 4f - label.length * 6f, yy - 14f),
                            style = textStyle,
                        )
                    }
                }
                if (parts.yAxis) {
                    val count = parts.yAxisTickCount
                    repeat(count + 1) { tick ->
                        val value = domainMax - (domainMax - domainMin) * tick / count.toDouble()
                        val label =
                            parts.yAxisTickFormatter?.invoke(value)
                                ?: if (stackType == StackType.Percent) "${(value * 100).toInt()}%"
                                else compact(value)
                        drawText(
                            textMeasurer,
                            label,
                            Offset(
                                (left - parts.yAxisTickMargin - label.length * 6f).coerceAtLeast(
                                    0f
                                ),
                                top + plotHeight * tick / count.toFloat() - 7f,
                            ),
                            style = textStyle,
                        )
                    }
                }
                if (parts.xAxis) {
                    val step =
                        max(1, kotlin.math.ceil(data.size.toDouble() / parts.xAxisMaxTicks).toInt())
                    data.indices
                        .filter { it % step == 0 || it == data.lastIndex }
                        .forEach { index ->
                            val x =
                                left +
                                    if (kind == CartesianKind.Bar)
                                        plotWidth * (index + .5f) / data.size
                                    else plotWidth * index / max(1, data.lastIndex)
                            val label =
                                parts.xAxisTickFormatter?.invoke(data[index].label, index)
                                    ?: data[index].label
                            drawText(
                                textMeasurer,
                                label.take(12),
                                Offset(x - 12f, top + plotHeight + parts.xAxisTickMargin),
                                style = textStyle,
                            )
                        }
                }

                val grid = gridFor(plotWidth, plotHeight)
                val zeroRow =
                    ((y(0.0) - top) / plotHeight * grid.rows).coerceIn(0f, grid.rows.toFloat())
                val positiveTotals =
                    DoubleArray(data.size) { index ->
                        parts.series.sumOf { max(0.0, data[index][it.dataKey]) }
                    }
                val negativeTotals =
                    DoubleArray(data.size) { index ->
                        parts.series.sumOf { min(0.0, data[index][it.dataKey]) }
                    }
                val positiveBase = DoubleArray(data.size)
                val negativeBase = DoubleArray(data.size)

                parts.series.forEachIndexed { seriesIndex, series ->
                    val emphasis = selectedKey ?: focusedKey
                    val dim = emphasis != null && emphasis != series.dataKey
                    val alpha = if (dim) .3f else 1f
                    val tops = FloatArray(data.size)
                    val bottoms = FloatArray(data.size)
                    data.indices.forEach { index ->
                        val raw = data[index][series.dataKey]
                        val value =
                            if (stackType == StackType.Percent) {
                                val total =
                                    if (raw >= 0) positiveTotals[index] else -negativeTotals[index]
                                if (total == 0.0) 0.0 else raw / total
                            } else raw
                        val base =
                            when {
                                stackType == StackType.Default -> 0.0
                                value >= 0 -> positiveBase[index]
                                else -> negativeBase[index]
                            }
                        val end = base + value
                        if (stackType != StackType.Default) {
                            if (value >= 0) positiveBase[index] = end else negativeBase[index] = end
                        }
                        tops[index] = ((y(end) - top) / plotHeight * grid.rows)
                        bottoms[index] =
                            if (series.kind == SeriesKind.Line)
                                (tops[index] + max(6f, grid.rows * .16f)).coerceAtMost(
                                    grid.rows.toFloat()
                                )
                            else ((y(base) - top) / plotHeight * grid.rows)
                    }

                    if (kind == CartesianKind.Bar || series.kind == SeriesKind.Bar) {
                        val categoryWidth = grid.columns.toFloat() / data.size
                        val seriesWidth =
                            if (stackType == StackType.Default) categoryWidth / parts.series.size
                            else categoryWidth
                        data.indices.forEach { index ->
                            val start =
                                (index * categoryWidth +
                                        if (stackType == StackType.Default)
                                            seriesIndex * seriesWidth
                                        else 0f + seriesWidth * .12f)
                                    .toInt()
                            val end =
                                ((index + 1) * categoryWidth - seriesWidth * .12f)
                                    .toInt()
                                    .coerceAtMost(grid.columns)
                            for (column in start until end) {
                                val localGrid = grid
                                val topRow = min(tops[index], bottoms[index]).toInt()
                                val bottomRow =
                                    max(tops[index], bottoms[index]).toInt().coerceAtMost(grid.rows)
                                val grow =
                                    easeOutCubic(
                                        (progress * 1.25f - index.toFloat() / data.size * .25f)
                                            .coerceIn(0f, 1f)
                                    )
                                val grownTop = bottomRow - ((bottomRow - topRow) * grow).toInt()
                                for (row in grownTop until bottomRow) {
                                    val density =
                                        when (series.variant) {
                                            DitherVariant.Gradient ->
                                                .25f +
                                                    .75f * (row - grownTop).toFloat() /
                                                        max(1, bottomRow - grownTop)
                                            DitherVariant.Dotted -> .5f
                                            DitherVariant.Hatched -> .75f
                                            DitherVariant.Solid -> 1f
                                        }
                                    ditherCell(
                                        column,
                                        row,
                                        localGrid,
                                        series.color.fill.copy(alpha = alpha),
                                        density,
                                        series.variant,
                                        if (isHovered) 1f else 0f,
                                        activeBloom,
                                        origin = Offset(left, top),
                                    )
                                }
                            }
                        }
                    } else {
                        fun interpolate(valuesArray: FloatArray, column: Int): Float {
                            if (valuesArray.size == 1) return valuesArray[0]
                            val p =
                                column.toFloat() / max(1, grid.columns - 1) * (valuesArray.size - 1)
                            val low = p.toInt().coerceAtMost(valuesArray.lastIndex)
                            val high = (low + 1).coerceAtMost(valuesArray.lastIndex)
                            return valuesArray[low] +
                                (valuesArray[high] - valuesArray[low]) * (p - low)
                        }
                        ditherBand(
                            grid,
                            series.color.fill,
                            series.variant,
                            topAt = { interpolate(tops, it) },
                            bottomAt = { interpolate(bottoms, it) },
                            reveal = easeInOutCubic(progress),
                            alpha = alpha,
                            intensityAt = { if (isHovered) 1f else 0f },
                            bloom = activeBloom,
                        )
                        if (series.kind == SeriesKind.Line) {
                            val path = Path()
                            tops.indices.forEach { index ->
                                val x = left + plotWidth * index / max(1, tops.lastIndex)
                                val yy = top + tops[index] / grid.rows * plotHeight
                                if (index == 0) path.moveTo(x, yy) else path.lineTo(x, yy)
                            }
                            val effect =
                                if (series.strokeVariant == StrokeVariant.Dashed)
                                    PathEffect.dashPathEffect(floatArrayOf(5f, 4f))
                                else null
                            drawPath(
                                path,
                                series.color.line.copy(alpha = alpha),
                                style = Stroke(width = 2f, pathEffect = effect),
                            )
                        }
                    }
                    series.dot?.let { dot ->
                        tops.indices.forEach { index ->
                            val x =
                                left +
                                    if (kind == CartesianKind.Bar)
                                        plotWidth * (index + .5f) / data.size
                                    else plotWidth * index / max(1, tops.lastIndex)
                            val yy = top + tops[index] / grid.rows * plotHeight
                            drawChartDot(Offset(x, yy), dot, series.color, alpha)
                        }
                    }
                    if (progress >= .999f)
                        markerIndex?.let { index ->
                            if (index in tops.indices)
                                series.activeDot?.let { dot ->
                                    val x =
                                        left +
                                            if (kind == CartesianKind.Bar)
                                                plotWidth * (index + .5f) / data.size
                                            else plotWidth * index / max(1, tops.lastIndex)
                                    val yy = top + tops[index] / grid.rows * plotHeight
                                    drawCircle(
                                        series.color.line.copy(alpha = .18f * alpha),
                                        dot.radius + 3f,
                                        Offset(x, yy),
                                    )
                                    drawChartDot(Offset(x, yy), dot, series.color, alpha)
                                }
                        }
                    if (kind != CartesianKind.Bar && series.kind != SeriesKind.Bar) {
                        val count = max(4, grid.columns / 14)
                        repeat(count) { starIndex ->
                            val seed = starIndex * 67 + 13 + seriesIndex * 131
                            val dataIndex = seed % max(1, data.size)
                            val column =
                                (dataIndex.toFloat() / max(1, data.lastIndex) * (grid.columns - 1))
                                    .toInt()
                            val topRow = tops.getOrNull(dataIndex) ?: return@repeat
                            val bottomRow = bottoms.getOrNull(dataIndex) ?: return@repeat
                            val depth = ((seed * 53 + 7) % 100) / 100f
                            val row = topRow + depth * (bottomRow - topRow)
                            val twinkle =
                                (sin(sparklePhase + ((seed * 41) % 360) * .0174533f) + 1f) / 2f
                            if (twinkle >= .55f) {
                                val center =
                                    Offset(
                                        left + (column + .5f) * grid.cellWidth,
                                        top + (row + .5f) * grid.cellHeight,
                                    )
                                drawRect(
                                    series.color.fill.copy(alpha = twinkle * alpha),
                                    center - Offset(grid.cellWidth / 2f, grid.cellHeight / 2f),
                                    androidx.compose.ui.geometry.Size(
                                        grid.cellWidth,
                                        grid.cellHeight,
                                    ),
                                )
                                if (twinkle > .9f) {
                                    drawLine(
                                        series.color.fill.copy(alpha = twinkle * .6f * alpha),
                                        center - Offset(grid.cellWidth, 0f),
                                        center + Offset(grid.cellWidth, 0f),
                                        grid.cellHeight,
                                    )
                                    drawLine(
                                        series.color.fill.copy(alpha = twinkle * .6f * alpha),
                                        center - Offset(0f, grid.cellHeight),
                                        center + Offset(0f, grid.cellHeight),
                                        grid.cellWidth,
                                    )
                                }
                            }
                        }
                    }
                }

                markerIndex?.let { index ->
                    val x =
                        left +
                            if (kind == CartesianKind.Bar) plotWidth * (index + .5f) / data.size
                            else plotWidth * index / max(1, data.lastIndex)
                    drawLine(
                        foreground.copy(alpha = .5f),
                        Offset(x, top),
                        Offset(x, top + plotHeight),
                        1f,
                    )
                }
            }

            if (parts.tooltip)
                markerIndex?.let { index ->
                    data.getOrNull(index)?.let { datum ->
                        val left = if (parts.yAxis) margins.start else 4f
                        val plotWidth = (canvasSize.width - left - margins.end).coerceAtLeast(1f)
                        val x =
                            left +
                                if (kind == CartesianKind.Bar)
                                    plotWidth * (index + .5f) / max(1, data.size)
                                else plotWidth * index / max(1, data.lastIndex)
                        TooltipCard(
                            datum,
                            parts.series,
                            parts.tooltipVariant,
                            parts.tooltipValueFormatter,
                            Modifier.align(Alignment.TopStart)
                                .offset { IntOffset(x.toInt(), 8) }
                                .graphicsLayer { translationX = -size.width / 2f },
                        )
                    }
                }
        }
    }
}

@Composable
internal fun ChartLegend(
    series: List<ChartSeries>,
    selectedKey: String?,
    clickable: Boolean,
    alignment: LegendAlignment = LegendAlignment.Start,
    onFocusChange: (String?) -> Unit = {},
    onSelect: (String) -> Unit,
) {
    val arrangement =
        when (alignment) {
            LegendAlignment.Start -> Arrangement.spacedBy(12.dp, Alignment.Start)
            LegendAlignment.Center -> Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            LegendAlignment.End -> Arrangement.spacedBy(12.dp, Alignment.End)
        }
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = arrangement) {
        series.forEach { item ->
            val interactions = remember(item.dataKey) { MutableInteractionSource() }
            val hovered by interactions.collectIsHoveredAsState()
            LaunchedEffect(hovered, item.dataKey) {
                onFocusChange(if (hovered) item.dataKey else null)
            }
            Row(
                Modifier.hoverable(interactions)
                    .then(
                        if (clickable || item.isClickable)
                            Modifier.clickable { onSelect(item.dataKey) }
                        else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    Modifier.size(8.dp)
                        .background(
                            item.color.fill.copy(
                                alpha =
                                    if (selectedKey == null || selectedKey == item.dataKey) 1f
                                    else .3f
                            )
                        )
                )
                androidx.compose.foundation.text.BasicText(
                    item.label,
                    style =
                        TextStyle(
                            color = Color(0xffa1a1aa),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                        ),
                )
            }
        }
    }
}

@Composable
internal fun TooltipCard(
    datum: ChartDatum,
    series: List<ChartSeries>,
    variant: TooltipVariant,
    valueFormatter: ((Double, String) -> String)? = null,
    modifier: Modifier = Modifier,
) {
    val background =
        if (variant == TooltipVariant.FrostedGlass) Color(0xcc18181b) else Color(0xff18181b)
    Column(modifier.background(background, RoundedCornerShape(6.dp)).padding(8.dp)) {
        androidx.compose.foundation.text.BasicText(
            datum.label,
            style = TextStyle(Color.White, 11.sp, fontFamily = FontFamily.Monospace),
        )
        series.forEach { item ->
            val value =
                valueFormatter?.invoke(datum[item.dataKey], item.dataKey)
                    ?: compact(datum[item.dataKey])
            androidx.compose.foundation.text.BasicText(
                "${item.label}: $value",
                style = TextStyle(item.color.line, 10.sp, fontFamily = FontFamily.Monospace),
            )
        }
    }
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChartDot(
    center: Offset,
    dot: DotStyle,
    color: DitherColor,
    alpha: Float,
) {
    when (dot.variant) {
        DotVariant.Filled -> {
            drawCircle(color.star.copy(alpha = alpha), dot.radius, center)
            drawCircle(color.line.copy(alpha = alpha), dot.radius, center, style = Stroke(1f))
        }
        DotVariant.ColoredBorder -> {
            drawCircle(Color(0xff0b0b0c).copy(alpha = alpha), dot.radius, center)
            drawCircle(color.line.copy(alpha = alpha), dot.radius, center, style = Stroke(1.5f))
        }
        DotVariant.Border -> {
            drawCircle(Color(0xff0b0b0c).copy(alpha = alpha), dot.radius, center)
            drawCircle(color.star.copy(alpha = .8f * alpha), dot.radius, center, style = Stroke(1f))
        }
    }
}

internal fun compact(value: Double): String =
    when {
        abs(value) >= 1_000_000 -> "${(value / 1_000_000).toInt()}m"
        abs(value) >= 1_000 -> "${(value / 1_000).toInt()}k"
        value % 1.0 == 0.0 -> value.toInt().toString()
        else -> ((value * 10).toInt() / 10.0).toString()
    }

private fun closestClickableSeries(
    point: Offset,
    index: Int,
    width: Float,
    height: Float,
    kind: CartesianKind,
    data: List<ChartDatum>,
    series: List<ChartSeries>,
    stackType: StackType,
    margins: Margins,
): String? {
    val clickable = series.filter { it.isClickable }
    if (clickable.isEmpty() || index !in data.indices) return null
    val values = data.flatMap { row -> series.map { row[it.dataKey] } }
    val stackedMax =
        if (stackType != StackType.Default)
            data.maxOfOrNull { row -> series.sumOf { max(0.0, row[it.dataKey]) } } ?: 0.0
        else 0.0
    val stackedMin =
        if (stackType != StackType.Default)
            data.minOfOrNull { row -> series.sumOf { min(0.0, row[it.dataKey]) } } ?: 0.0
        else 0.0
    var domainMin = min(0.0, min(values.minOrNull() ?: 0.0, stackedMin))
    var domainMax = max(0.0, max(values.maxOrNull() ?: 0.0, stackedMax))
    if (stackType == StackType.Percent) {
        domainMin = 0.0
        domainMax = 1.0
    }
    if (abs(domainMax - domainMin) < 1e-9) domainMax = domainMin + 1.0
    val top = margins.top
    val bottom = margins.bottom
    val plotHeight = (height - top - bottom).coerceAtLeast(1f)
    fun y(value: Double): Float =
        top + ((domainMax - value) / (domainMax - domainMin)).toFloat() * plotHeight
    var positive = 0.0
    var negative = 0.0
    var closest: Pair<String, Float>? = null
    series.forEach { item ->
        val raw = data[index][item.dataKey]
        val value =
            if (stackType == StackType.Percent) {
                val total =
                    if (raw >= 0) series.sumOf { max(0.0, data[index][it.dataKey]) }
                    else -series.sumOf { min(0.0, data[index][it.dataKey]) }
                if (total == 0.0) 0.0 else raw / total
            } else raw
        val base =
            when {
                stackType == StackType.Default -> 0.0
                value >= 0 -> positive
                else -> negative
            }
        val end = base + value
        if (stackType != StackType.Default) {
            if (value >= 0) positive = end else negative = end
        }
        if (item.isClickable) {
            val distance =
                if (kind == CartesianKind.Bar) {
                    val low = min(y(base), y(end))
                    val high = max(y(base), y(end))
                    when {
                        point.y < low -> low - point.y
                        point.y > high -> point.y - high
                        else -> 0f
                    }
                } else abs(point.y - y(end))
            if (distance < (closest?.second ?: Float.POSITIVE_INFINITY))
                closest = item.dataKey to distance
        }
    }
    return closest?.takeIf { it.second <= if (kind == CartesianKind.Bar) 8f else 24f }?.first
}
