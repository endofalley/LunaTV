package com.moontv.tv.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.moontv.tv.net.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class PlayerActivity : ComponentActivity() {
    private var player: SimpleExoPlayer? = null
    private var currentDetail: Detail? = null
    private var currentEpisodeIndex: Int = 0
    private var autoRetryCount: Int = 0
    private var currentSkip: SkipConfig? = null
    private var currentSpeedIdx = 0
    private val speeds = floatArrayOf(1.0f, 1.25f, 1.5f)
    private var playerView: StyledPlayerView? = null

    private var cachedTracks: Tracks? = null
    private data class TrackOption(val groupIndex: Int, val trackIndex: Int, val label: String)
    // Live EPG cache (for live streams)
    private var liveEpg: List<EpgProgram> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val source = intent.getStringExtra("source") ?: ""
        val id = intent.getStringExtra("id") ?: ""
        val streamUrl = intent.getStringExtra("streamUrl")
        val forcedEpisodeIndex = intent.getIntExtra("episodeIndex", -1)
        val title = intent.getStringExtra("title") ?: ""
        val year = intent.getStringExtra("year") ?: ""
        val cover = intent.getStringExtra("cover") ?: ""

        setContent { PlayerScreen(title = title) }

        lifecycleScope.launch {
            if (!streamUrl.isNullOrBlank()) {
                // 直播或直连播放
                startPlayer(streamUrl)
                // 如果是直播，尝试获取 EPG 信息
                val liveSource = intent.getStringExtra("liveSource")
                val liveTvgId = intent.getStringExtra("liveTvgId")
                if (!liveSource.isNullOrBlank() && !liveTvgId.isNullOrBlank()) {
                    withContext(Dispatchers.IO) {
                        runCatching { NetworkModule.createApi(this@PlayerActivity).getLiveEpg(liveSource, liveTvgId) }
                            .onSuccess { epgResp -> liveEpg = epgResp.data.programs }
                    }
                }
            } else {
                runCatching { playFlow(source, id, title, year, cover, forcedEpisodeIndex) }
            }
        }
    }

    private suspend fun playFlow(source: String, id: String, title: String, year: String, cover: String, forcedEpisodeIndex: Int) {
        val api = NetworkModule.createApi(this)
        val detail = api.getDetail(id = id, source = source)
        currentDetail = detail

        val key = "$source+$id"
        val skip = runCatching { api.getSkipConfig(source, id) }.getOrNull()
        currentSkip = skip
        val records = runCatching { api.getAllPlayRecords() }.getOrNull() ?: emptyMap()
        val record = records[key]

        currentEpisodeIndex = if (forcedEpisodeIndex >= 0) forcedEpisodeIndex else (record?.index ?: 1).coerceAtLeast(1) - 1
        val url = detail.episodes.getOrNull(currentEpisodeIndex) ?: detail.episodes.first()

        withContext(Dispatchers.Main) {
            startPlayer(url)
            attachPlayerListeners(source, id, title, year, cover)
        }

        // 续播
        val resumeSec = record?.play_time ?: 0
        if (resumeSec > 3) {
            player?.seekTo((resumeSec * 1000L).coerceAtLeast(0))
        }

        // 定时上报播放进度
        lifecycleScope.launch(Dispatchers.IO) {
            while (player != null) {
                val p = player ?: break
                val posMs = p.currentPosition
                val durMs = p.duration.takeIf { it > 0 } ?: 1L

                // 跳片头：若配置启用并且进度<片头长度，直接跳到片头结束
                skip?.let {
                    if (it.enable && it.intro_time > 0 && posMs in 1_000..(it.intro_time * 1000L)) {
                        p.seekTo(it.intro_time * 1000L)
                    }
                }

                // 跳片尾：若接近片尾阈值，直接跳到结尾（后续可扩展为下一集）
                skip?.let {
                    if (it.enable && it.outro_time > 0) {
                        val threshold = durMs - it.outro_time * 1000L
                        if (threshold > 0 && posMs >= threshold) {
                            p.seekTo(durMs)
                        }
                    }
                }

                // 每 5s 上报
                if (posMs > 0) {
                    val rec = PlayRecord(
                        title = title,
                        source_name = detail.source_name,
                        cover = cover,
                        year = year,
                        index = currentEpisodeIndex + 1,
                        total_episodes = detail.episodes.size,
                        play_time = (posMs / 1000).toInt(),
                        total_time = (durMs / 1000).toInt(),
                        save_time = System.currentTimeMillis(),
                        search_title = title
                    )
                    runCatching { api.savePlayRecord(SaveRecordRequest(key = key, record = rec)) }
                }

                delay(5_000)
            }
        }
    }

    private fun startPlayer(url: String) {
        val pv = playerView ?: StyledPlayerView(this).also { playerView = it }
        player?.release()
        player = SimpleExoPlayer.Builder(this).build().also { exo ->
            pv.player = exo
            val item = MediaItem.fromUri(url)
            exo.setMediaItem(item)
            exo.prepare()
            exo.playWhenReady = true
        }
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }

    private fun attachPlayerListeners(source: String, id: String, title: String, year: String, cover: String) {
        val p = player ?: return
        p.addListener(object : com.google.android.exoplayer2.Player.Listener {
            override fun onPlayerError(error: com.google.android.exoplayer2.PlaybackException) {
                // 简化：自动重试最多 3 次
                if (autoRetryCount < 3) {
                    autoRetryCount += 1
                    p.prepare()
                    p.playWhenReady = true
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == com.google.android.exoplayer2.Player.STATE_ENDED) {
                    // 自动下一集
                    val eps = currentDetail?.episodes ?: return
                    val next = currentEpisodeIndex + 1
                    if (next < eps.size) {
                        currentEpisodeIndex = next
                        val nextUrl = eps[next]
                        startPlayer(nextUrl)
                        attachPlayerListeners(source, id, title, year, cover)
                    }
                }
            }
        })
    }
}

