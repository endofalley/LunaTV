package com.moontv.tv.detail

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moontv.tv.net.Detail
import com.moontv.tv.net.Favorite
import com.moontv.tv.net.NetworkModule
import com.moontv.tv.net.SaveFavoriteRequest
import com.moontv.tv.player.PlayerActivity
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import com.moontv.tv.R
import kotlinx.coroutines.launch

class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { Surface(Modifier.fillMaxSize()) { DetailScreen() } }
        }
    }
}

@Composable
private fun DetailScreen() {
    val ctx = LocalContext.current
    val api = remember { NetworkModule.createApi(ctx) }
    val id = (ctx as ComponentActivity).intent.getStringExtra("id") ?: ""
    val source = ctx.intent.getStringExtra("source") ?: ""
    val title = ctx.intent.getStringExtra("title") ?: ""
    val year = ctx.intent.getStringExtra("year") ?: ""
    val poster = ctx.intent.getStringExtra("poster") ?: ""

    var detail by remember { mutableStateOf<Detail?>(null) }
    val firstItemFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(id, source) {
        runCatching { api.getDetail(id, source) }.onSuccess { detail = it }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth()) {
            AsyncImage(
                model = poster,
                contentDescription = title,
                placeholder = painterResource(id = R.drawable.poster_placeholder),
                error = painterResource(id = R.drawable.poster_error),
                modifier = Modifier.width(180.dp).aspectRatio(2f/3f)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = "$title ($year)")
                Spacer(Modifier.height(8.dp))
                Row { Button(onClick = {
                    val fav = Favorite(
                        source_name = detail?.source_name ?: source,
                        total_episodes = detail?.episodes?.size ?: 1,
                        title = title,
                        year = year,
                        cover = poster,
                        save_time = System.currentTimeMillis(),
                        search_title = title
                    )
                    val key = "$source+$id"
                    scope.launch { runCatching { api.saveFavorite(SaveFavoriteRequest(key, fav)) } }
                }) { Text("收藏") }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("选集")
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            itemsIndexed(detail?.episodes ?: emptyList()) { idx, _ ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .then(if (idx == 0) Modifier.focusRequester(firstItemFocus) else Modifier)
                        .focusable()
                        .clickable {
                            val i = Intent(ctx, PlayerActivity::class.java).apply {
                                putExtra("source", source)
                                putExtra("id", id)
                                putExtra("title", title)
                                putExtra("year", year)
                                putExtra("cover", poster)
                                putExtra("episodeIndex", idx) // 0-based
                            }
                            ctx.startActivity(i)
                        }
                ) {
                    Text(text = "第 ${idx + 1} 集", modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
    LaunchedEffect(detail) { if ((detail?.episodes?.isNotEmpty() == true)) firstItemFocus.requestFocus() }
}
