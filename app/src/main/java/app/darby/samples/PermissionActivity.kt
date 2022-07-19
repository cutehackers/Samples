package app.darby.samples

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.darby.samples.core.common.*

class PermissionActivity : AppCompatActivity(), RuntimePermissionCallback {

  // private val permission by single
  private val cameraPermissionAware by permissionAware(RC_CAMERA_PERMISSION)
  private val cameraStoragePermissionAware by permissionAware(RC_CAMERA_STORAGE_PERMISSION)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_permission)
    findViewById<View>(R.id.runView).setOnClickListener {
      // Run
      //startCamera()
      startCameraStorage()
    }
    findViewById<View>(R.id.settingsView).setOnClickListener {
      // Settings
      openAppSettings()
    }
  }

  @RuntimePermissionGranted(RC_CAMERA_PERMISSION)
  private fun onCameraPermissionGranted() {
    Log.d("Permission", "onCameraPermissionGranted")
    Toast.makeText(applicationContext, "Camera permission granted", Toast.LENGTH_SHORT)
      .show()

    doStartCamera()
  }

  @RuntimePermissionGranted(RC_CAMERA_STORAGE_PERMISSION)
  private fun onCameraStoragePermissionGranted() {
    Log.d("Permission", "onCameraStoragePermissionGranted")
    Toast.makeText(applicationContext, "Camera and Storage permission granted", Toast.LENGTH_SHORT)
      .show()
  }

  override fun onPartialPermissionGranted(perms: List<String>) {
    Log.d("Permission", "onPartialPermissionGranted: $perms")
  }

  override fun onPartialPermissionDenied(perms: List<String>) {
    Log.d("Permission", "onPartialPermissionDenied: $perms")
  }

  private fun startCamera() {
    cameraPermissionAware.withPermission(Manifest.permission.CAMERA) {
      doStartCamera()
    }
  }

  private fun doStartCamera() {
    Log.d("Permission", "starting camera with permission")
  }

  private fun startCameraStorage() {
    cameraStoragePermissionAware.withPermission(
      Manifest.permission.CAMERA,
      Manifest.permission.READ_EXTERNAL_STORAGE
    ) {
      doStartCameraStorage()
    }
  }

  private fun doStartCameraStorage() {
    Log.d("Permission", "starting camera and camera with permission")
  }

  companion object {
    const val RC_CAMERA_PERMISSION = 1
    const val RC_CAMERA_STORAGE_PERMISSION = 2
  }
}
