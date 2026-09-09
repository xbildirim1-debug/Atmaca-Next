package com.atmacanext.app.automation

/**
 * Detects media attached to one visible reply without confusing the author's
 * avatar/profile image with tweet media. This is used only to skip media replies
 * in commenter-follow discovery; it never clicks media.
 */
internal object ReplyMediaEvidence {
    fun hasMedia(nodes: List<NodeSnapshot>, rowTop: Int, rowBottom: Int): Boolean {
        if (rowBottom <= rowTop) return false
        return nodes.any { node ->
            if (!node.visible || node.bounds.bottom <= rowTop || node.bounds.top >= rowBottom) return@any false
            val id = node.viewId.orEmpty().lowercase()
            val desc = XUiVocabulary.normalize(node.contentDescription.orEmpty())
            val text = XUiVocabulary.normalize(node.text.orEmpty())
            val label = "$desc $text"

            val avatarLike = id.contains("avatar") || id.contains("profile_image") || id.contains("profile photo") ||
                label.contains("profil foto") || label.contains("profile photo") || label.contains("avatar")
            if (avatarLike) return@any false

            val mediaId = listOf(
                "tweet_photo", "tweet_image", "media_view", "media_container", "media_image",
                "video_player", "video_view", "gif_view", "card_image", "photo_view",
            ).any(id::contains)
            if (mediaId) return@any true

            val mediaLabel = listOf("fotoğraf", "photo", "image", "video", "gif", "medya", "media").any { token ->
                label == token || label.startsWith("$token ") || label.contains(" $token ")
            }
            if (mediaLabel) return@any true

            // Some Compose versions expose tweet media only as a large ImageView.
            // Avatars are small; require a clearly content-sized rectangle.
            val width = node.bounds.right - node.bounds.left
            val height = node.bounds.bottom - node.bounds.top
            node.className.orEmpty().contains("ImageView", ignoreCase = true) && width >= 180 && height >= 120
        }
    }
}
