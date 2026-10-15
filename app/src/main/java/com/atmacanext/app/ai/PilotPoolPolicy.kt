package com.atmacanext.app.ai

enum class ContentPool(val title: String) { TWEET("Tweet metinleri"), REPLY("Hazır yorum metinleri") }

/** The two pools change in one storage write. A short destination never deletes source text. */
object PilotPoolPolicy {
    data class Edit(val source: List<String>, val destination: List<String>, val count: Int, val added: Int)
    fun transfer(source: List<String>, destination: List<String>, count: Int, copy: Boolean = false,
                 capacity: Int = TweetPoolPolicy.MAX_LINES): Edit {
        require(count in 1..source.size) { "Kaynak havuzda $count metin yok (${source.size} var)" }
        val selected = source.takeLast(count)
        require(selected.all(TweetPoolPolicy::valid)) { "Taşınacak metinler 280 karakter sınırını aşmamalı" }
        val keys = destination.map(TweetPoolPolicy::key).toMutableSet()
        val added = selected.filter { keys.add(TweetPoolPolicy.key(it)) }
        require(destination.size + added.size <= capacity) { "Hedef havuzda yeterli yer yok; kaynak korunuyor" }
        return Edit(if (copy) source else source.dropLast(count), destination + added, count, added.size)
    }
    fun deleteLast(source: List<String>, count: Int): List<String> {
        require(count in 1..source.size) { "Havuzda $count metin yok (${source.size} var)" }
        return source.dropLast(count)
    }
    fun cursor(old: Int, size: Int): Int = if (size <= 0 || old !in 0 until size) 0 else old
}
