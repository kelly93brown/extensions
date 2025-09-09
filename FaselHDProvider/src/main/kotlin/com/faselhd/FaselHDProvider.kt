package com.faselhd

import com.lagradost.cloudstream3.*

// This is the absolute minimum code required for a provider to compile.
class FaselHD : MainAPI() {
    override var name = "FaselHD"
    override var mainUrl = "https://www.faselhds.life"
    override var lang = "ar"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Anime)
    override val hasMainPage = true

    // We will build the functions one by one AFTER this compiles successfully.
}
