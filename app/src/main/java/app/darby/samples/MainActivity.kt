package app.darby.samples

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Use Kotlin coroutines with Architecture components
 * refs: https://medium.com/androiddevelopers/simplifying-apis-with-coroutines-and-flow-a6fb65338765
 * refs: https://developer.android.com/topic/libraries/architecture/coroutines
 * refs: https://medium.com/androiddevelopers/easy-coroutines-in-android-viewmodelscope-25bffb605471
 */
class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // case 1
        sample()

        // case 2
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        withPermission(Manifest.permission.ACCESS_FINE_LOCATION) {
            getLastLocation()
        }
    }

    private fun sample() {
        lifecycleScope.launch {
            var texts = hello()
            texts = coroutine(texts)
            val result = world(texts)
            Log.i("MyApp>", result)
        }
    }

    private suspend fun hello(): String {
        delay(500)
        return "hello"
    }

    private fun coroutine(words: String): String {
        return "$words coroutine"
    }

    private suspend fun world(words: String): String {
        delay(500)
        return "$words world"
    }

    @RequiresPermission(anyOf = [
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    ])
    private fun getLastLocation() {
        // 1. callback
        fusedLocationClient.lastLocation.apply {
            addOnSuccessListener { location: Location? ->
                // Got last known location. In some rare situations this can be null.
                Log.i("MyApp>", "I'm here in: $location, from callback")
            }
            addOnFailureListener {

            }
        }

        // 2. asynchronous job with coroutine
        lifecycleScope.launch {
            try {
                val location = fusedLocationClient.awaitLastLocation()
                Log.i("MyApp>", "I'm here in: $location, from coroutine")

            } catch (e: Exception) {
                Log.e("MyApp>", "error while fetching last location!", e)
            }
        }
    }
}

@RequiresPermission(anyOf = [
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION]
)
private suspend fun FusedLocationProviderClient.awaitLastLocation(): Location =
        suspendCancellableCoroutine<Location> { continuation ->
            lastLocation.addOnSuccessListener { location ->
                // Resume coroutine and return location
                continuation.resume(location)
            }.addOnFailureListener { e ->
                // Resume the coroutine by throwing an exception
                continuation.resumeWithException(e)
            }
        }


// permission --------------------------------------------------------------------------------------
// dependency: implementation 'androidx.fragment:fragment-ktx:1.3.0-rc01'

/**
 * TODO update permission request below with following reference
 * refs: https://developer.android.com/training/basics/intents/result#separate
 *  # Receiving an activity result in a separate class
 */
private fun ComponentActivity.withPermission(permission: String, block: () -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        block()
        return
    }

    when {
        ContextCompat.checkSelfPermission(
                this,
                permission
        ) == PackageManager.PERMISSION_GRANTED -> {
            // You can use the API that requires the permission.
            block()
        }
        shouldShowRequestPermissionRationale(permission) -> {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            //showInContextUI(...)
        }
        else -> {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            //  - ActivityResultContracts.RequestPermission
            //  - ActivityResultContracts.RequestMultiplePermissions

            // permission request launcher
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                // onActivityResult callback
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                    block()
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            }.apply {
                launch(permission)
            }
        }
    }
}
