## 26.19 — doğrulanmış derleme ve doğrudan APK teslimi

- Kaynak main commit: 17c54a5e4100b36ac6be11776e6d883123279894; CI 34276417091 başarıyla tamamlandı. 208 test; 0 hata, başarısız, atlanan. Lint, assemble, imza ve manifest kontrolü geçti.
- APK: AtmacaNext-26.19.apk, code67 / 26.19-compose-feed-recovery. SHA256 c240b01c4eb8a5957e492098a1f2552b14f768cc68d5a97f616d61d9299cf4ad. TAM-PAKET SHA256 f8512a6913a0a1b70e3771fe077f039557e36d482b645eefc2f2fab61ecfe605. ZIP CRC ve paket içi/dışı APK eşitliği doğrulandı.
- APK/tam paket artifact 10075981654; rapor artifact 10075982546. Kullanıcı APK'yı GitHub bağlantısı yerine doğrudan sohbet dosyası olarak istiyor.
- Fiziksel X cihaz testi yapılmadı. 14 yeni test sentetik düğüm düzenidir; gerçek node ağacı olmadan her X varyantının uyumluluğu garanti edilmez. 26.18 cihaz başarısızlığı geçerlidir.
- Git komut satırında kimlik yoktu; kaynak mevcut yetkili GitHub bağlantısıyla main'e kaydedildi. Yerel 128d245 commit'i ile yayınlanan 17c54a5 kaynak ağacı aynıdır; bu kayıt yalnız belgelerdir.
- Kalıcı imza ve Releases workflow yetki engeli devam ediyor; yetki değişikliği veya kalıcı Releases yayını yapılmadı. APK imzası önceki sürümle uyumsuz olabilir; kaldırma kayıtları siler.

## 26.19 — 23:28 cihaz hatası ve ortak keşif akışı

- 1000026994.mp4 (66,88 sn) ve 8 Eylül 23:27–23:28 logu: hedef açılıyor, 4 sa gönderi atlanıyor, her taramada rows=0. Son ekran BaBaLa TV / Kimi takip etmeli kartıyla hâlâ Gönderiler; FOLLOWING_LIST bir sınıflama hatasıdır. 0/5 COMPLETED hatalıdır. 26.18 cihaz testini geçmedi.
- FeedRowEvidence: ortak parent/etkileşim düğmesi şartı olmadan birleşik veya aynı satırda ayrı yazar+saat alanı; ayrı metin/contentDescription. Yeni gönderi, medya ve öneri sınırını aşmadan metin seçimi. Sabitlenmiş/reklam işaretlerini atlama. SHA256 gönderi anahtarı yazar+metindir; yaş/sayaç/kaydırma değişimi aynı gönderiyi yeniden işlemeye neden olmaz. Ham cihaz node ağacı yok; bu yol sentetik düzen testleriyle sınanır.
- ScreenDetector: Gönderiler + gönderi sayısı/yazar-saat kanıtı öneri takip etiketlerinden önce gelir. Yorum yazma/alıntıları görüntüle kanıtı yorumları ilişki listesi sanmayı önler.
- Runtime: eksik keşif limiti artık COMPLETED olamaz; ilerleme korunarak PAUSED. Gönderi ancak detay açılınca işlenmiş sayılır. Başarısız yorumcu dokunuşunda kör Geri yok. Daralmış hedef profil başlığında feed/yazar kanıtıyla dönüş kabul edilir. Sonucu belirsiz takip yerine başka kullanıcı işlenmez; Follow ile Following/Requested çelişkisi başarı değildir. Yeni DISCOVERY_READ sınırlı, parola/girdi dışı ekran kanıtı kaydeder; tarama 180 sn bütçelidir.
- 14 yeni birim testi: ayrı/birleşik başlık, eksik aksiyonlar, açıklama metni, yanlış satır yaşı, body mention, medya, öneri, komşu gönderi sınırı, sabit anahtar, gizli/sabitlenmiş içerik, ekran sınıflama, ölçek.
- versionCode67 / 26.19-compose-feed-recovery. Derleme ve test sonuçları henüz bekleniyor; gerçek X cihaz testi yapılmadı. Kaynak/testler main'e gönderilecek. Kullanıcı doğrudan sohbetten APK teslimi istiyor.
- Kalıcı imza ve önceden reddedilen Releases workflow yetkisi değiştirilmedi. Yeni debug imza önceki APK üzerine kurulumla uyumsuz olabilir; kaldırma veri kaybıdır.

## 26.18 — Gönderiler listesinde dikey kaydırma ve metinden gönderi açma

- Kullanıcı 26.17 hedef aramasının fiziksel cihazda çalıştığını doğruladı. 1000026954.mp4 ve 8 Eylül 19:15 logu incelendi: hedeften sonra Yanıtlar, ardından Videolar sekmesine kayılıyor. Kodun genel ACTION_SCROLL_FORWARD seçimi profilin yatay pager'ını ilerletiyordu; istenen aşağı kaydırma gerçekleşmiyordu.
- Yorumcu/retweetçi keşif kaydırmaları artık yalnız dikey ListGesture kullanır. Profilde açık Gönderiler sekmesine ekstra giriş yok. 850 ms yerleşme süresiyle okunur. Çalışmayan üç deep-link denemesi başlangıçtan çıkarıldı; doğrudan cihazda doğrulanan X içi arama kullanılır.
- XTweetInspector tek satırdaki isim/@handle/2 sa başlığını okur. Kartın genel tıklaması kaldırıldı: gönderinin metin düğümüne gesture tap yapılır, video/medya/aksiyon düğmeleri seçilmez. Uygun gönderi metni bulunamazsa yanlış yere basmak yerine açık nedenle duraklar. Yorumcu yazarının birleşik başlığı da tıklanabilir. Birden fazla yazar başlığı içeren liste kapsayıcısı gönderi sayılmaz.
- Kaydırılmış yorum ekranı Yanıtını gönder + Alıntıları görüntüle kanıtıyla tanınır. Retweetçi akışı Alıntıları görüntüle görünene kadar aşağı ilerler, sonra seçili ve sayısı değişken yeniden gönderenler sekmesini doğrular.
- Görev toplam limiti dolunca durur; gönderinin adayları yetmezse aynı hedefte alttaki en az 120 dakikalık gönderiye geçer. Tek hedef, işlem/kimlik doğrulaması ve Beklemede sayımı korunur.
- DISCOVERY_SCAN görünür gönderi/saat/kaydırma; DISCOVERY_TWEET seçilen gönderi/metin kanıtını kaydeder.
- Yeni 9 regresyon testi: TR/EN birleşik başlık, yaş sınırı, gövde bahsetmesi reddi, metin/medya seçimi, gizli düğüm, kaydırılmış yorum ekranı ve 66 değişken sekme etiketi.
- code66 / 26.18-vertical-discovery-feed. CI sonucu henüz bekleniyor; fiziksel yeni sürüm testi yapılmadı. Kullanıcının main gönderimi ve APK derleme onayı geçerlidir. Kalıcı imza ve GitHub Releases workflow yetkisi değiştirilmedi.

## 26.17 — başarılı derleme ve teslim

- Kullanıcı main gönderimini açıkça onayladı. Kaynak main commit: 4865258e0d6f4de5fd9b8f240f5165ff32e241ca. CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34245948585 . Önceki gönderim engeli bu açık onayla aşılmıştır.
- 185 test geçti; 0 hata, başarısız ve atlanan. Test/lint/assemble/apksigner/manifest/paketleme başarılı. Yedi yeni test sorgu alanının sonuç sayılmamasını, tam kullanıcı adı eşleşmesini, profil içi arama simgesinin reddini, TR/EN ve farklı ekran ölçeklerini kapsar.
- APK: AtmacaNext-26.17.apk, code65 / 26.17-search-profile-navigation, 20.573.920 bayt. SHA256 64b4841567f214ff071f904ea846e2e52a018f564b448d6c6f3429c0e05303d8.
- TAM-PAKET.zip SHA256 146e22829d68a4d728fda494e04e2e34b8fa8f63a001bd2c8c3f5f9c3deff7d9. ZIP CRC, paket içi/dışı APK eşitliği, manifest sürümü/INTERNET izni yokluğu ve test XML toplamları doğrulandı.
- APK/tam paket artifact 10064098624; rapor artifact 10064099463. Actions çıktıları 7 Aralık 2026'da sona erer. Kalıcı Releases workflow'unun yetkilendirilmesi bu onayın kapsamına dahil edilmedi; kalıcı GitHub Releases yayını yapılmadı.
- Fiziksel X cihaz testi yapılmadı. Beklenen yeni kayıt: DISCOVERY_SEARCH target=@... action=open-search-tab/set-query/open-exact-result ve ardından hedef profil doğrulaması. Hedef bulunamazsa 45 saniyede duraklar, başarı yazmaz.
- Kalıcı imza çözülmedi; farklı debug sertifikası nedeniyle önceki APK üzerine kurulum reddedilebilir. Kaldırma yerel verileri siler.

## 26.17 — gönderim engeli ve doğrulama durumu

- Yerel değişiklik hazırlandı; GitHub main 6eb4b6fadd8de6f3094e68ce03c247321d3762bf üzerinde kaldı.
- Otomatik onay incelemesi git push HEAD:main işlemini, herkese açık main dalına kod/not yayını için açık kullanıcı onayı bulunmadığı gerekçesiyle reddetti. Alternatif yayın yolu denenmedi. Bu hazır 26.17 düzeltmesini gönderip mevcut CI derlemesini başlatmak için kullanıcıdan açık onay bekleniyor.
- Yedi yeni seçici regresyon testi yazıldı. Yerelde Gradle/Android SDK bulunmadığından testler ve APK derlemesi çalıştırılamadı; başarı iddia edilmez. Git diff biçim kontrolü yapıldı. Fiziksel cihaz testi yapılmadı.
- Yeni APK üretilmedi. main ile yerel fark, arama gezinmesi + sürüm/paket adları + testler + devam notlarıdır. Mevcut workflow contents:read olarak korunur; Releases otomasyon yetkisi eklenmedi.

## 26.17 — X içi hedef profil araması

- 8 Eylül 14:46 logu ve 1000026873.mp4: görev hesabı bildirimhaber1 doğrulanıyor; pusholder hedefi doğru okunmasına rağmen 26.16 native-user/twitter-web/x-web yollarının üçünde de kendi profilinde kalıyor. startActivity kabulü hedefin açılması değildir; Android/X seviyesindeki kesin neden bu kanıtla belirlenemez.
- Üç bağlantı sonucu hedef doğrulanmazsa veya bağlantı başlatılamazsa SEARCH_DISCOVERY_TARGET başlar. Kendi profilinden Geri ile alt gezinmeye, genel Ara sekmesine, arama alanına ve tam @hedef önerisine gider. Profilin üstteki kendi gönderilerinde arama simgesi kullanılmaz. Öneri gelmezse desteklenen cihazlarda IME araması bir kez gönderilir ve yalnız seçili Kişiler sekmesinde sonuç seçilir.
- Hedef profil başlığı doğrulanmadan gönderi taraması başlamaz. Sorgu alanı, kısmi kullanıcı adı, metin içi bahsetme ve takip düğmeleri sonuç sayılmaz. 45 saniyede hedef doğrulanmazsa görev tamamlanmış sayılmadan duraklar. DISCOVERY_SEARCH adım/eylem/kabul/ekran kayıtları eklendi.
- Değişiklikler: AutomationRuntime.kt, yeni DiscoverySearchSelector.kt ve DiscoverySearchSelectorTest.kt, sürüm code65 / 26.17-search-profile-navigation, mevcut salt-okuma build workflow paket adları.
- Tek hedef, en az 120 dakikalık hedef gönderileri, takip limitleri, Beklemede sayımı ve hesap senkronizasyonu korunur.
- Derleme/test sonucu henüz bekleniyor. Gerçek telefonda yeni arama yolu test edilmedi; video 26.16 hatasının kanıtıdır.
- Kalıcı imza çözülmedi. GitHub Releases contents:write workflow için önceki otomatik onay reddi nedeniyle yetki değişikliği yapılmadı; teslim arşivi durumu ayrıca kaydedilecek.

