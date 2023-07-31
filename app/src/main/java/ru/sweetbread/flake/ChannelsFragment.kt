package ru.sweetbread.flake

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
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
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject


class ChannelsFragment : Fragment() {
    private var categories = ArrayList<JSONObject>()
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
                categories,
                activity?.supportFragmentManager!!,
                view.findNavController()
            )
        getCategories(serverId)
    }

    @SuppressLint("NotifyDataSetChanged")
    @OptIn(DelicateCoroutinesApi::class)
    private fun getCategories(serverId: String) {
        GlobalScope.launch(Dispatchers.Default) {
            val request =
                client.get("$baseurl/dev/servers/$serverId/categories")
                { headers { bearerAuth(token) } }
            if (request.status == HttpStatusCode.OK) {
                categories = JSONArray(request.bodyAsText()).toArrayList()
                activity?.runOnUiThread {
                    recyclerView.adapter =
                        activity?.let {
                            CategoriesRecyclerAdapter(
                                categories, it.supportFragmentManager, view?.findNavController()!!
                            )
                        }
                    recyclerView.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    class CategoriesRecyclerAdapter(
        private val categories: ArrayList<JSONObject>,
        private val fragmentManager: FragmentManager,
        private val navController: NavController
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
            val category = categories[position]
            val channels = category.getJSONArray("channels")

            holder.categoryNameView.text = category.getString("name")
            for (i in 0 until channels.length()) {
                val channel = channels[i] as JSONObject
                val textView = TextView(holder.context).apply {
                    text = channel.getString("name")
                    setPadding(8, 8, 8, 8)
                    height = (45 * scale + 0.5f).toInt()
                    gravity = Gravity.CENTER_VERTICAL
                    setOnClickListener {
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
                }

                holder.channelsList.addView(textView)
            }
        }

        override fun getItemCount() = categories.size
    }
}

