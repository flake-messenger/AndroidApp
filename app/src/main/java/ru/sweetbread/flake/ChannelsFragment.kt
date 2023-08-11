package ru.sweetbread.flake

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import splitties.views.onClick


class ChannelsFragment : Fragment() {
    private var hiearchy = mutableListOf<JSONObject>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var serverId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_channels, container, false)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as AppCompatActivity).supportActionBar!!.apply {
            setHomeAsUpIndicator(R.drawable.arrow_back)
            setDisplayHomeAsUpEnabled(true)
        }

        activity?.findViewById<MaterialToolbar>(R.id.toolbar)?.setNavigationOnClickListener {
            if (parentFragmentManager.backStackEntryCount == 1) {
                activity?.title = "Flake"
                (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            } else {
                if (onePanelMode) ConnectionManager.detach("message")
            }
            parentFragmentManager.popBackStack()
        }

        serverId = requireArguments().getString("serverId")!!

        GlobalScope.launch(Dispatchers.Default) {
            val request =
                client.get("$baseurl/dev/servers/$serverId")
                { headers { bearerAuth(token) } }
            if (request.status == HttpStatusCode.OK) {
                val json = JSONObject(request.bodyAsText())
                val serverName = json.getString("name")
                activity?.runOnUiThread { activity?.title = serverName }
            }
        }

        recyclerView = view.findViewById(R.id.categories_list)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter =
            CategoriesRecyclerAdapter(
                hiearchy,
                activity?.supportFragmentManager!!,
                view.findNavController()
            )
        getHierarchy(serverId)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun getHierarchy(serverId: String) {
        lateinit var channels: MutableList<JSONObject>
        lateinit var categories: MutableList<JSONObject>

        val channelsCon = GlobalScope.launch(Dispatchers.Default) {
            val request =
                client.get("$baseurl/dev/servers/$serverId/channels")
                { headers { bearerAuth(token) } }
            if (request.status == HttpStatusCode.OK) {
                channels = JSONArray(request.bodyAsText()).toArrayList().toMutableList()
                    .filter{ it.getString("category_id") == "null" }.toMutableList()
            }
        }

        val categoriesCon = GlobalScope.launch(Dispatchers.Default) {
            val request =
                client.get("$baseurl/dev/servers/$serverId/categories")
                { headers { bearerAuth(token) } }
            if (request.status == HttpStatusCode.OK) {
                categories = JSONArray(request.bodyAsText()).toArrayList().toMutableList()
            }
        }

        GlobalScope.launch(Dispatchers.Default) {
            hiearchy.clear()

            while (!channelsCon.isCompleted) { delay(50) }
            channels.forEach {
                hiearchy.add(
                    JSONObject()
                        .put("type", "channel")
                        .put("channel", it)
                )
            }

            while (!categoriesCon.isCompleted) { delay(50) }
            categories.forEach {
                hiearchy.add(
                    JSONObject()
                        .put("type", "category")
                        .put("category", it)
                )
            }

            activity?.runOnUiThread {
                recyclerView.adapter =
                    activity?.let {
                        CategoriesRecyclerAdapter(
                            hiearchy, it.supportFragmentManager, view?.findNavController()!!
                        )
                    }
                recyclerView.adapter?.notifyItemRangeInserted(0, hiearchy.size)
            }
        }
    }

    class CategoriesRecyclerAdapter(
        private val hierarchy: MutableList<JSONObject>,
        private val fragmentManager: FragmentManager,
        private val navController: NavController
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        class ChannelHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameView: TextView = itemView.findViewById(R.id.channelName)
        }

        class CategoryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val categoryNameView: TextView = itemView.findViewById(R.id.category_name_view)
            val channelsList: LinearLayout = itemView.findViewById(R.id.channels_list)
            val context = itemView.context!!
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                0 -> {
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.recyclerview_channel, parent, false)

                    ChannelHolder(itemView)
                }

                1 -> {
                    val itemView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.recyclerview_category, parent, false)

                    CategoryHolder(itemView)
                }

                else -> {throw IndexOutOfBoundsException("Type unknown: $viewType")}
            }
        }

        override fun getItemViewType(position: Int): Int {
            return when (hierarchy[position].getString("type")) {
                "channel" -> 0
                "category" -> 1
                else -> -1
            }
        }

        @SuppressLint("InflateParams")
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            fun clickListener(channel: JSONObject) {
                val args = Bundle().apply { putString("channelId", channel.getString("id")) }

                if (!onePanelMode) {
                    fragmentManager
                        .beginTransaction()
                        .setCustomAnimations(R.anim.from_down, R.anim.to_down)
                        .replace(R.id.msgContainer, MessagesFragment().apply{ arguments = args })
                        .commit()
                } else {
                    navController.navigate(
                        R.id.action_channelsFragment_to_messagesFragment,
                        args
                    )
                }
            }

            when (holder.itemViewType) {
                0 -> {
                    val channelHolder = holder as ChannelHolder

                    val channel = hierarchy[position].getJSONObject("channel")
                    channelHolder.nameView.text = channel.getString("name")
                    channelHolder.itemView.onClick { clickListener(channel) }
                }
                1 -> {
                    val categoryHolder = holder as CategoryHolder

                    val category = hierarchy[position].getJSONObject("category")
                    val channels = category.getJSONArray("channels")

                    categoryHolder.categoryNameView.text = category.getString("name")
                    for (i in 0 until channels.length()) {
                        val channel = channels[i] as JSONObject
                        val textView = LayoutInflater.from(categoryHolder.itemView.context)
                            .inflate(R.layout.recyclerview_channel, null)
                            .apply {
                                findViewById<TextView>(R.id.channelName).text = channel.getString("name")
                                onClick {clickListener(channel)}
                            }

                        categoryHolder.channelsList.addView(textView)
                    }
                }
            }
        }

        override fun getItemCount() = hierarchy.size
    }
}

