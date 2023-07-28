package ru.sweetbread.flake

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ChannelsFragment : Fragment() {
    private var categories = ArrayList<JSONObject>()
    lateinit var recyclerView: RecyclerView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_channels, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val serverId = requireActivity().intent.extras!!.getString("server_id")
        recyclerView = view.findViewById(R.id.categories_list)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter =
            CategoriesRecyclerAdapter(categories, parentFragmentManager)
        getCategories(serverId)
    }

    private fun getCategories(serverId: String?) {
        val token = requireActivity().getSharedPreferences("Account", 0).getString("token", null)!!

        GlobalScope.launch(Dispatchers.Default) {
            val client = HttpClient()
            val request =
                client.get("$baseurl/dev/servers/$serverId/categories")
                { headers { bearerAuth(token) } }
            if (request.status == HttpStatusCode.OK) {
                categories = JSONArray(request.bodyAsText()).toArrayList()
                requireActivity().runOnUiThread {
                    recyclerView.adapter =
                        CategoriesRecyclerAdapter(categories, parentFragmentManager)
                    recyclerView.adapter!!.notifyDataSetChanged()
                }
            }
        }
    }
}

class CategoriesRecyclerAdapter(
    private val categories: ArrayList<JSONObject>,
    private val fragmentManager: FragmentManager
) : RecyclerView.Adapter<CategoriesRecyclerAdapter.MyViewHolder>() {

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryNameView: TextView = itemView.findViewById(R.id.category_name_view)
        val channelsList: LinearLayout = itemView.findViewById(R.id.channels_list)
        val context = itemView.context!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_channels, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val scale = holder.context.resources.displayMetrics.density
        val server = categories[position]
        val channels = server.getJSONArray("channels")

        holder.categoryNameView.text = server.getString("name")
        for (i in 0 until channels.length()) {
            val channel = channels[i] as JSONObject
            val textView = TextView(holder.context).apply {
                text = channel.getString("name")
                setPadding(8, 8, 8, 8)
                height = (45 * scale + 0.5f).toInt()
                gravity = Gravity.CENTER_VERTICAL
                setOnClickListener {
                    val bundle = Bundle()
                    bundle.putString("channel_id", channel.getString("id"))
                    val fragment2 = MessagesFragment()
                    fragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerView2, fragment2)
                        .commit()
                    fragment2.arguments = bundle
                }
            }

            holder.channelsList.addView(textView)
        }
    }

    override fun getItemCount() = categories.size
}