package com.ditherkit.compose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DitherCoreTest {
    @Test
    fun bayerMatrixContainsEveryThresholdOnce() {
        val values = Bayer4.flatMap { it.toList() }
        assertEquals(16, values.distinct().size)
        assertTrue(values.all { it in 0f..1f })
    }

    @Test
    fun fnvHashAndRandomAreDeterministic() {
        assertEquals(fnv1a("tripwire"), fnv1a("tripwire"))
        assertNotEquals(fnv1a("tripwire"), fnv1a("dither-kit"))
        val first = XorShift32(fnv1a("avatar"))
        val second = XorShift32(fnv1a("avatar"))
        repeat(64) { assertEquals(first.nextFloat(), second.nextFloat()) }
    }

    @Test
    fun fnvHashMatchesUpstreamUtf16Semantics() {
        assertEquals(0xef7fffb7u, fnv1a("tripwire"))
        assertEquals(0x1f617c09u, fnv1a("dithér"))
        assertEquals(0x492e7da9u, fnv1a("🎨"))
    }

    @Test
    fun hueFillMatchesUpstreamHslRecipe() {
        val red = hueColor(0f)
        assertEquals(.937f, red.red, .002f)
        assertEquals(.223f, red.green, .002f)
        assertEquals(.223f, red.blue, .002f)
    }

    @Test
    fun chartDataConvertsNumericTypes() {
        val rows = chartData("Jan" to mapOf("desktop" to 186, "mobile" to 80.5f))
        assertEquals("Jan", rows.single().label)
        assertEquals(186.0, rows.single()["desktop"])
        assertEquals(80.5, rows.single()["mobile"])
        assertEquals(0.0, rows.single()["missing"])
    }

    @Test
    fun gridIsBoundedLikeOriginalCanvas() {
        assertEquals(520, gridFor(10_000f, 10_000f, 2f).columns)
        assertEquals(200, gridFor(10_000f, 10_000f, 2f).rows)
        assertEquals(1, gridFor(1f, 1f, 3f).columns)
        assertEquals(50, gridFor(100f, 40f).columns)
        assertEquals(20, gridFor(100f, 40f).rows)
    }
}
