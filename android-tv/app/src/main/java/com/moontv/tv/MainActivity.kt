package com.moontv.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.Intent
import androidx.compose.material3.Button
import com.moontv.tv.player.PlayerActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.Toast
import com.moontv.tv.live.LiveActivity
import com.moontv.tv.search.SearchActivity
import com.moontv.tv.favorites.FavoritesActivity
import com.moontv.tv.auth.LoginActivity
import com.moontv.tv.net.TokenStore
import com.moontv.tv.net.Favorite
import com.moontv.tv.net.NetworkModule
import com.moontv.tv.net.SaveFavoriteRequest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvRoot()
        }
    }
}

@Composable
fun TvRoot() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "LunaTV Android TV")
                Text(text = "占位：首页 / 搜索 / 详情")
                val tokenStore = remember { TokenStore(this@MainActivity) }
                val hasToken = remember { mutableStateOf(tokenStore.getToken() != null) }
                Row {
                    Button(onClick = { startActivity(Intent(this@MainActivity, LoginActivity::class.java)) }) {
                        Text(text = if (hasToken.value) "重新登录" else "登录")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { tokenStore.clearToken(); hasToken.value = false; Toast.makeText(this@MainActivity, "已登出", Toast.LENGTH_SHORT).show() }) {
                        Text(text = "登出")
                    }
                }
                Button(onClick = { demoPlay() }) {
                    Text(text = "演示播放（需要配置 BASE_URL 与登录）")
                }
                Button(onClick = { demoLive() }) {
                    Text(text = "演示直播（自动播放第一个频道）")
                }
                Button(onClick = { demoFavorite() }) {
                    Text(text = "演示收藏（写入一条示例）")
                }
                Button(onClick = { demoSearch() }) {
                    Text(text = "演示搜索 → 详情 → 播放")
                }
                Button(onClick = { startActivity(Intent(this@MainActivity, FavoritesActivity::class.java)) }) {
                    Text(text = "打开我的收藏")
                }
            }
        }
    }
}

private fun ComponentActivity.demoPlay() {
    // 演示：以 source + id 触发 PlayerActivity。实际应从搜索/详情进入。
    val intent = Intent(this, PlayerActivity::class.java).apply {
        putExtra("source", "demo")
        putExtra("id", "demo-id")
        putExtra("title", "演示影片")
        putExtra("year", "2024")
        putExtra("cover", "")
    }
    startActivity(intent)
}

private fun ComponentActivity.demoLive() {
    startActivity(Intent(this, LiveActivity::class.java))
}

private fun ComponentActivity.demoFavorite() {
    lifecycleScope.launch {
        val api = NetworkModule.createApi(this@demoFavorite)
        val fav = Favorite(
            source_name = "demo",
            total_episodes = 1,
            title = "演示影片",
            year = "2024",
            cover = "",
            save_time = System.currentTimeMillis(),
            search_title = "演示影片"
        )
        runCatching { api.saveFavorite(SaveFavoriteRequest(key = "demo+demo-id", favorite = fav)) }
        Toast.makeText(this@demoFavorite, "已写入示例收藏", Toast.LENGTH_SHORT).show()
    }
}

private fun ComponentActivity.demoSearch() {
    startActivity(Intent(this, SearchActivity::class.java))
}