## 26.15 — doğrulanmış derleme sonucu

- Kaynak commit 90751a59ef7bae35dd24d392a2620218c787479f; başarılı CI https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34215844253 . APK/tam paket artifact 10051836401, rapor artifact 10051837267.
- 177 test; 0 başarısız/hata/atlanan. Lint, assemble, apksigner, manifest ve paketleme başarılı. APK code63 / 26.15-discovery-target-start, 20.557.536 bayt, SHA256 `3ab983c9ab97d253e7a1e851f3202ec395d7e3933f021c1a018eaa2c76b9338d`.
- Actions tam ZIP SHA256 `f361fb551b2d2f497468ae9eb5e6cdc95a38bcb3f8b3f0e975fcbd7c4e9e57b9`; ZIP CRC ve paket içi APK eşitliği doğrulandı. Manifestte INTERNET/Startup provider yok.
- Fiziksel cihaz testi yapılmadı. Kullanıcının videosu eski 26.14 başlangıç hatasının kanıtıdır; 26.15 hedef yönlendirme tekrarı telefonda ayrıca doğrulanmalıdır. Başarılı başlangıçta logda `DISCOVERY_TARGET ... target=@... attempt=...` ve ardından hedef profil görünmelidir.
- Actions dosyaları 7 Aralık 2026'da sona erer. Releases workflow'u onay engeli nedeniyle açılmadı; kalıcı imza sorunu sürer ve kaldırarak kurulum yerel verileri silebilir.

## 26.15 — yorumcu/retweetçi hedef başlangıcı ve hız

1000026864.mp4 cihaz videosu 26.14'ün retweetçi görevinde doğru görev hesabını doğruladığını, fakat kayıtlı hedefe gitmeden görev hesabının kendi profilinde kaldığını kanıtladı. Neden: `beginOperation`, hedef ACTION_VIEW isteğini kendi profil doğrulama callback'i içinde hemen gönderiyordu; X bu geçişi yuttuğunda OPEN_DISCOVERY_TARGET yalnız altı saniyelik zaman aşımını bekliyordu.

- 26.15 hedef yönlendirmesini ayrı aşamaya aldı: 450 ms sonra tek kayıtlı hedef açılır, tam hedef `@handle` görünene kadar en fazla üç kontrollü deneme yapılır. Denemeler farklı anahtarla gerçekten gönderilir ve `DISCOVERY_TARGET target/attempt/screen` olarak loglanır. Tam hedef kanıtı olmadan tweet veya kullanıcı üzerinde işlem yoktur.
- Varsayılan hızlar: işlemler arası 500 ms, hesap geçişi 1800 ms, görevler arası 1500 ms; runtime tick 250 ms. Kullanıcı ayarındaki işlem alt sınırı 500 ms. Kimlik/sonuç doğrulamaları korunmuştur.
- Yeni `DiscoveryTargetLaunchPolicy` ve üç regresyon testi eklendi. Sürüm code63 / 26.15-discovery-target-start. CI ve cihaz testi henüz yapılmadı; sonuç kaydı sonradan eklenecek.
- 26.14 fiziksel cihazda onaylı kullanıcı takibi çalıştı; yorumcu/retweetçi hedef başlangıcı çalışmadı. Bu yeni düzeltme fiziksel cihazda henüz kanıtlanmadı.
- GitHub Releases `contents:write` workflow'u önceki otomatik onay engeli nedeniyle etkinleştirilmedi. Kaynak main'e ve APK/tam ZIP başarılı Actions çıktısına eklenecek. Kalıcı imza hâlâ açık sorundur.

## 26.14 — doğrulanmış derleme ve teslim sonucu

- APK kaynak commit: cf753f84eb60567cf5ee4e06f94b0848f03023cb. Son üç düzeltme Compose gönderi ayrıntısı tanıma, erişilebilirlik açıklaması dönüşümü ve gövde tarihlerinin gönderi yaşı sayılmamasıdır.
- Başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34210727302 . APK/tam paket artifact 10049890216; rapor artifact 10049891213. 174 test geçti; 0 başarısız, hata veya atlanan. Test, lint, assemble, apksigner ve manifest kontrolleri başarılı.
- AtmacaNext-26.14.apk: versionCode62 / 26.14-engagement-follow, 20557532 bayt. SHA256 4fbba8da1c25feb43b96e44f990cd9a3e6ea85642b27ed3d50aad1c91e14e0da.
- Tam ZIP SHA256 42404ac7286798f5a89533c9fa206361d3d55c1f72c7003b886e031ce68f9c5a. ZIP CRC, paket içi/dışı APK eşitliği, sürüm manifesti, INTERNET izni yokluğu ve test XML toplamları ayrıca doğrulandı.
- Sertifika SHA256 b6786a2083b288229609c1185a5f5532f4499d7f6b72b9441a7102aac9ee32f9; 26.13 imzasından farklı. Üzerine kurulum reddedilebilir; kaldırmak kayıtlı hesap/görev/ayar/bildirimleri siler. Kalıcı imza sorunu sürüyor.
- 26.13 onaylı kullanıcı takibi kullanıcı tarafından fiziksel telefonda başarılı doğrulandı. 26.14'ün Beklemede sayımı, boş kaynak geçişi ve yorumcu/retweetçi akışları fiziksel telefonda henüz doğrulanmadı. 174 test sentetik erişilebilirlik/birim testidir; gerçek X cihaz uyumluluğunu tek başına kanıtlamaz.
- Hesap başına tek hedef nihai kullanıcı talimatıdır. Hedefin yalnız kendi gönderileri arasından yaşı en az 120 dakika olanlar işlenir; yeni, belirsiz yaşlı, sabitlenmiş ve reklam gönderileri atlanır. Değişken yeniden gönderim sayısı yalnız seçili etkileşim sekmesi kanıtı olarak okunur.
- Yeni takip sonrası Beklemede/Requested görülürse işlem bir kez sayılır; önceden Beklemede olan kullanıcı tekrar hedeflenmez. Onaylı kaynakta aday yoksa ilerleme korunarak önceki listeye dönülür ve ziyaret edilmemiş başka kullanıcı açılır.
- Kod/test/notlar main'dedir. APK ve tam ZIP Actions çıktısında ve sohbet teslimindedir. Actions artifact 7 Aralık 2026'da sona erer. Kalıcı Releases yayını önceki contents:write otomatik onay engeli nedeniyle yapılmadı; workflow yetkisi değiştirilmedi.

## 26.14 — Beklemede, boş kaynak, tek hedefli yorumcu/retweetçi takip

Kullanıcı 26.13 onaylı takibin fiziksel telefonda başarılı çalıştığını doğruladı. Bu cihaz doğrulaması önceki test edilmedi kayıtlarından daha günceldir; 26.13 taze erişilebilirlik okuma yolu korunur. Yeni istek: yeni takip sonrası Beklemede/Requested işlem sayılacak, boş onaylı kaynakta başka kullanıcı aranacak; hesap başına tek hedef ve en az 120 dakikalık gönderilerden eskiye ilerleyen yorumcu/retweetçi takip.

- VerifiedFollowPolicy istek gönderildi sonucunu başarı sayar; aynı anda Follow varsa çelişkili sonuç sayılmaz. Önceden Beklemede olan düğme takip adayı olmaz. Başarı processedHandles'a girer, sayaç bir kere artar; sonraki tick tekrar saymaz. İstek gönderme, karşı tarafın onayladığı anlamına gelmez. FOLLOW_REQUEST kaydı bunu belirtir.
- Onaylı kaynak aday havuzu boşsa RETURN_VERIFIED_SOURCE ile önceki profile/listelere geri gidilir. Görünür ziyaret edilmemiş kullanıcı kaynak seçilir; sayaç/işlenmiş kullanıcılar korunur. Onaylı sekmesi olmayan kaynak da atlanır. Ziyaret edilmiş kaynak yeniden seçilmez; 30 saniye içinde erişilebilir aday bulunamazsa kısmi ilerleme korunarak duraklar.
- Tek hedef: yeni hedef politikası en fazla 1. Hedef düzenleme tek alan; Kaydet eski çoklu kaydı bu hedefle değiştirir. Eski çoklu kayıtlar kullanıcı kaydedene kadar silinmez, runtime ilk aktif hedefi kullanır.
- Son beş gönderi sınırı kaldırıldı. Hedef yazar kimliği eşleşen, yaşı en az 120 dakika olan gönderiler işlenir; yaşı bilinmeyen/yeni/sabitlenmiş/reklam gönderileri atlanır. Kaynak biterse Geri ile aynı hedefin daha eski gönderilerine devam edilir, ilerleme sıfırlanmaz. Tarih-only kayıtlar saat bilinmediğinden gün sonuna göre muhafazakâr hesaplanır.
- Retweetçi akışı videodaki Alıntıları görüntüle → Gönderi Etkileşimleri → sayısı değişken '... tarafından yeniden gönderildi' seçili sekmesi. Retweet/Repost eylem düğmesi seçici değildir; seçili sekme doğrulanmadan takip yok. Türkçe/İngilizce adlar ve değişken sayılar için EngagementListEvidence.
- Yorumcu profil geçişi gerçek satırdaki tam kullanıcı adına dokunur; biyografi bahsetmesi kullanılmaz. Gönderinin yazarı ve kendi hesap dışlanır. Profilde header üstündeki düz Follow seçilir; öneri kullanıcılarının düğmesi seçilmez. Following/Beklemede sonucu aynı kimlikte doğrulanır, yorumlara Geri ile dönülür. Yorum ekranında genel Follow düğmelerine doğrudan basılmaz.
- Keşif başlangıcı hedef profile ACTION_VIEW/NEW_TASK ile yönlenir; eski REORDER_TO_FRONT bayrağı kullanılmaz. Sonraki dönüşler profil URL tekrarı yerine Geri kullanır. 26.13 fresh-root okuma yorumcu/retweetçi görevlerinde de etkin; import ve unfollow yolu korunur.
- 9 ek regresyon: değişken yeniden gönderim sayısı, yanlış sekme/eylem reddi, seçili sekme zorunluluğu, Requested sonucu/çelişki/seri sıfırlama, 119-120 dk sınırı ve belirsiz yaş, değişen zaman/etkileşim sayısıyla gönderi anahtarı, etkileşim giriş etiketi. Tek hedef testi yeni kurala güncellendi. CI sonucu bekleniyor.
- Sürüm 26.14-engagement-follow / code62. Bu yeni akışlar fiziksel cihazda çalıştırılmadı; birim testler X'in tüm cihaz varyantlarında uyumluluğu kanıtlamaz. Kalıcı imza ve Releases contents:write önceki otomatik onay engeli devam ediyor.

