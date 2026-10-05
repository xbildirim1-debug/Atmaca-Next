package com.atmacanext.app.automation

/**
 * Detects media attached to one visible reply without confusing the author's
 * avatar/profile image with tweet media. This is used only to skip media replies
 * in commenter-follow discovery; it never clicks media.
 */
internal object ReplyMediaEvidence {
    fun hasMedia(nodes: List<NodeSnapshot>, rowTop: Int, rowBottom: Int): Boolean {
        if (rowBottom <= rowTop) return false
        val viewportWidth = nodes.filter { it.visible }.maxOfOrNull { it.bounds.right } ?: 0
        return nodes.any { node ->
            // A previous reply's image, or a page/container spanning several
            // replies, does not belong to this header's vertical band.
            if (!node.visible || node.editable || node.bounds.top < rowTop || node.bounds.top >= rowBottom) return@any false
            val id = node.viewId.orEmpty().lowercase()
            val desc = XUiVocabulary.normalize(node.contentDescription.orEmpty())
            val text = XUiVocabulary.normalize(node.text.orEmpty())
            val label = "$desc $text"

            val avatarLike = id.contains("avatar") || id.contains("profile_image") || id.contains("profile photo") ||
                label.contains("profil foto") || label.contains("profile photo") || label.contains("avatar")
            if (avatarLike) return@any false

            val width = node.bounds.right - node.bounds.left
            val height = node.bounds.bottom - node.bounds.top
            if (width <= 0 || height <= 0) return@any false
            val contentSized = width >= maxOf(120, viewportWidth / 4) && height >= 80

            val mediaId = listOf(
                "tweet_photo", "tweet_image", "media_view", "media_container", "media_image",
                "video_player", "video_view", "gif_view", "card_image", "photo_view",
            ).any(id::contains)
            if (mediaId && contentSized) return@any true

            val mediaLabel = listOf("fotoğraf", "photo", "image", "video", "gif", "medya", "media").any { token ->
                label == token || label.startsWith("$token ") || label.contains(" $token ")
            }
            val image = node.className.orEmpty().contains("ImageView", ignoreCase = true)
            // Text mentioning a video/photo is still a text reply. Small generic
            // "Photo" avatars and the inline composer's GIF/image buttons are not
            // attached tweet media either.
            val shortMediaDescription = node.text.isNullOrBlank() && desc.length <= 40 &&
                !node.className.orEmpty().contains("TextView", ignoreCase = true)
            if (mediaLabel && contentSized && (image || shortMediaDescription)) return@any true

            // Some Compose versions expose tweet media only as a large ImageView.
            // Avatars are small; require a clearly content-sized rectangle.
            image && contentSized
        }
    }
}
