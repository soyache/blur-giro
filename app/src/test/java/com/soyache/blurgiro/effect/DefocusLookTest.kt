package com.soyache.blurgiro.effect

import com.soyache.blurgiro.data.BlurMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

class DefocusLookTest {

    @Test
    fun frontalMatchesSource() {
        val w = 96
        val h = 160
        val sharp = FakeHome.render(w, h)
        val out = FakeHome.defocus(w, h, 0f, 0f, 0.7f, BlurMode.DIRECTIONAL)
        var diff = 0L
        for (i in sharp.indices) {
            diff += abs((sharp[i] and 0xffffff) - (out[i] and 0xffffff))
        }
        assertEquals("de frente no se toca el contenido", 0L, diff)
    }

    @Test
    fun yawRightSoftensLeftKeepsRightReadable() {
        val w = 120
        val h = 200
        val sharp = FakeHome.render(w, h)
        val out = FakeHome.defocus(w, h, 0.85f, 0f, 0.7f, BlurMode.DIRECTIONAL)
        val leftChange = meanAbsDiff(sharp, out, w, h, 0, w / 3)
        val rightChange = meanAbsDiff(sharp, out, w, h, w * 2 / 3, w)
        assertTrue("cambio izq=$leftChange der=$rightChange", leftChange > rightChange * 1.6f)
        assertTrue("derecha casi nítida $rightChange", rightChange < 12f)
        assertTrue("no es niebla plana", colorVariance(out, w, h, 0, w / 3) > 80.0)
    }

    @Test
    fun yawLeftIsMirror() {
        val w = 120
        val h = 200
        val sharp = FakeHome.render(w, h)
        val out = FakeHome.defocus(w, h, -0.85f, 0f, 0.7f, BlurMode.DIRECTIONAL)
        val leftChange = meanAbsDiff(sharp, out, w, h, 0, w / 3)
        val rightChange = meanAbsDiff(sharp, out, w, h, w * 2 / 3, w)
        assertTrue("cambio izq=$leftChange der=$rightChange", rightChange > leftChange * 1.6f)
    }

    @Test
    fun writeLookProofPngs() {
        val w = 360
        val h = 640
        val dirs = listOf(
            File("build/look-proof"),
            File("/opt/cursor/artifacts"),
        ).filter { dir ->
            runCatching { dir.mkdirs(); dir.canWrite() }.getOrDefault(false)
        }
        val shots = listOf(
            "look_frontal_identidad.png" to FakeHome.defocus(w, h, 0f, 0f, 0.5f, BlurMode.DIRECTIONAL),
            "look_tilt_x_derecha.png" to FakeHome.defocus(w, h, 0.8f, 0f, 0.5f, BlurMode.DIRECTIONAL),
            "look_tilt_x_izquierda.png" to FakeHome.defocus(w, h, -0.8f, 0f, 0.5f, BlurMode.DIRECTIONAL),
        )
        for (dir in dirs) {
            for ((name, pixels) in shots) {
                writePng(File(dir, name), pixels, w, h)
            }
        }
        assertTrue(shots.all { it.second.size == w * h })
    }

    private fun meanAbsDiff(
        a: IntArray,
        b: IntArray,
        width: Int,
        height: Int,
        x0: Int,
        x1: Int,
    ): Double {
        var sum = 0.0
        var n = 0
        for (y in 0 until height) {
            val row = y * width
            for (x in x0 until x1) {
                val p = a[row + x]
                val q = b[row + x]
                sum += abs(((p shr 16) and 0xff) - ((q shr 16) and 0xff))
                sum += abs(((p shr 8) and 0xff) - ((q shr 8) and 0xff))
                sum += abs((p and 0xff) - (q and 0xff))
                n++
            }
        }
        return if (n == 0) 0.0 else sum / n
    }

    private fun colorVariance(pixels: IntArray, width: Int, height: Int, x0: Int, x1: Int): Double {
        var n = 0
        var sum = 0.0
        var sum2 = 0.0
        for (y in 0 until height) {
            val row = y * width
            for (x in x0 until x1) {
                val p = pixels[row + x]
                val l = 0.3 * ((p shr 16) and 0xff) + 0.59 * ((p shr 8) and 0xff) + 0.11 * (p and 0xff)
                sum += l
                sum2 += l * l
                n++
            }
        }
        if (n == 0) return 0.0
        val mean = sum / n
        return sum2 / n - mean * mean
    }

    private fun writePng(file: File, pixels: IntArray, width: Int, height: Int) {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        img.setRGB(0, 0, width, height, pixels, 0, width)
        ImageIO.write(img, "png", file)
    }
}
