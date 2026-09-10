package com.soyache.blurgiro.effect

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import com.soyache.blurgiro.data.BlurMode
import kotlin.math.hypot

/**
 * Dibuja la capa de cristal (escarcha, viñeta, brillo especular).
 * En API 33+ usa AGSL; en anteriores, degradados de Canvas.
 */
class GlassBlurRenderer {

    private var runtimeShader: RuntimeShader? = null
    private val shaderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val frostPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val noisePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = 28
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    private val noiseBitmap: Bitmap by lazy { createNoise(128) }
    private val noiseShader: Shader by lazy {
        BitmapShader(noiseBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }

    var tiltX: Float = 0f
    var tiltY: Float = 0f
    var intensity: Float = 0.62f
    var mode: BlurMode = BlurMode.CORNERS

    fun draw(canvas: Canvas, width: Int, height: Int) {
        if (width <= 0 || height <= 0 || intensity <= 0.01f) return
        if (Build.VERSION.SDK_INT >= 33) {
            drawAgsl(canvas, width, height)
        } else {
            drawFallback(canvas, width, height)
        }
    }

    private fun drawAgsl(canvas: Canvas, width: Int, height: Int) {
        val shader = runtimeShader ?: RuntimeShader(AGSL).also { runtimeShader = it }
        shader.setFloatUniform("uResolution", width.toFloat(), height.toFloat())
        shader.setFloatUniform("uTilt", tiltX, tiltY)
        shader.setFloatUniform("uIntensity", intensity)
        shader.setFloatUniform("uMode", if (mode == BlurMode.CORNERS) 0f else 1f)
        shaderPaint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shaderPaint)
    }

    private fun drawFallback(canvas: Canvas, width: Int, height: Int) {
        val w = width.toFloat()
        val h = height.toFloat()
        val tx = tiltX
        val ty = tiltY
        val frost = Color.argb((90 * intensity).toInt().coerceIn(0, 160), 210, 228, 245)

        if (mode == BlurMode.DIRECTIONAL) {
            val mag = hypot(tx, ty).coerceAtLeast(0.04f)
            val dirX = tx / mag
            val dirY = ty / mag
            val len = hypot(w, h) * 0.55f
            val cx = w * 0.5f
            val cy = h * 0.5f
            frostPaint.shader = LinearGradient(
                cx - dirX * len,
                cy - dirY * len,
                cx + dirX * len,
                cy + dirY * len,
                intArrayOf(Color.TRANSPARENT, frost),
                floatArrayOf(0.28f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, w, h, frostPaint)
        } else {
            val strengths = BlurMask.cornerStrengths(tx, ty)
            drawCorner(canvas, 0f, 0f, w, h, strengths.topLeft, frost)
            drawCorner(canvas, w, 0f, w, h, strengths.topRight, frost)
            drawCorner(canvas, 0f, h, w, h, strengths.bottomLeft, frost)
            drawCorner(canvas, w, h, w, h, strengths.bottomRight, frost)
        }

        val hx = w * (0.5f - tx * 0.22f)
        val hy = h * (0.32f - ty * 0.18f)
        val radius = minOf(w, h) * (0.22f + 0.08f * intensity)
        highlightPaint.shader = RadialGradient(
            hx,
            hy,
            radius,
            Color.argb((48 * intensity).toInt(), 255, 255, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(hx, hy, radius, highlightPaint)

        noisePaint.shader = noiseShader
        noisePaint.alpha = (22 * intensity).toInt().coerceIn(8, 40)
        canvas.drawRect(0f, 0f, w, h, noisePaint)
    }

    private fun drawCorner(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        strength: Float,
        color: Int,
    ) {
        val radius = hypot(w, h) * (0.22f + 0.16f * strength * intensity)
        val alpha = (Color.alpha(color) * (0.45f + 0.55f * strength)).toInt().coerceIn(0, 180)
        frostPaint.shader = RadialGradient(
            cx,
            cy,
            radius,
            Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius, frostPaint)
    }

    private fun createNoise(size: Int): Bitmap {
        val pixels = IntArray(size * size)
        var seed = 0xC15A1L
        for (i in pixels.indices) {
            seed = (seed * 1664525L + 1013904223L) and 0xFFFFFFFFL
            val n = (seed shr 16).toInt() and 255
            pixels[i] = Color.argb(n / 3, n, n, n)
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    companion object {
        // AGSL: cristal direccional / de esquinas. Alpha variable, tinte frío, brillo al inclinar.
        private const val AGSL = """
            uniform float2 uResolution;
            uniform float2 uTilt;
            uniform float uIntensity;
            uniform float uMode;

            float smoothstep2(float e0, float e1, float x) {
                float t = clamp((x - e0) / (e1 - e0), 0.0, 1.0);
                return t * t * (3.0 - 2.0 * t);
            }

            half4 main(float2 fragCoord) {
                float2 uv = fragCoord / uResolution;
                float2 p = uv * 2.0 - 1.0;
                float tx = clamp(uTilt.x, -1.0, 1.0);
                float ty = clamp(uTilt.y, -1.0, 1.0);
                float tiltMag = length(float2(tx, ty));
                float mask = 0.0;

                if (uMode < 0.5) {
                    float2 focus = float2(-tx, -ty) * 0.36;
                    float d = length(p - focus);
                    float vignette = smoothstep2(0.28, 1.12, d);
                    float cx = pow(abs(uv.x * 2.0 - 1.0), 3.0);
                    float cy = pow(abs(uv.y * 2.0 - 1.0), 3.0);
                    float corner = clamp(cx * cy * 4.5, 0.0, 1.0);
                    float far = 0.0;
                    if (tiltMag > 0.02) {
                        float2 pn = p / (length(p) + 1e-4);
                        far = max(dot(pn, float2(tx, ty) / tiltMag), 0.0) * tiltMag;
                    }
                    mask = clamp(vignette * 0.62 + corner * 0.72 + far * 0.34, 0.0, 1.0);
                } else {
                    float2 dir = tiltMag < 0.04 ? float2(0.0, 1.0) : float2(tx, ty) / tiltMag;
                    float proj = dot(p, dir);
                    float band = smoothstep2(-0.22, 0.78, proj);
                    float frame = smoothstep2(0.78, 1.0, max(abs(p.x), abs(p.y)));
                    mask = max(band * (0.55 + 0.45 * max(tiltMag, 0.2)), frame * 0.28);
                }

                mask = clamp(mask * uIntensity, 0.0, 1.0);

                float2 light = normalize(float2(-tx, -ty) + float2(0.18, 0.62));
                float spec = pow(max(dot(normalize(p + float2(0.001)), light) * 0.5 + 0.5, 0.0), 10.0);
                float rim = smoothstep2(0.58, 1.05, length(p * float2(0.72, 1.0)));
                float n = fract(sin(dot(fragCoord, float2(12.9898, 78.233))) * 43758.5453);

                half3 glass = half3(0.80, 0.88, 0.97);
                half3 hi = half3(1.0, 1.0, 1.0);
                half3 col = mix(glass, hi, spec * 0.50 * mask);
                col += (n - 0.5) * 0.07 * mask;

                float alpha = clamp(mask * 0.40 + spec * 0.14 * mask + rim * 0.07 * uIntensity, 0.0, 0.70);
                return half4(col, alpha);
            }
        """
    }
}
