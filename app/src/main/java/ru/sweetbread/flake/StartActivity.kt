package ru.sweetbread.flake

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import kotlinx.coroutines.runBlocking

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val context = this

        val sharedPref = getSharedPreferences("Account", 0)
        val token = sharedPref.getString("token", null)

        if (token == null) {
            startActivity(Intent(context, LoginActivity::class.java))
            finish()
        } else {
            runBlocking {
                val response: HttpResponse = client.get("$baseurl/dev/users/self")
                { headers { bearerAuth(token) } }
                if (response.status == HttpStatusCode.OK) {
                    startActivity(Intent(context, MainActivity::class.java))
                } else {
                    startActivity(Intent(context, LoginActivity::class.java))
                }
                finish()
            }
        }
    }
}