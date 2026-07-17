package com.ditherkit.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
public fun PieChart(
    data: List<ChartDatum>,
    dataKey: String,
    colors: Map<String, DitherColor>,
    modifier: Modifier = Modifier,
    innerRadius: Float = 0f,
    bloom: Bloom = Bloom.Off,
    animate: Boolean = true,
    animationDurationMillis: Int = 900,
    replayToken: Int = 0,
    bloomOnHover: Boolean = false,
    margins: Margins = Margins(),
    defaultSelectedDataKey: String? = null,
    onSelectionChange: (String?) -> Unit = {},
    contentDescription: String? = null,
    content: PieChartScope.() -> Unit = { pie() },
) {
    val parts = remember(content) { PolarParts().also { PieChartScope(it).content() } }
    var selected by remember(defaultSelectedDataKey) { mutableStateOf(defaultSelectedDataKey) }
    var focused by remember { mutableStateOf<String?>(null) }
    var hovered by remember { mutableStateOf<Int?>(null) }
    val revealAnimation =
        remember(data, content, replayToken) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(data, animate, animationDurationMillis, replayToken) {
        if (animate) {
            revealAnimation.snapTo(0f)
            revealAnimation.animateTo(1f, tween(animationDurationMillis.coerceAtLeast(0)))
        } else {
            revealAnimation.snapTo(1f)
        }
    }
    val reveal = revealAnimation.value
    val series = data.map {
        ChartSeries(
            it.label,
            it.label,
            colors[it.label] ?: DitherColor.Blue,
            parts.pieVariant,
            isClickable = true,
        )
    }

    val accessibleDescription = contentDescription ?: "Pie chart, ${data.size} slices"
    Column(modifier.semantics { this.contentDescription = accessibleDescription }) {
        if (parts.legend)
            ChartLegend(
                series,
                selected,
                parts.legendClickable,
                parts.legendAlignment,
                onFocusChange = { focused = it },
            ) { key ->
                selected = if (selected == key) null else key
                onSelectionChange(selected)
            }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(data) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Exit) {
                                hovered = null
                                continue
                            }
                            val point = event.changes.firstOrNull()?.position ?: continue
                            val index =
                                pieIndexAt(
                                    point,
                                    size.width.toFloat(),
                                    size.height.toFloat(),
                                    data,
                                    dataKey,
                                    innerRadius,
                                    margins,
                                )
                            hovered = index
                            if (index != null && event.changes.any { it.changedToUp() }) {
                                val key = data[index].label
                                selected = if (selected == key) null else key
                                onSelectionChange(selected)
                            }
                        }
                    }
                }
            ) {
                if (data.isEmpty()) return@Canvas
                val center =
                    Offset(
                        (size.width + margins.start - margins.end) / 2f,
                        (size.height + margins.top - margins.bottom) / 2f,
                    )
                val outer =
                    min(
                            size.width - margins.start - margins.end,
                            size.height - margins.top - margins.bottom,
                        )
                        .coerceAtLeast(1f) * .44f
                val inner = outer * innerRadius.coerceIn(0f, 1f)
                val total =
                    data.sumOf { max(0.0, it[dataKey]) }.takeIf { it > 0.0 } ?: return@Canvas
                val grid = gridFor(size.width, size.height)
                val values = data.map { max(0.0, it[dataKey]) }
                val starts = DoubleArray(data.size)
                var cumulative = 0.0
                values.indices.forEach { index ->
                    starts[index] = cumulative / total * PI * 2.0
                    cumulative += values[index]
                }
                val ends =
                    DoubleArray(data.size) { index ->
                        (starts[index] + values[index] / total * PI * 2.0)
                    }
                val revealedAngle = PI * 2.0 * easeInOutCubic(reveal)

                for (row in 0 until grid.rows) for (column in 0 until grid.columns) {
                    val px = (column + .5f) * grid.cellWidth
                    val py = (row + .5f) * grid.cellHeight
                    val dx = px - center.x
                    val dy = py - center.y
                    val radius = hypot(dx, dy)
                    if (radius !in inner..outer) continue
                    var angle = atan2(dy, dx) + PI / 2.0
                    if (angle < 0) angle += PI * 2.0
                    if (angle > revealedAngle) continue
                    val index =
                        starts.indices.lastOrNull { angle >= starts[it] && angle <= ends[it] }
                            ?: continue
                    val emphasis = selected ?: focused
                    val dim = emphasis != null && emphasis != data[index].label
                    val hoverLift = if (hovered == index) 1f else 0f
                    val density = .2f + .8f * ((radius - inner) / max(1f, outer - inner))
                    ditherCell(
                        column,
                        row,
                        grid,
                        (colors[data[index].label] ?: DitherColor.Blue)
                            .fill
                            .copy(alpha = if (dim) .3f else 1f),
                        density,
                        parts.pieVariant,
                        hoverLift,
                        if (bloomOnHover && hovered == null) Bloom.Off else bloom,
                    )
                }
            }
            if (parts.tooltip)
                hovered?.let { index ->
                    data.getOrNull(index)?.let { datum ->
                        val key = datum.label
                        val item = ChartSeries(key, key, colors[key] ?: DitherColor.Blue)
                        TooltipCard(
                            ChartDatum(key, mapOf(key to datum[dataKey])),
                            listOf(item),
                            parts.tooltipVariant,
                            parts.tooltipValueFormatter,
                            Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                        )
                    }
                }
        }
    }
}

