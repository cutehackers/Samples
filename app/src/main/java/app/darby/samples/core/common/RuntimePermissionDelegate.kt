package app.darby.samples.core.common

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import java.lang.reflect.InvocationTargetException
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class RuntimePermissionDelegate :
  ReadOnlyProperty<ComponentActivity, RuntimePermissionDelegate> {

  abstract fun withPermission(vararg permissions: String, callback: ActionCallback)

  /**
   * Determine if the project is using the AndroidAnnotations library.
   */
  protected fun isUsingAndroidAnnotations(host: Any): Boolean {
    return if (!host.javaClass.simpleName.endsWith("_")) {
      false
    } else try {
      val clazz = Class.forName("org.androidannotations.api.view.HasViews")
      clazz.isInstance(host)
    } catch (e: ClassNotFoundException) {
      false
    }
  }

  fun interface ActionCallback {
    fun onAction()
  }
}


fun ComponentActivity.permissionAware(
  requestCode: Int
) = RealRuntimePermissionDelegate(this, requestCode)

class RealRuntimePermissionDelegate(
  private val activity: ComponentActivity,
  private val requestCode: Int
) : RuntimePermissionDelegate() {

  override fun getValue(
    thisRef: ComponentActivity,
    property: KProperty<*>
  ): RuntimePermissionDelegate {
    return this
  }

  private val runtimePermissionLauncher = activity.registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { grantStateMap: Map<String, Boolean> ->
    onRequestPermissionResult(requestCode, grantStateMap, activity)
  }

  /**
   * @param permissions that an app wnat to request
   * @param callback will be called when the app has all [permissions]
   */
  override fun withPermission(vararg permissions: String, callback: ActionCallback) {
    if (permissions.isEmpty()) return

    when {
      hasPermissions(*permissions) -> {
        // You can use the API that requires the permission.
        callback.onAction()
      }
      ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions.first()) -> {
        // In an educational UI, explain to the user why your app requires this
        // permission for a specific feature to behave as expected. In this UI,
        // include a "cancel" or "no thanks" button that allows the user to
        // continue using your app without granting the permission.
        //showRequestPermissionRationale(...)
      }
      else -> {
        // You can directly ask for the permission.
        // The registered ActivityResultCallback gets the result of this request.
        runtimePermissionLauncher.launch(permissions)
      }
    }
  }

  @Suppress("MemberVisibilityCanBePrivate")
  fun hasPermissions(vararg permissions: String): Boolean {
    return if (permissions.size == 1) {
      ActivityCompat.checkSelfPermission(activity, permissions.single()) ==
          PackageManager.PERMISSION_GRANTED
    } else {
      permissions.asSequence()
        .map { ActivityCompat.checkSelfPermission(activity, it) }
        .filter { it != PackageManager.PERMISSION_GRANTED }
        .toList()
        .isEmpty()
    }
  }

  private fun onRequestPermissionResult(
    requestCode: Int,
    grantStateMap: Map<String, Boolean>,
    vararg receivers: Any
  ) {
    // Make a collection of granted and denied permissions from the request.
    val granted: MutableList<String> = ArrayList()
    val denied: MutableList<String> = ArrayList()

    grantStateMap.forEach { (permission, isGranted) ->
      if (isGranted) {
        granted.add(permission)
      } else {
        denied.add(permission)
      }
    }

    // iterate through all receivers
    for (host in receivers) {
      // Report granted permissions, if any.
      if (granted.isNotEmpty()) {
        if (host is RuntimePermissionCallback) {
          host.onPartialPermissionGranted(granted)
        }
      }

      // Report denied permissions, if any.
      if (denied.isNotEmpty()) {
        if (host is RuntimePermissionCallback) {
          host.onPartialPermissionDenied(denied)
        }
      }

      // If 100% successful, call annotated methods
      if (granted.isNotEmpty() && denied.isEmpty()) {
        runAnnotatedRuntimePermissionGranted(host, requestCode)
      }
    }
  }

  /**
   * Find all methods annotated with [RuntimePermissionGranted] on a given object with the
   * correct requestCode argument. AfterPermissionAllGranted
   *
   * @param host the object with annotated methods.
   */

  private fun runAnnotatedRuntimePermissionGranted(host: Any, requestCode: Int) {
    var clazz: Class<*>? = host.javaClass
    if (isUsingAndroidAnnotations(host)) {
      clazz = clazz!!.superclass
    }
    while (clazz != null) {
      for (method in clazz.declaredMethods) {
        val ann = method.getAnnotation(RuntimePermissionGranted::class.java)
        if (ann != null) {
          // Check for annotated methods with matching request code.
          if (ann.value == requestCode) {
            // Method must be void so that we can invoke it
            if (method.parameterTypes.isNotEmpty()) {
              throw RuntimeException("Cannot execute method " + method.name.toString() + " because it is non-void method and/or has input parameters.")
            }
            try {
              // Make method accessible if private
              if (!method.isAccessible) {
                method.isAccessible = true
              }
              method.invoke(host)
            } catch (e: IllegalAccessException) {
              //Timber.e("runDefaultMethod:IllegalAccessException", e)
            } catch (e: InvocationTargetException) {
              //Timber.e("runDefaultMethod:InvocationTargetException", e)
            }
          }
        }
      }
      clazz = clazz.superclass
    }
  }
}

fun Activity.openAppSettings() {
  startActivity(
    Intent(
      Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
      Uri.fromParts("package", packageName, null)
    )
  )
}