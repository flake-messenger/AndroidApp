package ru.sweetbread.flake

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.util.InternalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    @OptIn(InternalAPI::class)
    fun applyLogin(button: View) {
        val login = findViewById<TextView>(R.id.login).text.toString()
        val password = findViewById<TextView>(R.id.password).text.toString()
        Log.d("Meow", "\"login\": \"$login\", \"password\", \"$password\"")

        val client = HttpClient()
        GlobalScope.launch(Dispatchers.Main) {
            val response =
                client.post("https://flake.coders-squad.com/api/v1/web/authorization/token") {
                    body = TextContent("{\"login\": \"$login\", \"password\": \"$password\"}", ContentType.Application.Json)
                }

            val data = response.bodyAsText()
            if (response.status == HttpStatusCode.OK) {
                val editor = getSharedPreferences("Account", 0).edit()
                editor.putString("token", data)
                editor.apply()

                startActivity(Intent(button.context, StartActivity::class.java))
                finish()
            } else {
                finish();
                startActivity(intent);
            }
        }
    }
}