private fun pieIndexAt(
    point: Offset,
    width: Float,
    height: Float,
    data: List<ChartDatum>,
    dataKey: String,
    innerRadius: Float,
    margins: Margins,
): Int? {
    val center =
        Offset(
            (width + margins.start - margins.end) / 2f,
            (height + margins.top - margins.bottom) / 2f,
        )
    val outer =
        min(width - margins.start - margins.end, height - margins.top - margins.bottom)
            .coerceAtLeast(1f) * .44f
    val radius = hypot(point.x - center.x, point.y - center.y)
    if (radius !in (outer * innerRadius.coerceIn(0f, 1f))..outer) return null
    val total = data.sumOf { max(0.0, it[dataKey]) }
    if (total <= 0.0) return null
    var angle = atan2(point.y - center.y, point.x - center.x) + PI / 2.0
    if (angle < 0) angle += PI * 2.0
    var cumulative = 0.0
    data.forEachIndexed { index, datum ->
        cumulative += max(0.0, datum[dataKey]) / total * PI * 2.0
        if (angle <= cumulative) return index
    }
    return data.lastIndex
}

@Composable
public fun RadarChart(
    data: List<ChartDatum>,
    modifier: Modifier = Modifier,
    bloom: Bloom = Bloom.Off,
    animate: Boolean = true,
    animationDurationMillis: Int = 900,
    replayToken: Int = 0,
    bloomOnHover: Boolean = false,
    margins: Margins = Margins(),
    defaultSelectedDataKey: String? = null,
    onSelectionChange: (String?) -> Unit = {},
    contentDescription: String? = null,
    content: RadarChartScope.() -> Unit,
) {
    val parts = remember(content) { PolarParts().also { RadarChartScope(it).content() } }
    var selected by remember(defaultSelectedDataKey) { mutableStateOf(defaultSelectedDataKey) }
    var focused by remember { mutableStateOf<String?>(null) }
    var hovered by remember { mutableStateOf<Int?>(null) }
    val revealAnimation =
        remember(data, content, replayToken) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(data, animate, animationDurationMillis, replayToken) {
        if (animate) {
            revealAnimation.snapTo(0f)
            revealAnimation.animateTo(1f, tween(animationDurationMillis.coerceAtLeast(0)))
        } else {
            revealAnimation.snapTo(1f)
        }
    }
    val progress = revealAnimation.value
    val textMeasurer = rememberTextMeasurer()

    val accessibleDescription =
        contentDescription ?: "Radar chart, ${data.size} axes, ${parts.series.size} series"
    Column(modifier.semantics { this.contentDescription = accessibleDescription }) {
        if (parts.legend)
            ChartLegend(
                parts.series,
                selected,
                parts.legendClickable,
                parts.legendAlignment,
                onFocusChange = { focused = it },
            ) { key ->
                selected = if (selected == key) null else key
                onSelectionChange(selected)
            }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(data, margins) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Exit) {
                                hovered = null
                                continue
                            }
                            val point = event.changes.firstOrNull()?.position ?: continue
                            if (data.size >= 3) {
                                val center =
                                    Offset(
                                        (size.width + margins.start - margins.end) / 2f,
                                        (size.height + margins.top - margins.bottom) / 2f,
                                    )
                                var angle = atan2(point.y - center.y, point.x - center.x) + PI / 2.0
                                if (angle < 0) angle += PI * 2.0
                                hovered =
                                    ((angle / (PI * 2.0) * data.size).toInt()).coerceIn(
                                        0,
                                        data.lastIndex,
                                    )
                            }
                        }
                    }
                }
            ) {
                if (data.size < 3 || parts.series.isEmpty()) return@Canvas
                val center =
                    Offset(
                        (size.width + margins.start - margins.end) / 2f,
                        (size.height + margins.top - margins.bottom) / 2f,
                    )
                val radius =
                    min(
                            size.width - margins.start - margins.end,
                            size.height - margins.top - margins.bottom,
                        )
                        .coerceAtLeast(1f) * .36f
                val maxValue =
                    data
                        .maxOfOrNull { row -> parts.series.maxOfOrNull { row[it.dataKey] } ?: 0.0 }
                        ?.takeIf { it > 0.0 } ?: 1.0
                val angles = data.indices.map { -PI / 2.0 + it * PI * 2.0 / data.size }
                repeat(4) { level ->
                    val ringRadius = radius * (level + 1) / 4f
                    val path = Path()
                    angles.forEachIndexed { index, angle ->
                        val point =
                            Offset(
                                center.x + cos(angle).toFloat() * ringRadius,
                                center.y + sin(angle).toFloat() * ringRadius,
                            )
                        if (index == 0) path.moveTo(point.x, point.y)
                        else path.lineTo(point.x, point.y)
                    }
                    path.close()
                    drawPath(path, Color(0xff3f3f46), style = Stroke(1f))
                }
                angles.forEachIndexed { index, angle ->
                    val edge =
                        Offset(
                            center.x + cos(angle).toFloat() * radius,
                            center.y + sin(angle).toFloat() * radius,
                        )
                    drawLine(Color(0xff3f3f46), center, edge, 1f)
                    val labelPoint =
                        Offset(
                            center.x + cos(angle).toFloat() * (radius + 11f),
                            center.y + sin(angle).toFloat() * (radius + 11f),
                        )
                    drawText(
                        textMeasurer,
                        data[index].label.take(10),
                        labelPoint,
                        style =
                            TextStyle(Color(0xffa1a1aa), 10.sp, fontFamily = FontFamily.Monospace),
                    )
                }

                parts.series.forEachIndexed { seriesIndex, series ->
                    val points = data.mapIndexed { index, row ->
                        val r =
                            radius *
                                (row[series.dataKey] / maxValue).toFloat().coerceIn(0f, 1f) *
                                easeInOutCubic(progress)
                        Offset(
                            center.x + cos(angles[index]).toFloat() * r,
                            center.y + sin(angles[index]).toFloat() * r,
                        )
                    }
                    val emphasis = selected ?: focused
                    val dim = emphasis != null && emphasis != series.dataKey
                    val alpha = if (dim) .3f else 1f
                    val grid = gridFor(size.width, size.height)
                    for (row in 0 until grid.rows) for (column in 0 until grid.columns) {
                        val point =
                            Offset((column + .5f) * grid.cellWidth, (row + .5f) * grid.cellHeight)
                        if (!pointInPolygon(point, points)) continue
                        val density =
                            (.85f -
                                    hypot(point.x - center.x, point.y - center.y) / radius * .45f -
                                    seriesIndex * .08f)
                                .coerceIn(.15f, 1f)
                        ditherCell(
                            column,
                            row,
                            grid,
                            series.color.fill.copy(alpha = alpha),
                            density,
                            series.variant,
                            if (hovered != null) 1f else 0f,
                            if (bloomOnHover && hovered == null) Bloom.Off else bloom,
                        )
                    }
                    val path = Path()
                    points.forEachIndexed { index, point ->
                        if (index == 0) path.moveTo(point.x, point.y)
                        else path.lineTo(point.x, point.y)
                    }
                    path.close()
                    drawPath(path, series.color.line.copy(alpha = alpha), style = Stroke(1.5f))
                }
            }
            if (parts.tooltip)
                hovered?.let { index ->
                    data.getOrNull(index)?.let { datum ->
                        TooltipCard(
                            datum,
                            parts.series,
                            parts.tooltipVariant,
                            parts.tooltipValueFormatter,
                            Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                        )
                    }
                }
        }
    }
}

private fun pointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    var inside = false
    var previous = polygon.lastIndex
    polygon.indices.forEach { current ->
        val a = polygon[current]
        val b = polygon[previous]
        if (
            (a.y > point.y) != (b.y > point.y) &&
                point.x < (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x
        )
            inside = !inside
        previous = current
    }
    return inside
}
