// المسار: FaselHDProvider/src/main/kotlin/com/faselhd/FaselHDProvider.kt

package com.faselhd

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Element
import com.lagradost.nicehttp.NiceResponse
import com.lagradost.nicehttp.CloudflareKiller

class FaselHD : MainAPI() {
    override var name = "FaselHD"
    override var mainUrl = "https://www.faselhds.life"
    override var lang = "ar"
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie, TvType.Anime)
    
    // CloudflareKiller ضروري للمواقع المحمية
    private val cfKiller = CloudflareKiller()

    // دالة مساعدة لعمل الطلبات للموقع المحمي
    private suspend fun appGet(url: String): NiceResponse {
        return app.get(url, interceptor = cfKiller, timeout = 120)
    }
    
    override val hasMainPage = true

    // ===============================================================
    // ✨✨✨ سنعمل على هذه المنطقة الآن ✨✨✨
    // ===============================================================
    
    // هذه الدالة مسؤولة عن تحويل كود HTML الخاص بكل فيلم إلى كائن يمكن للتطبيق فهمه
    private fun Element.toSearchResponse(): SearchResponse {
        val url = this.selectFirst("a")?.attr("href") ?: ""
        val posterUrl = this.selectFirst("img")?.attr("data-src")
        val title = this.selectFirst("img")?.attr("alt") ?: ""
        
        // تحديد النوع بناءً على العنوان
        val type = when {
            title.contains("فيلم") -> TvType.Movie
            title.contains("انمي") || title.contains("أنمي") -> TvType.Anime
            else -> TvType.TvSeries
        }
        
        return newMovieSearchResponse(
            name = title.replace("الموسم الأول|برنامج|فيلم|مترجم|اون لاين|مسلسل|مشاهدة|انمي|أنمي".toRegex(),"").trim(),
            url = url,
            apiName = this@FaselHD.name,
        ) {
            this.posterUrl = posterUrl
            this.type = type // نحدد النوع هنا
            this.posterHeaders = cfKiller.getCookieHeaders(mainUrl).toMap()
        }
    }

    // هذه هي الأقسام التي ستظهر في الصفحة الرئيسية للتطبيق
    override val mainPage = mainPageOf(
        "$mainUrl/all-movies/page/" to "جميع الافلام",
        "$mainUrl/series/page/" to "مسلسلات",
        "$mainUrl/anime/page/" to "الأنمي",
        "$mainUrl/movies_top_views/page/" to "الافلام الاعلى مشاهدة"
    )

    // هذه الدالة الرئيسية التي تجلب البيانات
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // 1. نرسل طلب للموقع ونستقبل كود HTML
        val doc = appGet(request.data + page).document

        // 2. نبحث داخل كود HTML عن كل الأفلام/المسلسلات
        // الموقع يضع كل فيلم داخل <div class="postDiv">
        val allItems = doc.select("div.postDiv").map {
            // 3. نحول كل عنصر نجده إلى كائن SearchResponse باستخدام الدالة المساعدة
            it.toSearchResponse()
        }

        // 4. نعيد النتائج للتطبيق على شكل HomePageList
        return newHomePageResponse(request.name, allItems)
    }

    // ===============================================================
    // باقي الدوال (سنعمل عليها لاحقًا)
    // ===============================================================

    override suspend fun search(query: String): List<SearchResponse>? {
        return null
    }
    
    override suspend fun load(url: String): LoadResponse? {
        return null
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return false
    }
}
