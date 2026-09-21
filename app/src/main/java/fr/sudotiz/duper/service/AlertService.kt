package fr.sudotiz.duper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.net.toUri
import fr.sudotiz.duper.DuperApplication
import fr.sudotiz.duper.R

class AlertService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var isFlashing = false
    private var flashOn = false
    private var isRinging = false
    private var originalAlarmVolume: Int = -1
    private var unlockReceiverRegistered = false

    private val prefs by lazy { (applicationContext as DuperApplication).preferencesRepository }

    private val flashRunnable = object : Runnable {
        override fun run() {
            if (isFlashing) {
                toggleFlash()
                handler.postDelayed(this, FLASH_INTERVAL_MS)
            }
        }
    }

    private val stopAlertRunnable = Runnable { stopAlert() }

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                Log.d(TAG, "Device is unlocked, stopping alert")
                stopAlert()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager?.getCameraCharacteristics(id)
                    ?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error accessing camera", e)
        }
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RING -> {
                if (isRinging) return START_NOT_STICKY
                startForegroundNotification()
                val duration = prefs.ringDuration * 1000L
                wakeLock?.acquire(duration)
                registerUnlockReceiver()
                startRing()
            }
            ACTION_STOP_RING -> {
                Log.d(TAG, "Stop command received")
                stopAlert()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRing() {
        if (isRinging) return
        isRinging = true
        val duration = prefs.ringDuration * 1000L
        startRingtone(prefs.ringtoneUri)
        startVibration()
        startFlashing()
        handler.removeCallbacks(stopAlertRunnable)
        handler.postDelayed(stopAlertRunnable, duration)
    }

    private fun startRingtone(customUri: String?) {
        try {
            val uri: Uri = customUri?.toUri()
                ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                originalAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                    0
                )
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlertService, uri)
                isLooping = true
                prepare()
                start()
            }
            Log.d(TAG, "Ringtone started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ringtone", e)
        }
    }

    private fun startFlashing() {
        isFlashing = true
        handler.post(flashRunnable)
        Log.d(TAG, "Flash started")
    }

    private fun startVibration() {
        val vibrationAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 600, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0), vibrationAttributes)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0, vibrationAttributes)
        }
    }

    private fun toggleFlash() {
        try {
            cameraId?.let { id ->
                cameraManager?.setTorchMode(id, !flashOn)
                flashOn = !flashOn
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error toggling flash", e)
        }
    }

    private fun stopAlert() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null

        isFlashing = false
        handler.removeCallbacks(flashRunnable)
        handler.removeCallbacks(stopAlertRunnable)
        vibrator?.cancel()
        vibrator = null
        if (unlockReceiverRegistered) {
            unregisterReceiver(unlockReceiver)
            unlockReceiverRegistered = false
        }

        try {
            cameraId?.let { id -> cameraManager?.setTorchMode(id, false) }
            flashOn = false
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error stopping flash", e)
        }

        wakeLock?.let { if (it.isHeld) it.release() }

        if (originalAlarmVolume >= 0) {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalAlarmVolume, 0)
            originalAlarmVolume = -1
        }

        isRinging = false
        Log.d(TAG, "Alert stopped")
        stopSelf()
    }

    override fun onDestroy() {
        stopAlert()
        super.onDestroy()
    }

    private fun startForegroundNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_alert_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = getString(R.string.notification_channel_alert_description)
                }
            )
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
            .setContentTitle(getString(R.string.notification_alert_title))
            .setContentText(getString(R.string.notification_alert_text))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun registerUnlockReceiver() {
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(unlockReceiver, filter)
        }
        unlockReceiverRegistered = true
    }

    companion object {
        private const val TAG = "AlertService"
        private const val CHANNEL_ID = "duper_alerts"
        private const val NOTIFICATION_ID = 1
        private const val WAKE_LOCK_TAG = "Duper::AlertWakeLock"
        private const val FLASH_INTERVAL_MS = 500L

        const val ACTION_START_RING = "fr.sudotiz.duper.START_RING"
        const val ACTION_STOP_RING = "fr.sudotiz.duper.STOP_RING"
    }
}
