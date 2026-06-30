package com.example.duper

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

    private val flashRunnable = object : Runnable {
        override fun run() {
            if (isFlashing) {
                toggleFlash()
                handler.postDelayed(this, 500)
            }
        }
    }

    private val unlockCheckRunnable = object : Runnable {
        override fun run() {
            if (isRinging) {
                val isLocked = keyguardManager?.isKeyguardLocked ?: true
                if (!isLocked) {
                    Log.d("AlertService", "Device is unlocked, stopping alert")
                    stopAlert()
                } else {
                    handler.postDelayed(this, 1000)
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
            Log.e("AlertService", "Error accessing camera", e)
        }

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Duper::AlertWakeLock"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_RING" -> {
                createNotificationChannel()
                val notification = createNotification()
                startForeground(1, notification)
                val duration = getSharedPreferences("duper_prefs", MODE_PRIVATE)
                    .getInt("ring_duration", 30) * 1000L

                wakeLock?.acquire(duration)
                startRing()
            }

            "STOP_RING" -> {
                Log.d("AlertService", "Stop command received")
                stopAlert()
            }
        }

        return START_NOT_STICKY
    }

    private fun startRing() {
        if (isRinging) return
        isRinging = true

        val prefs = getSharedPreferences("duper_prefs", MODE_PRIVATE)
        val duration = prefs.getInt("ring_duration", 30) * 1000L
        val ringtoneUri = prefs.getString("ringtone_uri", null)

        startRingtone(ringtoneUri)
        startFlashing()

        handler.post(unlockCheckRunnable)

        handler.postDelayed({
            stopAlert()
        }, duration)
    }

    private fun startRingtone(customUri: String?) {
        try {
            val uri: Uri = customUri?.toUri()
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)

                val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
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

            Log.d("AlertService", "Ringtone started")
        } catch (e: Exception) {
            Log.e("AlertService", "Error starting ringtone", e)
        }
    }

    private fun startFlashing() {
        isFlashing = true
        handler.post(flashRunnable)
        Log.d("AlertService", "Flash started")
    }

    private fun toggleFlash() {
        try {
            cameraId?.let { id ->
                cameraManager?.setTorchMode(id, !flashOn)
                flashOn = !flashOn
            }
        } catch (e: CameraAccessException) {
            Log.e("AlertService", "Error toggling flash", e)
        }
    }

    private fun stopAlert() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null

        isFlashing = false
        handler.removeCallbacks(flashRunnable)
        handler.removeCallbacks(unlockCheckRunnable)
        try {
            cameraId?.let { id ->
                cameraManager?.setTorchMode(id, false)
            }
            flashOn = false
        } catch (e: CameraAccessException) {
            Log.e("AlertService", "Error stopping flash", e)
        }

        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }

        isRinging = false
        Log.d("AlertService", "Alert stopped")
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Duper Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Duper SMS alerts"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Duper Ring")
            .setContentText("Alert active...")
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
        private const val CHANNEL_ID = "duper_alerts"
    }
}
