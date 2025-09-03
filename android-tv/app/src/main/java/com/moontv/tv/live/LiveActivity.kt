package com.moontv.tv.live

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
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.moontv.tv.ApiConfig
import com.moontv.tv.net.LiveChannel
import com.moontv.tv.net.NetworkModule
import com.moontv.tv.player.PlayerActivity
import kotlinx.coroutines.launch

class LiveActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LiveScreen(onPlay = { source, channel ->
                        val proxied = buildM3u8Proxy(source, channel.url)
                        val i = Intent(this, PlayerActivity::class.java).apply {
                            putExtra("streamUrl", proxied)
                            putExtra("title", channel.name)
                            putExtra("liveSource", source)
                            putExtra("liveTvgId", channel.tvgId)
                        }
                        startActivity(i)
                    })
                }
            }
        }
    }
}

@Composable
private fun LiveScreen(onPlay: (String, LiveChannel) -> Unit) {
    val scope = rememberCoroutineScope()
    val api = remember { NetworkModule }
    val ctx = LocalContext.current
    var sourceKey by remember { mutableStateOf("") }
    var channels by remember { mutableStateOf<List<LiveChannel>>(emptyList()) }
    var selectedIndex by remember { mutableStateOf(0) }
    val firstItemFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        scope.launch {
            val svc = api.createApi(ctx)
            val sources = svc.getLiveSources().data
            val first = sources.firstOrNull()
            if (first != null) {
                sourceKey = first.key
                channels = svc.getLiveChannels(first.key).data
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "直播源：$sourceKey")
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            itemsIndexed(channels) { idx, ch ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .then(if (idx == 0) Modifier.focusRequester(firstItemFocus) else Modifier)
                        .focusable()
                        .clickable { if (sourceKey.isNotBlank()) onPlay(sourceKey, ch) }
                ) {
                    Text(text = ch.name, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }

    LaunchedEffect(channels) { if (channels.isNotEmpty()) firstItemFocus.requestFocus() }
}

private fun buildM3u8Proxy(sourceKey: String, url: String): String {
    val encoded = java.net.URLEncoder.encode(url, "UTF-8")
    return "${ApiConfig.BASE_URL}/api/proxy/m3u8?url=$encoded&moontv-source=$sourceKey"
}
