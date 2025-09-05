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

@OptIn(ExperimentalTvFoundationApi::class)
@Composable
fun HomeScreen(activity: ComponentActivity) {
    val siteName = remember { System.getenv("NEXT_PUBLIC_SITE_NAME") ?: "LunaTV" }
    var featured by remember { mutableStateOf<List<PosterItem>>(emptyList()) }
    var movies by remember { mutableStateOf<List<PosterItem>>(emptyList()) }
    var tvs by remember { mutableStateOf<List<PosterItem>>(emptyList()) }
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
            featured = f.results.map(toPoster)
            movies = m.results.map(toPoster)
            tvs = t.results.map(toPoster)
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
