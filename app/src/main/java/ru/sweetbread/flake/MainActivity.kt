package ru.sweetbread.flake

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.FragmentContainerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

const val baseurl = "https://flake.coders-squad.com/api/v1"
val elements = HashMap<String, View>()
class MainActivity : AppCompatActivity() {
    private var servers = ArrayList<JSONObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.servers_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        servers = getServers()
        recyclerView.adapter = CustomRecyclerAdapter(servers)

        findViewById<FloatingActionButton>(R.id.add_server_fab).setOnClickListener {
            findViewById<FragmentContainerView>(R.id.add_server_panel).visibility = VISIBLE
        }

        GlobalScope.launch(Dispatchers.Default) {
            val client = HttpClient {
                install(HttpTimeout) {
                    socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
                }
            }
            val request = client.prepareGet("$baseurl/dev/sse") {
                headers {
                    append(HttpHeaders.Accept, "text/event-stream")
                    bearerAuth(getSharedPreferences("Account", 0).getString("token", null)!!)
                }
            }
            request.execute {
                val channel = it.bodyAsChannel()
                while (true) {
                    if (channel.availableForRead > 0) {
                        channel.readUTF8Line()
                        val msg = channel.readUTF8Line(Int.MAX_VALUE)!!
                        channel.readUTF8Line()

                        val json = JSONObject(msg.drop(5))

                        when (json.getString("name")) {
                            "SERVER_CREATED", "SERVER_JOINED" -> {
                                servers.add(json.getJSONObject("server"))
                                runOnUiThread {
                                    recyclerView.adapter!!.notifyItemInserted(servers.size)
                                }
                            }

                            "SERVER_DELETED" -> {
                                val id = json.getJSONObject("server").getString("id")
                                runOnUiThread {
                                    servers.forEachIndexed { index, server ->
                                        if (server.getString("id") == id)
                                            recyclerView.adapter!!.notifyItemRemoved(index)
                                    }
                                    servers.removeIf { server -> server.getString("id") == id }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getServers(): ArrayList<JSONObject> {
        var servers = ArrayList<JSONObject>()
        val token = getSharedPreferences("Account", 0).getString("token", null)!!

        runBlocking {
            val client = HttpClient()
            val response =
                client.get("$baseurl/dev/servers")
                { headers { bearerAuth(token) } }
            if (response.status == HttpStatusCode.OK) {
                servers = JSONArray(response.bodyAsText()).toArrayList()
            }
        }

        return servers
    }
}

class CustomRecyclerAdapter(private val servers: ArrayList<JSONObject>) :
    RecyclerView.Adapter<CustomRecyclerAdapter.MyViewHolder>() {

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameView: TextView = itemView.findViewById(R.id.username_view)
        val descriptionView: TextView = itemView.findViewById(R.id.description_view)
        val context = itemView.context!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_servers, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val server = servers[position]
        elements[server.getString("id")] = holder.itemView

        holder.usernameView.text = server.getString("name")
        holder.descriptionView.text = server.getString("description")

        holder.itemView.setOnClickListener {
            val i = Intent(holder.context, ServerActivity::class.java)
            i.putExtra("server_id", server.getString("id"))
            startActivity(holder.context, i, null)
        }
    }

    override fun getItemCount() = servers.size
}