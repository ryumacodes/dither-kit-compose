package com.ditherkit.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Legend that participates in layout instead of overlaying chart content. */
@Composable
public fun BlockLegend(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    values: Map<String, Number> = emptyMap(),
    valueFormatter: (Double) -> String = ::compact,
    align: LegendAlignment = LegendAlignment.Start,
) {
    val arrangement =
        when (align) {
            LegendAlignment.Start -> Arrangement.Start
            LegendAlignment.Center -> Arrangement.Center
            LegendAlignment.End -> Arrangement.End
        }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = arrangement,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        series.forEach { item ->
            Row(
                Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(8.dp).background(item.color.fill))
                BasicText(
                    item.label,
                    style = TextStyle(item.color.line, 11.sp, fontFamily = FontFamily.Monospace),
                )
                values[item.dataKey]?.let { value ->
                    BasicText(
                        valueFormatter(value.toDouble()),
                        style =
                            TextStyle(item.color.star, 11.sp, fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }
    }
}