@Composable
private fun PlayerScreen(title: String) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            var controlsVisible by remember { mutableStateOf(true) }
            var showQuality by remember { mutableStateOf(false) }
            var showAudio by remember { mutableStateOf(false) }
            var showInfo by remember { mutableStateOf(false) }

            var controlsTick by remember { mutableStateOf(0) }

            val videoOptions = remember { mutableStateListOf<TrackOption>() }
            val audioOptions = remember { mutableStateListOf<TrackOption>() }

            fun refreshTrackOptions() {
                videoOptions.clear()
                audioOptions.clear()
                val tracks = player?.currentTracks ?: return
                cachedTracks = tracks
                tracks.groups.forEachIndexed { gIdx, g ->
                    val type = g.type
                    for (tIdx in 0 until g.length) {
                        val format = g.getTrackFormat(tIdx)
                        val label = when (type) {
                            C.TRACK_TYPE_VIDEO -> {
                                val h = format.height
                                val br = (format.bitrate / 1000).takeIf { it > 0 } ?: 0
                                if (h > 0 && br > 0) "${h}p · ${br}kbps" else if (h > 0) "${h}p" else "视频${tIdx+1}"
                            }
                            C.TRACK_TYPE_AUDIO -> {
                                val lang = format.language ?: "und"
                                val ch = format.channelCount
                                if (ch > 0) "$lang · ${ch}ch" else lang
                            }
                            else -> null
                        }
                        if (label != null) {
                            if (type == C.TRACK_TYPE_VIDEO) videoOptions.add(TrackOption(gIdx, tIdx, label))
                            if (type == C.TRACK_TYPE_AUDIO) audioOptions.add(TrackOption(gIdx, tIdx, label))
                        }
                    }
                }
            }

            BackHandler(enabled = controlsVisible || showQuality || showAudio || showInfo) {
                when {
                    showQuality -> showQuality = false
                    showAudio -> showAudio = false
                    showInfo -> showInfo = false
                    controlsVisible -> controlsVisible = false
                }
            }

            fun bumpTick() { controlsTick++ }

            Box(
                Modifier
                    .fillMaxSize()
                    .onKeyEvent { evt ->
                        when (evt.key.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_UP,
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                            android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                            android.view.KeyEvent.KEYCODE_ENTER -> {
                                controlsVisible = true; bumpTick(); true
                            }
                            else -> false
                        }
                    }
                    .clickable {
                        controlsVisible = !controlsVisible
                        if (controlsVisible) { refreshTrackOptions(); bumpTick() }
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        StyledPlayerView(ctx).also { pv ->
                            playerView = pv
                            pv.useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 简易操作层：底部一行按钮
                if (controlsVisible) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color(0x66000000))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FocusButton("跳片头") { skipIntro(); bumpTick() }
                        FocusButton("跳片尾") { skipOutro(); bumpTick() }
                        FocusButton("下一集") { playNextEpisode(); bumpTick() }
                        FocusButton("重试") { retryPlayback(); bumpTick() }
                        val speed = speeds.getOrNull(currentSpeedIdx) ?: 1.0f
                        FocusButton("${speed}x") { cycleSpeed(); bumpTick() }
                        FocusButton("清晰度") { refreshTrackOptions(); showAudio = false; showQuality = true; bumpTick() }
                        FocusButton("音轨") { refreshTrackOptions(); showQuality = false; showAudio = true; bumpTick() }
                        FocusButton("信息") { showInfo = !showInfo; bumpTick() }
                    }
                }

                if (showQuality) {
                    TrackPanel(
                        title = "选择清晰度",
                        options = videoOptions.map { it.label },
                        onSelect = { idx ->
                            val opt = videoOptions.getOrNull(idx) ?: return@TrackPanel
                            selectTrack(C.TRACK_TYPE_VIDEO, opt.groupIndex, opt.trackIndex)
                            showQuality = false; bumpTick()
                        },
                        onDismiss = { showQuality = false; bumpTick() }
                    )
                }

                if (showAudio) {
                    TrackPanel(
                        title = "选择音轨",
                        options = audioOptions.map { it.label },
                        onSelect = { idx ->
                            val opt = audioOptions.getOrNull(idx) ?: return@TrackPanel
                            selectTrack(C.TRACK_TYPE_AUDIO, opt.groupIndex, opt.trackIndex)
                            showAudio = false; bumpTick()
                        },
                        onDismiss = { showAudio = false; bumpTick() }
                    )
                }

                if (showInfo) {
                    InfoOverlay()
                }
            }

            // 自动隐藏控制条：无操作 5 秒后隐藏（若无面板）
            LaunchedEffect(controlsVisible, controlsTick, showQuality, showAudio, showInfo) {
                if (controlsVisible && !showQuality && !showAudio && !showInfo) {
                    kotlinx.coroutines.delay(5000)
                    controlsVisible = false
                }
            }
        }
    }
}

