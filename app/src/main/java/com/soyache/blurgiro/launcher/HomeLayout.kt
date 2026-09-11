package com.soyache.blurgiro.launcher

import android.graphics.RectF
import kotlin.math.floor

/**
 * Geometría compartida del home: grid paginado + dock. La vista pinta
 * y hit-testea con las mismas celdas.
 */
object HomeLayout {

    const val COLUMNS = 4
    const val ROWS = 5
    const val PAGE_SIZE = COLUMNS * ROWS
    const val DOCK_SLOTS = 5

    data class Metrics(
        val width: Float,
        val height: Float,
        val padLeft: Float,
        val padTop: Float,
        val padRight: Float,
        val padBottom: Float,
        val dockHeight: Float,
        val cellW: Float,
        val cellH: Float,
        val iconSize: Float,
        val pageWidth: Float,
    ) {
        val gridBottom: Float get() = height - padBottom - dockHeight
        val dockTop: Float get() = gridBottom + dockHeight * 0.08f
    }

    fun metrics(
        width: Float,
        height: Float,
        insetLeft: Float,
        insetTop: Float,
        insetRight: Float,
        insetBottom: Float,
    ): Metrics {
        val w = width.coerceAtLeast(1f)
        val h = height.coerceAtLeast(1f)
        val padL = insetLeft + w * 0.045f
        val padR = insetRight + w * 0.045f
        val padT = insetTop + h * 0.028f
        val padB = insetBottom + h * 0.012f
        val dockH = (h * 0.118f).coerceIn(72f, 140f)
        val gridH = (h - padT - padB - dockH).coerceAtLeast(1f)
        val gridW = (w - padL - padR).coerceAtLeast(1f)
        val cellW = gridW / COLUMNS
        val cellH = gridH / ROWS
        val icon = minOf(cellW * 0.62f, cellH * 0.58f, w * 0.145f)
        return Metrics(
            width = w,
            height = h,
            padLeft = padL,
            padTop = padT,
            padRight = padR,
            padBottom = padB,
            dockHeight = dockH,
            cellW = cellW,
            cellH = cellH,
            iconSize = icon,
            pageWidth = w,
        )
    }

    fun pageCount(appCount: Int): Int = if (appCount <= 0) 1 else (appCount + PAGE_SIZE - 1) / PAGE_SIZE

    fun cellRect(indexOnPage: Int, page: Int, scrollX: Float, m: Metrics): RectF {
        val col = indexOnPage % COLUMNS
        val row = indexOnPage / COLUMNS
        val left = m.padLeft + col * m.cellW + page * m.pageWidth - scrollX
        val top = m.padTop + row * m.cellH
        return RectF(left, top, left + m.cellW, top + m.cellH)
    }

    fun dockRect(slot: Int, m: Metrics): RectF {
        val usable = (m.width - m.padLeft - m.padRight).coerceAtLeast(1f)
        val slotW = usable / DOCK_SLOTS
        val left = m.padLeft + slot * slotW
        return RectF(left, m.dockTop, left + slotW, m.height - m.padBottom)
    }

    fun iconRect(cell: RectF, m: Metrics): RectF {
        val size = m.iconSize
        val cx = cell.centerX()
        val cy = cell.top + cell.height() * 0.38f
        return RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f)
    }

    fun hitApp(
        x: Float,
        y: Float,
        scrollX: Float,
        appCount: Int,
        m: Metrics,
    ): Int {
        if (y >= m.dockTop) return -1
        val pages = pageCount(appCount)
        val first = (scrollX / m.pageWidth).toInt().coerceAtLeast(0)
        val last = (first + 1).coerceAtMost(pages - 1)
        for (page in first..last) {
            val start = page * PAGE_SIZE
            val count = (appCount - start).coerceIn(0, PAGE_SIZE)
            for (i in 0 until count) {
                if (cellRect(i, page, scrollX, m).contains(x, y)) return start + i
            }
        }
        return -1
    }

    fun hitDock(x: Float, y: Float, m: Metrics): Int {
        if (y < m.dockTop) return -1
        for (i in 0 until DOCK_SLOTS) {
            if (dockRect(i, m).contains(x, y)) return i
        }
        return -1
    }

    fun nearestPage(scrollX: Float, pageCount: Int, pageWidth: Float): Int {
        val raw = scrollX / pageWidth.coerceAtLeast(1f)
        return floor(raw + 0.5f).toInt().coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    }
}
