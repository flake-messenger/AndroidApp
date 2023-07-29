package ru.sweetbread.flake

import android.content.res.Resources.getSystem
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import com.google.android.material.appbar.MaterialToolbar
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.headers
import io.ktor.client.request.post
import kotlinx.coroutines.runBlocking


const val baseurl = "https://flake.coders-squad.com/api/v1"
lateinit var token: String
val client = HttpClient() {
    install(HttpTimeout) {
        socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
    }
}

val elements = HashMap<String, View>()

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

        if (width < 600) {  // One-panel mode
            findViewById<FragmentContainerView>(R.id.msgContainer).visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        // |>-*
        runBlocking { client.post("$baseurl/dev/sse/echo") { headers { bearerAuth(token) } } }
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