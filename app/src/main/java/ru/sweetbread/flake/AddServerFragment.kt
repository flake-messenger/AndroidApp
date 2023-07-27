package ru.sweetbread.flake

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking

class AddServerFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_add_server, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val panel = requireActivity().findViewById<FragmentContainerView>(R.id.add_server_panel)
        panel.setOnClickListener { panel.visibility = GONE }
        val joinButton = view.findViewById<Button>(R.id.join_to_join_button)
        val linkView = view.findViewById<TextView>(R.id.link_to_join_view)
        linkView.doAfterTextChanged {
            joinButton.isEnabled =
                (it!!.length in 5..20) and (it.contains(Regex("[0-9A-z_]+")))
        }
        joinButton.setOnClickListener {
            val token =
                requireActivity().getSharedPreferences("Account", 0).getString("token", null)!!
            val client = HttpClient()
            runBlocking {
                val request = client.post("$baseurl/dev/servers/join${linkView.text}")
                { headers { bearerAuth(token) } }
                if (request.status == HttpStatusCode.OK) {
                    panel.visibility = GONE
                }
            }
        }
    }
}