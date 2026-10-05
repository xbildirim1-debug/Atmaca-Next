# Buse 1.3 — yavaş tarama ve 1/20 erken bitiş düzeltmesi

2026-10-06T00:44:28.775078+03:00

Kullanıcı 1.2’nin aşağı kaydırdığını fakat çok yavaş olduğunu; limit 20 iken bir çıkıştan sonra Buse’ye döndüğünü bildirdi. Bu deneme için yeni video/erişilebilirlik günlüğü yok. Kodda iki somut sorun bulundu: buseStableSince işlem dokunuşunda tutuluyor, onay/sonuç beklerken 1,5 saniye geçince aynı görünüm liste sonu olarak kabul edilip hiç yeni kaydırma yapılmadan finishCycleOrTask çağrılabiliyor. Ortak finishCycleOrTask UNFOLLOW eksik limitini engellemiyor; repeat=1 için 1/20 COMPLETED yapıp kuyruk Buse’ye dönebiliyor. Ayrıca Bitti kaydı UI ve kuyruğun Başlat yolunu kapatıyor.

BuseScrollBoundary zaman yerine üç ayrı başarılı drag/release ve sonrasındaki taze kararlı geometrinin hiç ilerlememesini izler. Bekleme/tekrar okuma tek başına sınır değildir. Aday işlem ve doğrulanmış sonuç eski sınır kanıtını temizler. UNFOLLOW limit dolmadan finish çağrısı PAUSED ve gerçek ilerlemeyle kalır; hayali tamamlanma veya sonraki tura geçiş yok. Normal Takipten Çıkma için de aynı tamamlanma koruması geçerlidir. BuseRepository.discardInterruptedWork açılışında BuseTaskPolicy.completionNeedsRecovery ile eski geçerli COMPLETED fakat eksik görevler PAUSED’ye döner; progress, lastTarget, lastActionAt ve günlük kullanım korunur. Kullanıcı Başlat ile devam eder, güncelleme otomatik işlem yapmaz.

Tarama hızında 1–1,5 kısa satır adımı yerine %72 görünüm ilerlemesi kullanılır; son iki tam görünür satır eski pencereyle örtüşür. Tek/iki satır görünümünde daha kısa adım korunur. 22 ms hareket + 120 ms sabit bırakma aynı; kararlı görünüm okumaları 16 ms, kanıt penceresi 32 ms. İlk 100 kişi/101’den devam, iki tam okuma ve doğru kişinin Seni takip ediyor/Follows you etiketi korunur.

BuseFollowingInspector her canlı düğümü bir kez yakalar; ebeveyn/çocuk indeksleri, alt ağaç kişi kümeleri, sınır/etiket kanıtı yerel taze NodeSnapshot verisinden hesaplanır. Önceki her kişi için dokuz üst-alt ağaç dolaşımı ve etiket/koordinat için tüm ağacın tekrar okunması kaldırılır. Aynı yakalanmış snapshot listesi kaydırma geometrisine verilir. Çok kimlikli, kırpılmış, isimsiz, eksik/büyük ağaç ve komşu etiket korumaları testlenmiştir. Bu toplam hızın cihazda ölçüldüğü iddiası değildir.

29 yeni anlamlı regresyonla 561 test geçti; 0 başarısız/hata/atlanan. recordSuccess gerçek özel runtime yolunda 1–19 RUNNING, 20 COMPLETED kontrol edildi; eksik liste bitişi ve tekrar turları da kontrol edildi. Lint 0 hata/fatal, 12 uyarı/1 öneri. Final temiz derleme, APK manifest/imza/CRC başarılı. Fiziksel yeni APK/X kabul testi yapılmadı; cihaz protokolü güncellendi.

VersionCode 4, 1.3-fast-limit; com.buse.mobile/Room/ayar kimlikleri aynı. Önceki Buse üzerine kur; uygulamayı kaldırma. Eski 1/20 Bitti görevi açılışta Duraklatıldı’ya dönüşür, Başlat kalan 19 doğrulanmış sonuca devam edebilir. Özel anahtar kaynak/tam pakete eklenmedi; mevcut özel imza yedeği geçerlidir.

APK SHA256 fb6d0de84b07f1a3815e80e8efc22ad0145b463ae4ca1784c617369e5ac04045
Sertifika SHA256 9bb1488d50538b0b69427f7b49f5cc809b6b715eeb81a1867e33fe930074b7a5
Temel GitHub Buse commit d40ae32f2f08368a8aed74e1d1399ab186211323
Kaynak dal buse/1.3-fast-limit, klasör Buse/
Atmaca main/dosyalar değiştirilmedi.

---

Önceki sürümlerin tarihsel kayıtları:

# Buse 1.2 — 100 kişi ve kaydırma sayımı düzeltmesi

2026-10-06T00:24:11.577847+03:00

Kullanıcı ilk 200 yerine ilk 100 kişiyi korumayı istedi ve 1.1'de hızlı kaydırma sonrası duraksamayı 1000033462.mp4 ile gösterdi. Video izlendi. Liste birkaç hareketten sonra uzun süre aynı yerde kalıyor; hata mesajı/erişilebilirlik ağacı veya günlük olmadığı için kesin cihaz nedeni doğrulanmadı. Kodda 22 ms hareketin tamamlanması X kaymasının bitmesi gibi kabul ediliyor, 25 ms sonra veya daha erken olay tick'iyle yeniden kaydırılabiliyor; sayım sırasında ara görünümde ortak kişi kaybolursa ilk 200 koruma denetimi hemen duraklatıyordu.

