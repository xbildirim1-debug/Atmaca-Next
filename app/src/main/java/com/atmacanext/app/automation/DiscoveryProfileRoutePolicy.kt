package com.atmacanext.app.automation

internal data class DiscoveryProfileRoute(val name: String, val uri: String)

/** Different X-supported routes; an accepted startActivity call is not proof
 * that X consumed a web profile link on the current device. */
internal object DiscoveryProfileRoutePolicy {
    fun route(cleanHandle: String, attempt: Int): DiscoveryProfileRoute = when (attempt) {
        1 -> DiscoveryProfileRoute("native-user", "twitter://user?screen_name=$cleanHandle")
        2 -> DiscoveryProfileRoute("twitter-web", "https://twitter.com/$cleanHandle")
        else -> DiscoveryProfileRoute("x-web", "https://x.com/$cleanHandle")
    }
}
