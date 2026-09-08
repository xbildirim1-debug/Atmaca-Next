package com.atmacanext.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.domain.policy.TargetPagePolicy
import kotlinx.coroutines.launch

@Composable
fun AccountTargetsDialog(account: Account, locked: Boolean, onDismiss: () -> Unit) {
    var fields by remember(account.id) { mutableStateOf(List(1) { "" }) }
    var initialized by remember(account.id) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(account.id) {
        val rows = AppServices.repository.getActiveTargets(account.id)
        fields = List(1) { rows.getOrNull(it)?.handle?.let { h -> "@$h" }.orEmpty() }
        initialized = true
    }
    val entered = fields.filter { it.isNotBlank() }
    val names = entered.mapNotNull(TargetPagePolicy::normalize)
    val valid = names.size == entered.size && names.distinct().size == names.size &&
        names.none { it == TargetPagePolicy.normalize(account.username) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("${account.username} · Hedef hesap") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Bu hesap için tek hedef gir. Yorumcu ve retweetçi görevleri en az iki saatlik gönderilerden başlar; kişiler bitince daha eski gönderiye geçer.")
                fields.forEachIndexed { i, value ->
                    OutlinedTextField(value = value, onValueChange = { text -> fields = fields.toMutableList().also { it[i] = text.take(16) } },
                        label = { Text("Hedef ${i + 1} · @kullanıcı_adı") }, singleLine = true,
                        enabled = initialized && !locked && !saving, modifier = Modifier.fillMaxWidth())
                }
                Text("Hedefi değiştirmek için yeni kullanıcı adını yaz; kaldırmak için boşaltıp kaydet. Önceki birden fazla hedef kaydı varsa Kaydet bunları bu tek hedefle değiştirir.")
                if (!valid) Text("Geçerli, birbirinden farklı kullanıcı adları gir. Kendi hesabını hedef seçme.", color = MaterialTheme.colorScheme.error)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = initialized && valid && !locked && !saving, onClick = {
                val snapshot = names.toList()
                saving = true
                scope.launch {
                    try { AppServices.repository.replaceTargets(account.id, snapshot); onDismiss() }
                    catch (e: Exception) { error = e.message ?: "Hedefler kaydedilemedi" }
                    finally { saving = false }
                }
            }) { Text(if (saving) "Kaydediliyor…" else "Kaydet") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Kapat") } },
    )
}
