// المسار: FaselHDProvider/src/main/kotlin/com/faselhd/FaselHDPlugin.kt

package com.faselhd

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin // ✨ تم إضافة هذا السطر المهم
import android.content.Context
import com.lagradost.cloudstream3.plugins.BasePlugin // ✨ تم التغيير إلى BasePlugin وهو الأفضل

@CloudstreamPlugin
class FaselHDPlugin: BasePlugin() { // ✨ تم التغيير إلى BasePlugin
    override fun load(context: Context) {
        // This function is called when the plugin is loaded
        registerMainAPI(FaselHD())
    }
}
