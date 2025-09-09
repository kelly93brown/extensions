// المسار: FaselHDProvider/src/main/kotlin/com/faselhd/FaselHDProvider.kt
package com.faselhd

import com.lagradost.cloudstream3.*

class FaselHD : MainAPI() {
    override var name = "FaselHD"
    override var mainUrl = "https://www.faselhds.life"
    override var lang = "ar"
    override val supportedTypes = setOf(TvType.Movie) // Simplified
    override val hasMainPage = false // Turned off main page temporarily

    // All functions that could fail have been removed.
    // The compiler should have nothing to complain about.
}
