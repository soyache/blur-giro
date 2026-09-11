package com.soyache.blurgiro.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {

    @Test
    fun pageCountRoundsUp() {
        assertEquals(1, HomeLayout.pageCount(0))
        assertEquals(1, HomeLayout.pageCount(20))
        assertEquals(2, HomeLayout.pageCount(21))
    }

    @Test
    fun cellsFillTheGrid() {
        val m = HomeLayout.metrics(1080f, 2400f, 0f, 80f, 0f, 40f)
        val first = HomeLayout.cellRect(0, 0, 0f, m)
        val last = HomeLayout.cellRect(HomeLayout.PAGE_SIZE - 1, 0, 0f, m)
        assertTrue(first.left >= m.padLeft - 0.1f)
        assertTrue(last.right <= m.width - m.padRight + 0.1f)
        assertTrue(last.bottom <= m.gridBottom + 0.1f)
        assertTrue(HomeLayout.iconRect(first, m).width() > 20f)
    }

    @Test
    fun hitFindsDockAndIcon() {
        val m = HomeLayout.metrics(1080f, 2400f, 0f, 80f, 0f, 40f)
        val icon = HomeLayout.cellRect(0, 0, 0f, m)
        assertEquals(0, HomeLayout.hitApp(icon.centerX(), icon.centerY(), 0f, 8, m))
        val dock = HomeLayout.dockRect(2, m)
        assertEquals(2, HomeLayout.hitDock(dock.centerX(), dock.centerY(), m))
        assertEquals(-1, HomeLayout.hitDock(icon.centerX(), icon.centerY(), m))
    }

    @Test
    fun nearestPageSnaps() {
        assertEquals(1, HomeLayout.nearestPage(1080f * 0.6f, 3, 1080f))
        assertEquals(0, HomeLayout.nearestPage(100f, 3, 1080f))
    }
}