## 26.13 — doğrulanmış derleme ve teslim

- APK kaynak commit: 79ee50f3be170db16e497ee5193255d9e732a049. Main kaynak kodu korunarak bu sonuç kaydı yalnız belgelere eklenir; APK tekrar derlenmez.
- Başarılı CI https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34189529216 ; APK/tam ZIP artifact 10041766953, rapor artifact 10041767453. 163 test; 0 başarısız, hata, atlanan. Lint/assemble/apksigner/manifest kontrolleri geçti.
- AtmacaNext-26.13.apk versionCode61 / 26.13-fresh-accessibility-tree, 20557536 bayt, SHA256 0c4635e8a33453229ca70dd97e46925eb5a58c87f953636cc5684f60ff40a888.
- Tam ZIP SHA256 e8a9207d312f7e8ce8d3f912ed358f96aa82067f6f3348f2ddd03c42835920fb. ZIP CRC, paket içi APK eşitliği, gerçek manifest sürümü, INTERNET/Startup provider yokluğu ve test XML toplamları ayrıca doğrulandı.
- Sertifika SHA256 c0549c123cf09a09180b1f9e0939154ecc8f5916a3ec6ece7efc21da82bc079a; 26.12'den farklı. Üzerine kurulum reddedilebilir. Kaldırmak kayıtlı hesap, görev, ayar ve bildirimleri siler. Kalıcı imza açık sorundur.
- Cihazda yeni sürüm test edilmedi. 26.12 cihazda takip başlatamamıştır. Yeni sürümün Android cache temizlemesi bu cihazda kanıtlanmadı; UI_FRESH ve VERIFIED_TAB kayıtları kontrol edilmelidir. Ham X ağacı tazelendikten sonra da yanlış sekme veriyorsa başka erişilebilirlik/katman sorunu vardır; seçili sekme doğrulamasını kaldırarak geçilmemeli.
- Taze root yoksa eski paket olayıyla X yeniden açılmaz; 15 saniye sınırla beklenir, sonra duraklar. Ekran yenileme hareketi yok. Başlangıç ve takip sonuçları aynı taze okuma yolunu kullanır.
- Kaynak/test/notlar main'de; APK ve tam ZIP Actions çıktılarında ve sohbet tesliminde. Tam ZIP kaynak, APK, test raporları ve CI sonuçlu not defterini içerir. Actions dosyaları sürelidir. Önceki otomatik onay reddi nedeniyle kalıcı Releases yayını yapılmadı, contents:write yetkisi etkinleştirilmedi.

## 26.13 — ekranda onaylı sekme açıkken eski Followers ağacının okunması

8 Eylül 08:03 logu ve 1000026808.mp4 incelendi. 26.12 kaynak profiline giriyor; yenileme tekrarı azalıyor. Ancak videoda Onaylanmış takipçiler açıkken VERIFIED_TAB logu selected=2/FOLLOWERS_LIST, eski Tanıdığın takipçiler/Takipçiler/Takip ediliyor başlıkları ve arkadaki profil sayacını içeriyor. Onaylı başlık logda yok. Bu nedenle önceki yalnız sözlük düzeltmesi cihaz sorununu çözmedi; 159 test cihaz doğrulaması değildi.

Kodda service XML ve onAccessibilityEvent filtresi TYPE_WINDOW_CONTENT_CHANGED ile TYPE_VIEW_SELECTED olaylarını dinlemiyordu. Her yeni rootInActiveWindow çağrısının taze alt düğüm getireceği varsayılmıştı; Android cache invalidasyonu yapılmıyordu. Kesin cihaz cache içeriği elimizde yok; video/log uyuşmazlığına ve bu kod boşluğuna yönelik düzeltme yapıldı. UI erişilebilirliği hâlâ uygulamanın yayımladığı bilgiye bağlıdır.

- XML ve servis filtresine içerik/sekme seçimi olayları eklendi; mevcut erken tick ve tek worker düzeni korundu.
- Onaylı takip görevinin her okumadan önce Android 13/API33+ clearCache çağrılır, ardından yeni root alınır ve refresh doğrulanır. Eski root önce alınarak tekrar kullanılmaz. Android 8–12 yalnız root.refresh ve içerik olaylarından yararlanır; clearCache API bu sürümlerde yoktur. Bu X pull-to-refresh değildir, ekrana dokunmaz/ağ isteği yapmaz.
- FreshRootReader bu sırayı ortak uygular. Ayrılmış veya bulunamayan root işlem için kullanılmaz. UI_FRESH logu aşama, Android sürümü, cacheCleared, rootRefreshed ve pencere kimliğini kaydeder.
- VERIFIED_TAB teşhisi genel Takip et düğmeleriyle dolmaz; sekme başlıklarını, durumlarını ve sınırlarını kaydeder.
- Hesap importunun cache okuma yolu ve takipten çıkma akışı değiştirilmedi. Yalnız Takip et/geri takip et ayrımı, limit ilerlemesi, son onaylı listeden rastgele yeni kaynak ve üç geri dönüş kuralları korundu. Ekranda seçili onaylı sekme kanıtı olmadan takip başlatılmadı.
- Dört test cache temizlemenin root okumadan önce yapılması, refresh başarısızlığının reddi, eksik rootta önceki okumanın kullanılmaması ve değişen ilişki durumunun yeni okumadan alınmasını kapsar. Bunlar Android/X cache davranışının fiziksel testi değildir.
- versionCode61 / 26.13-fresh-accessibility-tree. CI bekleniyor; sonuç sonraki kayıtta. Yeni APK fiziksel telefonda test edilmedi. Kaynak 26.12 main üzerinden alındı; eski yerel 26.9 dosyaları kullanılmadı.
- Android resmi API33 clearCache kaynağı: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#clearCache() . Kalıcı imza ve Releases contents:write onay engeli sürüyor; yayın yetkisi değiştirilmedi.

## 26.12 — doğrulanmış APK ve teslim sonucu

- Kaynak commit b8c023c2f6fa818ac9074b7182b3e479cd8d706f (main). Bu sonuç kaydı yalnız belgedir; APK aynı kaynak derlemesinden teslim edilir.
- Başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34188597647 . APK/tam paket artifact 10041442033; test/lint raporları 10041442244. 159 test, 0 başarısız/hata/atlanan. Test, lint, assemble, apksigner ve manifest kontrolleri geçti.
- AtmacaNext-26.12.apk: code60 / 26.12-verified-tab-label, 20557532 bayt; SHA256 d62945b98d7ed758f5fa3f78bd8718f8541b0191c426f81e9f941424064d8bc5.
- Tam ZIP SHA256 ce7de6dd45e25552cf0d44573c45c0f2447dfca31f2ad0b10de290fd15638b75. ZIP CRC, paket içi/dışı APK eşitliği, manifest sürümü/INTERNET izni yokluğu ve test XML toplamları doğrulandı. Tam paket kaynak, APK, test raporları, manifest, hash ve CI sonuçlu NOT_DEFTERI içerir.
- APK sertifikası SHA256 257e3fe18f7f1d6984a49c7ee97a0ce8036f43319e60600595db61dd2f0f4672; 26.11 sertifikasından farklı. Üzerine kurulum reddedilebilir; eski uygulamayı kaldırmak kayıtlı hesap/görev/ayar/bildirim verilerini siler. Kalıcı imza hâlâ açık sorundur.
- Fiziksel X/telefon testi yapılmadı. Verilen 26.11 videosu Türkçe Onaylanmış takipçiler etiketinin eksikliğini ve liste başındaki yenilemeleri gösterdi; yeni davranış cihazda doğrulanmalıdır. Takılma sürerse yeni VERIFIED_TAB kaydı incelenmeli; seçili durum kanıtı olmadan takip başlatılmamalı.
- Kod/test/notlar main'de. APK ve tam ZIP Actions üzerinde ve sohbetten teslim edilir. Kalıcı GitHub Releases yayını önceki otomatik onay reddi nedeniyle tamamlanmadı; contents:write workflow etkinleştirilmedi. Actions artifact süreli saklamadır, kalıcı Releases teslimi değildir. Eski sürümler silinmedi.

## 26.12 — cihazdaki Onaylanmış takipçiler etiketi ve yenileme düzeltmesi

8 Eylül 2026, 07:39–07:40 cihaz logu ve 1000026806.mp4 incelendi. Kullanıcının 26.11 cihazında dört hesap taranmış ve Atmaca dönüşü doğrulanmış. Dinamik kaynak profili artık PROFILE olarak okunuyor. Video açık/seçili sekmenin tam Türkçe adının "Onaylanmış takipçiler" olduğunu gösteriyor; önceki sözlük yalnız "Onaylı" ve "Doğrulanmış" biçimlerini içerdiğinden ekran UNKNOWN kalıyor. Bu, ham ağacın seçili alanının paylaşıldığını tek başına kanıtlamaz; düzeltme sonrası cihaz doğrulaması gerekir.

- XUiVocabulary.fullVerifiedFollowersHeaders ortak tam başlık sözlüğü eklendi. XUiActions sekme tıklaması ve RelationshipTabInspector/ScreenDetector aynı yeni Türkçe varyantı tanır. Sadece görünür başlık yeterli değildir: gerçekten seçili sekme kanıtı korunur.
- Kendi Followers listesinin başında geri kaydırma reddedildiğinde ListGesture.backward ile pull-to-refresh üretilmez. Yerel dikey liste eylemi kullanılır; kullanıcı adı ve konumları sabitlenince yeni bir kaydırma göndermeden kaynak seçilir. Kapsayıcı okunamıyorsa tahmini hareket yerine duraklar.
- Verified liste kaydırmaları da kullanıcı satırının dikey kapsayıcısından yapılır; genel yatay pager seçimi engellenir.
- Onaylı sekme doğrulanamazsa kendi profile gidip gezinmeyi tekrar başlatmak yerine VERIFIED_TAB teşhisiyle duraklar. Ekran, seçili sekme ve görünür takip başlıkları loglanır.
- Limit eksikse yalnız en son onaylı listeden toplanan adaylar arasından rastgele, ziyaret edilmemiş kullanıcı seçme ve gerçek satırına dokunma korunur. Doğrulanmış ilerleme ve işlenmiş hedefler sıfırlanmaz. Son listede aday kalmazsa kendi Followers listesinden başka kaynak üretmez; eksik sayıyı açıkça göstererek duraklar.
- Yalnız Takip et; Geri takip et/Takip ediliyor atlama, üç farklı ardışık geri dönüş ve bildirim davranışları korunur. Hesap importu ve takipten çıkma değişmedi.
- Beş regresyon testi: gerçek Türkçe başlık/ortak sözlük, yüklenmekte olan seçili sekme, komşu sekme yanlış pozitif reddi, ilişki düğmesi ayrımı, son listeden rastgele kaynak/ziyaret edilmiş hesap reddi.
- versionCode 60 / 26.12-verified-tab-label. CI henüz çalıştırılmadı; sonuçlar aşağıdaki sonraki kayıtta belirtilecek. Yeni APK fiziksel telefonda test edilmedi. Main'e kod/test/notlar kaydedilecek; yerel eski 26.9 çalışma dosyaları kullanılmadı ve ezilmedi.
- Kalıcı debug imzası ve Releases contents:write onay engeli sürüyor; yayın yetkisi değiştirilmedi. Mevcut Actions APK/tam ZIP üretimi korunur.

