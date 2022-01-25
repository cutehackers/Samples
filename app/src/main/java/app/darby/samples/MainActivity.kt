package app.darby.samples

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Use Kotlin coroutines with Architecture components
 * refs: https://medium.com/androiddevelopers/simplifying-apis-with-coroutines-and-flow-a6fb65338765
 * refs: https://developer.android.com/topic/libraries/architecture/coroutines
 * refs: https://medium.com/androiddevelopers/easy-coroutines-in-android-viewmodelscope-25bffb605471
 * refs: https://developer.android.com/codelabs/building-kotlin-extensions-library#5
 *
 * TODO LifecycleService 에서 Location 업데이트 하기
 */
class MainActivity : AppCompatActivity() {

  private lateinit var fusedLocationClient: FusedLocationProviderClient

  private var number: Int = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    initViews()

    // case 1
    sample()

    // case 2
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    withPermission(Manifest.permission.ACCESS_FINE_LOCATION) {
      // one-time
      getLastLocation()

      // periodic location request
      startUpdatingLocation()
    }
  }

  private fun initViews() {
    val navigator = registerForActivityResult(SampleResultContract()) {
      // onActivityResult
      Toast.makeText(applicationContext, "Sample result: $it", Toast.LENGTH_SHORT).show()
      number = it?.toInt() ?: 0
    }

    findViewById<View>(R.id.sample).setOnClickListener {
      navigator.launch(number)
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

  private fun startUpdatingLocation() {
    if (ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
      ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      // TODO: Consider calling
      //    ActivityCompat#requestPermissions
      // here to request the missing permissions, and then overriding
      //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
      //                                          int[] grantResults)
      // to handle the case where the user grants the permission. See the documentation
      // for ActivityCompat#requestPermissions for more details.
      return
    }
    fusedLocationClient.locationFlow()
        .conflate()
        .catch { e ->
          Log.e("MyApp>", "periodic location failed.", e)
        }
        .asLiveData()
        .observe(this) { location ->
          Log.i("MyApp>", "periodic location tick: $location, from callback flow")
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

@ExperimentalCoroutinesApi
@RequiresPermission(anyOf = [
  Manifest.permission.ACCESS_COARSE_LOCATION,
  Manifest.permission.ACCESS_FINE_LOCATION]
)
private fun FusedLocationProviderClient.locationFlow(): Flow<Location> = callbackFlow {
  val callback = object : LocationCallback() {
    override fun onLocationResult(result: LocationResult) {
      result ?: return
      for (location in result.locations) {
        try {
          offer(location)
        } catch (e: Throwable) {
          // Location count' be sent to the flow
        }
      }
    }
  }

  val request = LocationRequest.create().apply {
    interval = 3000
    fastestInterval = 2000
    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
  }

  requestLocationUpdates(request, callback, Looper.getMainLooper())
      .addOnFailureListener {
        // error -> close the flow
        close(it)
      }

  awaitClose {
    removeLocationUpdates(callback)
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

class SampleResultContract : ActivityResultContract<Int, String?>() {

  override fun createIntent(context: Context, input: Int): Intent =
    SampleActivity.createIntent(context, input)

  override fun parseResult(resultCode: Int, intent: Intent?): String? {
    if (resultCode != Activity.RESULT_OK) {
      return null
    }
    return intent?.getIntExtra(SampleActivity.EXTRA_RESULT, -1)?.toString()
  }
}

/**
 * abstract class ViewNavigator<I, O> : ActivityResultContract<I, O>()
 *
 * 1. define activity result contract class
 *
 * class SampleViewNavigator : ViewNavigator<Int, Uri>() {
 *   override fun createIntent(context: Context, input: Int?): Intent {
 *     ...
 *   }
 *
 *   override fun parseResult(resultCode: Int, intent: Intent?): Uri {
 *     ...
 *   }
 * }
 *
 * 2. bind activity result callback with contract
 * navigate(SampleViewNavigator(), 1) { uri: Uri ->
 *   // onActivityResult callback
 *   ...
 * }
 *
 * refs:
 *  https://developer.android.com/training/basics/intents/result#register
 */