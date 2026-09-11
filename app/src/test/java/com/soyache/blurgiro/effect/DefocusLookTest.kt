package com.soyache.blurgiro.effect

import com.soyache.blurgiro.data.BlurMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
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
            for (x in x0 until width.coerceAtMost(x1)) {
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
        val raw = ByteArray((width * 4 + 1) * height)
        var i = 0
        for (y in 0 until height) {
            raw[i++] = 0
            val row = y * width
            for (x in 0 until width) {
                val p = pixels[row + x]
                raw[i++] = ((p shr 16) and 0xff).toByte()
                raw[i++] = ((p shr 8) and 0xff).toByte()
                raw[i++] = (p and 0xff).toByte()
                raw[i++] = ((p ushr 24) and 0xff).toByte()
            }
        }
        val deflater = Deflater(Deflater.BEST_SPEED)
        deflater.setInput(raw)
        deflater.finish()
        val zipped = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        while (!deflater.finished()) {
            val n = deflater.deflate(buf)
            if (n > 0) zipped.write(buf, 0, n)
        }
        deflater.end()
        FileOutputStream(file).use { out ->
            out.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            writeChunk(out, "IHDR", ihdr(width, height))
            writeChunk(out, "IDAT", zipped.toByteArray())
            writeChunk(out, "IEND", ByteArray(0))
        }
    }

    private fun ihdr(width: Int, height: Int): ByteArray {
        val b = ByteArray(13)
        putInt(b, 0, width)
        putInt(b, 4, height)
        b[8] = 8
        b[9] = 6
        return b
    }

    private fun writeChunk(out: FileOutputStream, type: String, data: ByteArray) {
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        val len = ByteArray(4)
        putInt(len, 0, data.size)
        out.write(len)
        out.write(typeBytes)
        out.write(data)
        val crc = CRC32()
        crc.update(typeBytes)
        crc.update(data)
        val tail = ByteArray(4)
        putInt(tail, 0, crc.value.toInt())
        out.write(tail)
    }

    private fun putInt(dest: ByteArray, offset: Int, value: Int) {
        dest[offset] = ((value ushr 24) and 0xff).toByte()
        dest[offset + 1] = ((value ushr 16) and 0xff).toByte()
        dest[offset + 2] = ((value ushr 8) and 0xff).toByte()
        dest[offset + 3] = (value and 0xff).toByte()
    }
}
