package com.ditherkit.compose

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DitherUiTest {
    @Test
    fun buttonExposesClickSemanticsAndInvokesCallback() = runComposeUiTest {
        var clicks = 0
        setContent {
            DitherButton(
                onClick = { clicks += 1 },
                modifier = Modifier.testTag("button"),
            ) {
                BasicText("Save")
            }
        }

        onNodeWithTag("button").assertHasClickAction().performClick()
        runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun chartExposesGeneratedAccessibilitySummary() = runComposeUiTest {
        val data = chartData("Jan" to mapOf("desktop" to 186), "Feb" to mapOf("desktop" to 240))
        setContent {
            AreaChart(
                data = data,
                modifier = Modifier.size(240.dp, 160.dp).testTag("chart"),
                animate = false,
            ) {
                area("desktop")
            }
        }

        onNodeWithTag("chart").assertContentDescriptionEquals("Area chart, 2 data points, 1 series")
    }

    @Test
    fun chartAcceptsAnApplicationSpecificAccessibilitySummary() = runComposeUiTest {
        setContent {
            PieChart(
                data = emptyList(),
                dataKey = "visitors",
                colors = emptyMap(),
                modifier = Modifier.size(160.dp).testTag("chart"),
                animate = false,
                contentDescription = "Browser share chart with no results",
            )
        }

        onNodeWithTag("chart").assertContentDescriptionEquals("Browser share chart with no results")
    }
}
