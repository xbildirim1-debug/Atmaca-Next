package com.atmacanext.app.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentMemoryLikeAvatar26_78Test {
    @Test fun accountRemembersACommentedTweetAcrossANewTask() {
        val first = CommentedTweetPolicy.add(emptyList(), "tweet-a")
        val again = CommentedTweetPolicy.add(first, "tweet-a")
        val second = CommentedTweetPolicy.add(again, "tweet-b")
        assertEquals(listOf("tweet-a", "tweet-b"), second)
        assertTrue(CommentedTweetPolicy.contains(second, "tweet-a"))
        assertFalse(CommentedTweetPolicy.contains(second, "tweet-c"))
        val capped = (1..520).fold(emptyList<String>()) { rows, id -> CommentedTweetPolicy.add(rows, "k$id") }
        assertEquals(500, capped.size)
        assertFalse(capped.contains("k1"))
    }

    @Test fun likeIsTriedThenTheReplyStillCountsIfItCannotBeConfirmed() {
        assertEquals(ReplyLikePolicy.Step.TAP, ReplyLikePolicy.step(false, 0))
        assertEquals(ReplyLikePolicy.Step.LOOK_UP, ReplyLikePolicy.step(false, 1))
        assertEquals(ReplyLikePolicy.Step.DONE, ReplyLikePolicy.step(true, 1))
        assertEquals(ReplyLikePolicy.Step.GIVE_UP, ReplyLikePolicy.step(false, 4))
    }

    @Test fun drawerAvatarIsTheLargePhotoNotTheBadge() {
        val picked = AccountAvatarEvidence.pick(listOf(
            AccountAvatarEvidence.Candidate("doğrulandı", "badge", "android.widget.ImageView", 160, 80, 40, 40, true),
            AccountAvatarEvidence.Candidate("profil fotoğrafı", "profile_image", "android.widget.ImageView", 40, 70, 96, 96, true),
            AccountAvatarEvidence.Candidate("", "icon", "android.widget.ImageView", 20, 200, 24, 24, true),
        ))
        assertEquals(40, picked?.left)
        assertEquals(96, picked?.width)
    }

    @Test fun unlabeledDrawerStillCropsBesideTheHandle() {
        val box = AccountAvatarEvidence.besideHandle(
            listOf(AccountAvatarEvidence.Candidate("@saresirinnn", "", "android.widget.TextView", 280, 240, 160, 36, true)),
            120,
        )
        assertEquals(144, box?.left)
        assertEquals(120, box?.width)
        assertTrue((box?.left ?: 0) + (box?.width ?: 0) <= 280)
    }

    @Test fun textThatStartsAtTheEdgeIsNotUsedAsThePhoto() {
        val box = AccountAvatarEvidence.besideHandle(
            listOf(AccountAvatarEvidence.Candidate("@saresirinnn", "", "android.widget.TextView", 40, 240, 180, 36, true)),
            120,
        )
        assertEquals(68, box?.top)
        assertTrue((box?.top ?: 0) + (box?.height ?: 0) < 240)
    }
}
