package ru.sweetbread.flake

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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

@OptIn(DelicateCoroutinesApi::class)
suspend fun getHierarchy(serverId: String): MutableList<JSONObject> {
    lateinit var channels: MutableList<JSONObject>
    lateinit var categories: MutableList<JSONObject>
    val hierarchy = mutableListOf<JSONObject>()

    val channelsCon = GlobalScope.launch(Dispatchers.IO) {
        val request =
            client.get("$baseurl/dev/servers/$serverId/channels")
            { headers { bearerAuth(token) } }
        if (request.status == HttpStatusCode.OK) {
            channels = JSONArray(request.bodyAsText()).toArrayList().toMutableList()
                .filter { it.getString("category_id") == "null" }.toMutableList()
        }
    }

    val categoriesCon = GlobalScope.launch(Dispatchers.IO) {
        val request =
            client.get("$baseurl/dev/servers/$serverId/categories")
            { headers { bearerAuth(token) } }
        if (request.status == HttpStatusCode.OK) {
            categories = JSONArray(request.bodyAsText()).toArrayList().toMutableList()
        }
    }

    hierarchy.clear()

    while (!channelsCon.isCompleted) {
        delay(50)
    }
    channels.forEach {
        hierarchy.add(
            JSONObject()
                .put("type", "channel")
                .put("channel", it)
        )
    }

    while (!categoriesCon.isCompleted) {
        delay(50)
    }
    categories.forEach {
        hierarchy.add(
            JSONObject()
                .put("type", "category")
                .put("category", it)
        )
    }
    return hierarchy
}

@Composable
fun Channels(navController: NavController, hierarchy: MutableList<JSONObject>) {
    Log.d("Hierarchy", hierarchy.toString())
    LazyColumn(Modifier.fillMaxSize()) {
        items(hierarchy) {item ->
            if (item.getString("type") == "channel") {
                ChannelItem(navController, item.getJSONObject("channel"))
            } else {
                CategoryItem(navController, item.getJSONObject("category"))
            }
        }
    }
}

//@Preview
//@Composable
//fun ChannelsPreview() {
//    Column {
//        ChannelItem()
//        CategoryItem()
//    }
//}

@Composable
fun CategoryItem(navController: NavController, category: JSONObject) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(8.dp)
    ) {
        Text(category.getString("name"), fontWeight = FontWeight.Bold)

        Column(Modifier.fillMaxWidth()) {
            category.getJSONArray("channels").toArrayList().forEach {
                ChannelItem(navController, channel = it)
            }
        }
    }
}

@Composable
fun ChannelItem(navController: NavController, channel: JSONObject) {
    Text(text = channel.getString("name"),
        Modifier
            .fillMaxWidth()
            .padding(8.dp, 2.dp)
            .clip(RoundedCornerShape(25))
            .clickable {
                navController.navigate("channels/${channel.getString("id")}/messages")
            }
            .background(MaterialTheme.colorScheme.onPrimary)
            .padding(8.dp)
    )
}