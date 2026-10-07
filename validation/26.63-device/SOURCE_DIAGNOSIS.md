# 26.63 kaynak tanısı

Kullanıcının başka yapay zekâdan aktardığı öneri: ana detail yazarı FeedRowEvidence tarafından zamanlı satır sayılıp CommentDetailEvidence TIMED_INDEX korumasında null dönüyor. Bu kaynak yolu mümkündür; mevcut08:30 parser'ı ve tam tarih/görüntülenme satırı ise null döner. Kesin yeni cihaz ağacı/logu alınmadığı için gerçek telefonun tek nedeni diye gösterilmez.

QuoteDetailAuthor26_63Test ayrı7dk etiketi ve büyük handle bounds ile FeedRowEvidence içinde ana headerIndex=1 oluştuğunu, varsayılan sıkı header'ın null döndüğünü ve ölçülen ana bant içindeki false yolun doğru yazar/metinle devam ettiğini kontrol eder. Diğer testler farklı yazar/gönderi, eksik ana yazar, alt yorumun eş metni, kısaltılmış handle, sınırsız zamanlı satır, metin içi mention, büyük parent ve taze yazma kanıtını kapsar. Bu düzenler temsili regresyondur; gerçek telefon dökümü değildir.

ReplyFallback26_63Test metni değiştirmeden true dönen native bağlantı/PASTE senaryolarını ve geç gelen metin/duble yazmama davranışını kapsar. Varsayılan yorumcu takip seçimi korunur; yeni APK/CI sonucu ve gerçek cihaz sonucu ayrı kaydedilir.