Yeni PROTECTED_COUNT 100; 100. kişi korunur, 101 ve sonrası adaydır. Görev açıklamaları, tüm ilgili çalışma mesajları, erişilebilirlik açıklaması, README/AGENTS/cihaz protokolü ve 100/101 sınır regresyonları güncellendi. Normal Takipten Çıkma, hesap ekleme, pembe UI ve veri biçimleri korunur.

BuseFollowingScroller 22 ms drag sırasında teması sürdürür; aynı parmak yerini 120 ms sabit tutan continueStroke ile bırakır. Amaç hareket sonunda ataletsel fırlamayı azaltmak. Hareketin ve bırakmanın callback'leri tamamlanmadan worker devam etmez. BuseViewportGate her kaydırmadan sonra 32 ms güncel okumalarla 64 ms aynı kullanıcı/koordinat geometrisini arar. Hareketli/eksik/geçici ayrık ekran sayılmaz veya hemen sıra kopması gibi ele alınmaz; yeni kaydırma da gönderilmez. 2 saniyede durulmayan liste duraklatılır. Örtüşme gerçekten kaybolmuşsa ilk 100 kişiyi tahmin ederek işlem yapılmaz. İlk liste başı ve sonu 1,5 saniye kanıtı, kişinin kendi etiketi, iki tam satır okuması, onay ve sonuç doğrulaması korunur. Genel hızlı preset 7/25/21 ms ve /70 aynı; gerçek cihaz toplam hızına 10× garanti yok.

8 yeni sayım/kararlılık regresyonuyla 532 test geçti, 0 başarısız/hata/atlanan. Lint 0 hata/fatal, 12 uyarı/1 öneri. APK manifest/imza/CRC doğrulandı; yeni APK ile fiziksel X denemesi yapılmadı.

VersionCode 3, 1.2-fast100; com.buse.mobile ve Room/ayar kimlikleri aynı. Önceki Buse üzerine güncelleme kurulabilir; mevcut hesap/görev kaydı korunur. Aynı özel anahtar kullanıldı, kaynak/tam pakete özel anahtar konmadı; önceki özel imza yedeği geçerlidir.

APK SHA256 a86500b49ec3f979e70be8bfda1f4f6c5628688ea31c8cc0e3460ee8afc20977
Sertifika SHA256 9bb1488d50538b0b69427f7b49f5cc809b6b715eeb81a1867e33fe930074b7a5
Temel GitHub Buse commit 53a69526c6338e25a2ea1befe13b1dc829f8bc34
Kaynak dal buse/1.2-fast100, klasör Buse/
Atmaca main/dosyalar değiştirilmedi.

---

Önceki sürümlerin tarihsel kayıtları:

# Buse 1.1 — yalnız hız artışı

2026-10-06T00:09:48.403417+03:00

Kullanıcı Buse 1.0'da kaydırma/işlem hızını yavaş buldu, yalnız hızı 10× artırıp yeni APK istedi. Görev/hesap/200 kişi/etiket/sonuç algoritmaları değiştirilmedi. Pembe arayüzde yalnız hız etiketi güncellendi.

220 ms Buse kaydırması 22 ms; 250 ms arası 25 ms; 200 ms iki satır okuması arası 20 ms. Normal listenin 420 ms hareketi 42 ms; 60 ms dokunuş 6 ms. Varsayılan işlem/hesap/kuyruk arası 71/257/214→7/25/21 ms. Genel nominal gecikme /7 yerine /70, ekran okuma tabanı 16 ms. Daha önce kaydedilmiş yavaş süreler yeni buse_timing_11_saved anahtarıyla bu hız ayarına geçer. Ayar sınırları yeni preset minimumlarını kabul eder. Liste başı/sonu kararlılığı 1,5 saniye, kimlik/onay/sonuç/deneme/rate-limit kuralları korunur. X yükleme/ağ ve cihaz süresi 10× ölçülmüş kabul edilmez.

VersionCode 2, 1.1-fast10; aynı com.buse.mobile ve aynı Room/ayar dosyası. APK Buse 1.0 sertifikasıyla aynıdır ve Buse 1.0 üzerine güncellenebilir. Özel anahtar kaynağa/tam pakete eklenmez; mevcut özel 1.0 imza yedeği geçerlidir.

524 test geçti; yeni preset için mevcut üç hız regresyonunun beklentileri güncellendi, 42 Buse kişi/etiket/sınır regresyonu korundu. Lint 0 hata/fatal, 15 uyarı/1 öneri. Artımlı BuseApp derleme önbelleği hatası temiz derlemeyle giderildi. APK imza/manifest/CRC doğrulandı. Fiziksel yeni APK/X denemesi yapılmadı.

APK SHA256 365d9ec23242972fa3e6b4619ac2d65d764f8ac157cd2f2d149a563270190121
Sertifika SHA256 9bb1488d50538b0b69427f7b49f5cc809b6b715eeb81a1867e33fe930074b7a5
Temel GitHub Buse commit 968939024320e866936841b20fe02906f5981fd6
Kaynak dal buse/1.1-fast10, klasör Buse/
Atmaca main/dosyalar değiştirilmedi.

---

Önceki sürümün tarihsel kaydı:

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
