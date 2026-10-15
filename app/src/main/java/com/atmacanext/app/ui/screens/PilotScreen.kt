package com.atmacanext.app.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmacanext.app.automation.AutomationController
import com.atmacanext.app.core.AppServices
import com.atmacanext.app.data.settings.AppSettings
import com.atmacanext.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
private fun PilotPhoto(path: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val size = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, size)
                var sample = 1
                while (maxOf(size.outWidth, size.outHeight) / sample > 480) sample *= 2
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }.getOrNull()
        }
    }
    bitmap?.let { Image(it, "Eklenen resim", modifier.heightIn(max = 260.dp), contentScale = ContentScale.Fit) }
}

@Composable
fun PilotScreen(modifier: Modifier = Modifier) {
    val runtime by AutomationController.state.collectAsStateWithLifecycle()
    val chat by AppServices.pilotChat.state.collectAsStateWithLifecycle()
    val settings by AppServices.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    var draft by remember { mutableStateOf("") }
    val list = rememberLazyListState()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(AppServices.pilotChat::attachImage)
    }
    LaunchedEffect(chat.lines.size) { if (chat.lines.isNotEmpty()) list.animateScrollToItem(chat.lines.lastIndex) }
    Column(modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text("Pilot", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(if (settings.pilotEnabled) "Görev denetimi açık" else "Hızlı görev denetimi kapalı", color = TextSecondary, fontSize = 12.sp)
        Text("${runtime.status} · Tweet: ${chat.poolCount} · Hazır yorum: ${chat.replyCount}", color = TextSecondary, fontSize = 12.sp)
        chat.generation?.let { job ->
            Text("${job.pool.title}: ${job.saved}/${job.requested} kaydedildi", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            if (job.active) {
                LinearProgressIndicator(progress = { job.saved.toFloat() / job.requested.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { AppServices.pilotChat.stopGeneration() }) { Text("Üretimi durdur") }
            }
        }
        LazyColumn(Modifier.weight(1f).padding(vertical = 8.dp), state = list, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(chat.lines) { line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (line.mine) Arrangement.End else Arrangement.Start) {
                    val colors = PilotBubbleColors.current(line.mine)
                    Surface(color = colors.first, contentColor = colors.second,
                        border = BorderStroke(1.dp, if (line.mine) AtmacaBlue else Divider),
                        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp,
                            bottomStart = if (line.mine) 18.dp else 4.dp, bottomEnd = if (line.mine) 4.dp else 18.dp),
                        modifier = Modifier.fillMaxWidth(.9f).widthIn(max = 560.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(if (line.mine) "Sen" else "Pilot", color = colors.second.copy(alpha = .8f),
                                fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            line.imagePath?.let { PilotPhoto(it, Modifier.fillMaxWidth().padding(vertical = 6.dp)) }
                            SelectionContainer { Text(line.text, color = colors.second, fontSize = 14.sp) }
                        }
                    }
                }
            }
        }
        if (chat.chatting) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pilot düşünüyor…", color = TextSecondary, fontSize = 12.sp)
            }
        }
        if (chat.imageLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        chat.pendingImage?.let { selected ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                PilotPhoto(selected.path, Modifier.height(96.dp).weight(1f))
                IconButton(onClick = { AppServices.pilotChat.clearImage() }) { Icon(Icons.Filled.Close, "Resmi kaldır") }
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            IconButton(onClick = { photoPicker.launch("image/*") }, enabled = !chat.imageLoading) {
                Icon(Icons.Filled.Add, "Resim ekle", tint = AtmacaBlue)
            }
            OutlinedTextField(value = draft, onValueChange = { draft = it.take(4_000) },
                modifier = Modifier.weight(1f), minLines = 1, maxLines = 5)
            IconButton(enabled = !chat.imageLoading && (draft.isNotBlank() || chat.pendingImage != null), onClick = {
                val text = draft; draft = ""; AppServices.pilotChat.send(text)
            }) { Icon(Icons.Filled.Send, "Gönder", tint = AtmacaBlue) }
        }
    }
}
