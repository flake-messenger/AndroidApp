package ru.sweetbread.flake

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squareup.picasso.Picasso
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject


class ServersFragment : Fragment() {
    private var servers = mutableListOf<JSONObject>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_servers, container, false)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = "Flake"
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        val recyclerView: RecyclerView = view.findViewById(R.id.servers_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext().applicationContext)
        servers = getServers()

        view.findViewById<FloatingActionButton>(R.id.add_server_fab).setOnClickListener {
            ConnectionManager.detach("server")
            view.findNavController().navigate(R.id.action_serversFragment_to_addServerFragment)
        }

        recyclerView.adapter = ServersRecyclerAdapter(servers, view.findNavController())

        val sseCon = GlobalScope.launch(Dispatchers.Default) {
            val request = client.prepareGet("$baseurl/dev/sse") {
                headers {
                    append(HttpHeaders.Accept, "text/event-stream")
                    bearerAuth(
                        activity?.getSharedPreferences("Account", 0)
                            ?.getString("token", null)!!
                    )
                }
            }
            while (activity != null) {
                request.execute {
                    if (it.status != HttpStatusCode.OK) {
                        delay(5000)
                    } else {
                        val channel = it.bodyAsChannel()
                        while ((activity != null) and (!channel.isClosedForRead)) {
                            if (channel.availableForRead > 0) {
                                channel.readUTF8Line()
                                val msg = channel.readUTF8Line(Int.MAX_VALUE)!!
                                channel.readUTF8Line()

                                val json = JSONObject(msg.drop(5))

                                when (json.getString("name")) {
                                    "SERVER_CREATED", "SERVER_JOINED" -> {
                                        servers.add(json.getJSONObject("server"))
                                        activity?.runOnUiThread {
                                            recyclerView.adapter!!.notifyItemInserted(servers.size)
                                        }
                                    }

                                    "SERVER_DELETED" -> {
                                        val id = json.getJSONObject("server").getString("id")
                                        activity?.runOnUiThread {
                                            servers.forEachIndexed { index, server ->
                                                if (server.getString("id") == id)
                                                    recyclerView.adapter!!.notifyItemRemoved(index)
                                            }
                                            servers.removeIf { server -> server.getString("id") == id }
                                        }
                                    }
                                }
                                delay(500)
                            }
                            delay(100)
                        }
                        channel.cancel()
                    }
                    it.cancel()
                    return@execute
                }
            }
        }
        ConnectionManager.attach("server", sseCon)
    }

    private fun getServers(): MutableList<JSONObject> {
        var servers = mutableListOf<JSONObject>()

        runBlocking {
            val response =
                client.get("$baseurl/dev/servers")
                { headers { bearerAuth(token) } }
            if (response.status == HttpStatusCode.OK) {
                servers = JSONArray(response.bodyAsText()).toArrayList().toMutableList()
            }
        }

        return servers
    }

    class ServersRecyclerAdapter(
        private val servers: MutableList<JSONObject>,
        private val navController: NavController
    ) :
        RecyclerView.Adapter<ServersRecyclerAdapter.MyViewHolder>() {

        class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val usernameView: TextView = itemView.findViewById(R.id.username_view)
            val descriptionView: TextView = itemView.findViewById(R.id.description_view)
            val avatarView: ImageView = itemView.findViewById(R.id.serverAvatarView)
            val context = itemView.context!!
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.recyclerview_server, parent, false)
            return MyViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val server = servers[position]
            elements[server.getString("id")] = holder.itemView

            holder.usernameView.text = server.getString("name")
            holder.descriptionView.text = server.getString("description")

            Picasso.get()
                .load("https://flake.coders-squad.com/api/v1/cdn/servers/${server.getString("id")}")
                .into(holder.avatarView)

            holder.itemView.setOnClickListener {
                ConnectionManager.detach("server")
                navController.navigate(
                    R.id.action_serversFragment_to_channelsFragment,
                    Bundle().apply { putString("serverId", server.getString("id")) }
                )
            }
        }

        override fun getItemCount() = servers.size
    }
}
