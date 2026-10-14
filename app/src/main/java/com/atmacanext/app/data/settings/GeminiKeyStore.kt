package com.atmacanext.app.data.settings

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class GeminiKeyStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("atmaca_gemini_secure", Context.MODE_PRIVATE)

    fun hasKey(): Boolean = !prefs.getString(KEY_CIPHERTEXT, null).isNullOrBlank()

    fun save(apiKey: String) {
        val value = apiKey.trim()
        require(value.length in 20..300) { "Gemini API anahtarı geçersiz" }
        writeCurrent(value)
        val history = readHistory()
        history.removeAll { it == value }
        history.add(0, value)
        writeHistory(history.take(8))
    }

    fun history(): List<String> = readHistory()

    fun remove(apiKey: String) {
        val history = readHistory()
        history.removeAll { it == apiKey }
        writeHistory(history)
        if (load() == apiKey) {
            if (history.isEmpty()) clearCurrent() else writeCurrent(history.first())
        }
    }

    fun load(): String? = decrypt(prefs.getString(KEY_CIPHERTEXT, null), prefs.getString(KEY_IV, null))

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun writeCurrent(value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    private fun clearCurrent() {
        prefs.edit().remove(KEY_CIPHERTEXT).remove(KEY_IV).apply()
    }

    private fun readHistory(): MutableList<String> {
        val raw = decrypt(prefs.getString(KEY_HISTORY, null), prefs.getString(KEY_HISTORY_IV, null))
        val parsed = raw?.lineSequence()?.map { it.trim() }?.filter { it.length in 20..300 }?.distinct()?.toMutableList()
        if (!parsed.isNullOrEmpty()) return parsed
        return load()?.let { mutableListOf(it) } ?: mutableListOf()
    }

    private fun writeHistory(keys: List<String>) {
        if (keys.isEmpty()) {
            prefs.edit().remove(KEY_HISTORY).remove(KEY_HISTORY_IV).apply()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(keys.joinToString("\n").toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(KEY_HISTORY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_HISTORY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    private fun decrypt(ciphertext: String?, iv: String?): String? {
        val encrypted = ciphertext?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        val vector = iv?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, vector))
            cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "atmaca_next_gemini_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_CIPHERTEXT = "ciphertext"
        const val KEY_IV = "iv"
        const val KEY_HISTORY = "history"
        const val KEY_HISTORY_IV = "history_iv"
    }
}
