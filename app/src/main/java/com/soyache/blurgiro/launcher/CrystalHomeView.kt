package com.soyache.blurgiro.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.widget.OverScroller
import com.soyache.blurgiro.data.BlurMode
import com.soyache.blurgiro.effect.BlurMask
import com.soyache.blurgiro.effect.DefocusPyramid
import com.soyache.blurgiro.effect.PerspectiveWarp
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Pinta wallpaper + iconos + dock (píxeles propios) y aplica el cristal:
 * de frente identidad; al girar en X, blur direccional + warp suave.
 */
class CrystalHomeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var onAppClick: ((LaunchApp) -> Unit)? = null
    var onSettingsClick: (() -> Unit)? = null

    private var grid: List<LaunchApp> = emptyList()
    private var dock: List<LaunchApp> = emptyList()
    private var wallpaper: Bitmap? = null
    private var tiltX = 0f
    private var tiltY = 0f
    private var intensity = 0.48f
    private var mode: BlurMode = BlurMode.DIRECTIONAL

    private var insetL = 0f
    private var insetT = 0f
    private var insetR = 0f
    private var insetB = 0f

    private var scene: Bitmap? = null
    private var sceneDirty = true
    private var pyramid: DefocusPyramid? = null
    private var pyramidDirty = true
    private var lastIntensity = intensity

    private var scrollXpx = 0f
    private var dragStartX = 0f
    private var dragStartScroll = 0f
    private var dragging = false
    private val scroller = OverScroller(context)
    private var velocity: VelocityTracker? = null

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dockPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        setShadowLayer(3f, 0f, 1.2f, 0x99000000.toInt())
    }
    private val dstIn = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    private val warp = Matrix()
    private val srcRect = Rect()
    private val dstRect = RectF()
    private val tmpRect = RectF()
    private val iconSrc = Rect()

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)
        isClickable = true
        dockPaint.color = 0x66000000
    }

    fun setInsets(left: Int, top: Int, right: Int, bottom: Int) {
        val nl = left.toFloat()
        val nt = top.toFloat()
        val nr = right.toFloat()
        val nb = bottom.toFloat()
        if (nl == insetL && nt == insetT && nr == insetR && nb == insetB) return
        insetL = nl
        insetT = nt
        insetR = nr
        insetB = nb
        markSceneDirty()
    }

    fun bind(
        grid: List<LaunchApp>,
        dock: List<LaunchApp>,
        wallpaper: Bitmap?,
        tiltX: Float,
        tiltY: Float,
        intensity: Float,
        mode: BlurMode,
    ) {
        var contentChanged = false
        if (this.grid !== grid) {
            this.grid = grid
            contentChanged = true
        }
        if (this.dock !== dock) {
            this.dock = dock
            contentChanged = true
        }
        if (this.wallpaper !== wallpaper) {
            this.wallpaper = wallpaper
            contentChanged = true
        }
        this.tiltX = tiltX
        this.tiltY = tiltY
        if (this.intensity != intensity || this.mode != mode) {
            this.intensity = intensity
            this.mode = mode
            pyramidDirty = true
        }
        if (contentChanged) markSceneDirty()
        invalidate()
    }

    private fun markSceneDirty() {
        sceneDirty = true
        pyramidDirty = true
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        markSceneDirty()
        scrollXpx = HomeLayout.nearestPage(
            scrollXpx,
            HomeLayout.pageCount(grid.size),
            w.toFloat().coerceAtLeast(1f),
        ) * w.toFloat()
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollXpx = scroller.currX.toFloat()
            sceneDirty = true
            pyramidDirty = true
            invalidate()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) scroller.abortAnimation()
                dragging = false
                dragStartX = event.x
                dragStartScroll = scrollXpx
                velocity = VelocityTracker.obtain().also { it.addMovement(event) }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                velocity?.addMovement(event)
                val dx = event.x - dragStartX
                if (!dragging && abs(dx) > touchSlop()) {
                    dragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (dragging) {
                    val pages = HomeLayout.pageCount(grid.size)
                    val maxScroll = ((pages - 1).coerceAtLeast(0)) * width.toFloat()
                    scrollXpx = (dragStartScroll - dx).coerceIn(0f, maxScroll)
                    sceneDirty = true
                    pyramidDirty = true
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocity?.addMovement(event)
                val wasDragging = dragging
                dragging = false
                if (!wasDragging && event.actionMasked == MotionEvent.ACTION_UP) {
                    handleTap(event.x, event.y)
                } else {
                    settlePage(velocity)
                }
                velocity?.recycle()
                velocity = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun touchSlop(): Float = 24f * resources.displayMetrics.density

    private fun handleTap(x: Float, y: Float) {
        val m = metrics()
        val dockHit = HomeLayout.hitDock(x, y, m)
        if (dockHit >= 0) {
            val app = dock.getOrNull(dockHit) ?: return
            if (app.isSettings) onSettingsClick?.invoke() else onAppClick?.invoke(app)
            return
        }
        val index = HomeLayout.hitApp(x, y, scrollXpx, grid.size, m)
        grid.getOrNull(index)?.let { onAppClick?.invoke(it) }
    }

    private fun settlePage(tracker: VelocityTracker?) {
        val pages = HomeLayout.pageCount(grid.size)
        val pageW = width.toFloat().coerceAtLeast(1f)
        tracker?.computeCurrentVelocity(1000)
        val vx = tracker?.xVelocity ?: 0f
        val current = scrollXpx / pageW
        val target = when {
            vx < -800 -> (current.toInt() + 1)
            vx > 800 -> current.toInt()
            else -> HomeLayout.nearestPage(scrollXpx, pages, pageW)
        }.coerceIn(0, (pages - 1).coerceAtLeast(0))
        val dest = target * pageW
        scroller.startScroll(scrollXpx.roundToInt(), 0, (dest - scrollXpx).roundToInt(), 0, 280)
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) return
        val amount = BlurMask.effectAmount(tiltX, tiltY, intensity)
        if (amount <= 0.02f || dragging || !scroller.isFinished) {
            drawHome(canvas)
            return
        }
        ensureScene()
        val sharp = scene ?: return
        ensurePyramid(sharp)
        val layers = pyramid
        val dest = PerspectiveWarp.destQuad(width.toFloat(), height.toFloat(), tiltX, tiltY, intensity)
        warp.reset()
        warp.setPolyToPoly(
            PerspectiveWarp.identityQuad(width.toFloat(), height.toFloat()),
            0,
            dest,
            0,
            4,
        )
        val cover = 1f + PerspectiveWarp.maxCornerDelta(dest, width.toFloat(), height.toFloat()) /
            hypot(width.toFloat(), height.toFloat()).coerceAtLeast(1f) * 2.4f
        warp.postScale(cover.coerceIn(1f, 1.12f), cover.coerceIn(1f, 1.12f), width * 0.5f, height * 0.5f)
        canvas.save()
        canvas.concat(warp)
        drawBitmap(canvas, sharp, identity = false)
        if (layers != null) {
            drawMasked(canvas, layers.mid, farMask(width.toFloat(), height.toFloat(), mid = true))
            drawMasked(canvas, layers.heavy, farMask(width.toFloat(), height.toFloat(), mid = false))
        }
        canvas.restore()
    }

    private fun ensureScene() {
        val w = width
        val h = height
        val existing = scene
        if (!sceneDirty && existing != null && existing.width == w && existing.height == h && !existing.isRecycled) {
            return
        }
        val next = if (existing != null && existing.width == w && existing.height == h && !existing.isRecycled) {
            existing
        } else {
            existing?.recycle()
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        }
        val c = Canvas(next)
        drawHome(c)
        scene = next
        sceneDirty = false
        pyramidDirty = true
    }

    private fun ensurePyramid(sharp: Bitmap) {
        if (!pyramidDirty && pyramid != null && lastIntensity == intensity) return
        pyramid?.recycle()
        pyramid = runCatching { DefocusPyramid.build(sharp, intensity, ownSource = false) }.getOrNull()
        lastIntensity = intensity
        pyramidDirty = false
    }

    private fun drawHome(canvas: Canvas) {
        val m = metrics()
        drawWallpaper(canvas)
        val pages = HomeLayout.pageCount(grid.size)
        val first = (scrollXpx / m.pageWidth).toInt().coerceAtLeast(0)
        for (page in first..(first + 1).coerceAtMost(pages - 1)) {
            val start = page * HomeLayout.PAGE_SIZE
            val count = (grid.size - start).coerceIn(0, HomeLayout.PAGE_SIZE)
            for (i in 0 until count) {
                val app = grid[start + i]
                drawCell(canvas, HomeLayout.cellRect(i, page, scrollXpx, m), app, m)
            }
        }
        drawDock(canvas, m)
        drawDots(canvas, m, pages)
    }

    private fun drawWallpaper(canvas: Canvas) {
        val paper = wallpaper
        if (paper == null || paper.isRecycled) {
            canvas.drawColor(0xFF0B1020.toInt())
            return
        }
        val scale = DefocusPyramid.coverScale(paper.width, paper.height, width, height)
        val dw = paper.width * scale
        val dh = paper.height * scale
        val left = (width - dw) / 2f
        val top = (height - dh) / 2f
        srcRect.set(0, 0, paper.width, paper.height)
        dstRect.set(left, top, left + dw, top + dh)
        canvas.drawBitmap(paper, srcRect, dstRect, bitmapPaint)
    }

    private fun drawCell(canvas: Canvas, cell: Box, app: LaunchApp, m: HomeLayout.Metrics) {
        if (cell.right < 0f || cell.left > width) return
        val icon = HomeLayout.iconRect(cell, m)
        drawIcon(canvas, app.icon, icon)
        textPaint.textSize = m.iconSize * 0.22f
        val labelY = icon.bottom + textPaint.textSize * 1.25f
        val label = ellipsize(app.label, cell.width * 0.88f)
        canvas.drawText(label, cell.centerX(), labelY, textPaint)
    }

    private fun drawDock(canvas: Canvas, m: HomeLayout.Metrics) {
        tmpRect.set(
            m.padLeft * 0.45f,
            m.dockTop,
            m.width - m.padRight * 0.45f,
            m.height - m.padBottom * 0.35f,
        )
        val radius = tmpRect.height() * 0.28f
        canvas.drawRoundRect(tmpRect, radius, radius, dockPaint)
        for (i in dock.indices) {
            val cell = HomeLayout.dockRect(i, m)
            val icon = HomeLayout.iconRect(cell, m)
            drawIcon(canvas, dock[i].icon, icon)
        }
    }

    private fun drawDots(canvas: Canvas, m: HomeLayout.Metrics, pages: Int) {
        if (pages <= 1) return
        val y = m.dockTop - m.dockHeight * 0.08f
        val gap = 10f * resources.displayMetrics.density
        val r = 3.2f * resources.displayMetrics.density
        val total = (pages - 1) * gap
        val start = m.width / 2f - total / 2f
        val current = scrollXpx / m.pageWidth
        for (i in 0 until pages) {
            val t = (1f - abs(current - i)).coerceIn(0.35f, 1f)
            dotPaint.color = Color.argb((t * 220).toInt(), 255, 255, 255)
            canvas.drawCircle(start + i * gap, y, r * (0.75f + 0.25f * t), dotPaint)
        }
    }

    private fun drawIcon(canvas: Canvas, icon: Bitmap, dest: Box) {
        if (icon.isRecycled) return
        iconSrc.set(0, 0, icon.width, icon.height)
        dstRect.set(dest.left, dest.top, dest.right, dest.bottom)
        canvas.drawBitmap(icon, iconSrc, dstRect, bitmapPaint)
    }

    private fun ellipsize(text: String, maxWidth: Float): String {
        if (textPaint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && textPaint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "…"
    }

    private fun drawBitmap(canvas: Canvas, bitmap: Bitmap, identity: Boolean) {
        if (bitmap.isRecycled) return
        srcRect.set(0, 0, bitmap.width, bitmap.height)
        dstRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawBitmap(bitmap, srcRect, dstRect, bitmapPaint)
        if (identity) return
    }

    private fun drawMasked(canvas: Canvas, bitmap: Bitmap, shader: Shader?) {
        if (bitmap.isRecycled || shader == null) return
        srcRect.set(0, 0, bitmap.width, bitmap.height)
        dstRect.set(0f, 0f, width.toFloat(), height.toFloat())
        val save = canvas.saveLayer(dstRect, null)
        canvas.drawBitmap(bitmap, srcRect, dstRect, bitmapPaint)
        maskPaint.xfermode = dstIn
        maskPaint.shader = shader
        canvas.drawRect(dstRect, maskPaint)
        maskPaint.xfermode = null
        maskPaint.shader = null
        canvas.restoreToCount(save)
    }

    private fun farMask(w: Float, h: Float, mid: Boolean): Shader {
        val tx = tiltX.coerceIn(-1f, 1f)
        val ty = (tiltY * PerspectiveWarp.Y_WEIGHT).coerceIn(-1f, 1f)
        if (mode == BlurMode.CORNERS) {
            val fx = w * (0.5f + tx * 0.42f)
            val fy = h * (0.5f + ty * 0.42f)
            val radius = hypot(w, h) * if (mid) 0.92f else 0.78f
            val stops = if (mid) floatArrayOf(0f, 0.32f, 1f) else floatArrayOf(0f, 0.52f, 1f)
            return RadialGradient(
                fx,
                fy,
                radius,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.WHITE),
                stops,
                Shader.TileMode.CLAMP,
            )
        }
        val mag = hypot(tx, ty).coerceAtLeast(0.04f)
        val dx = tx / mag
        val dy = ty / mag
        val cx = w * 0.5f
        val cy = h * 0.5f
        val len = hypot(w, h) * 0.58f
        val closerX = cx + dx * len
        val closerY = cy + dy * len
        val farX = cx - dx * len
        val farY = cy - dy * len
        val stops = if (mid) {
            floatArrayOf(0f, 0.24f, 0.72f, 1f)
        } else {
            floatArrayOf(0f, 0.50f, 0.86f, 1f)
        }
        return LinearGradient(
            closerX,
            closerY,
            farX,
            farY,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.WHITE, Color.WHITE),
            stops,
            Shader.TileMode.CLAMP,
        )
    }

    private fun metrics(): HomeLayout.Metrics =
        HomeLayout.metrics(width.toFloat(), height.toFloat(), insetL, insetT, insetR, insetB)
}
