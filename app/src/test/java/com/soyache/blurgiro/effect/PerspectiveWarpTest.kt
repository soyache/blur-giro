package com.soyache.blurgiro.effect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerspectiveWarpTest {

    @Test
    fun restIsIdentity() {
        val q = PerspectiveWarp.destQuad(1000f, 2000f, 0f, 0f, 1f)
        assertEquals(0f, PerspectiveWarp.maxCornerDelta(q, 1000f, 2000f), 0.001f)
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
