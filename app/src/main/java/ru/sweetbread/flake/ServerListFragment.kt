package ru.sweetbread.flake

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject


class ServerListFragment : Fragment() {
    private var servers = ArrayList<JSONObject>()
    private lateinit var sseCoroutine: Job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_server_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().title = "Flake"
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        val recyclerView: RecyclerView = view.findViewById(R.id.servers_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext().applicationContext)
        servers = getServers()

        view.findViewById<FloatingActionButton>(R.id.add_server_fab).setOnClickListener {
            view.findViewById<FragmentContainerView>(R.id.add_server_panel).visibility =
                View.VISIBLE
        }

        sseCoroutine = GlobalScope.launch(Dispatchers.Default) {
            val request = client.prepareGet("$baseurl/dev/sse") {
                headers {
                    append(HttpHeaders.Accept, "text/event-stream")
                    bearerAuth(
                        requireActivity().getSharedPreferences("Account", 0)
                            .getString("token", null)!!
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
                                        requireActivity().runOnUiThread {
                                            recyclerView.adapter!!.notifyItemInserted(servers.size)
                                        }
                                    }

                                    "SERVER_DELETED" -> {
                                        val id = json.getJSONObject("server").getString("id")
                                        requireActivity().runOnUiThread {
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

        recyclerView.adapter = CustomRecyclerAdapter(servers, parentFragmentManager)
    }

    private fun getServers(): ArrayList<JSONObject> {
        var servers = ArrayList<JSONObject>()
        val token = requireActivity().getSharedPreferences("Account", 0).getString("token", null)!!

        runBlocking {
            val response =
                client.get("$baseurl/dev/servers")
                { headers { bearerAuth(token) } }
            if (response.status == HttpStatusCode.OK) {
                servers = JSONArray(response.bodyAsText()).toArrayList()
            }
        }

        return servers
    }

    class CustomRecyclerAdapter(
        private val servers: ArrayList<JSONObject>,
        private val parentFragmentManager: FragmentManager
    ) :
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
                parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.mainContainer, ChannelsFragment(server.getString("id")))
                    .commit()
            }
        }

        override fun getItemCount() = servers.size
    }
}
