package ru.sweetbread.flake

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.rasalexman.kdispatcher.KDispatcher
import com.rasalexman.kdispatcher.subscribe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date


class MessagesFragment : Fragment() {
    private var messages = mutableListOf<JSONObject>()
    private lateinit var channelId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_messages, container, false)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        channelId = requireArguments().getString("channelId")!!

        GlobalScope.launch(Dispatchers.Default) {
            val request =
                client.get("$baseurl/dev/channels/$channelId")
                { headers { bearerAuth(token) } }
            if (request.status == HttpStatusCode.OK) {
                val json = JSONObject(request.bodyAsText())
                val channelName = json.getString("name")
                activity?.runOnUiThread { activity?.title = channelName }
            }
        }

        val mesList = view.findViewById<RecyclerView>(R.id.messages_list)
        mesList.apply {
            layoutManager = LinearLayoutManager(activity)
            (layoutManager as LinearLayoutManager).stackFromEnd = false
            (layoutManager as LinearLayoutManager).reverseLayout = true
            messages = getMessages(channelId)
            adapter = MessagesRecyclerAdapter(messages)
        }

        val sendButton = view.findViewById<Button>(R.id.send_button)
        val messageInput = view.findViewById<TextInputEditText>(R.id.message_input)
        messageInput.doAfterTextChanged { sendButton.isEnabled = it!!.isNotEmpty() }

        sendButton.apply {
            setOnClickListener {
                val messageText = messageInput.text
                val message = JSONObject()
                    .put("author", self)
                    .put("content", messageText)
                    .put("sent_at", System.currentTimeMillis()/2)
                    .put("id", "0")
                messages.add(0, message)
                mesList.adapter!!.notifyItemInserted(0)
                messageInput.setText("")

                GlobalScope.launch(Dispatchers.Default) {
                    val json = JSONObject()
                        .put("content", messageText)
                    val request = client
                        .post("$baseurl/dev/channels/$channelId/messages")
                        {
                            headers { bearerAuth(token) }
                            setBody(json.toString())
                            contentType(ContentType.Application.Json)
                        }
                    if (request.status == HttpStatusCode.OK) {
                        activity?.runOnUiThread {
                            val pos = messages.indexOf(message)
                            messages.remove(message)
                            mesList.adapter!!.notifyItemRemoved(pos)
                        }
                    } else {
                        activity?.runOnUiThread {
                            val pos = messages.indexOf(message)
                            message.put("id", "-1")
                            mesList.adapter!!.notifyItemChanged(pos)
                        }
                    }
                }
            }
        }

        KDispatcher.subscribe<JSONObject>("MESSAGE_CREATED") {
            val json = it.data!!
            if (json.getJSONObject("message").getString("channelId") == channelId) {
                messages.add(0, json.getJSONObject("message"))
                activity?.runOnUiThread {
                    mesList.adapter!!.notifyItemInserted(0)
                    mesList.smoothScrollToPosition(0)
                }
            }
        }

        KDispatcher.subscribe<JSONObject>("MESSAGE_DELETED") {
            val json = it.data!!
            if (json.getJSONObject("message").getString("channelId") == channelId) {
                val id = json.getJSONObject("message").getString("id")
                activity?.runOnUiThread {
                    messages.forEachIndexed { index, msg ->
                        if (msg.getString("id") == id)
                            mesList.adapter!!.notifyItemRemoved(index)
                    }
                    messages.removeIf { msg -> msg.getString("id") == id }
                }
            }
        }
    }

    private fun getMessages(channelId: String): MutableList<JSONObject> {
        var messages = mutableListOf<JSONObject>()

        runBlocking {
            val request =
                client.get("$baseurl/dev/channels/$channelId/messages")
                {headers { bearerAuth(token) }}
            if (request.status == HttpStatusCode.OK) {
                messages = JSONArray(request.bodyAsText()).toArrayList().toMutableList()
            }
        }

        return messages
    }
}

fun JSONArray.toArrayList(): MutableList<JSONObject> {
    val list = mutableListOf<JSONObject>()
    for (i in 0 until this.length()) {
        list.add(this.getJSONObject(i))
    }

    return list
}

class MessagesRecyclerAdapter(private val messages: MutableList<JSONObject>) : RecyclerView.Adapter<MessagesRecyclerAdapter.MyViewHolder>() {

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeView: TextView = itemView.findViewById(R.id.time_view)
        val nicknameView: TextView = itemView.findViewById(R.id.author_name_view)
        val messageView: TextView = itemView.findViewById(R.id.message_view)
        val context = itemView.context!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = when (viewType) {
            -1 -> R.layout.recyclerview_error_message
            0 -> R.layout.recyclerview_my_message
            1 -> R.layout.recyclerview_processing_message
            else -> R.layout.recyclerview_message
        }
        val itemView = LayoutInflater.from(parent.context)
            .inflate(view, parent, false)
        return MyViewHolder(itemView)
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        when (message.getString("id")) {
            "-1" -> return -1
            "0" -> return 1
            else -> {
                if (message.getJSONObject("author").getString("id") == self.getString("id"))
                    return 0
                return 2
            }
        }
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val message = messages[position]
        val date = SimpleDateFormat.getTimeInstance()
            .format(Date(message.getLong("sent_at")))
        holder.timeView.text = date
        holder.nicknameView.text = message.getJSONObject("author").getString("username")
        holder.messageView.text = message.getString("content")
        holder.itemView.setOnLongClickListener {
            //Log.d("Meow", message.getString("id"))
            true
        }
    }

}