private fun skipIntro() {
    val p = player ?: return
    val cfg = currentSkip ?: return
    if (cfg.enable && cfg.intro_time > 0) {
        p.seekTo(cfg.intro_time * 1000L)
    }
}

private fun skipOutro() {
    val p = player ?: return
    val cfg = currentSkip ?: return
    val dur = p.duration.takeIf { it > 0 } ?: return
    if (cfg.enable && cfg.outro_time > 0) {
        val target = (dur - cfg.outro_time * 1000L).coerceAtLeast(0)
        p.seekTo(target)
    }
}

private fun playNextEpisode() {
    val eps = currentDetail?.episodes ?: return
    val next = currentEpisodeIndex + 1
    if (next < eps.size) {
        currentEpisodeIndex = next
        val url = eps[next]
        startPlayer(url)
        // 重新挂载监听器
        // 注意：此处无法直接获得 source/id/title 等，可根据需要缓存
    }
}

private fun retryPlayback() {
    val p = player ?: return
    p.prepare()
    p.playWhenReady = true
    autoRetryCount = 0
}

private fun cycleSpeed() {
    val p = player ?: return
    currentSpeedIdx = (currentSpeedIdx + 1) % speeds.size
    val rate = speeds[currentSpeedIdx]
    p.playbackParameters = PlaybackParameters(rate)
}

