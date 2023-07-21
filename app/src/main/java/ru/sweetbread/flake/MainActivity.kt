package ru.sweetbread.flake

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

const val baseurl = "https://flake.coders-squad.com/api/v1"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.servers_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = CustomRecyclerAdapter(getSerers())
    }

    private fun getSerers(): JSONArray {
        var servers = JSONArray()
        val token = getSharedPreferences("Account", 0).getString("token", null)!!

        runBlocking {
            val client = HttpClient()
            val response =
                client.get("$baseurl/dev/servers")
                { headers { bearerAuth(token) } }
            if (response.status == HttpStatusCode.OK) {
                servers = JSONArray(response.bodyAsText())
            }
        }

        return servers
    }
}

class CustomRecyclerAdapter(private val servers: JSONArray) : RecyclerView.Adapter<CustomRecyclerAdapter.MyViewHolder>() {

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameView: TextView = itemView.findViewById(R.id.username_view)
        val descriptionView: TextView = itemView.findViewById(R.id.description_view)
        val context = itemView.context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_servers, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val server = servers[position] as JSONObject
        holder.usernameView.text = server.getString("name")
        holder.descriptionView.text = server.getString("description")

        holder.itemView.setOnClickListener {
            val i = Intent(holder.context, ServerActivity::class.java)
            i.putExtra("server_id", server.getString("id"))
            startActivity(holder.context, i, null)
        }
    }

    override fun getItemCount() = servers.length()
}