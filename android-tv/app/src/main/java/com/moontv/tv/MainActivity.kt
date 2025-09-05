package com.moontv.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moontv.tv.player.PlayerActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.moontv.tv.tvui.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvRoot(this)
        }
    }
}

@Composable
fun TvRoot(activity: ComponentActivity) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            HomeScreen(activity)
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
