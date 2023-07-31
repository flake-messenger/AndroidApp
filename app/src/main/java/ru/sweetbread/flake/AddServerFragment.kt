package ru.sweetbread.flake

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
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

        (activity as AppCompatActivity).supportActionBar!!.apply {
            setHomeAsUpIndicator(R.drawable.arrow_back)
            setDisplayHomeAsUpEnabled(true)
        }

        activity?.findViewById<MaterialToolbar>(R.id.toolbar)
            ?.setNavigationOnClickListener { back() }

        val joinButton = view.findViewById<Button>(R.id.join_to_join_button)
        val linkView = view.findViewById<TextView>(R.id.link_to_join_view)
        linkView.doAfterTextChanged {
            joinButton.isEnabled =
                (it!!.length in 5..20) and (it.contains(Regex("[0-9A-z_]+")))
        }
        joinButton.setOnClickListener {
            runBlocking {
                val request = client.post("$baseurl/dev/servers/join${linkView.text}")
                { headers { bearerAuth(token) } }
                if (request.status == HttpStatusCode.OK) { back() }
            }
        }
    }

    private fun back() {
        if (parentFragmentManager.backStackEntryCount == 1) {
            activity?.title = "Flake"
            (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        }
        parentFragmentManager.popBackStack()
    }
}