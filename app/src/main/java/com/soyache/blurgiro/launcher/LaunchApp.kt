package com.soyache.blurgiro.launcher

import android.graphics.Bitmap

data class LaunchApp(
    val label: String,
    val packageName: String,
    val className: String,
    val icon: Bitmap,
    val isSettings: Boolean = false,
) {
    val key: String get() = "$packageName/$className"
}
