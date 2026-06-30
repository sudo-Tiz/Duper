package com.example.duper

import android.Manifest
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.edit

class LocationService : Service(), LocationListener {

    private var locationManager: LocationManager? = null
    private var keyguardManager: KeyguardManager? = null
    private var senderPhoneNumber: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isTracking = false
    private var lastLocation: Location? = null

    private val sendLocationRunnable = object : Runnable {
        override fun run() {
            if (isTracking) {
                lastLocation?.let { location ->
                    sendLocationSms(location)
                }

                val prefs = getSharedPreferences("duper_prefs", MODE_PRIVATE)
                val interval = prefs.getInt("locate_interval", 30) * 1000L
                handler.postDelayed(this, interval)
            }
        }
    }

    private val unlockCheckRunnable = object : Runnable {
        override fun run() {
            if (isTracking) {
                val isLocked = keyguardManager?.isKeyguardLocked ?: true
                if (!isLocked) {
                    Log.d("LocationService", "Device is unlocked, stopping tracking")
                    stopTracking()
                } else {
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_LOCATE" -> {
                senderPhoneNumber = intent.getStringExtra("sender")

                createNotificationChannel()
                val notification = createNotification()
                startForeground(2, notification)

                startTracking()
            }

            "STOP_LOCATE" -> {
                Log.d("LocationService", "Stop command received")
                stopTracking()
            }
        }

        return START_STICKY
    }

    private fun startTracking() {
        if (isTracking) return
        isTracking = true

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("LocationService", "Location permission not granted")
            SmsUtil.send(
                this,
                senderPhoneNumber,
                "Duper: Location permission not granted, cannot locate"
            )
            stopTracking()
            return
        }

        var gpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        var networkEnabled =
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false

        if (!gpsEnabled && !networkEnabled) {
            Log.d("LocationService", "Location is off, attempting to enable it")
            tryEnableLocation()
            gpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
            networkEnabled =
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false
        }

        if (!gpsEnabled && !networkEnabled) {
            Log.e("LocationService", "Location services are disabled and could not be enabled")
            SmsUtil.send(
                this, senderPhoneNumber,
                "Duper: Location is OFF and I cannot turn it on. Grant WRITE_SECURE_SETTINGS via ADB to allow auto-enable."
            )
            stopTracking()
            return
        }

        val providers = buildString {
            if (gpsEnabled) append("gps")
            if (gpsEnabled && networkEnabled) append("+")
            if (networkEnabled) append("network")
        }
        SmsUtil.send(
            this,
            senderPhoneNumber,
            "Duper: Location ON ($providers), sending coordinates..."
        )

        try {
            if (gpsEnabled) {
                try {
                    locationManager?.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 5000L, 0f, this
                    )
                } catch (e: Exception) {
                    Log.e("LocationService", "Error requesting GPS updates", e)
                }
            }

            if (networkEnabled) {
                try {
                    locationManager?.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 5000L, 0f, this
                    )
                } catch (e: Exception) {
                    Log.e("LocationService", "Error requesting network updates", e)
                }
            }

            lastLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val prefs = getSharedPreferences("duper_prefs", MODE_PRIVATE)
            val duration = prefs.getInt("locate_duration", 300) * 1000L
            val interval = prefs.getInt("locate_interval", 30) * 1000L

            handler.post(sendLocationRunnable)
            handler.post(unlockCheckRunnable)

            handler.postDelayed({
                stopTracking()
            }, duration)

            Log.d(
                "LocationService",
                "Location tracking started (interval: ${interval}ms, duration: ${duration}ms)"
            )
        } catch (e: SecurityException) {
            Log.e("LocationService", "Security exception", e)
            stopTracking()
        }
    }

    private fun tryEnableLocation(): Boolean {
        // Strategy 1: LocationManager.setLocationEnabledForUser (works on API 28+ with WRITE_SECURE_SETTINGS)
        try {
            val method = LocationManager::class.java.getMethod(
                "setLocationEnabledForUser",
                Boolean::class.javaPrimitiveType,
                UserHandle::class.java
            )
            method.invoke(locationManager, true, Process.myUserHandle())
            Thread.sleep(800)
            if (isAnyProviderEnabled()) {
                Log.d("LocationService", "Location enabled via setLocationEnabledForUser")
                return true
            }
        } catch (e: Exception) {
            Log.w("LocationService", "setLocationEnabledForUser failed: ${e.message}")
        }

        // Strategy 2: Write LOCATION_MODE secure setting (works on older versions)
        try {
            @Suppress("DEPRECATION")
            Settings.Secure.putInt(
                contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
            )
            Thread.sleep(800)
            if (isAnyProviderEnabled()) {
                Log.d("LocationService", "Location enabled via LOCATION_MODE setting")
                return true
            }
        } catch (e: SecurityException) {
            Log.e("LocationService", "WRITE_SECURE_SETTINGS not granted", e)
        } catch (e: Exception) {
            Log.e("LocationService", "Failed to write LOCATION_MODE", e)
        }

        return isAnyProviderEnabled()
    }

    private fun isAnyProviderEnabled(): Boolean {
        val gps = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        val net = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false
        return gps || net
    }

    private fun sendLocationSms(location: Location) {
        val accuracy = if (location.hasAccuracy()) " \u00b1${location.accuracy.toInt()}m" else ""
        val provider = location.provider ?: "unknown"
        SmsUtil.send(
            this,
            senderPhoneNumber,
            "${location.latitude}, ${location.longitude} ($provider$accuracy)"
        )

        val prefs = getSharedPreferences("duper_prefs", MODE_PRIVATE)
        prefs.edit {
            putLong("last_location_time", System.currentTimeMillis())
            putString("last_location_lat", location.latitude.toString())
            putString("last_location_lng", location.longitude.toString())
        }
    }

    override fun onLocationChanged(location: Location) {
        Log.d("LocationService", "Location update received (${location.provider})")
        if (isBetterLocation(location, lastLocation)) {
            lastLocation = location
        }
    }

    private fun isBetterLocation(candidate: Location, current: Location?): Boolean {
        if (current == null) return true

        val timeDelta = candidate.time - current.time
        val isSignificantlyNewer = timeDelta > 30_000L
        val isSignificantlyOlder = timeDelta < -30_000L
        val isNewer = timeDelta > 0

        if (isSignificantlyNewer) return true
        if (isSignificantlyOlder) return false

        val accuracyDelta = candidate.accuracy - current.accuracy
        val isMoreAccurate = accuracyDelta < 0
        val isLessAccurate = accuracyDelta > 0
        val isSignificantlyLessAccurate = accuracyDelta > 200
        val isSameProvider = candidate.provider == current.provider

        return when {
            isMoreAccurate -> true
            isNewer && !isLessAccurate -> true
            isNewer && !isSignificantlyLessAccurate && isSameProvider -> true
            else -> false
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String) {
        Log.d("LocationService", "Provider enabled: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        Log.d("LocationService", "Provider disabled: $provider")
    }

    private fun stopTracking() {
        isTracking = false
        handler.removeCallbacks(sendLocationRunnable)
        handler.removeCallbacks(unlockCheckRunnable)

        try {
            locationManager?.removeUpdates(this)
        } catch (e: Exception) {
            Log.e("LocationService", "Error removing location updates", e)
        }

        Log.d("LocationService", "Location tracking stopped")
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Duper Location",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Location tracking notifications"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Duper Locate")
            .setContentText("Tracking location...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "duper_location"
    }
}
