package com.keyiflerolsun

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import org.jsoup.nodes.Element

@CloudstreamPlugin
class DDizi : MainAPI() {
    override var mainUrl              = "https://www.ddizi.im"
    override var name                 = "DDizi"
    override var lang                 = "tr"
    override val hasMainPage         = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport   = true
    override val supportedTypes      = setOf(TvType.TvSeries)

    override val mainPage = mainPageOf(
        "$mainUrl/son-bolumler" to "Son Bölümler",
        "$mainUrl/son-eklenen-diziler" to "Son Eklenen Diziler",
        "$mainUrl/populer" to "Popüler Diziler",
        "$mainUrl/imdb-7-plus" to "IMDB 7+ Diziler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = request.data
        val document = app.get(url).document
        val home = document.select("div.movie-box").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.movie-details h2")?.text()?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/arama/$query").document
        return document.select("div.movie-box").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("div.movie-detail h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.movie-detail img")?.attr("src"))
        val plot = document.selectFirst("div.description")?.text()?.trim()
        
        val year = document.select("div.movie-detail span.year").text().toIntOrNull()
        val tags = document.select("div.categories a").map { it.text() }
        val rating = document.select("span.imdb").text().toRatingInt()

        val episodes = document.select("div.episode-box").map { ep ->
            val epTitle = ep.selectFirst("span.episode-name")?.text() ?: ""
            val epHref = fixUrl(ep.selectFirst("a")?.attr("href") ?: return null)
            val epNum = ep.selectFirst("span.episode-number")?.text()?.substringAfter("Bölüm ")?.toIntOrNull()
            val seasonNum = ep.selectFirst("span.episode-number")?.text()?.substringAfter("Sezon ")?.substringBefore(" ")?.toIntOrNull() ?: 1

            newEpisode(epHref) {
                this.name = epTitle
                this.season = seasonNum
                this.episode = epNum
            }
        }.reversed()

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.plot = plot
            this.tags = tags
            this.year = year
            this.rating = rating
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        document.select("div.video-player iframe").map { iframe ->
            val src = iframe.attr("src")
            loadExtractor(src, data, subtitleCallback, callback)
        }

        return true
    }
} 