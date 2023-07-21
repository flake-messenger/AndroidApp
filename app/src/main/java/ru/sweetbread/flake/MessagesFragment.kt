package ru.sweetbread.flake

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date


class MessagesFragment : Fragment() {
    private var channelId = ""
    private var messages = java.util.ArrayList<JSONObject>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (arguments != null) {
            if (requireArguments().getString("channel_id") != null) {
                channelId = requireArguments().getString("channel_id")!!
            }
        }
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (channelId != "") {
            val token = requireActivity().getSharedPreferences("Account", 0).getString("token", null)!!
            val client = HttpClient()

            view.findViewById<RecyclerView>(R.id.messages_list).apply {
                layoutManager = LinearLayoutManager(activity)
                (layoutManager as LinearLayoutManager).stackFromEnd = false
                (layoutManager as LinearLayoutManager).reverseLayout = true
                messages = getMessages(channelId)
                adapter = MessagesRecyclerAdapter(messages)
            }

            val sendButton = view.findViewById<Button>(R.id.send_button)
            val messageInput = view.findViewById<EditText>(R.id.message_input)
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
                            messages.add(0, JSONObject(request.bodyAsText()))
                            messageInput.setText("")
                            view.findViewById<RecyclerView>(R.id.messages_list).apply {
                                adapter!!.notifyItemInserted(0)
                                smoothScrollToPosition(0)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getMessages(channelId: String): ArrayList<JSONObject> {
        val token = requireActivity().getSharedPreferences("Account", 0).getString("token", null)!!
        var messages = ArrayList<JSONObject>()

        runBlocking {
            val client = HttpClient()
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
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_message, parent, false)
        return MyViewHolder(itemView)
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
