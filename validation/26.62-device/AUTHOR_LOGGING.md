# Detail yazar okuması — 7 Ekim 2026 tanı eklemesi

09:01 Europe/Istanbul kullanıcı isteği: TweetContentEvidence.header(), FeedRowEvidence.rows(), XUiVocabulary.normalize() kodlarını ver; CommentDetailEvidence.header() içinde title bulundu mu, candidates sayısı ve döngü çıkış adımı için 2–3 Log.d ekle.

CommentDetailEvidence.kt içine android.util.Log ve üç Log.d çağrı noktası eklendi. Tag AtmacaDetailHeader. titleFound/titleIndex/titleBounds/nodeCount; candidates ve timedHeaders indeksleri; çıkış reason/step/nodeIndex/handle/viewId/bounds kaydedilir. Çıkış nedenleri NO_TITLE, TIMED_INDEX, TIMED_LABEL, AUTHOR_FOUND, NO_HANDLE. Döngü step değeri 1 tabanlı; NO_TITLE için 0. Title yoksa candidates aşaması çalışmaz; title ve çıkış olmak üzere iki satır, normal çağrıda üç satır oluşur.

Başlık seçme, aday sırası, timed header reddi, kullanıcı adı parse etme ve dönen Header/null değerleri korunur. Normalizasyon veya yazar okuma hatası bu tur düzeltilmedi; amaç kullanıcının teşhis için istediği kayıtları eklemektir. Log.d Android Logcat'e yazılır; filtre: adb logcat -s AtmacaDetailHeader:D '*:S'. Bu kaynakla APK yeniden derlenmeden önceki telefondaki APK bu yeni kayıtları üretemez.

Locale.ROOT çıktısı yerel JVM17 üzerinde Java String.toLowerCase(Locale.ROOT) ile çalıştırıldı: GÖNDERİ -> gönderi + U+0307; I -> i; İ -> i + U+0307; ı -> ı. Tam kod noktası çıktısı NORMALIZE_ROOT.txt içinde. Resmi Kotlin API: https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/lowercase.html .

Kaynak başlangıcı main 4f752ad39faf0c21799f0ad839493322b71858d9 / tree 4dfed17bac448d1944e67d07052b29f7ee29af17. Bu yalnız tanı kaynağı eklemesidir; sürüm110/26.62 ve mevcut APK dosyaları değişmedi. Yeni APK derlenmedi veya teslim edilmedi. Log eklemesi için yeni birim test yazılmadı; git diff --check geçti. Önceki643 test sonucu tarihsel derlemeye aittir; yeni kaynakta CI veya fiziksel X testi yapıldığı iddia edilmez. Kullanıcının aynı yerde takılma raporu açık kalır. Başlangıç çalışma dalı fix/26.62-inline-reply/937ff8429112717e49a6ef42ea9466f41006fd5d ve birleşmiş PR #11 korunur; tanı kaynakları main'e kaydedilir. Buse değişmedi.
