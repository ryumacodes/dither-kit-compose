package com.ditherkit.compose

@DslMarker public annotation class DitherChartDsl

internal data class CartesianParts(
    var xAxis: Boolean = false,
    var yAxis: Boolean = false,
    var grid: Boolean = false,
    var legend: Boolean = false,
    var legendClickable: Boolean = false,
    var tooltip: Boolean = false,
    var tooltipVariant: TooltipVariant = TooltipVariant.Default,
    var legendAlignment: LegendAlignment = LegendAlignment.End,
    var xAxisMaxTicks: Int = 8,
    var xAxisTickMargin: Float = 8f,
    var xAxisTickFormatter: ((String, Int) -> String)? = null,
    var yAxisTickCount: Int = 4,
    var yAxisTickMargin: Float = 8f,
    var yAxisTickFormatter: ((Double) -> String)? = null,
    var gridHorizontal: Boolean = true,
    var gridVertical: Boolean = false,
    var gridStrokeVariant: StrokeVariant = StrokeVariant.Dashed,
    var tooltipValueFormatter: ((Double, String) -> String)? = null,
    val series: MutableList<ChartSeries> = mutableListOf(),
    val referenceLines: MutableList<ReferenceLineStyle> = mutableListOf(),
)

@DitherChartDsl
public class CartesianChartScope internal constructor(private val parts: CartesianParts) {
    public fun xAxis(
        maxTicks: Int = 8,
        tickMargin: Float = 8f,
        tickFormatter: ((String, Int) -> String)? = null,
    ) {
        parts.xAxis = true
        parts.xAxisMaxTicks = maxTicks.coerceAtLeast(1)
        parts.xAxisTickMargin = tickMargin
        parts.xAxisTickFormatter = tickFormatter
    }

    public fun yAxis(
        tickCount: Int = 4,
        tickMargin: Float = 8f,
        tickFormatter: ((Double) -> String)? = null,
    ) {
        parts.yAxis = true
        parts.yAxisTickCount = tickCount.coerceAtLeast(1)
        parts.yAxisTickMargin = tickMargin
        parts.yAxisTickFormatter = tickFormatter
    }

    public fun grid(
        horizontal: Boolean = true,
        vertical: Boolean = false,
        strokeVariant: StrokeVariant = StrokeVariant.Dashed,
    ) {
        parts.grid = true
        parts.gridHorizontal = horizontal
        parts.gridVertical = vertical
        parts.gridStrokeVariant = strokeVariant
    }

    public fun legend(isClickable: Boolean = false, align: LegendAlignment = LegendAlignment.End) {
        parts.legend = true
        parts.legendClickable = isClickable
        parts.legendAlignment = align
    }

    public fun tooltip(
        variant: TooltipVariant = TooltipVariant.Default,
        valueFormatter: ((Double, String) -> String)? = null,
    ) {
        parts.tooltip = true
        parts.tooltipVariant = variant
        parts.tooltipValueFormatter = valueFormatter
    }

    public fun referenceLine(
        value: Number = 0,
        label: String? = null,
        strokeVariant: StrokeVariant = StrokeVariant.Dashed,
    ) {
        parts.referenceLines += ReferenceLineStyle(value.toDouble(), label, strokeVariant)
    }

    public fun area(
        dataKey: String,
        label: String = dataKey,
        color: DitherColor = DitherColor.Blue,
        variant: DitherVariant = DitherVariant.Gradient,
        isClickable: Boolean = false,
        dot: DotStyle? = null,
        activeDot: DotStyle? = null,
    ) {
        parts.series +=
            ChartSeries(
                dataKey,
                label,
                color,
                variant,
                isClickable = isClickable,
                dot = dot,
                activeDot = activeDot,
                kind = SeriesKind.Area,
            )
    }

    public fun line(
        dataKey: String,
        label: String = dataKey,
        color: DitherColor = DitherColor.Blue,
        strokeVariant: StrokeVariant = StrokeVariant.Solid,
        isClickable: Boolean = false,
        variant: DitherVariant = DitherVariant.Gradient,
        dot: DotStyle? = null,
        activeDot: DotStyle? = null,
    ) {
        parts.series +=
            ChartSeries(
                dataKey,
                label,
                color,
                variant,
                strokeVariant,
                isClickable,
                dot,
                activeDot,
                SeriesKind.Line,
            )
    }

    public fun bar(
        dataKey: String,
        label: String = dataKey,
        color: DitherColor = DitherColor.Blue,
        variant: DitherVariant = DitherVariant.Gradient,
        isClickable: Boolean = false,
        dot: DotStyle? = null,
        activeDot: DotStyle? = null,
    ) {
        parts.series +=
            ChartSeries(
                dataKey,
                label,
                color,
                variant,
                isClickable = isClickable,
                dot = dot,
                activeDot = activeDot,
                kind = SeriesKind.Bar,
            )
    }
}

internal data class PolarParts(
    var legend: Boolean = false,
    var legendClickable: Boolean = false,
    var tooltip: Boolean = false,
    var tooltipVariant: TooltipVariant = TooltipVariant.Default,
    var tooltipValueFormatter: ((Double, String) -> String)? = null,
    var legendAlignment: LegendAlignment = LegendAlignment.End,
    var pieVariant: DitherVariant = DitherVariant.Gradient,
    val series: MutableList<ChartSeries> = mutableListOf(),
)

@DitherChartDsl
public class PieChartScope internal constructor(private val parts: PolarParts) {
    public fun legend(isClickable: Boolean = false, align: LegendAlignment = LegendAlignment.End) {
        parts.legend = true
        parts.legendClickable = isClickable
        parts.legendAlignment = align
    }

    public fun tooltip(
        variant: TooltipVariant = TooltipVariant.Default,
        valueFormatter: ((Double, String) -> String)? = null,
    ) {
        parts.tooltip = true
        parts.tooltipVariant = variant
        parts.tooltipValueFormatter = valueFormatter
    }

    public fun pie(variant: DitherVariant = DitherVariant.Gradient) {
        parts.pieVariant = variant
    }
}

@DitherChartDsl
public class RadarChartScope internal constructor(private val parts: PolarParts) {
    public fun legend(isClickable: Boolean = false, align: LegendAlignment = LegendAlignment.End) {
        parts.legend = true
        parts.legendClickable = isClickable
        parts.legendAlignment = align
    }

    public fun tooltip(
        variant: TooltipVariant = TooltipVariant.Default,
        valueFormatter: ((Double, String) -> String)? = null,
    ) {
        parts.tooltip = true
        parts.tooltipVariant = variant
        parts.tooltipValueFormatter = valueFormatter
    }

    public fun radar(
        dataKey: String,
        label: String = dataKey,
        color: DitherColor = DitherColor.Blue,
        variant: DitherVariant = DitherVariant.Gradient,
    ) {
        parts.series += ChartSeries(dataKey, label, color, variant, kind = SeriesKind.Area)
    }
}
