package ru.sweetbread.flake

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputEditText
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

        val loginView = findViewById<TextInputEditText>(R.id.loginView)
        val passwordView = findViewById<TextInputEditText>(R.id.passwordView)
        val button = findViewById<Button>(R.id.confirmBtn)

        fun validate() {
            button.isEnabled = (loginView.text!!.length in 5..20) and
                    (loginView.text!!.contains(Regex("[0-9a-z._-]+"))) and (passwordView.text!!.length in 4..30)
        }

        loginView.doAfterTextChanged { validate() }
        passwordView.doAfterTextChanged { validate() }
    }

    fun applyLogin(button: View) {
        val login = findViewById<TextView>(R.id.loginView).text!!
        val password = findViewById<TextView>(R.id.passwordView).text!!

        runBlocking {
            val response =
                client.post("$baseurl/auth/login") {
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

    fun toSignup(btn: View) {
        startActivity(Intent(btn.context, SignupActivity::class.java))
        finish()
    }
}