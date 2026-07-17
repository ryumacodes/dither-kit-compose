package com.ditherkit.compose.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ditherkit.compose.AreaChart
import com.ditherkit.compose.BarChart
import com.ditherkit.compose.Bloom
import com.ditherkit.compose.ChartDatum
import com.ditherkit.compose.DitherAvatar
import com.ditherkit.compose.DitherButton
import com.ditherkit.compose.DitherColor
import com.ditherkit.compose.DitherGradient
import com.ditherkit.compose.DitherVariant
import com.ditherkit.compose.GradientDirection
import com.ditherkit.compose.LineChart
import com.ditherkit.compose.PieChart
import com.ditherkit.compose.RadarChart
import com.ditherkit.compose.Sparkline
import com.ditherkit.compose.StackType

private val background = Color(0xff09090b)
private val panel = Color(0xff111114)
private val border = Color(0xff27272a)
private val foreground = Color(0xfffafafa)
private val muted = Color(0xffa1a1aa)

private val monthlyData =
    listOf(
        ChartDatum("Jan", mapOf("desktop" to 186.0, "mobile" to 80.0)),
        ChartDatum("Feb", mapOf("desktop" to 240.0, "mobile" to 118.0)),
        ChartDatum("Mar", mapOf("desktop" to 205.0, "mobile" to 142.0)),
        ChartDatum("Apr", mapOf("desktop" to 278.0, "mobile" to 165.0)),
        ChartDatum("May", mapOf("desktop" to 255.0, "mobile" to 188.0)),
        ChartDatum("Jun", mapOf("desktop" to 322.0, "mobile" to 215.0)),
    )

@Composable
fun SampleApp() {
    MaterialTheme(colorScheme = darkColorScheme(background = background, surface = panel)) {
        Surface(Modifier.fillMaxSize(), color = background) {
            LazyColumn(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item { Header() }
                item { CartesianGallery() }
                item { PolarGallery() }
                item { ComponentGallery() }
            }
        }
    }
}

@Composable
private fun Header() {
    Box(Modifier.fillMaxWidth().height(150.dp)) {
        DitherGradient(
            DitherColor.Purple,
            Modifier.fillMaxSize(),
            direction = GradientDirection.Right,
            bloom = Bloom.Aura,
        )
        Column(Modifier.align(Alignment.CenterStart).padding(20.dp)) {
            Label("DITHER KIT / COMPOSE")
            Text("Composable dithered charts", 28.sp, foreground, FontWeight.Bold)
            Text("Android · Desktop · iOS", 12.sp, muted)
        }
    }
}

@Composable
private fun CartesianGallery() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("CARTESIAN")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DemoCard("Area + scrub", Modifier.weight(1f)) {
                AreaChart(monthlyData, Modifier.fillMaxWidth().height(250.dp), bloom = Bloom.Aura) {
                    grid()
                    xAxis()
                    yAxis()
                    legend(isClickable = true)
                    tooltip()
                    area("desktop", "Desktop", DitherColor.Blue, DitherVariant.Gradient)
                    area("mobile", "Mobile", DitherColor.Purple, DitherVariant.Hatched)
                }
            }
            DemoCard("Stacked bars", Modifier.weight(1f)) {
                BarChart(
                    monthlyData,
                    Modifier.fillMaxWidth().height(250.dp),
                    stackType = StackType.Stacked,
                    bloom = Bloom.Low,
                ) {
                    grid()
                    xAxis()
                    yAxis()
                    legend(isClickable = true)
                    tooltip()
                    bar("desktop", "Desktop", DitherColor.Green)
                    bar("mobile", "Mobile", DitherColor.Orange, DitherVariant.Dotted)
                }
            }
        }
        DemoCard("Line chart", Modifier.fillMaxWidth()) {
            LineChart(monthlyData, Modifier.fillMaxWidth().height(220.dp), bloom = Bloom.Aura) {
                grid()
                xAxis()
                yAxis()
                legend(isClickable = true)
                tooltip()
                line("desktop", "Desktop", DitherColor.Pink)
                line("mobile", "Mobile", DitherColor.Blue)
                referenceLine(200)
            }
        }
    }
}

@Composable
private fun PolarGallery() {
    val pie =
        listOf(
            ChartDatum("Chrome", mapOf("visitors" to 275.0)),
            ChartDatum("Safari", mapOf("visitors" to 200.0)),
            ChartDatum("Firefox", mapOf("visitors" to 145.0)),
            ChartDatum("Edge", mapOf("visitors" to 90.0)),
        )
    val colors =
        mapOf(
            "Chrome" to DitherColor.Blue,
            "Safari" to DitherColor.Green,
            "Firefox" to DitherColor.Orange,
            "Edge" to DitherColor.Purple,
        )
    val radar =
        listOf(
            ChartDatum("Speed", mapOf("desktop" to 92.0, "mobile" to 74.0)),
            ChartDatum("Power", mapOf("desktop" to 76.0, "mobile" to 88.0)),
            ChartDatum("Range", mapOf("desktop" to 84.0, "mobile" to 58.0)),
            ChartDatum("Control", mapOf("desktop" to 68.0, "mobile" to 82.0)),
            ChartDatum("Focus", mapOf("desktop" to 89.0, "mobile" to 70.0)),
        )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("POLAR")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DemoCard("Donut", Modifier.weight(1f)) {
                PieChart(
                    pie,
                    "visitors",
                    colors,
                    Modifier.fillMaxWidth().height(280.dp),
                    innerRadius = .48f,
                    bloom = Bloom.Aura,
                ) {
                    legend(isClickable = true)
                    tooltip()
                    pie(DitherVariant.Gradient)
                }
            }
            DemoCard("Radar", Modifier.weight(1f)) {
                RadarChart(radar, Modifier.fillMaxWidth().height(280.dp), bloom = Bloom.Low) {
                    legend(isClickable = true)
                    tooltip()
                    radar("desktop", "Desktop", DitherColor.Blue)
                    radar("mobile", "Mobile", DitherColor.Pink, DitherVariant.Hatched)
                }
            }
        }
    }
}

@Composable
private fun ComponentGallery() {
    var count by remember { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("COMPONENTS")
        DemoCard("Standalone primitives", Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                DitherAvatar("dither-kit-compose", size = 72.dp, bloom = Bloom.Aura)
                DitherAvatar("tripwire", size = 72.dp, hue = 220f)
                DitherButton({ count++ }, color = DitherColor.Blue, bloom = Bloom.Low) {
                    Text("clicked $count", 11.sp, foreground)
                }
                Sparkline(
                    listOf(3, 7, 5, 9, 8, 12),
                    DitherColor.Green,
                    Modifier.width(150.dp).height(52.dp),
                    bloom = Bloom.Aura,
                )
            }
        }
    }
}

@Composable
private fun DemoCard(title: String, modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier.background(panel, RoundedCornerShape(8.dp)).padding(14.dp)) {
        Label(title.uppercase())
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable private fun SectionTitle(text: String) = Text(text, 12.sp, foreground, FontWeight.Bold)

@Composable private fun Label(text: String) = Text(text, 9.sp, DitherColor.Green.line)

@Composable
private fun Text(
    text: String,
    size: androidx.compose.ui.unit.TextUnit,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
) {
    androidx.compose.foundation.text.BasicText(
        text,
        style =
            TextStyle(
                color = color,
                fontSize = size,
                fontWeight = weight,
                fontFamily = FontFamily.Monospace,
            ),
    )
}
