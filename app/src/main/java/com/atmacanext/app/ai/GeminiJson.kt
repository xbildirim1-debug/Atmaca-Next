package com.atmacanext.app.ai

import org.json.JSONArray
import org.json.JSONObject

/** JSON stays intact across quoted text, Unicode and bulk responses. */
internal object GeminiJson {
    fun payload(prompt: String, maxTokens: Int): JSONObject = JSONObject().apply {
        put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
        put("generationConfig", JSONObject().put("temperature", 0.9)
            .put("maxOutputTokens", maxTokens).put("responseMimeType", "application/json"))
    }

    fun drafts(raw: String): List<String> {
        val body = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val array = runCatching { JSONArray(body) }.getOrNull()
            ?: runCatching { JSONObject(body).optJSONArray("drafts") }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { (array.opt(it) as? String)?.trim()?.takeIf(String::isNotEmpty) }
    }
}
