package ru.sweetbread.flake

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    fun applyLogin(button: View) {
        val login = findViewById<TextView>(R.id.login).text.toString()
        val password = findViewById<TextView>(R.id.password).text.toString()
        Log.d("Meow", "\"login\": \"$login\", \"password\", \"$password\"")

        val client = HttpClient()
        GlobalScope.launch(Dispatchers.Main) {
            val response =
                client.post("https://flake.coders-squad.com/api/v1/web/authorization/token") {
                    setBody("{\"login\": \"$login\", \"password\", \"$password\"}")
                }

            val data = response.bodyAsText()
            val toast = Toast.makeText(applicationContext, data, Toast.LENGTH_SHORT)
            toast.show()
            if (response.status == HttpStatusCode.OK) {
                val editor = getSharedPreferences("Account", 0).edit()
                editor.putString("token", data)
                editor.apply()

                startActivity(Intent(button.context, StartActivity::class.java))
                finish()
            } else {
                val toast = Toast.makeText(applicationContext, "Error!", Toast.LENGTH_SHORT)
                toast.show()

                finish();
                startActivity(intent);
            }
        }
    }
}