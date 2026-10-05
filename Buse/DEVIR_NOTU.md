# Buse 1.0 devir notu

Tamamlanma: 2026-10-05T23:50:39.711651+03:00

Kullanıcı Atmaca'yı değiştirmeden ayrı Buse uygulaması istedi. Pembe tasarım, yalnız Hesaplar/Görevler, normal Takipten Çıkma ve ilk 200 kişiyi koruyan Takip Etmeyenleri Çıkma eklendi. En hızlı 7× ayarı varsayılan tutuldu.

Atmaca 26.59 kaynağı `8b467a3baaa991abaa3202dca78ff9299ad1c2c0` ayrı projeye kopyalandı. com.buse.mobile paket kimliği, com.buse.app kod alanı, BuseApplication/BuseAccessibilityService, buse_next.db, buse_settings, buse bildirim/intent isimleri ve pembe B simgesi kullanılır. Atmaca dosyaları ve main değişmedi.

Yeni arayüz BuseApp.kt ve pembe tema dosyalarındadır. Eski çok ekranlı Atmaca arayüzü kopyalanmadı. BuseTaskPolicy yalnız iki çıkış türünü kabul eder; normal UNFOLLOW ve kalıcı içerik alanındaki buse:non-followers:v1 işaretçisi iki türü ayırır. BuseRepository ve TaskOrchestrator kabul filtresi diğer görevleri engeller. Ortak motorun önceki testlenmiş yardımcıları kaynakta korunmuştur.

BuseFollowingRun ilk 200 farklı @kullanıcı adını sayar. Örtüşen ekranlar aynı kişiyi yeniden saymaz; 200 sınırından önce atlanan veya ters sıradaki ekran duraklatır. BuseFollowingInspector kişinin tek kimlikli tam satırını kullanır. Seni takip ediyor/Follows you etiketi satır alt ağacından ve aynı kişiye ait üstteki kardeş etiketten alınır; komşu kişiye taşınmaz. Kırpılmış, görünmeyen, disabled, eksik ağacın satırı veya ilişki düğmesi belirsiz kişi çıkarılmaz. Eski X listesi konumunda 200 saymaya başlamadan önce üst sınıra dönülür. 201 ve sonrası aşağı yönde işlenir; liste sonunda yukarı dönüş yok. Liste kısa veya aday az ise sayaç gerçekleşen işlem sayısını korur.

220 ms kısa kaydırma ekranlar arasında kişi örtüşmesini korur. 200 ms arayla iki güncel tam satır okuması yokluğu doğrular. X onayı ve Takip et sonucunun kanıtı ortak 26.59 akışında korunur. Görev/durdur/ekran kilidi/rate-limit/belirsiz sonuç korumaları ve çok hesaplı tur sırası korunur. 7×, X'in ağ ve yükleme süresinin yedi kat hızlandığı garantisi değildir.

İlk test koşusunda büyük harfli Türkçe etiket ve birleşik satır açıklaması regresyonları başarısız oldu. Unicode normalizasyonu ve satır içindeki etiket tespiti düzeltildi. Bir ara artımlı derlemede BuseApp overload hatası temiz derlemeyle giderildi. Lint'te Android 26 için uyumsuz windowLightNavigationBar tema özniteliği kaldırıldı; açık sistem çubuğu davranışını Activity uyumluluk API'si yönetir. Son doğrulama: 524 test/42 yeni Buse testi geçti, 0 test başarısızlığı/hatası/atlama; lint 0 hata/fatal, 12 uyarı ve 1 öneri; assemble, APK imza/manifest/CRC başarılı.

Sentetik testler gerçek X ekran ağacı değildir. Fiziksel Android/X testleri yapılmadı. DEVICE_TEST_PROTOCOL.md adımları kalır. Özellikle X'in farklı sürümde satır/badge ağaç gruplaması ve gerçek listedeki 200. kişi geçişi cihazda kontrol edilmeli. Belirsiz satırlar atlanır; 524 test sonucu gerçek telefonda işlem başarısı iddiası değildir.

APK SHA256 63d700071bb9ef519e0be1b52401d39777fc4758be4f3737756522d6eec95623
İmza sertifikası SHA256 9bb1488d50538b0b69427f7b49f5cc809b6b715eeb81a1867e33fe930074b7a5
Atmaca ana kaynak 8b467a3baaa991abaa3202dca78ff9299ad1c2c0
GitHub Buse dalı buse/1.0, klasör Buse/
