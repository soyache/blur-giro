package com.soyache.blurgiro.overlay

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
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.soyache.blurgiro.MainActivity
import com.soyache.blurgiro.R
import com.soyache.blurgiro.data.AppSettings
import com.soyache.blurgiro.effect.CrossWindowBlur
import com.soyache.blurgiro.sensor.TiltTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OverlayService : Service() {

    private lateinit var settings: AppSettings
    private var overlay: OverlayController? = null
    private var tiltTracker: TiltTracker? = null
    private var receiversRegistered = false
    private var stopBlurListen: (() -> Unit)? = null

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
                Intent.ACTION_SCREEN_OFF -> {
                    tiltTracker?.stop()
                    overlay?.onTilt(0f, 0f)
                }
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

        if (!Settings.canDrawOverlays(this) || !CrossWindowBlur.isEnabled(this)) {
            settings.overlayRequested = false
            stopSelf()
            return START_NOT_STICKY
        }

        startInForeground()
        settings.overlayRequested = true
        ensureOverlay()
        listenBlur()
        registerAux()
        startSensorsIfInteractive()
        _running.value = true
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlay?.onDisplayChanged()
    }

    override fun onDestroy() {
        stopBlurListen?.invoke()
        stopBlurListen = null
        tiltTracker?.stop()
        tiltTracker = null
        overlay?.hide()
        overlay = null
        settings.unregister(prefsListener)
        if (receiversRegistered) {
            unregisterReceiver(screenReceiver)
            receiversRegistered = false
        }
        _running.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureOverlay() {
        if (overlay == null) {
            overlay = OverlayController(this).also { it.show() }
        }
        if (tiltTracker == null) {
            tiltTracker = TiltTracker(
                context = this,
                smoothness = { settings.smoothness },
                onTilt = { x, y -> overlay?.onTilt(x, y) },
            )
        }
    }

    private fun listenBlur() {
        if (stopBlurListen != null) return
        stopBlurListen = CrossWindowBlur.listen(this) { enabled ->
            if (!enabled && _running.value) {
                settings.overlayRequested = false
                stopSelf()
            }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
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

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()

        fun start(context: Context) {
            val app = context.applicationContext
            val intent = Intent(app, OverlayService::class.java)
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
