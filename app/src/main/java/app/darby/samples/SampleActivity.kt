package app.darby.samples

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView

class SampleActivity : AppCompatActivity() {
    companion object {
        internal const val EXTRA_ARGUMENT = "EXTRA_ARGUMENT"
        internal const val EXTRA_RESULT = "EXTRA_RESULT"

        fun createIntent(context: Context, number: Int) =
            Intent(context, SampleActivity::class.java).apply {
                putExtra(EXTRA_ARGUMENT, number)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        initViews()
    }

    private fun initViews() {
        val number = intent.getIntExtra(EXTRA_ARGUMENT, -1)

        findViewById<TextView>(R.id.title).text = "Sample:$number"

        findViewById<View>(R.id.close).setOnClickListener {
            setResult(RESULT_OK, Intent().apply {
                putExtra(EXTRA_RESULT, number + 1)
            })
            finish()
        }
    }
}