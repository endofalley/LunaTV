package com.moontv.tv.net

data class TokenResponse(
    val token: String,
    val user: User,
    val exp: Long
)

data class User(
    val username: String,
    val role: String
)

data class Detail(
    val id: String,
    val title: String,
    val poster: String?,
    val episodes: List<String>,
    val episodes_titles: List<String>?,
    val source: String,
    val source_name: String,
    val year: String?,
    val desc: String?
)

data class PlayRecord(
    val title: String,
    val source_name: String,
    val cover: String,
    val year: String,
    val index: Int,
    val total_episodes: Int,
    val play_time: Int,
    val total_time: Int,
    val save_time: Long,
    val search_title: String
)

data class SkipConfig(
    val enable: Boolean,
    val intro_time: Int,
    val outro_time: Int
)

data class SaveRecordRequest(
    val key: String,
    val record: PlayRecord
)

data class Favorite(
    val source_name: String,
    val total_episodes: Int,
    val title: String,
    val year: String,
    val cover: String,
    val save_time: Long,
    val search_title: String,
    val origin: String? = null
)

data class SaveFavoriteRequest(
    val key: String,
    val favorite: Favorite
)

data class LiveSource(
    val key: String,
    val name: String,
    val url: String,
    val ua: String? = null,
    val epg: String? = null,
    val from: String,
    val channelNumber: Int? = null,
    val disabled: Boolean? = null
)

data class LiveSourcesResponse(
    val success: Boolean,
    val data: List<LiveSource>
)

data class LiveChannel(
    val id: String,
    val tvgId: String,
    val name: String,
    val logo: String,
    val group: String,
    val url: String
)

data class LiveChannelsResponse(
    val success: Boolean,
    val data: List<LiveChannel>
)

data class EpgProgram(
    val start: String,
    val end: String,
    val title: String
)

data class LiveEpgData(
    val tvgId: String,
    val source: String,
    val epgUrl: String,
    val programs: List<EpgProgram>
)

data class LiveEpgResponse(
    val success: Boolean,
    val data: LiveEpgData
)

data class SearchItem(
    val id: String,
    val title: String,
    val poster: String?,
    val episodes: List<String>?,
    val episodes_titles: List<String>?,
    val source: String,
    val source_name: String,
    val year: String? = null,
    val desc: String? = null,
    val type_name: String? = null,
    val douban_id: Long? = null
)

data class SearchResponse(
    val results: List<SearchItem>
)