# Atmaca Next — sohbetler arası devir notu

Son güncelleme: 7 Eylül 2026. Bu not ve kaynak kod GitHub'da tutulur; başka ChatGPT hesabından devam ederken önce bu dosyayı oku. Önceki sohbet dosyalarına erişebildiğini varsayma.

## 26.11 — doğrulanmış derleme sonucu

- Kaynak commit: 3ab25eab6570e268fd7a5e07802475f2ea45f154 (main). Bu sonuç kaydı belgedir; APK bu kaynak commit'ten üretildi.
- Başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34187259126 . APK/tam paket artifact 10040984103; raporlar 10040984479. 154 test geçti; 0 başarısız, hata, atlanan. Test/lint/assemble ve APK imza/manifest kontrolleri başarılı.
- AtmacaNext-26.11.apk, code59 / 26.11-public-profile-evidence, 20557532 bayt. SHA256 92f8ae4418f4c52b65f4b98768a9cfbc5e8bcb85565286c0a6b11ff6a8b9c7df.
- Tam ZIP SHA256 9067a2f8e959b9c4e680f4b4dcff2e2fdbddc777f07c856caff78382f14223ff. ZIP CRC, test XML toplamları, gerçek manifest ve paket içi APK eşitliği doğrulandı. Paket kaynak ZIP, test XML ZIP, APK, manifest, hash ve gerçek CI sonuçları ekli NOT_DEFTERI içerir.
- Sertifika SHA256 83e98b0b3cf9298107d1a449539e5702bc943e1b99fa73a8891554a7b100f474. Önceki 26.10 sertifikasından farklı. Üzerine kurulum uyumsuz olabilir; kaldırmak hesap/görev/bildirim verilerini siler. Kalıcı imza sorunu açık.
- Fiziksel telefonda/X üzerinde yeni sürüm test edilmedi. Kullanıcının 26.10 cihaz kanıtı kaynak profiline dokunmanın çalıştığı; profil UNKNOWN kaldığıdır. 26.11'in split-counter düzeltmesi sentetik ağaç testleriyle doğrulandı, cihazdaki kesin node ayrımı henüz paylaşılmadı. Başarısızlıkta PROFILE_EVIDENCE satırını ve erişilebilirlik teşhisini incele; süre artırarak veya rastgele kimlik kabul ederek geçme.
- Kod/test/notlar main'dedir, APK/ZIP ayrıca sohbet teslimidir. Actions artifact 7 Aralık 2026'da sona erer. Kalıcı Releases yayını önceki contents:write otomatik onay reddi nedeniyle tamamlanmadı; kullanıcı bu kapsamı onaylamadı. Yayın workflow yetkisi değiştirilmedi; eski APK'lar silinmedi.

## 26.11 — başka profil UNKNOWN / ortak profil kanıtı

8 Eylül 07:25 cihaz logu: 26.10 artık doğru dinamik kullanıcının profiline giriyor; sonra UNKNOWN olarak kalıyor ve 15 saniyede duraklıyor. Kullanıcı profilin açıldığını doğruladı. Ham erişilebilirlik ağacı paylaşılmadığı için alanların cihazdaki kesin ayrımı henüz bilinmiyor. Kod incelemesi: kendi profil Edit profile ile tanınırken başka profil için iki sayacın sayı+etiketinin aynı düğümde olması şarttı; split/stacked sayaçlar desteklenmiyordu. Önceki 147 test bu varyantı kapsamıyordu.

ProfileSurfaceEvidence, görünür başlık kullanıcı adı + aynı satırdaki iki gerçek sayacı okur. Sayı/etiket ayrı veya dikey bölünmüş olabilir; yakınlık ekranın gerçek metin boyutuna göredir. Biyografi bahsetmesi/tweet yazarı başlık kimliğini değiştirmez. Seçili ilişki sekmesi profil kanıtı değildir; uzak sayılar, gizli alanlar ve yalın Follow düğmesi sayaç değildir. ScreenDetector, XIdentityDetector ve XNavigator artık bu kanıtı ortak kullanır; sayaç tıklaması doğrulanan etiketin sınırına yapılır, geniş üst kapsayıcıya çıkılmaz. Eski tanıma yolları uyumluluk için korunur.

OPEN_SOURCE_PROFILE sayaç tıklaması reddedildiğinde yeniden tick planlamaması da düzeltildi; bu dal artık sessizce asılı kalmaz. Zaman aşımında PROFILE_EVIDENCE satırı ekran/okunan başlık/ikili sayaç/seçili sekme durumunu loglar; sonraki teşhis yalnız UNKNOWN'a dayanmaz.

7 regresyon: public split sayılar, Türkçe birleşik sayılar, dikey sayılar/farklı ölçekler, liste-profil ayrımı, gizli/uzak sayı reddi, yanlış biyografi kimliği, gerçek üretim okuyucularıyla Followers→dinamik profil→Verified→düz Follow→sonraki kaynak→3 geri dönüş sözleşmesi. Bunlar sentetik erişilebilirlik testleridir; Android/X fiziksel uçtan uca testi değildir. Fiziksel test yapılmadı. Yalnız Follow, ilerleme koruma, üç geri dönüş/bildirim kuralları korunur. Sürüm 26.11-public-profile-evidence / code59. CI bekleniyor; main/teslim sonuçları sonraki kayıtta. İmza ve kalıcı Releases yetki engeli sürer.

## 26.10 — başarılı APK / devir sonucu

- Kaynak commit: abf9a9df1816774f373063b458a73480cfb604e7 (main). Bu sonuç kaydı yalnız dokümandır; teslim APK bu kaynaktan derlendi.
- CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34186206047 . APK/tam ZIP artifact 10040636855; raporlar 10040637307. 147 test geçti, 0 başarısız/hata/atlanan. Test, lint, assemble, APK imza/manifest kontrolü başarılı.
- APK: AtmacaNext-26.10.apk; versionCode 58 / 26.10-source-row-navigation; 20557528 bayt; SHA256 2a3ef649832f03d6fcffc3d8a42e665b0f51d7bbc891719cbf5484c5c0cd4609.
- Tam ZIP SHA256: 65dde36a78f1acfc5757b979d5417a33a784ce822955b80aeec4ca271718359b. ZIP CRC, manifest sürümü, dış/iç APK eşitliği, test XML toplamları doğrulandı. Tam paket APK, hash, manifest, kaynak, test raporları ve CI sonuçları ekli not defteridir.
- Sertifika SHA256: b9a851ec7ed412309493b46667cf7140195fd75c334ec1ce8c11b4857e69856c. 26.9 teslim sertifikasından farklı; üzerine kurulum uyumsuz olabilir, kaldırma yerel hesap/görev/bildirim verilerini siler. Kalıcı imza sorunu açık.
- Fiziksel telefon/X testi yapılmadı. Kullanıcının videosundaki ilk kaynakta kalma hatasını doğrulamak için: gerçek Followers listesindeki değişken ilk kullanıcıya dokunma → aynı kimlikte PROFILE → kaynak takipçileri → Verified followers. Rastgele sonraki kaynakta aynı listedeki satır bulunmalı; düz Follow/geri dönüş/bildirim kuralları korunur.
- Kod/test/notlar main'de. APK/ZIP sohbetten teslim edilir. Actions dosyaları 7 Aralık 2026'ya kadar süreli; kalıcı Releases yayını önceki contents:write otomatik onay reddi nedeniyle hâlâ engelli. Kullanıcı bu kapsamı onaylamadı; workflow yetkisi değiştirilmedi. Eski derlemeler silinmedi.

## 26.10 — 26.9 cihaz hatası: kaynak profile gerçek satır dokunuşu

8 Eylül 2026. Kullanıcının 7 Eylül 23:04 logları ve 1000026774.mp4 videosu incelendi. Kendi hesap ve gerçek Followers sekmesi doğru; dinamik ilk takipçi doğru okunuyor. 23:04:54 profile/temmytiwa92 komutundan sonra ekran hiç PROFILE olmuyor, FOLLOWERS_LIST kalıyor. Aynı URL yöntemiyle kendi profile dönüş de sonuçsuz. Bu isim örnek olup kodda sabitlenmedi. Önceki birim testler sekme/kullanıcı seçimini doğruluyordu, gerçek Android URL yönlendirmesini değil.

- Onaylı takip akışında kaynak için launchXProfile kaldırıldı. SourceProfileTarget görünür, etkin, ayrı ve tam @kullanıcı alanını seçer; biyografi bahsetmesi, düğme açıklaması, gizli/sıfır boyutlu alan reddedilir. XUiActions yalnız bu alanın güncel ekran sınırına erişilebilirlik dokunuşu gönderir; bütün satır/üst kapsayıcı/takip düğmesine yükselmez. Sabit koordinat yok.
- OPEN_SOURCE_PROFILE gerçek PROFILE + tam kaynak kimliği bekler. Ekran listede kaldıysa 2 saniye sonra aynı kullanıcı alanına yalnız bir kez tekrar dokunur. 15 saniyede doğrulanmazsa kullanıcı atlayıp yanlış profilde ilerlemek yerine duraklar. Gönderilen dokunuş başarı kanıtı sayılmaz.
- Rastgele kaynak mevcut onaylı listeden görülmüş havuzdan seçilir; LOCATE_SOURCE_ROW o kullanıcı görünmüyorsa aynı listede geriye kaydırıp gerçek satırı bulur ve dokunur. Profil doğrulanınca ziyaret edilmiş sayılır; doğrulanmış takip sayısı korunur. 30 saniyede bulunmazsa sahte başarı/URL atlaması yok, duraklama var.
- Onaylı görevin başlangıcında zaten doğrulanmış kendi profil tekrar URL ile başlatılmaz. Kendi profile dönüş gerektiğinde X içinde Geri, Anasayfa hesap menüsü ve Profil satırı kullanılır, tam kimlik doğrulanır. 30 saniye/10 gezinme sınırı var.
- Düz Takip et kuralı, üç farklı ardışık geri dönüşte hesap atlama, bildirimler ve Anasayfa korunur. Hesap importu ve takipten çıkma kod yolu değiştirilmedi.
- Sürüm 26.10-source-row-navigation / versionCode 58. 5 hedef seçimi regresyon testi eklendi. CI sonucu bekleniyor; fiziksel X cihaz testi yapılmadı. Kalıcı imza ve Releases yetki engeli devam ediyor.

## 26.9 — doğrulanmış derleme ve teslim (7 Eylül 2026)

