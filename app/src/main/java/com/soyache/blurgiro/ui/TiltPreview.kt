package com.soyache.blurgiro.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.soyache.blurgiro.data.BlurMode
import com.soyache.blurgiro.effect.DefocusPyramid
import com.soyache.blurgiro.effect.FakeHome
import com.soyache.blurgiro.overlay.CrystalOverlayView

@Composable
fun TiltPreview(
    tiltX: Float,
    tiltY: Float,
    intensity: Float,
    mode: BlurMode,
    modifier: Modifier = Modifier,
) {
    val pyramid = remember(intensity) {
        val w = 240
        val h = 420
        val pixels = FakeHome.render(w, h)
        val src = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        src.setPixels(pixels, 0, w, 0, 0, w, h)
        DefocusPyramid.build(src, intensity, ownSource = true)
    }
    DisposableEffect(pyramid) {
        onDispose { pyramid.recycle() }
    }

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                CrystalOverlayView(context).apply { previewMode = true }
            },
            update = { view ->
                view.previewMode = true
                view.setPyramid(pyramid)
                view.setTilt(tiltX, tiltY, intensity, mode)
            },
        )
    }
}
