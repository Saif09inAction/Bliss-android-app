package com.laiza.worker.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionHelper: PermissionHelper
) {
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Attempts to fetch the last known location or request current precise location.
     * Returns null if permission is denied or location is unavailable.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!permissionHelper.isLocationPermissionGranted()) {
            return null
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        val isGpsEnabled = lm?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true
        val isNetworkEnabled = lm?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
        if (!isGpsEnabled && !isNetworkEnabled) {
            return null
        }

        fun fallbackSystemLocation(): Location? {
            return try {
                val gps = lm?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                val net = lm?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                val pas = lm?.getLastKnownLocation(android.location.LocationManager.PASSIVE_PROVIDER)
                gps ?: net ?: pas
            } catch (_: SecurityException) {
                null
            }
        }

        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    if (continuation.isActive) continuation.resume(location)
                } else {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { loc2: Location? ->
                        if (loc2 != null) {
                            if (continuation.isActive) continuation.resume(loc2)
                        } else {
                            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                                if (continuation.isActive) continuation.resume(lastLoc ?: fallbackSystemLocation())
                            }.addOnFailureListener {
                                if (continuation.isActive) continuation.resume(fallbackSystemLocation())
                            }
                        }
                    }.addOnFailureListener {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (continuation.isActive) continuation.resume(lastLoc ?: fallbackSystemLocation())
                        }.addOnFailureListener {
                            if (continuation.isActive) continuation.resume(fallbackSystemLocation())
                        }
                    }
                }
            }.addOnFailureListener {
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    if (continuation.isActive) continuation.resume(lastLoc ?: fallbackSystemLocation())
                }.addOnFailureListener {
                    if (continuation.isActive) continuation.resume(fallbackSystemLocation())
                }
            }

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }

    /**
     * Resolves the coordinate into a user-friendly physical address string.
     */
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? = 
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.getAddressLine(0)
            } catch (e: Exception) {
                null
            }
        }
}