- Kaynak commit: 980614e2bc8296fdac40893eec3d856d85d23cf4, main. Bu sonuç kaydı yalnız belge güncellemesidir; teslim APK'sı bu kaynak commit'ten üretilmiştir.
- Başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34156844490 . APK/tam paket artifact: 10031315464; test/lint raporları: 10031315734.
- 142 test geçti: 0 başarısız, 0 hata, 0 atlanan. testDebugUnitTest, lintDebug, assembleDebug, APK imza ve manifest kontrolleri başarılı. ZIP CRC, APK SHA256, tam paket içindeki APK eşitliği ve test XML toplamları ayrıca doğrulandı.
- APK: AtmacaNext-26.9.apk, 20.557.528 bayt, versionCode 57 / 26.9-followers-tab-source, package com.atmacanext.v258. SHA256: 25b38540fffa988f180fe487cf20b52046e6e99bf91c3f092d691e3720579287.
- Sertifika SHA256: be83861dc35edd3a3b5d96a9f394095d5d994cd804a49028cbd4d742d115ed0e. Teslim 26.8 sertifikasından farklıdır; üzerine kurulum uyumsuz olabilir. Uygulamayı kaldırmak hesap/görev/bildirim verilerini siler. Kalıcı imza sorunu çözülmedi.
- Tam ZIP: AtmacaNext-26.9-TAM-PAKET.zip; SHA256 d25a3d28b89780f61946c88944aa246681476a43727cf2a57e10af0ce352fb55. APK, hash, manifest, gerçek CI sonuçları eklenmiş NOT_DEFTERI.txt, kaynak ZIP ve test XML ZIP içerir.
- Değişen kod: ScreenDetector, RecentFollowerSelector, AutomationRuntime, VerifiedFollowPolicy, XUiActions, AppServices, TaskOrchestrator, AtmacaNextApp, DashboardScreen; yeni NotificationStore. Regresyonlar: RecentFollowerRegressionTest, VerifiedFollowFlowTest, yeni NotificationFeedTest. Ayrıntılı davranış ve gerekçeler alttaki 26.9 bölümlerindedir.
- Fiziksel cihaz/X testi ve yeni Anasayfa görsel cihaz doğrulaması yapılmadı. Cihazda denenecekler: gerçek Followers ilk değişken kaynak → seçili Verified followers; yalnız düz Takip et; 10/30 ilerlemeyi koruyan rastgele kaynak geçişi; üç farklı ardışık geri dönüşte hesabı atlayıp sıradaki hesap, zil bildirimi ve yeniden açılışta kalıcılık.
- Kaynak/testler/notlar main'dedir. Actions artifact sürelidir (6 Aralık 2026). Kalıcı GitHub Releases teslimi henüz tamamlanmadı: AGENTS.md'deki önceki otomatik onay reddi nedeniyle contents:write yayın taslağı etkinleştirilmedi; bu somut kapsam için kullanıcı onayı gerekir. APK ve ZIP sohbetten ayrıca teslim edilir. Başarılı derlemeyi tekrar çalıştırıp aynı sürümü başka debug imzasıyla değiştirmeyin.

## 26.9 — kullanıcının son kuralları ve Anasayfa

Kullanıcı bu çalışmada açıkça şu kapsamı ekledi: yalnız Verified followers sekmesinde Takip et/Follow; hiçbir Geri takip et/Follow back/Sen de takip et düğmesine basma. Düğmenin kendi metniyle beraber tıklanacak üst öğenin açıklaması da denetlenir; çelişki varsa işlem yapılmaz.

Üç farklı hedefte ardışık Takip ediliyor→Takip et geri dönüşü hesabın onaylı takip işlemini durdurur. Başarı sayısı artmaz; sıradaki kuyruk hesabıyla devam edilir. Bu bir kullanıcı durdurma kuralıdır, X günlük limitinin teknik kanıtı değildir. Kuyrukta aynı hesabın kalan çalıştırılabilir işleri önceki akıştaki gibi atlanır. NotificationStore bu olay için kullanıcı adı, doğrulanan/hedef sayısı, tarih ve sıradaki hesabı kalıcı uygulama içi bildirim olarak kaydeder. Olay kimliği kuyruk+hesap bazındadır; tekrar eden runtime olayı bildirim çoğaltmaz. Son 200 bildirim ve okundu durumu SharedPreferences içinde saklanır; uygulama silinirse bunlar da silinir.

Yeni Anasayfa varsayılan sekmedir: sağ üst bildirim zili/okunmamış rozeti, durum özeti, aktif hesap, çalışan görev ve Görevler/Hesaplar kısayolları. Bildirimler zil ile açılır, açılınca mevcut bildirimler okundu işaretlenir. Android push/izin sistemi eklenmedi; istenen bildirim alanı uygulama içindedir.

Kaynakta uygun kişi biterse O KAYNAĞIN onaylı listesinde görülen kullanıcılar arasından rastgele, ziyaret edilmemiş ve kendi hesap olmayan bir kaynak seçilir. Sonraki kaynak açılırken aday havuzu temizlenir; eski kaynaklardan rastgele seçim yapılmaz. 100 kaynakta keyfi durdurma kaldırıldı. Doğrulanmış ilerleme ve işlenmiş hedefler korunur; 10/30 sonrasında kalan 20 için devam edilir. Erişilebilir aday hiç kalmaması, belirsiz işlem sonucu veya gerçek X uyarısı gibi durumlarda limit dolmuş sayılmaz. İlk kaynak yine gerçek Followers listesinin en üstündeki değişken kullanıcıdır; örnek kullanıcı adı kodda sabitlenmez.

4 bildirim regresyon testi eklendi, rastgele kaynak testi uygun aday kümesini/ziyaret edilmiş kaynak dışlamasını denetler. 26.9 tamamlanmış yeni kapsamın CI sonucu ve teslim bilgisi sonraki kayıtla güncellenecek. Yeni Anasayfa ve X akışının fiziksel cihazda çalıştırıldığı iddia edilmez.

## 26.9 tamamlama — gerçek seçili sekme / değişken kaynak

Kullanıcının 1000026767.mp4 videosu yeniden incelendi: Followers you know sekmesinde kalınıyor. İşaretli ekranlar gerçek Followers sekmesinin ilk satırındaki DEĞİŞKEN kullanıcıyı ve onun Verified followers sekmesini gösteriyor. Örnekteki kullanıcı adı sabitlenmedi.

Önceki 26.9 CI (34154887746) 133 testten 1'inde kaldı: switchingToActualFollowersAllowsSourceSelection. Ham seçili sekme ve snapshot yolları farklı davranıyordu; snapshot'ta tek kullanıcı/ilişki düğmesi olmayan liste UNKNOWN oluyordu. ScreenDetector artık her iki yolda da seçili sekmeyi satır yüklenmesinden bağımsız tanır. Followers you know OTHER olarak korunur; başlığın yalnız görünmesi yetmez.

AutomationRuntime kaynak listesi yükleme kontrolünde takip düğmesi aramıyor; görünür, ayrı kullanıcı adı alanını esas alıyor. RecentFollowerSelector liste başı denetiminde kullanıcı adlarıyla beraber dikey sınırları da izler; aynı isimler kayarken erken liste başı kararı verilmez. Her çalıştırmada ilk uygun kullanıcı güncel ekrandan yeniden seçilir. Kaynak profil kimliği doğrulanır, ardından Verified followers seçili olmadan takip işlemi başlamaz. Soldaki Verified followers görünmüyorsa sağa kaydırma ve görünür sekmeye tıklama 26.9 akışında korunur.

5 ek regresyon testi: tek satır/boş seçili sekme, ham-snapshot eşitliği, komşu Verified başlığı, kaymaya devam eden aynı kullanıcılar. Önceki başarısız test değiştirilmeden korunmuştur. CI sonucu bekleniyor; gerçek cihazda yeni 26.9 çalıştırılmadı. Sürüm adı 26.9-followers-tab-source / versionCode 57 korunur; önceki 26.9 yayımlanmış APK üretmemişti. İmza ve kalıcı Releases workflow onay sınırlamaları devam eder.

## 26.9 — gerçek Followers sekmesi ve değişken ilk kullanıcı

1000026767.mp4 (28,07 sn) incelendi. X, kendi profilinden takipçilere girince Followers you know sekmesinde kalıyor. 1000026769.jpg ve 1000026772.jpg işaretleri gerçek Followers listesinin en üstündeki kullanıcı profilini, ardından o profilin Verified followers listesini gösteriyor. Kullanıcı bu kişinin sürekli değişeceğini açıkça belirtti; örnek kullanıcı adı kodda sabitlenmedi.

- RelationshipTabInspector, Followers you know etiketini OTHER olarak ayırır. ScreenDetector bu seçili sekmeyi düz Followers gibi sınıflamaz; görünür komşu Followers başlığı yeterli değildir.
- OPEN_MY_FOLLOWERS gerçek Followers sekmesine tıklar; seçili olmasını bekler. Liste başına kaydırmada yatay pager yerine kullanıcı satırlarının dikey kapsayıcısı kullanılır.
- RecentFollowerSelector görünür, ayrı @kullanıcı_adı alanlarını üstten alta sıralar. Biyografideki bahsetmeler, gizli düğümler, kendi hesap ve ziyaret edilmiş kaynaklar dışarıda kalır. Takip ediliyor veya Geri takip et olması kaynak profil seçimini engellemez. En üst kullanıcı her çalıştırmada yeniden okunur; örnek resimdeki ad kaydedilmez.
- Kaynak profil, okunan kullanıcı adıyla X içinde açılır ve kimliği doğrulanır. Kaynağın takipçilerinde Followers you know açılırsa yine Verified followers seçimine devam edilir. Resimde Verified followers sol tarafta olduğundan görünmüyorsa sağa hareketle sol sekme gösterilir; öncelik görünür sekmeye doğrudan tıklamadır.
- Sadece Takip et düğmesini işleme, doğrulanmış işlem limiti, üç ardışık geri dönüşte hesap değiştirme, koyu görev arayüzü korunur.
- Sürüm 26.9-followers-tab-source, versionCode 57. Sekme ayrımı ve değişken kaynak için 8 yeni regresyon testi. CI bekleniyor; fiziksel cihazda 26.9 test edilmedi.
- Kod/notlar main, APK ve tam ZIP GitHub Actions çıktısında. Kalıcı imza ve Releases yetkisi konusundaki önceki sınırlamalar devam ediyor.

## 26.8 doğrulanmış APK ve ZIP teslimi

- Kaynak: 3e4e49b81b217e0588ae1ca3ed6b5f9d7b8debb7, main. Bu not commit'i aynı kodun devamıdır.
- Başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34153574384 . APK ve tam paket artifact: 10030254744; rapor artifact: 10030255188.
- APK: AtmacaNext-26.8.apk, versionCode 56 / 26.8-verified-follow, 20.524.760 bayt. SHA256: b6e1aba207615655c45abcb9f324c43048580ea2bc54f8ca86b9ce03a01cde7b.
- Sertifika SHA256: 023f4a43d95024eca7b98e68ce29f04123b3760172f9bcc31e5cce5d7c58a43f. 26.7 teslim sertifikasından farklı; kaldırıp kurma Atmaca verilerini siler. İmza sorunu hâlâ açık.
- 125 test: 0 başarısız, 0 hata, 0 atlanan. ZIP CRC, manifest, APK SHA256, paket içindeki APK eşitliği ve test XML toplamları kontrol edildi. APK INTERNET izni içermez. Gerçek X cihaz testi yapılmadı.
- Tam ZIP: APK, manifest, SHA256, NOT_DEFTERI.txt, kaynak ZIP ve test XML ZIP içerir. APK ve ZIP kullanıcıya sohbetten de teslim edilir. GitHub Actions arşivi sürelidir; kalıcı Releases yayın taslağı etkin değil.
- Son sertleştirme: takipçi listesi yüklenmeden kaynak seçilmez; standart kaydırma yoksa erişilebilirlik hareketiyle geriye kaydırılır, sabit liste başı görülünce ilk kaynak seçilir. Başarı sonucundaki düz Follow denetimi çelişkili Follow back açıklamasını da reddeder.
- Cihazda sıradaki kontrol: kendi profil → ilk takipçi → kaynak takipçileri → seçili Verified Followers; sadece Follow; kaynak değişimi; limitte bitiş; üç ardışık Following→Follow dönüşünde sıradaki hesaba geçiş. Bu geri dönüş kuralı kullanıcı isteğiyle uygulanan durdurma sezgisidir, X'in kesin günlük limit bilgisi değildir.

