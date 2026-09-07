# Atmaca Next — sohbetler arası devir notu

Son güncelleme: 7 Eylül 2026. Bu not ve kaynak kod GitHub'da tutulur; başka ChatGPT hesabından devam ederken önce bu dosyayı oku. Önceki sohbet dosyalarına erişebildiğini varsayma.

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
