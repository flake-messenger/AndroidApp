package ru.sweetbread.flake

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.json.JSONArray
import org.json.JSONObject


suspend fun getServers(): MutableList<JSONObject> {
    val servers = mutableListOf<JSONObject>()
    val response =
        client.get("$baseurl/dev/servers")
        { headers { bearerAuth(token) } }
    if (response.status == HttpStatusCode.OK) {
        servers.addAll(JSONArray(response.bodyAsText()).toArrayList().toMutableList())
        Log.d("Meow", "Loaded")
    }
    return servers
}

@Composable
fun Servers(navController: NavController, servers: MutableList<JSONObject>) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(servers) {server ->
            ServerItem(
                server = server,
                Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { navController.navigate("servers/${server.getString("id")}/channels") }
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
        "    \"created_at\": 1689090909090,\n" +
        "    \"description\": \"Server description\",\n" +
        "    \"name\": \"server name\",\n" +
        "}")
    )
}