## 26.8 — onaylı kullanıcı takip akışı

Kullanıcı önceki şikayette yanlış APK yüklediğini doğruladı ve doğru 26.7 sürümünün güzel çalıştığını bildirdi. 26.7 limit/arayüz düzeltmeleri korunuyor. Yeni istek yalnız onaylı kullanıcı takibi.

- Kendi profil kimliği doğrulanır, takipçi sayacına girilir ve takipçiler listesinin başındaki ilk ziyaret edilmemiş kullanıcı kaynak seçilir. Bu sıralama X'in görünen liste sırasıdır; görünmeyen zaman bilgisi tahmin edilmez.
- Kaynak profilin takipçileri açılır. Onaylı/Doğrulanmış Takipçiler veya Verified Followers sekmesinin seçili olduğu doğrulanır. Etiket görünüyorsa tıklanır; görünmüyorsa ekran boyutuna göre sola kaydırıp tekrar aranır (en fazla 6 deneme). Görünen ama seçili olmayan başlık takip yetkisi değildir.
- Yalnız Takip et / Follow düğmeleri işlenir. Geri takip et, Sen de takip et, Follow back ve Takip ediliyor / Following atlanır. Çelişkili metin-açıklama varsa düğmeye basılmaz.
- Aynı kullanıcı satırında Takip ediliyor durumu en az 2 saniye sabit görülünce başarı sayılır. Satır yoksa veya durum kesinleşmezse 7 saniye sonunda duraklar; başarı veya X limiti uydurulmaz.
- Takip ediliyor görüldükten sonra aynı satır yeniden Takip et olursa geri dönüş sayılır. Üç ardışık farklı işlemde geri dönüş: mevcut hesap kuyruğundaki çalıştırılabilir işler atlanır ve sıradaki hesabın görevi başlatılır. Bu, kullanıcının istediği durdurma sezgisidir; X günlük limitinin kesin teknik kanıtı değildir. Başarılı takip geri dönüş serisini sıfırlar; sayılmamış geri dönüşler başarılı sayıya eklenmez. Açık X limit/izin/uyarı pencerelerinin genel duraklama davranışı korunur.
- Kaynakta uygun kişi kalmazsa açık onaylı listeden görülen başka kullanıcı kaynak seçilir; onun profili ve onaylı takipçileriyle devam edilir. Kendi hesap ve ziyaret edilmiş kaynaklar tekrar seçilmez. Açık listeden aday kalmazsa kendi takipçilerinden başka kaynak aranır. 100 kaynakta koruma duraklaması, kendi liste başını bulmada 30 saniye sınırı vardır.
- Değişen bölümler: AutomationRuntime, TaskOrchestrator, RelationshipTabInspector, ScreenDetector, XUiActions, ListGesture; yeni VerifiedFollowPolicy ve 11 regresyon testi. Önceki iki ekran testi artık seçili sekme kanıtı içerir.
- Sürüm 26.8-verified-follow, versionCode 56. 125 test geçti; 0 hata/başarısız/atlanan. Test, lint, APK ve imza/manifest kontrolleri başarılı. Fiziksel telefonda bu yeni akış test edilmedi.
- APK/TAM-PAKET.zip/kaynak/test ZIP/not defteri GitHub Actions çıktısında üretilir. Kalıcı Releases yayın yetkisi hâlâ etkin değil. Kalıcı imza çözülmedi; yeni debug imzası eski uygulama üstüne kurulumla uyuşmayabilir.

## 26.7 doğrulanmış teslim

- Kaynak commit: c9a1853f48c115f4d88476db1eebae55e99ec6f1 (main). Bu not güncellemesi aynı kodun devamıdır; başka sohbetten daha yeni kaynak olup olmadığını kontrol et.
- Başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34127246584 . APK/TAM-PAKET ZIP artifact: 10020680411, test/lint rapor artifact: 10020681215.
- AtmacaNext-26.7.apk: 20.524.764 bayt, SHA256 4c1e0c0a1650e2ed3787b65495993597049e898d3ab440ede2dabfd89d857ab5.
- Sertifika SHA256: c69d27cbfcaa3e5c38cb42e63559fba4983bc275498a9c9fe84695aeba57fb9d. Teslim 26.6 imzasından farklı; üzerine kurulum mümkün olmayabilir, kaldırma verileri siler. Kullanıcı bilgilendirildi.
- 114 test geçti. APK sürümü 55 / 26.7-unfollow-limit-ui; INTERNET ve InitializationProvider yok. ZIP CRC, APK hash, tam paketteki APK eşitliği ve test XML toplamları doğrulandı.
- Tam paket kaynak ZIP, test XML ZIP, APK, SHA256, manifest ve gerçek CI sonuçları eklenmiş NOT_DEFTERI.txt içerir. Kullanıcıya APK ve ZIP doğrudan verilir; GitHub Actions üzerinde aynı paket saklanır. Releases otomatik yayın taslağı önceki yetki engeli nedeniyle etkin değil. Actions artifact sürelidir; kalıcı Releases arşivi tamamlanmış sayılmamalı.
- Yeni görev sekmeleri arasında geçerken önceki sekmenin seçimi temizlenir; görünmeyen bir görev türü yanlışlıkla eklenmez. Düzenleme ilk açılışında mevcut görevin kategorisi seçilir.
- Açık cihaz kontrolü: iki hesapta 5+5 (Geri Takip Et sonuçları dahil), belirsiz sonuçta ek kişiye geçmeme, daha kısa aralıklar ve yeni arayüz. Birim test/derleme fiziksel X doğrulaması değildir.

## 26.7 — limit, hız ve görev ekranı (7 Eylül 2026)

Kullanıcı iki hesapta geçiş ve görevin Atmaca'ya dönüşünü başarılı bildirdi; ikinci hesap limit 5 olmasına rağmen fazladan takipten çıktı. 1000026740.mp4 (219,88 sn) incelendi: ikinci hesapta sonuç düğmeleri Geri Takip Et oluyor. Kod sadece takip et/follow kabul ettiği için bu işlemleri saymayıp yeni kişilere devam ediyordu.

- AutomationRuntime: sonuç kontrolü XUiVocabulary.followActions kümesinin tamamını (Geri Takip Et / Follow back dahil) kullanır. Başlatılan işlem sayısına ayrı hesap/döngü sınırı eklendi; yanlış veya eksik sonuç sayımı altıncı işleme izin vermez. Belirsiz sonuç artık başka kişiyle telafi edilmez, görev duraklar. Başarı sayısı yalnız ekranda doğrulanmış işlemdir. Kısmi görev yeniden başlatılırken döngü başlangıç sayacı düzeltilmiştir.
- Sonucun sabit görülme süresi 1500 ms yerine 650 ms; varsayılan işlemler arası bekleme 1800 yerine 800 ms. Önceden kullanıcı tarafından kaydedilmiş özel bekleme değeri korunur. Asıl 5–10 sn gecikme Geri Takip Et sonucunun okunmaması nedeniyle oluşuyordu.
- TasksScreen: diğer ekranların koyu kart/zemin renkleri, okunaklı açık yazı ve yeşil vurgu; beyaz zemin üstünde beyaz yazı hatası giderildi. Yeni görevlerde Takip ve Etkileşim sekmeleri. Tweet/görselli tweet/alıntı oluşturma seçenekleri ve kategori kaldırıldı; eski kayıtlar veritabanından silinmedi, görev ekranındaki çalıştırılabilir listeye dahil edilmez. Yeni görev varsayılanı Takipten çık.
- Altı yeni regresyon testi: TR/EN Follow back, yanlış etiketler, 5 deneme sınırı, iki hesap, döngü ve kısmi ilerleme.
- Sürüm 26.7-unfollow-limit-ui, versionCode 55. 114 birim testi geçti (0 hata, 0 başarısız, 0 atlanan); test/lint/APK ve imza/manifest kontrolü başarılı. Gerçek telefonda 26.7 test edilmedi.
- Her derleme GitHub Actions çıktısına APK yanında kaynak ZIP, NOT_DEFTERI.txt, SHA256 ve manifest içeren TAM-PAKET.zip ekler. Yayımlama yetkili Releases taslağı etkinleştirilmedi; önceki otomatik onay engeli sürüyor. Kaynak, test ve notlar main'e kaydedilir. Kalıcı imzalama çözülmedi; farklı debug derlemeleri üzerine kurulum uyuşmayabilir.

## Kalıcı APK / ZIP ve not defteri arşivi

Kullanıcının isteğiyle her başarılı main APK derlemesi için GitHub Releases arşivleme taslağı hazırlandı. OTOMATİK YAYIN HENÜZ ETKİN DEĞİL: otomatik onay incelemesi main'e contents:write yetkili kalıcı workflow eklenmesini, bu yetki kapsamı açıkça onaylanmadığı gerekçesiyle reddetti. Etkinleştirme denenmedi; taslak docs/archive-release.proposed.yml konumunda ve çalışmaz. Kullanıcıya neden ve somut kapsam sorulur. Kod/not kaydı ile mevcut Actions APK/ZIP dosyaları bu engelden etkilenmez. Aşağıdaki yayın davranışı tasarım açıklamasıdır. scripts/archive_release.py mevcut Actions çıktısını indirir, SHA256 ve test XML sonuçlarını doğrular, aynı APK'yı yeniden derlemeden yayımlar. APK yanında kaynak, raporlar, manifest ve ayrıntılı NOT_DEFTERI.txt içeren tam paket ZIP vardır. Her yayın kaynak sürüm ve build numarasıyla ayrıdır. Güncel teslim edilen 26.6 için başlangıç arşivi build 34111507110'dur. Başka build aynı sürüm numarasına sahip olsa bile farklı imzalı olabilir; teslim edilen APK kaynağı korunur. Bu değişiklik uygulama motorunu değiştirmez; arşivleme ve proje devamlılığı içindir.

## 26.6 teslim ve inceleme sonucu — 7 Eylül 2026

