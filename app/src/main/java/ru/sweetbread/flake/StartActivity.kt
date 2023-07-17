package ru.sweetbread.flake

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val context = this

        val sharedPref = getSharedPreferences("Account", 0)
        val token = sharedPref.getString("token", null)

        val toast = Toast.makeText(applicationContext, token, Toast.LENGTH_SHORT)
        toast.show()

        if (token == null) {
            startActivity(Intent(context, LoginActivity::class.java))
            finish()
        } else {
            val client = HttpClient()

            GlobalScope.launch(Dispatchers.Main) {
                val response: HttpResponse = client.get(
                    "https://flake.coders-squad.com/api/v1/dev/users/self"
                ){
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }
                if (response.status == HttpStatusCode.OK) {
                    val toast = Toast.makeText(applicationContext, "Ok", Toast.LENGTH_SHORT)
                    toast.show()

//                    startActivity(Intent(context, MainActivity::class.java))
                    finish()
                } else {
                    startActivity(Intent(context, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }
}