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
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
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
import java.text.SimpleDateFormat
import java.util.Date


class MessagesFragment(private val channelId: String) : Fragment() {
    private var messages = java.util.ArrayList<JSONObject>()
    lateinit var sseCon: Job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // |>-*
        runBlocking { client.post("$baseurl/dev/sse/echo") { headers { bearerAuth(token) } } }


        GlobalScope.launch(Dispatchers.Default) {
            val request =
                client.get("$baseurl/dev/channels/$channelId")
                { headers { bearerAuth(token) } }
            if (request.status == HttpStatusCode.OK) {
                val json = JSONObject(request.bodyAsText())
                val channelName = json.getString("name")
                requireActivity().runOnUiThread { requireActivity().title = channelName }
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
                runBlocking {
                    val json = JSONObject()
                        .put("content", messageInput.text)
                    val request = client
                        .post("$baseurl/dev/channels/$channelId/messages")
                        {
                            headers { bearerAuth(token) }
                            setBody(json.toString())
                            contentType(ContentType.Application.Json)
                        }
                    if (request.status == HttpStatusCode.OK) {
                        messageInput.setText("")
                    }
                }
            }
        }

        sseCon = GlobalScope.launch(Dispatchers.Default) {
            val request = client.prepareGet("$baseurl/dev/sse") {
                headers {
                    append(HttpHeaders.Accept, "text/event-stream")
                    bearerAuth(token)
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
                                    "MESSAGE_CREATED" -> {
                                        if (json.getJSONObject("message").getString("channelId") == channelId) {
                                            messages.add(0, json.getJSONObject("message"))
                                            requireActivity().runOnUiThread {
                                                mesList.adapter!!.notifyItemInserted(0)
                                                mesList.smoothScrollToPosition(0)
                                            }
                                        }
                                    }

                                    "MESSAGE_DELETED" -> {
                                        if (json.getJSONObject("message").getString("channelId") == channelId) {
                                            val id = json.getJSONObject("message").getString("id")
                                            requireActivity().runOnUiThread {
                                                messages.forEachIndexed { index, msg ->
                                                    if (msg.getString("id") == id)
                                                        mesList.adapter!!.notifyItemRemoved(index)
                                                }
                                                messages.removeIf { msg -> msg.getString("id") == id }
                                            }
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
    }

    private fun getMessages(channelId: String): ArrayList<JSONObject> {
        var messages = ArrayList<JSONObject>()

        runBlocking {
            val request =
                client.get("$baseurl/dev/channels/$channelId/messages")
                {headers { bearerAuth(token) }}
            if (request.status == HttpStatusCode.OK) {
                messages = JSONArray(request.bodyAsText()).toArrayList()
            }
        }

        return messages
    }
}

fun JSONArray.toArrayList(): ArrayList<JSONObject> {
    val list = arrayListOf<JSONObject>()
    for (i in 0 until this.length()) {
        list.add(this.getJSONObject(i))
    }

    return list
}

class MessagesRecyclerAdapter(private val messages: ArrayList<JSONObject>) : RecyclerView.Adapter<MessagesRecyclerAdapter.MyViewHolder>() {

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeView: TextView = itemView.findViewById(R.id.time_view)
        val nicknameView: TextView = itemView.findViewById(R.id.author_name_view)
        val messageView: TextView = itemView.findViewById(R.id.message_view)
        val context = itemView.context!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = when (viewType) {
            0 -> R.layout.recyclerview_my_message
            else -> R.layout.recyclerview_message
        }
        val itemView = LayoutInflater.from(parent.context)
            .inflate(view, parent, false)
        return MyViewHolder(itemView)
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        if(message.getJSONObject("author").getString("id") == selfId)
            return 0
        return 1
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
