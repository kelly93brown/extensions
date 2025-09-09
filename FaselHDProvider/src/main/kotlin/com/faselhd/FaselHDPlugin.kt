// المسار: FaselHDProvider/src/main/kotlin/com/faselhd/FaselHDPlugin.kt
package com.faselhd

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin
import android.content.Context

@CloudstreamPlugin
class FaselHDPlugin: BasePlugin() {
    override fun load(context: Context) {
        registerMainAPI(FaselHD())
    }
}
