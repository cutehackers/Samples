package app.darby.samples.core.common

import androidx.core.app.ActivityCompat

interface RuntimePermissionCallback : ActivityCompat.OnRequestPermissionsResultCallback {

    fun onPartialPermissionGranted(perms: List<String>)

    fun onPartialPermissionDenied(perms: List<String>)
}
