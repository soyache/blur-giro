package com.soyache.blurgiro.overlay

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.soyache.blurgiro.MainActivity
import com.soyache.blurgiro.R
import com.soyache.blurgiro.data.AppSettings
import com.soyache.blurgiro.sensor.TiltTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OverlayService : Service() {

    private lateinit var settings: AppSettings
    private var overlay: OverlayController? = null
    private var tiltTracker: TiltTracker? = null
    private var receiversRegistered = false
    private var projection: MediaProjection? = null

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == AppSettings.KEY_INTENSITY ||
            key == AppSettings.KEY_MODE ||
            key == AppSettings.KEY_SMOOTHNESS
        ) {
            overlay?.onSettingsChanged()
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> tiltTracker?.stop()
                Intent.ACTION_SCREEN_ON -> startSensorsIfInteractive()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        settings = AppSettings.get(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            settings.overlayRequested = false
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            settings.overlayRequested = false
            stopSelf()
            return START_NOT_STICKY
        }

        startInForeground()

        val token = readProjection(intent)
        if (token == null) {
            settings.overlayRequested = false
            stopSelf()
            return START_NOT_STICKY
        }

        if (projection == null) {
            val mgr = getSystemService(MediaProjectionManager::class.java)
            projection = runCatching { mgr.getMediaProjection(token.first, token.second) }.getOrNull()
        }
        val live = projection
        if (live == null) {
            settings.overlayRequested = false
            stopSelf()
            return START_NOT_STICKY
        }

        settings.overlayRequested = true
        ensureOverlay(live)
        registerAux()
        startSensorsIfInteractive()
        _running.value = true
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlay?.onDisplayChanged()
    }

    override fun onDestroy() {
        tiltTracker?.stop()
        tiltTracker = null
        overlay?.hide()
        overlay = null
        projection = null
        settings.unregister(prefsListener)
        if (receiversRegistered) {
            unregisterReceiver(screenReceiver)
            receiversRegistered = false
        }
        _running.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun readProjection(intent: Intent?): Pair<Int, Intent>? {
        if (intent == null) return null
        val code = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (code != Activity.RESULT_OK || data == null) return null
        return code to data
    }

    private fun ensureOverlay(projection: MediaProjection) {
        if (overlay == null) {
            overlay = OverlayController(this).also {
                it.show(projection) {
                    settings.overlayRequested = false
                    stopSelf()
                }
            }
        }
        if (tiltTracker == null) {
            tiltTracker = TiltTracker(
                context = this,
                smoothness = { settings.smoothness },
                onTilt = { x, y -> overlay?.onTilt(x, y) },
            )
        }
    }

    private fun startSensorsIfInteractive() {
        val power = getSystemService(PowerManager::class.java)
        if (power.isInteractive) {
            tiltTracker?.start()
        }
    }

    private fun registerAux() {
        if (!receiversRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(screenReceiver, filter)
            }
            receiversRegistered = true
        }
        settings.register(prefsListener)
    }

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else if (Build.VERSION.SDK_INT >= 29) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, getString(R.string.notification_stop), stop)
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_STOP = "com.soyache.blurgiro.STOP_OVERLAY"
        const val CHANNEL_ID = "cristalgiro_overlay"
        const val NOTIFICATION_ID = 17
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()

        fun start(context: Context, resultCode: Int, data: Intent) {
            val app = context.applicationContext
            val intent = Intent(app, OverlayService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                app.startForegroundService(intent)
            } else {
                app.startService(intent)
            }
        }

        fun stop(context: Context) {
            val app = context.applicationContext
            AppSettings.get(app).overlayRequested = false
            app.stopService(Intent(app, OverlayService::class.java))
        }
    }
}
