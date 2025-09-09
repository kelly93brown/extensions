
package com.faselhd

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.getQualityFromString
import org.jsoup.nodes.Element
import com.lagradost.nicehttp.CloudflareKiller // ✨ تأكد من وجود هذا السطر الصحيح

class FaselHD : MainAPI() {
    override var lang = "ar"
    override var mainUrl = "https://www.faselhds.life"
    override var name = "FaselHD"
    override val usesWebView = false
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie, TvType.AsianDrama, TvType.Anime)
    
    private val cfKiller = CloudflareKiller()

    private fun String.getIntFromText(): Int? {
        return Regex("""\d+""").find(this)?.groupValues?.firstOrNull()?.toIntOrNull()
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        val url = selectFirst("div.postDiv a")?.attr("href") ?: return null
        val posterUrl = selectFirst("div.postDiv a div img")?.attr("data-src")?.ifEmpty {
            selectFirst("div.postDiv a div img")?.attr("src")
        }
        val title = selectFirst("div.postDiv a div img")?.attr("alt") ?: ""
        val quality = selectFirst(".quality")?.text()?.replace("1080p |-".toRegex(), "")
        
        val type = when {
            title.contains("فيلم") -> TvType.Movie
            title.contains("انمي") || title.contains("أنمي") -> TvType.Anime
            else -> TvType.TvSeries
        }

        return MovieSearchResponse(
            title.replace("الموسم الأول|برنامج|فيلم|مترجم|اون لاين|مسلسل|مشاهدة|انمي|أنمي".toRegex(),"").trim(),
            url,
            this@FaselHD.name,
            type,
            posterUrl,
            null,
            null,
            quality = getQualityFromString(quality),
            posterHeaders = cfKiller.getCookieHeaders(mainUrl).toMap()
        )
    }

    override val mainPage = mainPageOf(
        "$mainUrl/all-movies/page/" to "جميع الافلام",
        "$mainUrl/movies_top_views/page/" to "الافلام الاعلى مشاهدة",
        "$mainUrl/dubbed-movies/page/" to "الأفلام المدبلجة",
        "$mainUrl/movies_top_imdb/page/" to "الافلام الاعلى تقييما IMDB",
        "$mainUrl/series/page/" to "مسلسلات",
        "$mainUrl/recent_series/page/" to "المضاف حديثا",
        "$mainUrl/anime/page/" to "الأنمي",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = request.data + page
        val doc = app.get(url, interceptor = cfKiller, timeout = 120).document
        val list = doc.select("div#postList div.col-xl-2.col-lg-2.col-md-3.col-sm-3")
            .mapNotNull { it.toSearchResponse() }
        return newHomePageResponse(request.name, list)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val doc = app.get(url, interceptor = cfKiller, timeout = 120).document
        return doc.select("div#postList div.col-xl-2.col-lg-2.col-md-3.col-sm-3")
            .mapNotNull { it.toSearchResponse() }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, interceptor = cfKiller, timeout = 120).document
        val isMovie = doc.select("div.epAll").isEmpty()
        val posterUrl = doc.select("div.posterImg img").attr("src")
            .ifEmpty { doc.select("div.seasonDiv.active img").attr("data-src") }

        val year = doc.select("div#singleList div.col-xl-6").firstOrNull {
            it.text().contains("سنة|موعد".toRegex())
        }?.text()?.getIntFromText()

        val title = doc.select("title").text().substringBefore(" - فاصل").trim()
            .replace("الموسم الأول|برنامج|فيلم|مترجم|اون لاين|مسلسل|مشاهدة|انمي|أنمي".toRegex(), "").trim()

        val duration = doc.select("div#singleList div.col-xl-6").firstOrNull {
            it.text().contains("مدة|توقيت".toRegex())
        }?.text()?.getIntFromText()

        val tags = doc.select("div#singleList div.col-xl-6:contains(تصنيف الفيلم) a").map { it.text() }
        val recommendations = doc.select("div#postList div.postDiv").mapNotNull { it.toSearchResponse() }
        val synopsis = doc.select("div.singleDesc p").text()

        return if (isMovie) {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = posterUrl
                this.year = year
                this.plot = synopsis
                this.duration = duration
                this.tags = tags
                this.recommendations = recommendations
                this.posterHeaders = cfKiller.getCookieHeaders(mainUrl).toMap()
            }
        } else {
            val episodes = mutableListOf<Episode>()
            doc.select("div.epAll a").mapNotNull {
                episodes.add(
                    Episode(
                        it.attr("href"),
                        it.text(),
                        doc.selectFirst("div.seasonDiv.active div.title")?.text()?.getIntFromText() ?: 1,
                        it.text().getIntFromText(),
                    )
                )
            }
            
            doc.select("div#seasonList div.seasonDiv:not(.active)").apmap { seasonElement ->
                val seasonId = seasonElement.attr("onclick").replace(".*\\/\\?p=|'".toRegex(), "")
                val seasonDoc = app.get("$mainUrl/?p=$seasonId", interceptor = cfKiller).document
                seasonDoc.select("div.epAll a").map { episodeElement ->
                    episodes.add(
                        Episode(
                            episodeElement.attr("href"),
                            episodeElement.text(),
                            seasonDoc.selectFirst("div.seasonDiv.active div.title")?.text()?.getIntFromText(),
                            episodeElement.text().getIntFromText(),
                        )
                    )
                }
            }
            
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes.distinct().sortedBy { it.episode }) {
                this.duration = duration
                this.posterUrl = posterUrl
                this.year = year
                this.plot = synopsis
                this.tags = tags
                this.recommendations = recommendations
                this.posterHeaders = cfKiller.getCookieHeaders(mainUrl).toMap()
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, interceptor = cfKiller).document
        
        val iframeSrc = doc.selectFirst("iframe[name=player_iframe]")?.attr("src")
        if (iframeSrc != null) {
            val iframeDoc = app.get(iframeSrc, interceptor = cfKiller, referer = data).document
            val m3u8Link = Regex("""(https?://[^\s"'<>]+\.m3u8)""").find(iframeDoc.html())?.value
            
            if (m3u8Link != null) {
                M3u8Helper.generateM3u8(this.name, m3u8Link, mainUrl).forEach(callback)
            }
        }
        
        return true
    }
}
