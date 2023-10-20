package ru.sweetbread.flake

import android.content.res.Resources.getSystem
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rasalexman.kdispatcher.KDispatcher
import com.rasalexman.kdispatcher.call
import com.rasalexman.kdispatcher.subscribe
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.headers
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import ru.sweetbread.flake.ui.theme.FlakeTheme


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

var onePanelMode: Boolean = true

class MainActivity : AppCompatActivity() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FlakeTheme {
                Surface {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "servers") {
                        composable("servers") {
                            val servers = remember { getServers() }

                            KDispatcher.subscribe<JSONObject>("SERVER_CREATED") {
                                val json = it.data!!
                                servers.add(json.getJSONObject("server"))
                            }
                            KDispatcher.subscribe<JSONObject>("SERVER_JOINED") {
                                val json = it.data!!
                                servers.add(json.getJSONObject("server"))
                            }

                            KDispatcher.subscribe<JSONObject>("SERVER_DELETED") {
                                val json = it.data!!
                                val id = json.getJSONObject("server").getString("id")
                                servers.removeIf { server -> server.getString("id") == id }
                            }

                            Servers(navController, servers)
                        }

                        composable("servers/{serverId}/channels") { backStackEntry ->
                            val hierarchy = remember { getHierarchy(backStackEntry.arguments?.getString("serverId")!!) }
                            Channels(navController, hierarchy)
                        }

                        composable("channels/{channelId}/messages") {}
                    }
                }
            }
        }

        token = getSharedPreferences("Account", 0).getString("token", null)!!

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels / getSystem().displayMetrics.density

        onePanelMode = (width < 600)
//        if (onePanelMode) {findViewById<FragmentContainerView>(R.id.msgContainer).visibility = View.GONE}

        GlobalScope.launch(Dispatchers.Default) {
            val request = client.prepareGet("$baseurl/dev/sse") {
                headers {
                    append(HttpHeaders.Accept, "text/event-stream")
                    bearerAuth(token)
                }
            }

            while (true) {
                request.execute {
                    if (it.status != HttpStatusCode.OK) {
                        delay(5000)
                    } else {
                        KDispatcher.call("SSE_STARTED")
                        val channel = it.bodyAsChannel()
                        while (!channel.isClosedForRead) {
                            if (channel.availableForRead > 0) {
                                channel.readUTF8Line()
                                val msg = channel.readUTF8Line(Int.MAX_VALUE)!!
                                channel.readUTF8Line()

                                val json = JSONObject(msg.drop(5))
                                KDispatcher.call(json.getString("name"), json)

                                delay(250)
                            }
                            delay(250)
                        }
                    }
                    return@execute
                }
                KDispatcher.call("SSE_FINISHED")
            }
        }
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