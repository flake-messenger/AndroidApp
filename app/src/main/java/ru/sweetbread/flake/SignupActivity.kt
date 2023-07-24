package ru.sweetbread.flake

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class SignupActivity : AppCompatActivity() {
    lateinit var loginView: EditText
    lateinit var passView: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        loginView = findViewById(R.id.loginView)
        passView = findViewById(R.id.passwordView)
        val confPassView = findViewById<EditText>(R.id.confirmPasswordView)
        val btn = findViewById<Button>(R.id.confirmBtn)

        loginView.doAfterTextChanged {
            btn.isEnabled = false
            if (it!!.length !in 5..20) {
                loginView.error = "Login must be in 5 to 20 symbols"
            } else if (!(it.contains(Regex("[0-9a-z_]+")))) {
                loginView.error = "Login must have only lowercase letters, underscore and numbers"
            } else {
                loginView.error = null
                btn.isEnabled = ((passView.error == null) and (confPassView.error == null)
                        and passView.text.isNotBlank() and confPassView.text.isNotBlank())
            }
        }
        passView.doAfterTextChanged {
            btn.isEnabled = false
            if (it!!.length !in 4..30) {
                passView.error = "Password must be in 4 to 30 symbols"
            } else {
                passView.error = null
                btn.isEnabled = ((loginView.error == null) and (confPassView.error == null)
                        and loginView.text.isNotBlank() and confPassView.text.isNotBlank())
            }
        }
        confPassView.doAfterTextChanged {
            btn.isEnabled = false
            if (it!!.toString() != passView.text.toString()) {
                confPassView.error = "Passwords must be the same"
            } else {
                confPassView.error = null
                btn.isEnabled = ((passView.error == null) and (loginView.error == null)
                        and loginView.text.isNotBlank() and passView.text.isNotBlank())
            }
        }
    }

    fun applySignup(btn: View) {
        runBlocking {
            val client = HttpClient()
            val response = client.post("$baseurl/web/authorization/register") {
                setBody(
                    JSONObject()
                        .put("login", loginView.text.trim().toString())
                        .put("username", loginView.text.trim().toString())
                        .put("password", passView.text.trim().toString())
                        .put("locale", "RU")
                        .toString()
                )
                contentType(ContentType.Application.Json)
            }
            if (response.status == HttpStatusCode.OK) {
                val json = JSONObject(response.bodyAsText())
                val token = json.getString("token")

                val editor = getSharedPreferences("Account", 0).edit()
                editor.putString("token", token)
                editor.apply()

                startActivity(Intent(btn.context, MainActivity::class.java))
                finish()
            } else {
                TODO("Raise errors in dependence of error code in body")
            }
        }
    }

    fun toLogin(btn: View) {
        startActivity(Intent(btn.context, LoginActivity::class.java))
        finish()
    }
}