- 1000026724.mp4 incelendi (9,51 saniye): BildirimHaber1 menüsü ve hesap seçici art arda açılıp kapanıyor; aktif hesap değişmiyor. Bu kayıt takipten çıkma işlemini göstermiyor. Kullanıcının limit 5 iken 1 işlem sonrası durma bildirimi için cihaz sonucu hâlâ açık.
- Diğer sohbetin 1219b9d0f12c644e7e6287a310d05c0637f15b86 commit'indeki 26.6 düzeltmeleri incelendi; çalışan 26.5 hesap import akışı korundu. Bu incelemede yeni motor değişikliği yapılmadı.
- Test edilmiş 26.6 kaynak commit'i: 1219b9d0f12c644e7e6287a310d05c0637f15b86, dal: fix/task-switch-unfollow-26-6. Bu not commit'i aynı kaynağın devamıdır ve main'e ileri taşıma ile aktarılır. Main'in kodu 26.6 ile aynı; ek değişiklik devir notudur.
- Başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34111507110 . İndirilen XML raporlarında 108 test, 0 hata, 0 başarısız, 0 atlanan; lint, APK derleme ve imza/manifest doğrulama adımları başarılı.
- Teslim APK: AtmacaNext-26.6.apk, 20.524.752 bayt, versionCode 54; SHA256 d42ac79b403103f809ae31a128158ba26ab3cedd0555b8a79e7532aa1164314b. APK artifact 10014628414; test raporu artifact 10014629217. ZIP CRC ve APK SHA256 kontrol edildi. Manifest INTERNET ve InitializationProvider içermiyor.
- APK sertifika SHA256: 08548462e04814cb2425e2d56f414fcbf8e636709826b64a7f3f9dc09db69fd1. Teslim edilen 26.5 sertifikasından farklı; üzerine güncelleme kurulamaz. Kaldırma uygulama verilerini siler. Kullanıcıya bu somut risk belirtilir; kalıcı imzalama çözülmedi.
- Gerçek telefonda 26.6 görev hesabı geçişi ve 5 doğrulanmış takipten çıkma henüz test edilmedi. Kullanıcının mevcut kurulu sürümü videodan belirlenemiyor; videodaki hatanın 26.6'da tekrarlandığı varsayılmamalı. Sonraki çalışma önce 26.6 cihaz sonucu ve RUNTIME/NAV loglarını esas almalı.
- Aşağıdaki 26.5 test/APK bölümleri tarihsel kayıttır; güncel teslim bilgileri bu bölümdedir.

## 26.6 güncellemesi — görev hesabı seçimi / takipten çıkma

Kullanıcı 26.5 hesap eklemesini cihazında BAŞARILI doğruladı: 4 hesap, 0 okunamayan; Atmaca'ya dönüş de doğrulandı (7 Eylül 12:56:15–12:56:55). Hesap ekleme tamamlandı kabul ediliyor; bu akışı yeniden tasarlama.

Yeni sorun: aktif hesabın dışındaki iki hesap seçilerek başlatılan takipten çıkma görevinde hedefe geçilmiyor. Logda hedefin seçili olduğu söylenerek hesap seçici kapanıyor, yanlış aktif hesabın menüsünde seçici tekrar açılıyor. 4 kurtarma denemesi aynı döngüyü tekrarlıyor. Kullanıcı aktif hesapta 1 takipten çıkma sonrası durma da bildirdi; gönderilen log 12:57:46'da ilk denemenin ortasında kesiliyor, ikinci denemenin logu yok. 1000026723.mp4 47 saniye ve başarılı hesap ekleme akışını gösteriyor; görev hatasını gösteren video olarak değerlendirilmemeli.

Kodda bulunan nedenler ve 26.6 değişiklikleri:

- AccountSwitcherInspector.isHandleSelected hedef satırından ortak liste atasına çıkarak başka hesabın seçim işaretini hedefe ait sayabiliyordu. Seçim kanıtı yalnız tek kullanıcı adını içeren alt ağaçla sınırlandı; AccountRowSelectionEvidence eklendi.
- AutomationRuntime görev hesap seçicisini görünce artık seçili işaretine dayanarak satırı atlamıyor; hesap eklemede cihazda çalışan tam kullanıcı adı tıklamasını yapıyor. Sonrasında aktif menü kimliği ve kendi profilindeki hedef doğrulanıyor. Yanlış hesaptan işlem yapılmıyor.
- Menü/seçici/profil açılışına 1200 ms yerleşme beklemesi eklendi; olay akışının art arda tıklama üretmesi engelleniyor.
- Takibi bırak onayına basıldıktan sonra hâlâ görünür olan aynı pencere, genel popup kolunda görevi durdurabiliyordu. UnfollowConfirmationPolicy ile yalnız bir kez onay, kapanış için sınırlı bekleme ve gerçek zaman aşımında duraklatma eklendi.
- İlişki sonucu kontrolü hedef kullanıcı adı yaprağında durmak yerine o kullanıcıya ait düğmeyi içeren satırı arıyor. Geometrik geri dönüş yalnız dikey olarak örtüşen kullanıcı adı/düğmeyi bağlıyor; komşu satır veya üstteki Following sekmesi bağlanmıyor. RelationshipRowGeometry eklendi.
- Çelişkili Follow/Following kanıtı başarı sayılmıyor. Takip durumunun geri dönmesi tek başına günlük limit sayılıp görev tamamlanmıyor; doğrulanamayan durum duraklatılıyor. Gerçek X limit penceresinde duraklama korunuyor.
- Hesap ekleme kayıt/sayaç/harvest akışı değişmedi. İlk işlemden sonra durmanın kullanıcı cihazındaki kesin nedeni ikinci deneme logu olmadan kanıtlanmış değildir; yukarıdakiler kaynakta bulunan hatalardır.
- Sürüm: 26.6-task-account-switch, versionCode 54. 13 yeni regresyon testi eklendi; 108 testin tamamı geçti, güncel teslim ve test bilgileri en üstte.

## Kullanıcının çalışma tercihi

Kullanıcı sohbet dolduğunda başka ChatGPT hesabından devam ediyor. Yapılan bütün kod değişiklikleri, testler ve güncel durum bu depoda kalmalı. İşi yalnız sohbet mesajında veya geçici çalışma dizininde bırakma. Sonraki düzeltmelerde bu notu güncelle; hangi dalda/commit'te olduklarını ve ana dala aktarılıp aktarılmadıklarını açık yaz. Şifre, token ve özel imzalama anahtarı ekleme.

## Depo ve sürüm

- Depo: https://github.com/xbildirim1-debug/Atmaca-Next
- Ana dal: main.
- Güncel düzeltme: 26.6-task-account-switch, versionCode 54.
- Android paket adı: com.atmacanext.v258; namespace: com.atmacanext.app.
- Önceki sürüm: 26.4-reliable-navigation, versionCode 52.
- 26.5 kod commit'i: 782349ccd7f7a4c9b7808e144623d9fa5c62dbd4.
- Düzeltme dalı: fix/account-sync-home-26-5.
- İnceleme kaydı: https://github.com/xbildirim1-debug/Atmaca-Next/pull/1
- Bu devir notunu içeren commit, test edilmiş düzeltmenin devamıdır. main bu commit'e normal ileri taşıma ile güncellenir; kod yalnız düzeltme dalında bırakılmaz. PR durum etiketi yerine main geçmişini esas al.

## Son bildirilen hata ve kanıt

Kullanıcı 1000026716.mp4 videosunu ve 7 Eylül 2026 loglarını gönderdi. Video incelendi: X ana akışta kalıyor, hesap menüsü açılmıyor. Ham video bu depoya yüklenmedi.

Log özeti:

```text
12:33:12.343 screen=HOME popup=NONE
12:33:12.374 ACCOUNT_SYNC DRAWER | Hesap menüsü açılıyor
12:33:13.137 screen=FOLLOWING_LIST popup=NONE
12:33:13–28 stage=DRAWER screen=FOLLOWING_LIST saved=0 skipped=0
12:33:32.802 X ekranına ulaşılamadı
12:33:33.050 Atmaca Next'e dönüş doğrulandı
12:33:47.920 DRAWER | Hesap menüsü açılıyor
12:33:48.183 screen=FOLLOWING_LIST
12:34:07.964 Zaman aşımı stage=DRAWER target=null screen=FOLLOWING_LIST
```

Kök neden: ana akıştaki Following/Takip edilenler sekmesi ile gönderilerdeki kullanıcı adları, ilişki listesi kurallarını tetikleyebiliyor. HOME'dan DRAWER aşamasına geçtikten sonraki snapshot FOLLOWING_LIST olarak sınıflanınca yalnız HOME için çalışan menü tıklaması yürümüyor. Ham seçili sekme denetimi de aynı ana akış sekmesini gerçek takip listesi sanabiliyor. Bu bir hesap şifresi veya oturum açma hatası değil.

## 26.5'te değişen dosyalar

1. `app/src/main/java/com/atmacanext/app/automation/HomeTimelineEvidence.kt`: yeni. Görünür Sana özel/For you ile Following sekme çiftinden veya home_timeline kimliğinden ana akış kanıtı üretir; süslenmiş sekme etiketlerini destekler.
2. `ScreenDetector.kt` (aynı klasör): menü/seçici katmanları ve ana akış kanıtı, ham seçili ilişki sekmesi ve liste sezgilerinden önce değerlendirilir. Tek parametreli mevcut detect çağrısı korunur. Gerçek takip listesi ve profil ayrımları korunur.
3. `AccountSyncController.kt`: hesap menüsü ve seçici tıklamalarında 1200 ms animasyon beklemesi; tıklama sonucu logu; DRAWER/SWITCHER/VERIFY_DRAWER aşamalarında beklenmeyen ekran için sınırlı toparlanma. Var olan 3 denemelik bütçe ve hesap süre sınırları korunur.
4. `XNavigator.kt`: genel avatar/profile_image/user_image kimlikleri ve genel profil fotoğrafı etiketleri sınırsız menü seçiminden çıkarıldı. Özel menü etiketleri ve üst araç çubuğu sınırlarıyla avatar seçimi korundu; görünürlük kontrolü eklendi. Önceden var olan oran tabanlı son çare bu değişiklikte kaldırılmadı.
5. `app/src/test/java/com/atmacanext/app/automation/AccountSyncHomeRegressionTest.kt`: 9 yeni regresyon testi.
6. `app/build.gradle.kts`: versionCode 53, versionName 26.5-account-sync-home.
7. `.github/workflows/android-build.yml`: APK/artifact adları 26.5'e güncellendi.

## Test sonucu ve sınırı

Başarılı çalışma: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34108017552

- Test edilmiş kaynak commit'i: 782349ccd7f7a4c9b7808e144623d9fa5c62dbd4.
- 95 birim testi: 0 başarısız, 0 hata, 0 atlanan.
- Bunların 9'u yeni hesap ekleme regresyon testidir.
- Türkçe/İngilizce ana akış, seçili Following akışı, gönderi yazarları yüklenirken ekran ayrımı, ham seçili sekme, süslenmiş sekmeler, akış üzerindeki menü/seçici, gerçek ilişki listesi, gizli ana akış ve profil örnekleri kontrol edildi.
- testDebugUnitTest, lintDebug, assembleDebug ve apksigner verify başarılı.
- İndirilen APK manifesti ve SHA256 dosyası doğrulandı.
- Gerçek Android cihazında X oturumlarıyla uçtan uca test YAPILMADI. Birim testi ve başarılı APK derlemesi, kullanıcının telefonundaki tüm akışların çalıştığını kanıtlamaz. Bu tarihten sonra kullanıcı 26.5 hesap eklemesini cihazında başarıyla doğruladı; yeni görev sorunu en üstte kayıtlı.
- Görevlerin tamamının düzeldiği iddia edilmemeli. Hesap ekleme için son kullanıcı doğrulaması en üstte.

## Kullanıcıya teslim edilen APK

- Dosya: AtmacaNext-26.5.apk; 20.524.760 bayt.
- SHA256: aa1b6763b711951315af526151231d7f367597bfcb6e6feeab01adaa4380400c
- İlgili GitHub Actions APK artifact ID: 10013278541, adı AtmacaNext-26.5-APK.
- Test raporu artifact ID: 10013279421, adı validation-reports.
- Yukarıdaki çalışma sayfasının Artifacts bölümünde bulunur. Artifact ZIP'inin içinde APK, SHA256 ve manifest.xml vardır.
- Kullanıcıya APK doğrudan sohbetten de teslim edildi. APK ikili dosyası git kaynak ağacına eklenmedi; GitHub Actions çıktısındadır.
- Bu artifact'in mevcut son kullanma tarihi 6 Aralık 2026. Süresi dolduğunda aynı kaynaktan yeniden derlenebilir, ancak aşağıdaki imza sorunu nedeniyle byte/imza eşitliği varsayılmamalı.

