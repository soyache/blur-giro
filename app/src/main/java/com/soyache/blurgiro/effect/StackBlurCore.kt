package com.soyache.blurgiro.effect

/**
 * Stack blur (aprox. gaussiana) sobre ARGB. Sin APIs de Android: sirve
 * en la app y en tests JVM para el look de la foto de Héctor.
 */
object DefocusLook {
    fun midRadius(intensity: Float): Int =
        (3 + 6 * intensity.coerceIn(0f, 1f)).toInt().coerceIn(2, 10)

    fun heavyRadius(intensity: Float): Int =
        (7 + 12 * intensity.coerceIn(0f, 1f)).toInt().coerceIn(6, 20)
}

object StackBlurCore {

    fun blur(src: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val r = radius.coerceAtLeast(1)
        if (width <= 0 || height <= 0) return src.copyOf()
        val pix = src.copyOf()
        val wm = width - 1
        val hm = height - 1
        val wh = width * height
        val div = r + r + 1

        val rChan = IntArray(wh)
        val gChan = IntArray(wh)
        val bChan = IntArray(wh)
        val vmin = IntArray(width.coerceAtLeast(height))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (i in dv.indices) dv[i] = i / divsum

        var yw = 0
        var yi = 0
        val stack = Array(div) { IntArray(3) }

        for (y in 0 until height) {
            var rSum = 0
            var gSum = 0
            var bSum = 0
            var rOut = 0
            var gOut = 0
            var bOut = 0
            var rIn = 0
            var gIn = 0
            var bIn = 0
            for (i in -r..r) {
                val p = pix[yi + i.coerceIn(-yi, wm - yi)]
                val sir = stack[i + r]
                sir[0] = (p shr 16) and 0xff
                sir[1] = (p shr 8) and 0xff
                sir[2] = p and 0xff
                val rbs = r + 1 - kotlin.math.abs(i)
                rSum += sir[0] * rbs
                gSum += sir[1] * rbs
                bSum += sir[2] * rbs
                if (i > 0) {
                    rIn += sir[0]
                    gIn += sir[1]
                    bIn += sir[2]
                } else {
                    rOut += sir[0]
                    gOut += sir[1]
                    bOut += sir[2]
                }
            }
            var stackPointer = r
            for (x in 0 until width) {
                rChan[yi] = dv[rSum]
                gChan[yi] = dv[gSum]
                bChan[yi] = dv[bSum]
                rSum -= rOut
                gSum -= gOut
                bSum -= bOut
                val stackStart = (stackPointer - r + div) % div
                val sir = stack[stackStart]
                rOut -= sir[0]
                gOut -= sir[1]
                bOut -= sir[2]
                if (y == 0) vmin[x] = (x + r + 1).coerceAtMost(wm)
                val p = pix[yw + vmin[x]]
                sir[0] = (p shr 16) and 0xff
                sir[1] = (p shr 8) and 0xff
                sir[2] = p and 0xff
                rIn += sir[0]
                gIn += sir[1]
                bIn += sir[2]
                rSum += rIn
                gSum += gIn
                bSum += bIn
                stackPointer = (stackPointer + 1) % div
                val sir2 = stack[stackPointer]
                rOut += sir2[0]
                gOut += sir2[1]
                bOut += sir2[2]
                rIn -= sir2[0]
                gIn -= sir2[1]
                bIn -= sir2[2]
                yi++
            }
            yw += width
        }

        for (x in 0 until width) {
            var rSum = 0
            var gSum = 0
            var bSum = 0
            var rOut = 0
            var gOut = 0
            var bOut = 0
            var rIn = 0
            var gIn = 0
            var bIn = 0
            var yp = -r * width
            for (i in -r..r) {
                yi = (0.coerceAtLeast(yp) + x)
                val sir = stack[i + r]
                sir[0] = rChan[yi]
                sir[1] = gChan[yi]
                sir[2] = bChan[yi]
                val rbs = r + 1 - kotlin.math.abs(i)
                rSum += rChan[yi] * rbs
                gSum += gChan[yi] * rbs
                bSum += bChan[yi] * rbs
                if (i > 0) {
                    rIn += sir[0]
                    gIn += sir[1]
                    bIn += sir[2]
                } else {
                    rOut += sir[0]
                    gOut += sir[1]
                    bOut += sir[2]
                }
                if (i < hm) yp += width
            }
            yi = x
            var stackPointer = r
            for (y in 0 until height) {
                val a = pix[yi] ushr 24
                pix[yi] = (a shl 24) or (dv[rSum] shl 16) or (dv[gSum] shl 8) or dv[bSum]
                rSum -= rOut
                gSum -= gOut
                bSum -= bOut
                val stackStart = (stackPointer - r + div) % div
                val sir = stack[stackStart]
                rOut -= sir[0]
                gOut -= sir[1]
                bOut -= sir[2]
                if (x == 0) vmin[y] = ((y + r + 1).coerceAtMost(hm)) * width
                val p = x + vmin[y]
                sir[0] = rChan[p]
                sir[1] = gChan[p]
                sir[2] = bChan[p]
                rIn += sir[0]
                gIn += sir[1]
                bIn += sir[2]
                rSum += rIn
                gSum += gIn
                bSum += bIn
                stackPointer = (stackPointer + 1) % div
                val sir2 = stack[stackPointer]
                rOut += sir2[0]
                gOut += sir2[1]
                bOut += sir2[2]
                rIn -= sir2[0]
                gIn -= sir2[1]
                bIn -= sir2[2]
                yi += width
            }
        }
        return pix
    }

    /**
     * Mezcla pirámide nítida / media / fuerte según [mask] 0..1.
     * mask alta = más desenfoque (lado que se aleja).
     */
    fun composite(sharp: IntArray, mid: IntArray, heavy: IntArray, mask: FloatArray): IntArray {
        val n = sharp.size
        val out = IntArray(n)
        for (i in 0 until n) {
            val m = mask[i].coerceIn(0f, 1f)
            val tMid = (m * 1.65f).coerceIn(0f, 1f)
            val tHeavy = (m * 1.65f - 0.72f).coerceIn(0f, 1f)
            val a = lerpPixel(sharp[i], mid[i], tMid)
            out[i] = lerpPixel(a, heavy[i], tHeavy)
        }
        return out
    }

    fun lerpPixel(c0: Int, c1: Int, t: Float): Int {
        if (t <= 0f) return c0
        if (t >= 1f) return c1
        val a0 = c0 ushr 24
        val r0 = (c0 shr 16) and 0xff
        val g0 = (c0 shr 8) and 0xff
        val b0 = c0 and 0xff
        val a1 = c1 ushr 24
        val r1 = (c1 shr 16) and 0xff
        val g1 = (c1 shr 8) and 0xff
        val b1 = c1 and 0xff
        val a = (a0 + (a1 - a0) * t).toInt()
        val r = (r0 + (r1 - r0) * t).toInt()
        val g = (g0 + (g1 - g0) * t).toInt()
        val b = (b0 + (b1 - b0) * t).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
