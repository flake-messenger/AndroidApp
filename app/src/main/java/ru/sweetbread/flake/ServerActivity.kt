package ru.sweetbread.flake

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.discord.panels.OverlappingPanelsLayout
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class ServerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        findViewById<OverlappingPanelsLayout>(R.id.overlapping_panels)
            .setEndPanelLockState(OverlappingPanelsLayout.LockState.CLOSE)

        GlobalScope.launch(Dispatchers.Default) {
            val client = HttpClient()
            val serverId = intent.extras!!.getString("server_id")
            val token = getSharedPreferences("Account", 0).getString("token", null)!!

            val request =
                client.get("$baseurl/dev/servers/$serverId")
                { headers { bearerAuth(token) } }
            if (request.status == HttpStatusCode.OK) {
                val json = JSONObject(request.bodyAsText())
                runOnUiThread {
                    title = json.getString("name")
                }
            }
        }
    }
}
