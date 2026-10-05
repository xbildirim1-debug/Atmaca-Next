# Buse 1.3 — hız ve gerçek işlem limiti

- Temiz yerel testDebugUnitTest, lintDebug ve assembleDebug başarılı. 561 test, 0 başarısız/hata/atlanan; lint 0 hata/fatal, 12 uyarı, 1 öneri.
- 29 yeni regresyon: üç gerçek kaydırma ile baş/son kanıtı, bekleme zamanının liste sonu sayılmaması, eski kaydırma kanıtının işlemden sonra silinmesi, daha büyük adımda gerçek satır örtüşmesi, tek ağaçta kişi/etiket/komşu/kırpılma/truncation, gerçek recordSuccess akışında 1–19 RUNNING / 20 COMPLETED ve eski eksik COMPLETED kaydı kurtarma.
- UNFOLLOW limiti bitmeden COMPLETED olmaz. Limit 20, sonuç 1 olduğunda bitiş isteği PAUSED/1/20 olarak kalır; tüm 20 doğrulanınca tamamlanır. Gerçek liste sonunda yeterli uygun kişi yoksa tamamlanmış gibi gösterilmez.
- BuseScrollBoundary yalnız tamamlanıp bırakılmış ve sonra taze kararlı görünümde aynı kalan üç kaydırmayı sınır sayar. Onay/sonuç beklemesinin süresi artık sınır üretmez. Aday işlem ve sonuç doğrulamasından sonra eski kanıt temizlenir.
- Daha büyük adım: görünümün en fazla %72’si, en az iki tam eski satır örtüşmesi. 22 ms drag + 120 ms bırakma aynı; viewport kararlılığı 16 ms okumalarla 32 ms. Tek taze ağaç yakalanır; satır/üst satır/etiket kontrolleri yerel indekslerden yapılır, aynı ağaç kaydırma geometrisinde yeniden kullanılır.
- Önceki Buse’nin COMPLETED ama progress<totalLimit kaydettiği geçerli çıkış görevleri açılışta PAUSED olarak düzeltilir; progress/lastTarget/lastActionAt ve günlük kullanım sayısı korunur. Başlat yeniden kullanılabilir; güncelleme kendi başına görevi çalıştırmaz.
- İlk 100 kişi ve kendi Seni takip ediyor/Follows you etiketi korunur. İki tam satır okuması, kimlik, onay, sonuç, deneme bütçesi ve X hata kontrolleri aynı.
- APK: com.buse.mobile / versionCode 4 / versionName 1.3-fast-limit. Önceki Buse ile aynı imza; üzerine güncelleme kurulabilir. Room/ayar/veri kimlikleri ve şema aynı.
- Yeni APK fiziksel Android/X’te denenmedi; daha büyük adım ve daha az ağaç okumasının cihazdaki toplam hız etkisi ölçülmedi. Kullanıcı raporu 1.2’de yavaş kaydırma ve 20 seçilmesine rağmen 1 çıkıştan sonra Buse’ye dönüş; yeni video/günlük yok.
- APK SHA256: fb6d0de84b07f1a3815e80e8efc22ad0145b463ae4ca1784c617369e5ac04045
- Sertifika SHA256: 9bb1488d50538b0b69427f7b49f5cc809b6b715eeb81a1867e33fe930074b7a5
- Atmaca değiştirilmedi; ayrı buse/1.3-fast-limit dalı, Buse/ klasörü. Raporlar validation/1.3-local/.
