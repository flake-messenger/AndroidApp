package ru.sweetbread.flake

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    fun applyLogin(button: View) {
        val login = findViewById<TextView>(R.id.login).text!!
        val password = findViewById<TextView>(R.id.password).text!!

        val client = HttpClient()
        runBlocking {
            val response =
                client.post("$baseurl/web/authorization/login") {
                    setBody(
                        JSONObject()
                            .put("login", login)
                            .put("password", password)
                            .toString()
                    )
                    contentType(ContentType.Application.Json)
                }

            if (response.status == HttpStatusCode.OK) {
                val json = JSONObject(response.bodyAsText())
                val token = json.getString("token")
                Toast.makeText(this@LoginActivity, R.string.success, Toast.LENGTH_SHORT).show()

                val editor = getSharedPreferences("Account", 0).edit()
                editor.putString("token", token)
                editor.apply()

                startActivity(Intent(button.context, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(
                    this@LoginActivity,
                    "${resources.getString(R.string.error)}: ${response.bodyAsText()}",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
                startActivity(intent)
            }
        }
    }
}