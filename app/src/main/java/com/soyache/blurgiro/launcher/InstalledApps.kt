package com.soyache.blurgiro.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.soyache.blurgiro.R

object InstalledApps {

    private const val ICON_PX = 192

    data class Catalog(
        val grid: List<LaunchApp>,
        val dock: List<LaunchApp>,
        val settings: LaunchApp,
    )

    fun load(context: Context): Catalog {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = queryLaunchers(pm, query)
        val grid = resolved
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .mapNotNull { toLaunchApp(pm, it) }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
            .toList()

        val settings = LaunchApp(
            label = context.getString(R.string.settings_shortcut),
            packageName = context.packageName,
            className = SETTINGS_SLOT,
            icon = vectorIcon(context, R.drawable.ic_settings),
            isSettings = true,
        )
        val dock = buildDock(context, pm, grid, settings)
        return Catalog(grid = grid, dock = dock, settings = settings)
    }

    fun launchIntent(app: LaunchApp): Intent {
        return Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(app.packageName, app.className)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun buildDock(
        context: Context,
        pm: PackageManager,
        grid: List<LaunchApp>,
        settings: LaunchApp,
    ): List<LaunchApp> {
        val preferred = listOfNotNull(
            resolve(pm, Intent(Intent.ACTION_DIAL)),
            resolve(pm, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))),
            resolve(pm, Intent(Intent.ACTION_VIEW, Uri.parse("https://"))),
            resolve(
                pm,
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                },
            ),
        ).distinctBy { it.key }
            .filter { it.packageName != context.packageName }

        val extras = grid.filter { candidate ->
            preferred.none { it.key == candidate.key }
        }
        val filled = (preferred + extras).take((HomeLayout.DOCK_SLOTS - 1).coerceAtLeast(0))
        return filled + settings
    }

    private fun resolve(pm: PackageManager, intent: Intent): LaunchApp? {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(intent, 0)
        } ?: return null
        return toLaunchApp(pm, info)
    }

    private fun queryLaunchers(pm: PackageManager, intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
    }

    private fun toLaunchApp(pm: PackageManager, info: ResolveInfo): LaunchApp? {
        val activity = info.activityInfo ?: return null
        val label = info.loadLabel(pm)?.toString().orEmpty().ifBlank { activity.packageName }
        val icon = drawableToBitmap(info.loadIcon(pm))
        return LaunchApp(
            label = label,
            packageName = activity.packageName,
            className = activity.name,
            icon = icon,
        )
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
            val src = drawable.bitmap
            if (src.width == ICON_PX && src.height == ICON_PX) return src
            return Bitmap.createScaledBitmap(src, ICON_PX, ICON_PX, true)
        }
        val bitmap = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (drawable == null) {
            canvas.drawColor(0xFF3A4663.toInt())
        } else {
            drawable.setBounds(0, 0, ICON_PX, ICON_PX)
            drawable.draw(canvas)
        }
        return bitmap
    }

    private fun vectorIcon(context: Context, resId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, resId)
        return drawableToBitmap(drawable)
    }

    const val SETTINGS_SLOT = "cristalgiro.settings"
}
