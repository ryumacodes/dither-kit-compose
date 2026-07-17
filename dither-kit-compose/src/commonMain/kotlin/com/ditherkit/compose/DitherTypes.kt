package com.ditherkit.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/** Colors available to chart series and components. */
public enum class DitherColor(
    public val fill: Color,
    public val line: Color,
    public val star: Color,
) {
    Green(Color(40, 210, 110), Color(150, 255, 180), Color(200, 255, 220)),
    Blue(Color(53, 143, 243), Color(150, 200, 255), Color(205, 228, 255)),
    Purple(Color(150, 110, 255), Color(200, 175, 255), Color(225, 210, 255)),
    Pink(Color(240, 90, 190), Color(255, 170, 220), Color(255, 205, 235)),
    Orange(Color(255, 150, 50), Color(255, 195, 130), Color(255, 220, 175)),
    Red(Color(240, 70, 70), Color(255, 150, 140), Color(255, 195, 185)),
    Grey(Color(92, 92, 100), Color(140, 140, 150), Color(165, 165, 175)),
}

public enum class DitherVariant {
    Gradient,
    Dotted,
    Hatched,
    Solid,
}

public enum class StrokeVariant {
    Solid,
    Dashed,
}

public enum class StackType {
    Default,
    Stacked,
    Percent,
}

public enum class DotVariant {
    Border,
    ColoredBorder,
    Filled,
}

public enum class SeriesKind {
    Area,
    Line,
    Bar,
}

public enum class TooltipVariant {
    Default,
    FrostedGlass,
}

public enum class LegendAlignment {
    Start,
    Center,
    End,
}

public enum class GradientDirection {
    Up,
    Down,
    Left,
    Right,
}

public enum class AvatarMirror {
    Auto,
    Horizontal,
    Vertical,
}

@Immutable
public data class Bloom(
    val blurRadius: Float,
    val brightness: Float,
    val opacity: Float,
    val saturation: Float,
) {
    public companion object {
        public val Off: Bloom = Bloom(0f, 1f, 0f, 1f)
        public val Low: Bloom = Bloom(3f, 1.35f, .70f, 1.4f)
        public val High: Bloom = Bloom(5f, 1.5f, .78f, 1.5f)
        public val Aura: Bloom = Bloom(15f, 2.9f, .10f, 3f)
    }
}

@Immutable
public data class Margins(
    val start: Float = 36f,
    val top: Float = 10f,
    val end: Float = 12f,
    val bottom: Float = 22f,
)

@Immutable
public data class ChartDatum(
    val label: String,
    val values: Map<String, Double>,
) {
    public operator fun get(key: String): Double = values[key] ?: 0.0
}

@Immutable
public data class ChartSeries(
    val dataKey: String,
    val label: String = dataKey,
    val color: DitherColor = DitherColor.Blue,
    val variant: DitherVariant = DitherVariant.Gradient,
    val strokeVariant: StrokeVariant = StrokeVariant.Solid,
    val isClickable: Boolean = false,
    val dot: DotStyle? = null,
    val activeDot: DotStyle? = null,
    val kind: SeriesKind = SeriesKind.Area,
)

@Immutable
public data class DotStyle(
    val variant: DotVariant = DotVariant.Border,
    val radius: Float = 2f,
)

@Immutable
public data class ReferenceLineStyle(
    val value: Double,
    val label: String? = null,
    val strokeVariant: StrokeVariant = StrokeVariant.Dashed,
)

public fun chartData(vararg rows: Pair<String, Map<String, Number>>): List<ChartDatum> =
    rows.map { (label, values) ->
        ChartDatum(label, values.mapValues { it.value.toDouble() })
    }

internal fun hueColor(hue: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f / 60f
    val chroma = .714f
    val x = chroma * (1f - abs(h % 2f - 1f))
    val (r1, g1, b1) =
        when (h.toInt()) {
            0 -> Triple(chroma, x, 0f)
            1 -> Triple(x, chroma, 0f)
            2 -> Triple(0f, chroma, x)
            3 -> Triple(0f, x, chroma)
            4 -> Triple(x, 0f, chroma)
            else -> Triple(chroma, 0f, x)
        }
    val m = .223f
    return Color(r1 + m, g1 + m, b1 + m)
}
