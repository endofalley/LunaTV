package com.moontv.tv.tvui

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import com.moontv.tv.detail.DetailActivity
import com.moontv.tv.net.NetworkModule
import com.moontv.tv.net.SearchItem
import com.moontv.tv.net.Favorite
import com.moontv.tv.net.PlayRecord
import com.moontv.tv.player.PlayerActivity
import com.moontv.tv.auth.LoginActivity
import com.moontv.tv.favorites.FavoritesActivity
import com.moontv.tv.live.LiveActivity
import com.moontv.tv.search.SearchActivity

data class PosterItem(
    val id: String,
    val source: String,
    val title: String,
    val image: String?,
    val year: String? = null,
)

data class ContinueItem(
    val id: String,
    val source: String,
    val title: String,
    val image: String?,
    val year: String? = null,
    val episodeIndex: Int = 0, // 0-based
    val playSeconds: Int = 0,
    val totalSeconds: Int = 0,
)

@OptIn(ExperimentalTvFoundationApi::class)
@Composable
fun HomeScreen(activity: ComponentActivity) {
    val siteName = remember { System.getenv("NEXT_PUBLIC_SITE_NAME") ?: "LunaTV" }
    var featured by remember { mutableStateOf<List<PosterItem>>(emptyList()) }
    var movies by remember { mutableStateOf<List<PosterItem>>(emptyList()) }
    var tvs by remember { mutableStateOf<List<PosterItem>>(emptyList()) }
    var continueItems by remember { mutableStateOf<List<ContinueItem>>(emptyList()) }
    var favorites by remember { mutableStateOf<List<PosterItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        val api = NetworkModule.createApi(activity)
        loading = true; error = null
        runCatching {
            val f = api.search("热门")
            val m = api.search("电影")
            val t = api.search("电视剧")
            val recs = api.getAllPlayRecords()
            val favs = api.getAllFavorites()
            featured = f.results.map(toPoster)
            movies = m.results.map(toPoster)
            tvs = t.results.map(toPoster)
            continueItems = recs.entries.mapNotNull { (k, v) -> toContinue(k, v) }
            favorites = favs.entries.mapNotNull { (k, v) -> toPosterFromFavorite(k, v) }
        }.onFailure { e -> error = e.message }.also { loading = false }
    }

    TvLazyColumn(
        modifier = Modifier.fillMaxSize().padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(horizontal = 36.dp),
    ) {
        item {
            // Top branding + quick actions
            Text(
                text = siteName,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "搜索 / 浏览 / 直播 / 收藏",
                fontSize = 16.sp,
                color = Color(0xFFBBBBBB)
            )
            Spacer(Modifier.height(8.dp))
            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item { QuickAction("登录") { activity.startActivity(Intent(activity, LoginActivity::class.java)) } }
                item { QuickAction("搜索") { activity.startActivity(Intent(activity, SearchActivity::class.java)) } }
                item { QuickAction("直播") { activity.startActivity(Intent(activity, LiveActivity::class.java)) } }
                item { QuickAction("收藏") { activity.startActivity(Intent(activity, FavoritesActivity::class.java)) } }
            }
        }

        if (error != null) {
            item { Text(text = "加载出错: ${error}", color = Color.Red) }
        }

        item { SectionHeader("继续观看") }
        item {
            if (loading && continueItems.isEmpty()) {
                Text(text = "加载中…", color = Color(0xFFBBBBBB))
            } else {
                TvLazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(continueItems) { it ->
                        val percent = if (it.totalSeconds > 0) (it.playSeconds * 100 / it.totalSeconds).coerceIn(0, 100) else 0
                        TvPosterCard(
                            imageUrl = it.image,
                            contentDescription = it.title,
                            title = it.title,
                            topLeftBadge = "第 ${it.episodeIndex + 1} 集",
                            topRightBadge = if (percent > 0) "$percent%" else null,
                            onClick = {
                                val i = Intent(activity, PlayerActivity::class.java).apply {
                                    putExtra("source", it.source)
                                    putExtra("id", it.id)
                                    putExtra("title", it.title)
                                    putExtra("year", it.year ?: "")
                                    putExtra("cover", it.image ?: "")
                                    putExtra("episodeIndex", it.episodeIndex)
                                }
                                activity.startActivity(i)
                            }
                        )
                    }
                }
            }
        }

        item { SectionHeader("热门推荐") }
        item {
            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(featured) { it ->
                    TvPosterCard(
                        imageUrl = it.image,
                        contentDescription = it.title,
                        title = it.title,
                        onClick = {
                            val i = Intent(activity, DetailActivity::class.java).apply {
                                putExtra("id", it.id)
                                putExtra("source", it.source)
                                putExtra("title", it.title)
                                putExtra("year", it.year ?: "")
                                putExtra("poster", it.image ?: "")
                            }
                            activity.startActivity(i)
                        }
                    )
                }
            }
        }

        item { SectionHeader("电影") }
        item {
            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(movies) { it ->
                    TvPosterCard(
                        imageUrl = it.image,
                        contentDescription = it.title,
                        title = it.title,
                        onClick = {
                            val i = Intent(activity, DetailActivity::class.java).apply {
                                putExtra("id", it.id)
                                putExtra("source", it.source)
                                putExtra("title", it.title)
                                putExtra("year", it.year ?: "")
                                putExtra("poster", it.image ?: "")
                            }
                            activity.startActivity(i)
                        }
                    )
                }
            }
        }

        item { SectionHeader("电视剧") }
        item {
            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tvs) { it ->
                    TvPosterCard(
                        imageUrl = it.image,
                        contentDescription = it.title,
                        title = it.title,
                        onClick = {
                            val i = Intent(activity, DetailActivity::class.java).apply {
                                putExtra("id", it.id)
                                putExtra("source", it.source)
                                putExtra("title", it.title)
                                putExtra("year", it.year ?: "")
                                putExtra("poster", it.image ?: "")
                            }
                            activity.startActivity(i)
                        }
                    )
                }
            }
        }
        item { SectionHeader("我的收藏") }
        item {
            if (loading && favorites.isEmpty()) {
                Text(text = "加载中…", color = Color(0xFFBBBBBB))
            } else {
                TvLazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(favorites) { it ->
                        TvPosterCard(
                            imageUrl = it.image,
                            contentDescription = it.title,
                            title = it.title,
                            onClick = {
                                val i = Intent(activity, DetailActivity::class.java).apply {
                                    putExtra("id", it.id)
                                    putExtra("source", it.source)
                                    putExtra("title", it.title)
                                    putExtra("year", it.year ?: "")
                                    putExtra("poster", it.image ?: "")
                                }
                                activity.startActivity(i)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
    )
}

@Composable
private fun QuickAction(label: String, onClick: () -> Unit) {
    androidx.compose.material3.Button(onClick = onClick) {
        Text(text = label, color = Color.White)
    }
    Spacer(Modifier.width(8.dp))
}

private val toPoster: (SearchItem) -> PosterItem = { s ->
    PosterItem(
        id = s.id,
        source = s.source,
        title = s.title,
        image = s.poster,
        year = s.year,
    )
}

private fun splitKey(key: String): Pair<String, String>? {
    val idx = key.indexOf('+')
    if (idx <= 0 || idx >= key.length - 1) return null
    val source = key.substring(0, idx)
    val id = key.substring(idx + 1)
    return source to id
}

private val toContinue: (String, PlayRecord) -> ContinueItem? = { key, r ->
    val parts = splitKey(key) ?: return@toContinue null
    ContinueItem(
        id = parts.second,
        source = parts.first,
        title = r.title,
        image = r.cover,
        year = r.year,
        episodeIndex = (r.index - 1).coerceAtLeast(0),
        playSeconds = r.play_time,
        totalSeconds = r.total_time,
    )
}

private val toPosterFromFavorite: (String, Favorite) -> PosterItem? = { key, f ->
    val parts = splitKey(key) ?: return@toPosterFromFavorite null
    PosterItem(
        id = parts.second,
        source = parts.first,
        title = f.title,
        image = f.cover,
        year = f.year,
    )
}
