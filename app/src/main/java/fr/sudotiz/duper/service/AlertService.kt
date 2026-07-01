package fr.sudotiz.duper.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import fr.sudotiz.duper.DuperApplication
import fr.sudotiz.duper.R

class AlertService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var keyguardManager: KeyguardManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isFlashing = false
    private var flashOn = false
    private var isRinging = false
    private var originalAlarmVolume: Int = -1

    private val prefs by lazy { (applicationContext as DuperApplication).preferencesRepository }

    private val flashRunnable = object : Runnable {
        override fun run() {
            if (isFlashing) {
                toggleFlash()
                handler.postDelayed(this, FLASH_INTERVAL_MS)
            }
        }
    }

    private val unlockCheckRunnable = object : Runnable {
        override fun run() {
            if (isRinging) {
                val isLocked = keyguardManager?.isKeyguardLocked ?: true
                if (!isLocked) {
                    Log.d(TAG, "Device is unlocked, stopping alert")
                    stopAlert()
                } else {
                    handler.postDelayed(this, UNLOCK_CHECK_INTERVAL_MS)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        try {
            cameraId = cameraManager?.cameraIdList?.get(0)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error accessing camera", e)
        }

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RING -> {
                createNotificationChannel()
                startForeground(NOTIFICATION_ID, createNotification())
                val duration = prefs.ringDuration * 1000L
                wakeLock?.acquire(duration)
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
        startFlashing()

        handler.post(unlockCheckRunnable)
        handler.postDelayed({ stopAlert() }, duration)
    }

    private fun startRingtone(customUri: String?) {
        try {
            val uri: Uri = customUri?.toUri()
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)

                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                originalAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

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
        handler.removeCallbacks(unlockCheckRunnable)
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_alert_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_alert_description)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_alert_title))
            .setContentText(getString(R.string.notification_alert_text))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlert()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AlertService"
        private const val CHANNEL_ID = "duper_alerts"
        private const val NOTIFICATION_ID = 1
        private const val WAKE_LOCK_TAG = "Duper::AlertWakeLock"
        private const val FLASH_INTERVAL_MS = 500L
        private const val UNLOCK_CHECK_INTERVAL_MS = 1000L

        const val ACTION_START_RING = "fr.sudotiz.duper.START_RING"
        const val ACTION_STOP_RING = "fr.sudotiz.duper.STOP_RING"
    }
}
