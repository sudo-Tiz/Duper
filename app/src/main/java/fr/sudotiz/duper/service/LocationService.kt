package fr.sudotiz.duper.service

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import fr.sudotiz.duper.DuperApplication
import fr.sudotiz.duper.R
import fr.sudotiz.duper.util.SmsUtil

class LocationService : DuperForegroundService(), LocationListener {

    private var locationManager: LocationManager? = null
    private var senderPhoneNumber: String? = null
    private var isTracking = false
    private var lastLocation: Location? = null

    private val prefs by lazy { (applicationContext as DuperApplication).preferencesRepository }

    override val isActive: Boolean get() = isTracking
    override fun onDeviceUnlocked() {
        Log.d(TAG, "Device is unlocked, stopping tracking")
        stopTracking()
    }

    override val notifChannelId = "duper_location"
    override val notifChannelName = R.string.notification_channel_location_name
    override val notifChannelDescription = R.string.notification_channel_location_description
    override val notifChannelImportance = NotificationManager.IMPORTANCE_LOW
    override val notifId = 2
    override val notifTitle = R.string.notification_location_title
    override val notifText = R.string.notification_location_text
    override val notifIcon = android.R.drawable.ic_menu_mylocation
    override val notifPriority = NotificationCompat.PRIORITY_LOW

    private val sendLocationRunnable = object : Runnable {
        override fun run() {
            if (isTracking) {
                lastLocation?.let { sendLocationSms(it) }
                handler.postDelayed(this, prefs.locateInterval * 1000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOCATE -> {
                senderPhoneNumber = intent.getStringExtra(EXTRA_SENDER)
                buildAndStartForeground()
                startTracking()
            }
            ACTION_STOP_LOCATE -> {
                Log.d(TAG, "Stop command received")
                stopTracking()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTracking() {
        if (isTracking) return
        isTracking = true

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permission not granted")
            SmsUtil.send(this, senderPhoneNumber, getString(R.string.sms_location_permission_denied))
            stopTracking()
            return
        }

        val gpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        val networkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false

        if (!gpsEnabled && !networkEnabled) {
            Log.d(TAG, "Location is off, attempting to enable it")
            SmsUtil.send(this, senderPhoneNumber, getString(R.string.sms_location_enabling))
            Thread {
                tryEnableLocation()
                handler.post { registerUpdatesOrFail() }
            }.start()
        } else {
            registerUpdatesOrFail()
        }
    }

    private fun registerUpdatesOrFail() {
        if (!isTracking) return

        val gpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        val networkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false

        if (!gpsEnabled && !networkEnabled) {
            Log.e(TAG, "Location services are disabled and could not be enabled")
            SmsUtil.send(this, senderPhoneNumber, getString(R.string.sms_location_disabled))
            stopTracking()
            return
        }

        val providers = buildString {
            if (gpsEnabled) append("gps")
            if (gpsEnabled && networkEnabled) append("+")
            if (networkEnabled) append("network")
        }
        SmsUtil.send(this, senderPhoneNumber, getString(R.string.sms_location_started, providers))

        try {
            if (gpsEnabled) {
                try {
                    locationManager?.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, GPS_UPDATE_INTERVAL_MS, 0f, this
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error requesting GPS updates", e)
                }
            }
            if (networkEnabled) {
                try {
                    locationManager?.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, GPS_UPDATE_INTERVAL_MS, 0f, this
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error requesting network updates", e)
                }
            }

            lastLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            handler.post(sendLocationRunnable)
            startUnlockCheck()
            handler.postDelayed({ stopTracking() }, prefs.locateDuration * 1000L)

            Log.d(TAG, "Location tracking started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception", e)
            stopTracking()
        }
    }

    private fun tryEnableLocation(): Boolean {
        // Strategy 1: LocationManager.setLocationEnabledForUser (API 28+ with WRITE_SECURE_SETTINGS)
        try {
            val method = LocationManager::class.java.getMethod(
                "setLocationEnabledForUser",
                Boolean::class.javaPrimitiveType,
                UserHandle::class.java
            )
            method.invoke(locationManager, true, Process.myUserHandle())
            Thread.sleep(800)
            if (isAnyProviderEnabled()) {
                Log.d(TAG, "Location enabled via setLocationEnabledForUser")
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "setLocationEnabledForUser failed: ${e.message}")
        }

        // Strategy 2: Write LOCATION_MODE secure setting (older versions)
        try {
            @Suppress("DEPRECATION")
            Settings.Secure.putInt(
                contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
            )
            Thread.sleep(800)
            if (isAnyProviderEnabled()) {
                Log.d(TAG, "Location enabled via LOCATION_MODE setting")
                return true
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "WRITE_SECURE_SETTINGS not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write LOCATION_MODE", e)
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
        prefs.recordLocation(location.latitude, location.longitude)
    }

    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "Location update received (${location.provider})")
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
        val isSameProvider = candidate.provider == current.provider
        return when {
            accuracyDelta < 0 -> true
            isNewer && accuracyDelta <= 0 -> true
            isNewer && accuracyDelta <= 200 && isSameProvider -> true
            else -> false
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) { Log.d(TAG, "Provider enabled: $provider") }
    override fun onProviderDisabled(provider: String) { Log.d(TAG, "Provider disabled: $provider") }

    private fun stopTracking() {
        isTracking = false
        handler.removeCallbacks(sendLocationRunnable)
        stopUnlockCheck()
        try {
            locationManager?.removeUpdates(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing location updates", e)
        }
        Log.d(TAG, "Location tracking stopped")
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }

    companion object {
        private const val TAG = "LocationService"
        private const val GPS_UPDATE_INTERVAL_MS = 5000L

        const val ACTION_START_LOCATE = "fr.sudotiz.duper.START_LOCATE"
        const val ACTION_STOP_LOCATE = "fr.sudotiz.duper.STOP_LOCATE"
        const val EXTRA_SENDER = "sender"
    }
}
