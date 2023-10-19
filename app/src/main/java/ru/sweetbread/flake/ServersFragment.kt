package ru.sweetbread.flake

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import coil.compose.AsyncImage
import com.rasalexman.kdispatcher.KDispatcher
import com.rasalexman.kdispatcher.subscribe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import ru.sweetbread.flake.ui.theme.FlakeTheme


class ServersFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                FlakeTheme {
                    Surface {
                        Main(
                            { getServers() },
                            { id: String ->
                                view?.findNavController()?.navigate(
                                    R.id.action_serversFragment_to_channelsFragment,
                                    Bundle().apply { putString("serverId", id) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.title = "Flake"
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)
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
}

@Composable
private fun Main(getServers: () -> MutableList<JSONObject>, navigate: (String) -> Unit) {
    val servers = remember { mutableStateListOf<JSONObject>()}
    servers.addAll(getServers())

    KDispatcher.subscribe<JSONObject>("SERVER_CREATED") {
        val json = it.data!!
        servers.add(json.getJSONObject("server"))
    }
    KDispatcher.subscribe<JSONObject>("SERVER_JOINED") {
        val json = it.data!!
        servers.add(json.getJSONObject("server"))
    }

    KDispatcher.subscribe<JSONObject>("SERVER_DELETED") {
        val json = it.data!!
        val id = json.getJSONObject("server").getString("id")
        servers.removeIf { server -> server.getString("id") == id }
    }

    LazyColumn(Modifier.fillMaxSize()) {
        items(servers.size) {
            ServerItem(
                server = servers[it],
                Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { navigate(servers[it].getString("id")) }
            )
        }
    }
}

@Composable
fun ServerItem(server: JSONObject, modifier: Modifier = Modifier) {
    Row(modifier) {
        AsyncImage(
            model = "https://flake.coders-squad.com/api/v1/cdn/servers/${server.getString("id")}",
            contentDescription = null,
            modifier = Modifier
                .padding(16.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(25))
        )
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterVertically)
                .padding(8.dp)
        ) {
            Text(
                fontWeight = FontWeight.Bold,
                text = server.getString("name"),
                color = LocalContentColor.current.copy()
            )
            val desc = server.optString("description", "")
            if (desc != "") Text(desc, maxLines = 1)
        }
    }
}

@Preview
@Composable
fun Preview() {
    ServerItem(JSONObject(
        "{\n" +
            "        \"created_at\": 1689090909090,\n" +
            "        \"description\": \"Server description\",\n" +
            "        \"name\": \"server name\",\n" +
            "    }")
    )
}