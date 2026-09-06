package com.atmacanext.app.ai

import com.atmacanext.app.data.settings.GeminiKeyStore
import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.domain.model.ScheduledTask
import com.atmacanext.app.domain.model.TaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

data class GeminiConnectionResult(val model: String, val message: String)

private class GeminiHttpException(val statusCode: Int, message: String) : IllegalStateException(message)

class GeminiContentService(private val keyStore: GeminiKeyStore) {
    fun hasApiKey(): Boolean = keyStore.hasKey()

    suspend fun prepare(task: ScheduledTask, account: Account, model: String): List<String> = withContext(Dispatchers.IO) {
        val count = task.repeatCount.coerceIn(1, 100)
        if (!task.useGemini) {
            val manual = task.contentText?.trim().orEmpty()
            require(manual.isNotBlank()) { "Görev metni boş" }
            return@withContext List(count) { manual }
        }
        val key = keyStore.load()?.takeIf(String::isNotBlank)
            ?: error("Gemini API anahtarı Ayarlar bölümünde kayıtlı değil")
        val prompt = buildPrompt(task, account, count)
        val raw = requestWithFallback(key, model, prompt).second
        val drafts = parseDrafts(raw)
            .map(::sanitize)
            .filter(::validDraft)
            .distinct()
        require(drafts.size >= count) { "Gemini $count farklı ve geçerli metin üretmedi (${drafts.size})" }
        drafts.take(count)
    }

    suspend fun testConnection(model: String): GeminiConnectionResult = withContext(Dispatchers.IO) {
        val key = keyStore.load()?.takeIf(String::isNotBlank)
            ?: error("Önce Gemini API anahtarını kaydet")
        val prompt = "Yalnız geçerli JSON dizi döndür: [\"Atmaca bağlantısı hazır\"]"
        val (resolvedModel, raw) = requestWithFallback(key, model, prompt)
        require(parseDrafts(raw).isNotEmpty()) { "Gemini yanıt verdi fakat beklenen metin alınamadı" }
        GeminiConnectionResult(resolvedModel, "Bağlantı başarılı: $resolvedModel")
    }

    private fun requestWithFallback(apiKey: String, requestedModel: String, prompt: String): Pair<String, String> {
        val candidates = GeminiModelResolver.orderedCandidates(discoverModels(apiKey), requestedModel)
        require(candidates.isNotEmpty()) { "Bu API anahtarına açık generateContent modeli bulunamadı" }
        var lastError: Throwable? = null
        for (candidate in candidates.take(5)) {
            try {
                return candidate to request(apiKey, candidate, prompt)
            } catch (error: GeminiHttpException) {
                lastError = error
                if (error.statusCode !in setOf(400, 404)) throw error
            }
        }
        throw lastError ?: IllegalStateException("Gemini modeli seçilemedi")
    }

    private fun discoverModels(apiKey: String): List<GeminiModelDescriptor> {
        val encodedKey = URLEncoder.encode(apiKey, Charsets.UTF_8.name())
        val endpoint = URL("https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000&key=$encodedKey")
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 30_000
        }
        return try {
            val code = connection.responseCode
            val body = readBody(connection, code)
            if (code !in 200..299) throw httpError(code, body, "Gemini model listesi alınamadı")
            val models = JSONObject(body).optJSONArray("models") ?: JSONArray()
            buildList {
                for (index in 0 until models.length()) {
                    val row = models.optJSONObject(index) ?: continue
                    val methods = row.optJSONArray("supportedGenerationMethods") ?: JSONArray()
                    add(
                        GeminiModelDescriptor(
                            name = row.optString("name"),
                            supportedGenerationMethods = buildSet {
                                for (methodIndex in 0 until methods.length()) add(methods.optString(methodIndex))
                            },
                        ),
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun request(apiKey: String, model: String, prompt: String): String {
        val safeModel = GeminiModelResolver.normalize(model).take(80)
        require(safeModel.matches(Regex("[a-z0-9._-]+"))) { "Geçersiz Gemini model adı" }
        val encodedKey = URLEncoder.encode(apiKey, Charsets.UTF_8.name())
        val endpoint = URL("https://generativelanguage.googleapis.com/v1beta/models/$safeModel:generateContent?key=$encodedKey")
        val payload = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.9)
                    .put("maxOutputTokens", 2_048)
                    .put("responseMimeType", "application/json"),
            )
        }
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        return try {
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val body = readBody(connection, code)
            if (code !in 200..299) throw httpError(code, body, "Gemini isteği başarısız")
            val root = JSONObject(body)
            root.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        } finally {
            connection.disconnect()
        }
    }

    private fun readBody(connection: HttpURLConnection, code: Int): String =
        (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

    private fun httpError(code: Int, body: String, prefix: String): GeminiHttpException {
        val reason = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
        return GeminiHttpException(code, "$prefix ($code): ${reason?.take(220) ?: "sunucu hatası"}")
    }

    private fun buildPrompt(task: ScheduledTask, account: Account, count: Int): String = buildString {
        appendLine("Türkçe X içeriği üret. Yalnız geçerli bir JSON dizi döndür; açıklama ve markdown kullanma.")
        appendLine("Dizi tam $count farklı metin içersin. Her metin en fazla 270 karakter olsun.")
        appendLine("Aynı cümleyi veya küçük varyasyonlarını tekrarlama. Uydurma veri, kişi veya kaynak ekleme.")
        appendLine("Hesap: ${account.username}")
        appendLine("Dil: ${account.aiLanguage}; ton: ${account.aiTone}")
        account.aiPersona.takeIf(String::isNotBlank)?.let { appendLine("Hesap kişiliği: $it") }
        appendLine("Görev: ${task.type.title}")
        task.contentPrompt?.takeIf(String::isNotBlank)?.let { appendLine("Konu/talimat: $it") }
        task.targetUrl?.takeIf(String::isNotBlank)?.let { appendLine("İlgili X bağlantısı: $it") }
        when (task.type) {
            TaskType.COMMENT -> appendLine("Metinler ilgili gönderiye doğal yanıt olacak; kaynak metni görmeden kesin bilgi iddia etme.")
            TaskType.QUOTE -> appendLine("Metinler alıntılanan gönderiye kısa ve ilgili yorum olacak.")
            TaskType.TEXT_TWEET, TaskType.IMAGE_TWEET -> appendLine("Her metin bağımsız bir X gönderisi olarak yayınlanabilir olsun.")
            else -> Unit
        }
    }

    private fun parseDrafts(raw: String): List<String> {
        val clean = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val array = runCatching { JSONArray(clean) }.getOrNull()
            ?: runCatching { JSONObject(clean).optJSONArray("drafts") }.getOrNull()
            ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optString(index).trim()
                if (item.isNotBlank()) add(item)
            }
        }
    }

    private fun sanitize(value: String): String = value
        .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "")
        .trim()

    private fun validDraft(value: String): Boolean = value.isNotBlank() && value.codePointCount(0, value.length) <= 280
}
