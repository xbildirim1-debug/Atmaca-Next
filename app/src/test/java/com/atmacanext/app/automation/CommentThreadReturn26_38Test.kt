package com.atmacanext.app.automation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentThreadReturn26_38Test {
    @Test
    fun changedMediaLayoutStillReturnsToSameReplyThreadWhenRowsOverlap() {
        assertTrue(
            DiscoveryViewportEvidence.returnedToParent(
                before = "before-media-layout",
                current = "after-autoplay-layout",
                beforeKeys = listOf("target-post", "comment-medo", "comment-two", "comment-three"),
                currentKeys = listOf("target-post", "comment-two", "comment-three", "comment-four"),
                openedAuthor = "darkwebhaber",
                child = "medo",
            ),
        )
    }

    @Test
    fun stillOpenCommentDetailIsNeverMistakenForParentEvenWithSharedKey() {
        assertFalse(
            DiscoveryViewportEvidence.returnedToParent(
                before = "parent",
                current = "child-detail",
                beforeKeys = listOf("target-post", "comment-medo"),
                currentKeys = listOf("comment-medo"),
                openedAuthor = "medo",
                child = "medo",
            ),
        )
    }

    @Test
    fun temporarilyMissingParentHeaderRequiresTwoSharedRows() {
        assertFalse(
            DiscoveryViewportEvidence.returnedToParent(
                before = "parent",
                current = "hydrating",
                beforeKeys = listOf("target-post", "comment-medo", "comment-two"),
                currentKeys = listOf("comment-medo"),
                openedAuthor = null,
                child = "medo",
            ),
        )
        assertTrue(
            DiscoveryViewportEvidence.returnedToParent(
                before = "parent",
                current = "hydrated",
                beforeKeys = listOf("target-post", "comment-medo", "comment-two"),
                currentKeys = listOf("target-post", "comment-two", "comment-four"),
                openedAuthor = null,
                child = "medo",
            ),
        )
    }

    @Test
    fun unrelatedTweetDetailDoesNotCountAsReturnWithoutSharedRows() {
        assertFalse(
            DiscoveryViewportEvidence.returnedToParent(
                before = "parent",
                current = "other-tweet",
                beforeKeys = listOf("target-post", "comment-medo", "comment-two"),
                currentKeys = listOf("other-post", "other-comment"),
                openedAuthor = "someoneelse",
                child = "medo",
            ),
        )
    }
}
