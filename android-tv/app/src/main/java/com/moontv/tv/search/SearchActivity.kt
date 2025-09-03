package com.moontv.tv.search

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.moontv.tv.net.NetworkModule
import com.moontv.tv.net.SearchItem
import com.moontv.tv.detail.DetailActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import com.moontv.tv.tvui.TvPosterCard

class SearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { Surface(modifier = Modifier.fillMaxSize()) { SearchScreen() } }
        }
    }
}

@Composable
private fun SearchScreen() {
    val ctx = LocalContext.current
    val api = remember { NetworkModule.createApi(ctx) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchItem>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    val pageSize = 24
    var job by remember { mutableStateOf<Job?>(null) }

    val firstItemFocus = remember { FocusRequester() }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        OutlinedTextField(value = query, onValueChange = {
            query = it
            job?.cancel()
            job = (ctx as? ComponentActivity)?.lifecycleScope?.launch {
                delay(300)
                if (query.isNotBlank()) {
                    runCatching { api.search(query).results }.onSuccess { results = it; page = 1 }
                } else results = emptyList()
            }
        }, label = { Text("搜索影片…") })
        Spacer(Modifier.height(12.dp))
        val shown = results.take(page * pageSize)
        val gridState = rememberLazyGridState()
        LazyVerticalGrid(columns = GridCells.Adaptive(140.dp), state = gridState, modifier = Modifier.fillMaxSize()) {
            items(shown, key = { it.id + it.source }) { item ->
                TvPosterCard(
                    imageUrl = item.poster,
                    contentDescription = item.title,
                    title = item.title,
                    topRightBadge = item.source_name,
                    topLeftBadge = item.year,
                    onClick = {
                        val i = Intent(ctx, DetailActivity::class.java).apply {
                            putExtra("id", item.id)
                            putExtra("source", item.source)
                            putExtra("title", item.title)
                            putExtra("year", item.year ?: "")
                            putExtra("poster", item.poster ?: "")
                        }
                        ctx.startActivity(i)
                    }
                )
            }
        }

        // 滚动到底部自动加载下一页
        LaunchedEffect(results) {
            snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to gridState.layoutInfo.totalItemsCount }
                .filter { it.second > 0 }
                .distinctUntilChanged()
                .collectLatest { (last, total) ->
                    if (last != null && last >= total - 6 && shown.size < results.size) {
                        page += 1
                    }
                }
        }
    }
    LaunchedEffect(results) { if (results.isNotEmpty()) firstItemFocus.requestFocus() }
}
