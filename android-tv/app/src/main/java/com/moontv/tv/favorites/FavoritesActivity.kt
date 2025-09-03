package com.moontv.tv.favorites

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moontv.tv.net.Favorite
import com.moontv.tv.net.NetworkModule
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import com.moontv.tv.R

class FavoritesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { Surface(Modifier.fillMaxSize()) { FavoritesScreen() } }
        }
    }
}

@Composable
private fun FavoritesScreen() {
    val ctx = LocalContext.current
    val api = remember { NetworkModule.createApi(ctx) }
    var items by remember { mutableStateOf<List<Pair<String, Favorite>>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { api.getAllFavorites() }.onSuccess { map ->
            items = map.entries.map { it.key to it.value }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("我的收藏")
        Spacer(Modifier.height(12.dp))
        LazyColumn { items(items) { (key, f) ->
            Row(Modifier.fillMaxWidth().height(120.dp).focusable().padding(vertical = 6.dp)) {
                AsyncImage(
                    model = f.cover,
                    contentDescription = f.title,
                    placeholder = painterResource(id = R.drawable.poster_placeholder),
                    error = painterResource(id = R.drawable.poster_error),
                    modifier = Modifier.width(90.dp).aspectRatio(2f/3f)
                )
                Column(Modifier.weight(1f).padding(12.dp)) {
                    Text(text = f.title)
                    Text(text = f.source_name)
                }
                Button(onClick = { scope.launch { runCatching { api.deleteFavorite(key) }.onSuccess {
                    items = items.filterNot { it.first == key }
                } } }) { Text("删除") }
            }
        } }
    }
}