@Composable
private fun InfoOverlay() {
    val p = player
    var pos by remember { mutableStateOf(0L) }
    var dur by remember { mutableStateOf(0L) }
    var height by remember { mutableStateOf(0) }
    var width by remember { mutableStateOf(0) }
    var rate by remember { mutableStateOf(1.0f) }
    var epi by remember { mutableStateOf(currentEpisodeIndex + 1) }
    var nowTitle by remember { mutableStateOf("") }
    var nextTitle by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            p?.let {
                pos = it.currentPosition
                dur = it.duration.takeIf { d -> d > 0 } ?: 0
                height = it.videoSize.height
                width = it.videoSize.width
                rate = it.playbackParameters.speed
                epi = currentEpisodeIndex + 1
                // 直播 EPG（若存在）
                if (liveEpg.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    val cur = liveEpg.firstOrNull { parseEpgTime(it.start) <= now && now < parseEpgTime(it.end) }
                    val nxt = liveEpg.firstOrNull { parseEpgTime(it.start) > now }
                    nowTitle = cur?.title ?: ""
                    nextTitle = nxt?.title ?: ""
                }
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(12.dp)
            .background(Color(0x66000000), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(text = "分辨率: ${width}x${height}", color = Color.White)
            Text(text = "倍速: ${"%.2f".format(rate)}x", color = Color.White)
            Text(text = "进度: ${formatTime(pos)} / ${formatTime(dur)}", color = Color.White)
            Text(text = "集数: 第 ${epi} 集", color = Color.White)
            if (liveEpg.isNotEmpty()) {
                if (nowTitle.isNotBlank()) Text(text = "正在播放: ${nowTitle}", color = Color.White)
                if (nextTitle.isNotBlank()) Text(text = "下一节目: ${nextTitle}", color = Color.White)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val total = (ms / 1000).toInt()
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

private fun parseEpgTime(s: String): Long {
    // 期望格式形如: 20250121 120000 +0800 或 20250121120000 +0800
    return try {
        val digits = s.filter { it.isDigit() }
        val y = digits.substring(0, 4).toInt()
        val M = digits.substring(4, 6).toInt()
        val d = digits.substring(6, 8).toInt()
        val h = digits.substring(8, 10).toInt()
        val m = digits.substring(10, 12).toInt()
        val sec = digits.substring(12, 14).toInt()
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, y)
            set(java.util.Calendar.MONTH, M - 1)
            set(java.util.Calendar.DAY_OF_MONTH, d)
            set(java.util.Calendar.HOUR_OF_DAY, h)
            set(java.util.Calendar.MINUTE, m)
            set(java.util.Calendar.SECOND, sec)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    } catch (_: Throwable) {
        0L
    }
}

private fun selectTrack(trackType: Int, groupIndex: Int, trackIndex: Int) {
    val p = player ?: return
    val tracks = p.currentTracks ?: return
    val group = tracks.groups.getOrNull(groupIndex)?.mediaTrackGroup ?: return
    val override = TrackSelectionOverride(group, listOf(trackIndex))
    val builder = p.trackSelectionParameters.buildUpon()
    when (trackType) {
        C.TRACK_TYPE_VIDEO -> builder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        C.TRACK_TYPE_AUDIO -> builder.clearOverridesOfType(C.TRACK_TYPE_AUDIO)
    }
    builder.addOverride(override)
    p.trackSelectionParameters = builder.build()
}

@Composable
private fun FocusButton(text: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Text(
        text = text,
        color = Color.White,
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .border(
                width = if (focused) 2.dp else 0.dp,
                brush = SolidColor(Color.White),
                shape = RoundedCornerShape(6.dp)
            )
            .graphicsLayer {
                val scale = if (focused) 1.1f else 1.0f
                scaleX = scale; scaleY = scale
            }
            .clickable { onClick() }
            .padding(8.dp)
    )
}

@Composable
private fun TrackPanel(
    title: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var focusedIndex by remember { mutableStateOf(0) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color(0xFF202020), RoundedCornerShape(8.dp))
                .padding(16.dp)
                .widthIn(min = 320.dp)
        ) {
            Text(text = title, color = Color.White)
            Spacer(Modifier.height(8.dp))
            options.forEachIndexed { idx, label ->
                var focused by remember { mutableStateOf(false) }
                Text(
                    text = label,
                    color = if (focusedIndex == idx) Color.Yellow else Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .onFocusChanged { focused = it.isFocused; if (focused) focusedIndex = idx }
                        .border(
                            width = if (focused) 2.dp else 0.dp,
                            brush = SolidColor(Color.White),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .graphicsLayer {
                            val scale = if (focused) 1.05f else 1.0f
                            scaleX = scale; scaleY = scale
                        }
                        .clickable { onSelect(idx) }
                        .padding(8.dp)
                )
            }
        }
    }
}
