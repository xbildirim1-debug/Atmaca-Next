package com.atmacanext.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.domain.policy.TargetPagePolicy
import kotlinx.coroutines.launch

@Composable
fun QuoteTargetsDialog(account: Account, locked: Boolean, onDismiss: () -> Unit) {
    val allTargets by AppServices.repository.targetAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val existing = allTargets.filter { it.ownerAccountId == account.id && it.kind.name == "QUOTE" }.map { "@${it.handle}" }
    var fields by remember(account.id, existing) { mutableStateOf(List(5) { existing.getOrNull(it).orEmpty() }) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val names = fields.filter { it.isNotBlank() }.mapNotNull(TargetPagePolicy::normalize)
    val valid = names.size == fields.count { it.isNotBlank() } && names.distinct().size == names.size &&
        names.none { it == TargetPagePolicy.normalize(account.username) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        modifier = Modifier.imePadding(),
        title = { Text("${account.username} · Alıntı Hedefleri") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Bu hesaba en fazla 5 hedef ekle. Yorum Alıntısı görevi her hedefin profilindeki son gönderileri sırayla işler.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                fields.forEachIndexed { i, value ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { text -> fields = fields.toMutableList().also { it[i] = text.take(16) } },
                        label = { Text("Alıntı hedefi ${i + 1}") },
                        placeholder = { Text("@kullanıcı_adı") },
                        singleLine = true,
                        enabled = !locked && !saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text("Boş bıraktığın hedef kaydedilmez.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = valid && !locked && !saving, onClick = {
                saving = true
                scope.launch {
                    try { AppServices.repository.replaceQuoteTargets(account.id, names); onDismiss() }
                    catch (e: Exception) { error = e.message ?: "Alıntı hedefleri kaydedilemedi" }
                    finally { saving = false }
                }
            }) { Text(if (saving) "Kaydediliyor…" else "Kaydet") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Kapat") } },
    )
}
