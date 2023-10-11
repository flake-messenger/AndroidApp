package ru.sweetbread.flake

import android.content.res.Resources.getSystem
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import com.google.android.material.appbar.MaterialToolbar
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.Job


const val baseurl = "https://flake.coders-squad.com/api/v1"
lateinit var token: String
val client = HttpClient {
    install(HttpTimeout) {
        socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
    }
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 5)
        exponentialDelay()
        modifyRequest { request ->
            request.headers.append("x-retry-count", retryCount.toString())
        }
    }
}

val elements = HashMap<String, View>()
var onePanelMode: Boolean = true

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        token = getSharedPreferences("Account", 0).getString("token", null)!!

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels / getSystem().displayMetrics.density

        onePanelMode = (width < 600)
        if (onePanelMode) {findViewById<FragmentContainerView>(R.id.msgContainer).visibility = View.GONE}
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when (supportFragmentManager.backStackEntryCount) {
            0 -> super.onBackPressed()
            1 -> {
                title = "Flake"
                supportActionBar!!.setDisplayHomeAsUpEnabled(false)
                supportFragmentManager.popBackStack()
            }
            else -> supportFragmentManager.popBackStack()
        }
    }
}

class ConnectionManager {
    companion object {
        private val connections = hashMapOf<String, Job>()

        fun attach(key: String, connection: Job) {
            connections[key]?.cancel()
            connections[key] = connection
        }

        fun detach(key: String) {
            connections[key]?.cancel()
        }
    }
}