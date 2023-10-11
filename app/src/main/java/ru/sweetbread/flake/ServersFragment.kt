package ru.sweetbread.flake

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
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
import com.rasalexman.kdispatcher.KDispatcher
import com.rasalexman.kdispatcher.subscribe
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = "Flake"
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        val recyclerView: RecyclerView = view.findViewById(R.id.servers_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext().applicationContext)
        servers = getServers()

        view.findViewById<FloatingActionButton>(R.id.add_server_fab).setOnClickListener {
            view.findNavController().navigate(R.id.action_serversFragment_to_addServerFragment)
        }

        recyclerView.adapter = ServersRecyclerAdapter(servers, view.findNavController())

        KDispatcher.subscribe<JSONObject>("SERVER_CREATED") {
            val json = it.data!!
            servers.add(json.getJSONObject("server"))
            recyclerView.adapter!!.notifyItemInserted(servers.size)
        }
        KDispatcher.subscribe<JSONObject>("SERVER_JOINED") {
            val json = it.data!!
            servers.add(json.getJSONObject("server"))
            activity?.runOnUiThread {
                recyclerView.adapter!!.notifyItemInserted(servers.size)
            }
        }

        KDispatcher.subscribe<JSONObject>("SERVER_DELETED") {
            val json = it.data!!
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
                .transform(RoundedTransformation(150, 0))
                .into(holder.avatarView)

            holder.itemView.setOnClickListener {
                navController.navigate(
                    R.id.action_serversFragment_to_channelsFragment,
                    Bundle().apply { putString("serverId", server.getString("id")) }
                )
            }
        }

        override fun getItemCount() = servers.size
    }

    class RoundedTransformation(private val radius: Int, private val margin: Int) : Transformation {
        override fun transform(source: Bitmap): Bitmap {
            val paint = Paint()
            paint.isAntiAlias = true
            paint.shader = BitmapShader(
                source, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
            )
            val output = Bitmap.createBitmap(
                source.width, source.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(output)
            canvas.drawRoundRect(
                RectF(
                    margin.toFloat(), margin.toFloat(), (source.width - margin).toFloat(),
                    (
                            source.height - margin).toFloat()
                ), radius.toFloat(), radius.toFloat(), paint
            )
            if (source != output) {
                source.recycle()
            }
            return output
        }

        override fun key(): String {
            return "rounded(r=$radius, m=$margin)"
        }
    }
}
