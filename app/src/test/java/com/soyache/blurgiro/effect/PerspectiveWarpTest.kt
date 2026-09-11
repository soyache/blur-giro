package com.soyache.blurgiro.effect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerspectiveWarpTest {

    @Test
    fun restIsIdentity() {
        val q = PerspectiveWarp.destQuad(1000f, 2000f, 0f, 0f, 1f)
        assertEquals(0f, PerspectiveWarp.maxCornerDelta(q, 1000f, 2000f), 0.001f)
        val p = PerspectiveWarp.mapPoint(0.25f, 0.4f, 0f, 0f, 1f)
        assertEquals(0.25f, p.first, 0.001f)
        assertEquals(0.4f, p.second, 0.001f)
    }

    @Test
    fun yawRightRecedesLeftEdge() {
        val w = 1000f
        val h = 2000f
        val q = PerspectiveWarp.destQuad(w, h, 0.85f, 0f, 0.7f)
        val leftWidth = q[0]
        val rightInset = w - q[2]
        assertTrue("izquierda entra=$leftWidth derecha=$rightInset", leftWidth > rightInset + 8f)
        val leftHeight = q[7] - q[1]
        val rightHeight = q[5] - q[3]
        assertTrue("alto izq=$leftHeight alto der=$rightHeight", leftHeight < rightHeight - 8f)
        val left = PerspectiveWarp.mapPoint(0f, 0.5f, 0.85f, 0f, 0.7f)
        val right = PerspectiveWarp.mapPoint(1f, 0.5f, 0.85f, 0f, 0.7f)
        assertTrue("punto izq recede u=${left.first}", left.first > 0.01f)
        assertTrue("punto der casi al borde u=${right.first}", right.first > 0.97f)
    }

    @Test
    fun yawLeftIsMirror() {
        val w = 1000f
        val h = 2000f
        val q = PerspectiveWarp.destQuad(w, h, -0.85f, 0f, 0.7f)
        val leftWidth = q[0]
        val rightInset = w - q[2]
        assertTrue("derecha recede: izq=$leftWidth der=$rightInset", rightInset > leftWidth + 8f)
    }
}
