package com.atmacanext.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * CP15: Dashboard'dan Analiz'e taşınan üç bölüm.
 * Data objects intentionally UI-only; callers bind current project state.
 */
data class EngineStatusUi(
    val accessibilityConnected: Boolean,
    val activeScreen: String,
    val engineMessage: String,
    val queueStatus: String,
    val completed: Int,
    val total: Int,
    val skipped: Int,
)

data class ScheduledAutomationUi(
    val enabled: Boolean,
    val intervalMinutes: Int,
    val repeatCount: Int,
    val nextRunLabel: String?,
)

data class PersistenceUi(
    val activeTaskId: String?,
    val username: String?,
    val progress: Int,
    val limit: Int,
    val lastCheckpoint: String?,
)

@Composable
fun AnalysisOperationsSection(
    engine: EngineStatusUi,
    schedule: ScheduledAutomationUi,
    persistence: PersistenceUi,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AtmacaCard {
            SectionTitle("Motor Durumu")
            Spacer(Modifier.height(8.dp))
            StatusPill(
                if (engine.accessibilityConnected) "Erişilebilirlik aktif"
                else "Erişilebilirlik kapalı",
                engine.accessibilityConnected,
            )
            Spacer(Modifier.height(6.dp))
            Text("Ekran: ${engine.activeScreen}", fontSize = 11.sp)
            Text("Motor: ${engine.engineMessage}", fontSize = 11.sp)
            Text(
                "Kuyruk: ${engine.queueStatus} • ${engine.completed}/${engine.total} • ${engine.skipped} atlandı",
                fontSize = 11.sp,
            )
        }

        AtmacaCard {
            SectionTitle("Saatli Otomasyon")
            Spacer(Modifier.height(8.dp))
            Text(
                if (schedule.enabled) "Aktif" else "Kapalı",
                fontWeight = FontWeight.SemiBold,
            )
            Text("Döngü: ${schedule.intervalMinutes} dk", fontSize = 11.sp)
            Text("Tekrar: ${schedule.repeatCount}", fontSize = 11.sp)
            schedule.nextRunLabel?.let {
                Text("Sonraki çalışma: $it", fontSize = 11.sp)
            }
        }

        AtmacaCard {
            SectionTitle("Kalıcı Kayıt Sistemi")
            Spacer(Modifier.height(8.dp))
            if (persistence.activeTaskId == null) {
                Text("Aktif checkpoint yok", fontSize = 11.sp)
            } else {
                Text(
                    "${persistence.username.orEmpty()} • ${persistence.progress}/${persistence.limit}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                )
                persistence.lastCheckpoint?.let {
                    Text("Checkpoint: $it", fontSize = 11.sp)
                }
            }
        }
    }
}