## İmzalama: somut açık sorun

Mevcut CI geçici Android debug anahtarı kullanıyor. 26.4 ile kullanıcıya teslim edilen 26.5'in sertifikaları karşılaştırıldı ve FARKLI oldukları doğrulandı:

- 26.4 sertifika SHA256: 06e9ab2a6841f9d37363c1afecebe4336e96f1f95e0acee76aa2a3eef1d6c17c
- Teslim edilen 26.5 sertifika SHA256: f3022244f909fd47d13313c078b590eaeb6c3caa59fafa6a5f2067ac46f341e5

Aynı paket adıyla 26.5, önceki 26.4'ün üzerine güncelleme olarak kurulamaz. Eski Atmaca'yı kaldırmak uygulama içi verileri siler; kullanıcı bu konuda bilgilendirildi. Önceki imzalama özel anahtarı bu oturumda bulunmadı. Eski APK'dan özel anahtar çıkarılamaz. Sonraki üretim güncellemelerinde güvenli, kalıcı imzalama kurulmalı; keystore veya parolalar herkese açık depoya yazılmamalı. main'e taşıma sonrası yeniden derlenen APK'nın da bu teslim edilen APK ile aynı sertifikada olduğu varsayılmamalı.

## Derleme ve çalışma mimarisi

- Java 17; Gradle 9.5.0; compileSdk 37.1; targetSdk 36; minSdk 26; build-tools 36.0.0.
- Workflow: `.github/workflows/android-build.yml`; main push, pull_request ve workflow_dispatch tetikler.
- Komut: `gradle --no-daemon testDebugUnitTest lintDebug :app:assembleDebug --stacktrace`.
- Gradle wrapper JAR mevcut değil; CI setup-gradle kullanır.
- Yerel X Android uygulaması ve AccessibilityService kullanılır. X API/Selenium ile değiştirme.
- APK INTERNET izni istemez; internet bağlantısını X kullanır.
- Hesap import/switch: AccountSyncController; ekranlar: ScreenDetector; menü gezinmesi: XNavigator; oturum kanıtı: AccountSwitcherInspector.
- Hesap tarama kimlik ve sayaç doğrulaması yapar; eksik bilgi başarılı kayıt sayılmaz. Mevcut en fazla 10 hesap sınırı korunur.
- Tablet ve farklı ekran boyutları gereksinimi devam eder; yeni sabit piksel koordinatları ekleme.
- README ve eski V19/V25/V26 notları tarihsel içerik taşır. Örneğin eski 100 kullanıcı derinliği talebi ile README'deki 26.3'ün görünür satırdan başlama davranışı farklıdır. Bu hesap ekleme düzeltmesi takipten çıkma davranışını değiştirmedi; eski gereksinimi kendiliğinden geri yükleme.

## Sonraki asistanın başlayacağı yer

1. Bu dosyayı, README'yi, güncel main commit'ini ve app/build.gradle.kts sürümünü oku. Başka bir sohbetin daha yeni commit eklemiş olabileceğini kontrol et.
2. Hesap ekleme 26.5 cihaz doğrulaması tamamlandı. Yeni görev düzeltmesinin cihaz sonucunu esas al; henüz yapılmayan testi yapılmış gibi yazma.
3. Beklenen import akışı: HOME → DRAWER → ACCOUNT_DRAWER → SWITCHER → ACCOUNT_SWITCHER → HARVEST → SELECT → VERIFY_DRAWER → SAVING; sonra sıradaki hesap ve sonunda Atmaca'ya dönüş.
4. Sorun sürerse ACCOUNT_SYNC ve NAV loglarını, ilgili ekranın erişilebilirlik kanıtını incele. Kullanıcı adını/takip sayaçlarını okumadan hesaba başarı yazma; açılmayan menüye başarılı deme.
5. Hesap ekleme doğrulandıktan sonra çoklu hesap görev sırası, görev sonu dönüş ve diğer bildirilen görev sorunlarını ayrı kanıtlarla değerlendir. Bu değişiklik bunların tamamını doğrulamaz.
6. Mevcut telefon uygulaması kararlı olmadan daha önce ertelenen çoklu emülatör mimarisine geçme.
7. Her yeni düzeltmeyi test et, GitHub'a kaydet, bu notu son testler, APK ve kalan sorunlarla güncelle. Kullanıcı aynı talimatları başka hesapta tekrar etmek zorunda kalmasın.
## 26.16 — doğrulanmış derleme sonucu

- Kaynak commit b82e89d7d3267bcd01e783015d3a9eabbd7a2440; başarılı CI https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34219134897 . APK/tam paket artifact 10053247649, rapor artifact 10053248488.
- 178 test, sıfır hata/başarısız/atlanan. APK code64 / 26.16-native-profile-route, 20.573.912 bayt, SHA256 `afbfd5174610264f1cd825c3f0b72cc5c397c01b1cb70a6ebb391a21e28d8b0d`.
- Tam paket SHA256 `1fa266432501f141aa3fef83de35602f3640da577be6fb348f0d3b5a96b64179`; CRC, paket içi APK, manifest sürümü ve INTERNET izni yokluğu doğrulandı.
- 26.16 yerel kullanıcı rotası fiziksel telefonda henüz test edilmedi. Başarılı olursa NAV kaydı `discovery-profile/native-user/pusholder/1` sonrasında ekran `@pusholder` olmalı. Olmazsa yeni log hangi alternatif rotaların tüketildiğini açıkça gösterecek.

## 26.16 — hedef web bağlantısı X tarafından yutuluyor

26.15 cihaz logu: hedef listeden doğru `@pusholder` geliyor; `discovery-profile/pusholder/1..3` başlangıç çağrıları başarılı yazılıyor ama ekran `@bildirimhaber1` kendi profilinden ayrılmıyor. Üç çağrı da aynı x.com web URI'siydi. 26.16 ilk olarak X'in yerel `twitter://user?screen_name=` yolunu, ardından twitter.com ve x.com yollarını ayrı ayrı kullanır. CLEAR_TOP/SINGLE_TOP eklenmiştir; her rotaya 2500 ms verilir. Tam hedef handle doğrulanmadan tarama veya takip yoktur.

- code64 / 26.16-native-profile-route. Yeni rota testi eklendi; CI ve cihaz testi bekleniyor.
- 26.15 hızlı ayarları korunur. GitHub Releases yetkisi ve kalıcı imza durumu değiştirilmedi.
## 26.20 — tweet ayrıntısındaki satır içi yanıt alanı

- Kaynak main commit: 2606b92cefd2005a9c325537149b02f003481397; CI 34309944401 başarıyla tamamlandı. 211 test; 0 hata, başarısız, atlanan. Lint, assemble, imza, manifest ve paketleme geçti.
- APK: AtmacaNext-26.20.apk, code68 / 26.20-inline-reply-engagement. SHA256 a38bfb35388bb1ac99b386f7990ff0973007c52275f508d8af74fd648b573cd4. TAM-PAKET SHA256 8062dbae87207dc370570ac60e8e183ded902357fb97e811acffda6621209e4b. ZIP CRC ve sürüm manifesti doğrulandı.
- APK/tam paket artifact 10088057069; rapor artifact 10088057552. Fiziksel X cihazında 26.20 henüz denenmedi.
- 1000026996.mp4 ve 8 Eylül 23:53 cihaz logu incelendi. Hedef profil ve iki saatlik gönderi doğru açılıyor; gerçek tweet ayrıntısında Mavi Deniz ve altındaki yorumlar görünür durumda kalıyor. Log `screen=COMPOSER` bildirdiği için yorum işleme aşaması hiç başlamıyor.
- Kök neden: X, tweet ayrıntısının altındaki `Yanıtını gönder / Post your reply` alanını editable `tweet_box` olarak yayımlıyor. 26.19 bunu tam ekran oluşturucu sanıyordu. Geri düğmesi + gönderi başlığı/aksiyon araç çubuğu/Alıntıları görüntüle kanıtı bulunan satır içi alan artık TWEET_DETAIL sayılır; gerçek tam ekran yanıt oluşturucu COMPOSER kalır.
- Yorumcu adı sabit değildir. Ayrıntıdaki hedef gönderi yazarı ve görev hesabı dışlandıktan sonra ekranda yukarıdan aşağı ilk gerçek yorum yazarı seçilir, profili açılır; zaten Takip ediliyor/Beklemede ise yeni işlem yapılmadan geri dönülür. Yeni Beklemede sonucu bir işlem sayılır. Ardından aynı tweetin sıradaki yorumuna devam edilir.
- Retweetçi yolu değişmedi fakat bu ekran düzeltmesiyle artık tweet ayrıntısında çalışabilir: `Alıntıları görüntüle` açılır, sonra sayısı değişken `... tarafından yeniden gönderildi` sekmesi seçilip doğrulanır.
- İki ekran regresyon testi ve dinamik yorum sırası testi eklendi. versionCode68 / 26.20-inline-reply-engagement. Birim test/CI ve fiziksel cihaz testi henüz yapılmadı; sonuçlar tamamlanınca bu kayıt güncellenecek.
## 26.21 — yorumcu profil hedefi ve boş etkileşim döngüsü

- 1000027014.mp4, 1000027015.jpg ve 9 Eylül 07:29 cihaz logu incelendi. 26.20 tweet ayrıntısını ilk anda doğru tanıyor; Compose toolbar/back semantiği sonraki tazelemede kaybolunca alttaki editable `Yanıtını gönder` nedeniyle tekrar COMPOSER oluyor. Görünür gerçek tweet/yorum satırları artık kaydırılmış ayrıntı kanıtıdır.
- Yorum başlığının merkezine dokunmak yorumcunun profili yerine yorumun kendi tweet ayrıntısını açıyordu. Birleşik `Ad @kullanıcı · süre` başlığının profil olan sol ad/kullanıcı bölgesine dokunulur. İsim sabit değildir; hedef ve görev hesabı dışlandıktan sonra görünür yorumcular yukarıdan aşağı dinamik işlenir. Takip ediliyor/Beklemede ise işlem yapılmadan geri dönülür; yeni Beklemede limitten bir işlem sayılır.
- Yorum kalmazsa ayrıntı aşağı taranır; ekran sonu kararlıysa hedef profile dönülüp sıradaki en az iki saatlik gönderi açılır. Retweetçi görevinde `Alıntıları görüntüle` yoksa en fazla sekiz tarama veya iki kararlı ekran sonunda aynı geçiş yapılır. Limit dolana ya da uygun gönderi kalmayana kadar döngü korunur.
- versionCode69 / 26.21-comment-profile-loop. Yeni regresyon testi eklendi; CI ve fiziksel cihaz testi henüz yapılmadı.
- İlk CI 34312711274 ürün kodundan değil yeni test düzeneğinin tüm düğümleri aynı koordinatta oluşturmasından dolayı tek testte başarısız oldu; gerçek ekran bantlarını temsil eden ayrı koordinatlar verilerek test düzeltildi ve yeniden çalıştırılacak.
