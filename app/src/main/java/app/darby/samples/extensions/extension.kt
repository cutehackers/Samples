package app.darby.samples.extensions

import android.app.Activity
import android.widget.Toast
import androidx.annotation.StringRes

fun Activity.toast(text: String, isShortDuration: Boolean = true) {
    Toast.makeText(
        this.applicationContext,
        text,
        if (isShortDuration) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
    ).show()
}

fun Activity.toast(@StringRes textResId: Int, isShortDuration: Boolean = true) {
    Toast.makeText(
        this.applicationContext,
        textResId,
        if (isShortDuration) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
    ).show()
}