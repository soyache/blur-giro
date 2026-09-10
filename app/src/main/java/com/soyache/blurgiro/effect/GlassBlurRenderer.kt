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
 * Niebla mate direccional / de esquinas.
 * No pinta highlights blancos ni escarcha cian: eso se leía como un velo de brillo
 * cuando el compositor no desenfoca detrás de un TYPE_APPLICATION_OVERLAY.
 */
class GlassBlurRenderer {

    private var runtimeShader: RuntimeShader? = null
    private val shaderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val frostPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val noisePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    private val noiseBitmap: Bitmap by lazy { createNoise(96) }
    private val noiseShader: Shader by lazy {
        BitmapShader(noiseBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }

    var tiltX: Float = 0f
    var tiltY: Float = 0f
    var intensity: Float = 0.62f
    var mode: BlurMode = BlurMode.CORNERS
    var compositorBlurLive: Boolean = false

    fun draw(canvas: Canvas, width: Int, height: Int) {
        if (width <= 0 || height <= 0 || intensity <= 0.01f) return
        if (Build.VERSION.SDK_INT >= 33) {
            drawAgsl(canvas, width, height)
        } else {
            drawFallback(canvas, width, height)
        }
    }

    private fun mistScale(): Float {
        return if (compositorBlurLive) GlassLook.MIST_WHEN_BLUR_LIVE else 1f
    }

    private fun drawAgsl(canvas: Canvas, width: Int, height: Int) {
        val shader = runtimeShader ?: RuntimeShader(AGSL).also { runtimeShader = it }
        shader.setFloatUniform("uResolution", width.toFloat(), height.toFloat())
        shader.setFloatUniform("uTilt", tiltX, tiltY)
        shader.setFloatUniform("uIntensity", intensity * mistScale())
        shader.setFloatUniform("uMode", if (mode == BlurMode.CORNERS) 0f else 1f)
        shaderPaint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shaderPaint)
    }

    private fun drawFallback(canvas: Canvas, width: Int, height: Int) {
        val w = width.toFloat()
        val h = height.toFloat()
        val tx = tiltX
        val ty = tiltY
        val engage = BlurMask.tiltEngage(hypot(tx, ty))
        if (engage <= 0.001f) return

        val strength = (intensity * engage * mistScale()).coerceIn(0f, 1f)
        val haze = Color.argb(
            (255 * GlassLook.MAX_MIST_ALPHA * strength).toInt().coerceIn(0, 72),
            GlassLook.HAZE_R_BYTE,
            GlassLook.HAZE_G_BYTE,
            GlassLook.HAZE_B_BYTE,
        )

        if (mode == BlurMode.DIRECTIONAL) {
            val mag = hypot(tx, ty).coerceAtLeast(0.04f)
            val dirX = tx / mag
            val dirY = ty / mag
            val len = hypot(w, h) * 0.62f
            val cx = w * 0.5f
            val cy = h * 0.5f
            frostPaint.shader = LinearGradient(
                cx - dirX * len,
                cy - dirY * len,
                cx + dirX * len,
                cy + dirY * len,
                intArrayOf(Color.TRANSPARENT, haze),
                floatArrayOf(0.38f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, w, h, frostPaint)
        } else {
            val strengths = BlurMask.cornerStrengths(tx, ty)
            drawCorner(canvas, 0f, 0f, w, h, strengths.topLeft, haze)
            drawCorner(canvas, w, 0f, w, h, strengths.topRight, haze)
            drawCorner(canvas, 0f, h, w, h, strengths.bottomLeft, haze)
            drawCorner(canvas, w, h, w, h, strengths.bottomRight, haze)
        }

        noisePaint.shader = noiseShader
        noisePaint.alpha = (18 * strength).toInt().coerceIn(0, 28)
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
        if (strength <= 0.02f) return
        val radius = hypot(w, h) * (0.18f + 0.20f * strength * intensity)
        val alpha = (Color.alpha(color) * strength).toInt().coerceIn(0, 72)
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
            // Grano oscuro, no motas blancas.
            val v = 20 + (n * 40) / 255
            pixels[i] = Color.argb(n / 5, v, v, v + 4)
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    companion object {
        private const val AGSL = """
            uniform float2 uResolution;
            uniform float2 uTilt;
            uniform float uIntensity;
            uniform float uMode;

            float smoothstep2(float e0, float e1, float x) {
                float t = clamp((x - e0) / (e1 - e0), 0.0, 1.0);
                return t * t * (3.0 - 2.0 * t);
            }

            float hash(float2 p) {
                return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
            }

            half4 main(float2 fragCoord) {
                float2 uv = fragCoord / uResolution;
                float2 p = uv * 2.0 - 1.0;
                float tx = clamp(uTilt.x, -1.0, 1.0);
                float ty = clamp(uTilt.y, -1.0, 1.0);
                float tiltMag = length(float2(tx, ty));
                float engage = smoothstep2(0.06, 0.34, tiltMag);
                if (engage < 0.001) {
                    return half4(0.0, 0.0, 0.0, 0.0);
                }

                float mask = 0.0;
                if (uMode < 0.5) {
                    float2 focus = float2(-tx, -ty) * 0.55;
                    float d = length(p - focus);
                    float dof = smoothstep2(0.42, 1.28, d);
                    float cx = abs(uv.x * 2.0 - 1.0);
                    float cy = abs(uv.y * 2.0 - 1.0);
                    float cornerness = (cx * cy) * (cx * cy);
                    float toward = max(dot(p, float2(tx, ty)), 0.0);
                    float far = 0.0;
                    if (tiltMag > 0.02) {
                        float2 pn = p / (length(p) + 1e-4);
                        far = max(dot(pn, float2(tx, ty) / tiltMag), 0.0);
                    }
                    mask = clamp(dof * 0.58 + toward * 0.42 * (0.30 + 0.70 * cornerness) + far * 0.22, 0.0, 1.0);
                } else {
                    float2 dir = float2(tx, ty) / max(tiltMag, 1e-4);
                    float proj = dot(p, dir);
                    float band = smoothstep2(-0.12, 0.90, proj);
                    float frame = smoothstep2(0.55, 1.02, max(abs(p.x), abs(p.y)));
                    mask = max(band, frame * 0.12 * engage);
                }

                mask = clamp(mask * engage * uIntensity, 0.0, 1.0);

                float n = hash(fragCoord);
                float n2 = hash(fragCoord * 1.73 + 9.1);
                float cloud = 0.5 + 0.5 * sin(uv.x * 17.0 + n * 6.0) * cos(uv.y * 13.0 + n2 * 5.0);
                cloud = smoothstep2(0.28, 0.82, cloud);

                half3 haze = mix(half3(0.14, 0.15, 0.18), half3(0.20, 0.21, 0.24), cloud);
                float grain = (n - 0.5) * 0.10 * mask;
                haze += grain;

                float rim = smoothstep2(0.78, 1.08, length(p * float2(0.78, 1.0)));
                float alpha = mask * 0.22 + rim * mask * 0.05;
                alpha = clamp(alpha, 0.0, 0.26);
                if (alpha < 0.004) {
                    return half4(0.0, 0.0, 0.0, 0.0);
                }
                return half4(haze, alpha);
            }
        """
    }
}
