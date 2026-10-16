package com.atmacanext.app.automation

/** Cross-account engagement uses the newest own post, with no discovery age threshold. */
internal object AccountEngagementTweetPolicy {
    data class Post(val key: String, val author: String?, val text: String, val age: Long?, val pinned: Boolean = false)
    fun latest(posts: List<Post>, peer: String): Post? = posts.filter {
        it.author == peer && it.key.isNotBlank() && it.text.isNotBlank() && it.age != null && it.age >= 0
    }.minByOrNull { it.age!! }
    fun validTweetUrl(raw: String?): Boolean = runCatching {
        val uri = java.net.URI(raw.orEmpty().trim())
        uri.scheme in setOf("https", "http") && uri.host?.lowercase() in setOf("x.com", "www.x.com", "twitter.com", "www.twitter.com") &&
            Regex("^/(?:[A-Za-z0-9_]{1,15}|i)/status/[0-9]+/?$").matches(uri.path)
    }.getOrDefault(false)
    fun linkedTarget(nodes: List<NodeSnapshot>, url: String?): Boolean {
        if (!validTweetUrl(url)) return false
        val expected = java.net.URI(url!!.trim()).path.split('/')[1].lowercase()
        val actual = ReplyComposerEvidence.postHeader(nodes)?.handle ?: return false
        return expected == "i" || expected == actual
    }
}
