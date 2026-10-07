## 26.62 güncel kullanıcı sonucu — takılma sürüyor (7 Ekim 2026)

08:46 Europe/Istanbul: kullanıcı son APK sonrasında Yorum Alıntısı'nın aynı yerde kaldığını bildirdi ve mevcut kodları istedi. Cihazda çözüldüğü doğrulanmadı; güncel kullanıcı sonucu başarısız. Önceki 643 otomatik test ve paket/imza doğrulamaları fiziksel X akışının başarı kanıtı değildir. Son mesajda yeni runtime logu veya sürüm ekranı yok.

[Mevcut yorum kodlarının dökümü](docs/Yorum-Alintisi-26.62-Kodlari.md) ve [kullanıcı sonucu / kaynak incelemesi](validation/26.62-device/USER_REPORT.md) kaydedildi. Native bağlantı metni değiştirmeden true döndürürse her yazma denemesinde ilk sırada seçilerek PASTE/SET_TEXT alternatiflerini engelleyebilir; kaynakta bu açık görülüyor. Gerçek takılma aşaması yeni log olmadan kesinleştirilemez. Bu tur uygulama kaynakları, APK ve sürüm değiştirilmedi; yeni test çalıştırılmadı. Kod dökümünün 14 kaynak dosyası teslim APK'sının f26d76c1c8676e2613fe8326d9d3442652f7cf01 ağacına blob SHA ile doğrulandı. Başlangıç main e74dc59a19f8805232cb926a23de9d60fed1ba0d; PR #11 birleşmiş, çalışma dalı fix/26.62-inline-reply / 937ff8429112717e49a6ef42ea9466f41006fd5d. Bu ek yalnız belgelerden oluşur; Buse değişmedi.

## Önceki teslim anındaki kayıtlar

## 26.62 — alt yorum alanı düzeltmesi ve aynı imzalı APK (7 Ekim 2026)

26.61'de kullanıcı cihaz testi başarısızdı: @pushholder gönderisi açıldı, alttaki Yanıtını gönder alanı boş kaldı. Görsel incelendi; paylaşılan log hesap seçme sırasında kesildiği için yorum aşamasının gerçek cihaz düğüm ağacı bilinmiyor.26.61'in622 birim test başarısı fiziksel çalışma kanıtı değildi. Bu sürüm özellikle odak ve metin giriş yolunu değiştirir;26.62 de henüz fiziksel cihazda denenmedi.

Alt kutunun ölçülen metin bölgesine fiziksel dokunuş yapılarak gerçek giriş odağı beklenir. Kabul edilmiş fakat etkisiz ACTION_FOCUS/SET_TEXT artık tek yol değildir. Android13+ odaklı/görünür/etkin X yorum alanında native erişilebilirlik giriş bağlantısı kullanılır; tam önceki taslak okunup tümü seçilerek değiştirilir. Başka paket, şifre veya eksik/stale metin reddedilir. Native bağlantı yoksa SET_TEXT ve PASTE kullanılır; PASTE'nin işlem listesinde yer almaması desteklendiğinde denemeyi engellemez. SET_TEXT kabulüne rağmen boş kalan sonraki kare PASTE yolunu kullanır. Yorumun tamamı yeni kare veya aynı odaklı canlı metin bağlantısından okunmadan Yanıtla'ya basılmaz. Kullanıcının klavyesi değiştirilmez. GIF/resim ve alt yorum eylemi seçilmez. Büyük birleşik yazar parent bounds ve aynı alanın semantik kopyaları desteklenir; hedef/post kontrolü korunur.

PR #11 main'e alındı: `f26d76c1c8676e2613fe8326d9d3442652f7cf01`. PR CI 37502501645 ve main CI 37576469307 başarılı: 643 test, 0 başarısız/hata/atlanan; 21 yeni ReplyTextInput26_62Test regresyonu dahil. Lint 0 hata/fatal, 22 uyarı. İki tarihsel SQLite5→6 veri koruma kontrolü geçti. Test edilen kaynak ZIP'in 283 dosyası main `10dac51fb14714a251e7268388dce3085fe0610f` Git blob SHA'larıyla eşleşti. [Gerçek main iş logu](validation/26.62-ci/main-job-log.txt) / [teslim doğrulaması](validation/26.62-signing/summary.json). İlk PR derlemesindeki public fonksiyon/internal sonuç tipi derleyici hatası giderildi; başarılı iki CI güncel internal writeReplyText sürümünü test etti.

`AtmacaNext-26.62-Yorum-Duzeltmesi.apk` mevcut26.61 ile aynı `9bb1488d50538b0b69427f7b49f5cc809b6b715eeb81a1867e33fe930074b7a5` sertifikasını taşır; Atmaca kaldırılmadan üzerine kurulabilir. com.atmacanext.v258 /versionCode110 /26.62-inline-reply-input /Room6 korunur. APK 20919906 bayt;SHA256 `23f88022c648ff90b80a93f421ceadb0a93ee5e021ca377c95cb69fe93402559`. Tam paket `AtmacaNext-26.62-Yorum-Duzeltmesi-TAM-PAKET.zip`, doğru imzalı tek APK, test edilmiş tam kaynak,JUnit/lint raporları, gerçek CI logu, güncel notlar, kurulum/cihaz protokolü, manifest, imza ve checksumları içerir. Kaynak ZIP içindeki hazırlık notları tarihsel kalır; bu son dış teslim kaydı esas alınır.

Derleme GitHub CI'da yapıldı; yerelde mevcut özel yedekle yeniden imzalandı. İmza dışındaki 151 APK ZIP girdisi başarılı main CI çıktısıyla byte olarak aynı. v2/v3 imza,26.61 sertifika eşleşmesi, manifest110,16KB ZIP hizalaması, APK/tam paket CRC ve iç/dış APK eşitliği geçti. Gönderim tek kez yapılır; eski bildirim/alanın boşalması başarı sayılmaz. Önceki pending, ilerleme, toplu kuyruk ve ilk100 koruması korunur. Buse değiştirilmedi. Özel anahtar depoya/pakete eklenmedi.

Yeni cihaz kabul adımları DEVICE_TEST_PROTOCOL.md'de. QUOTE_REPLY_TARGET/QUOTE_REPLY_OPEN/QUOTE_REPLY_WRITE/QUOTE_REPLY_SUBMIT ve REPLY_INPUT gerçek tanı aşamalarını kaydeder; yorum metni loga basılmaz. Yeni otomatik test düzeni görselden kurulan temsili semantik düzendir; gerçek cihaz dökümü olarak gösterilmez. Fiziksel26.62 cihaz testi henüz yapılmadı; cihazda çalıştığı veya hatasız olduğu iddia edilmez.

GitHub Releases yayını yapılmadı; AGENTS.md'deki önceki otomatik onay reddi nedeniyle contents:write workflow'u etkinleştirilmedi. contents:read CI korunur. APK ve tam paket ayrı 26.62 adlarıyla hazırlandı ve kaydedildi; eski 26.61 dosyaları değiştirilmedi. Kaydetme sonrası iki dosyanın boyut ve SHA256 değerleri tekrar eşleşti. Tam paket 21371181 bayt; SHA256 `5bf226c6bc189f45e35b9cc397f535fcf6ddde2ae1dd99bc385b312336e0e997`. Uygulama kaynakları başarılı CI sonrasında değiştirilmedi; bu son commit yalnız teslim notu ve doğrulama kayıtlarını ekler.

## 26.62 hazırlık kaydı ve önceki sürümler

# Atmaca Next 26.62 — boş kalan alt yorum alanı

6 Ekim 2026. Kullanıcı26.61'i telefonda denedi: @pushholder gönderisi açıkken alt “Yanıtını gönder” alanı boş kaldı. 1000033708.jpg görseli incelendi. Paylaşılan log @xhesaplar1 / limit5 /20:03 hesabı seçme sırasında kesiliyor; yorum aşamasının kesin cihaz ağacı/logu yok.26.61'in622 test başarısı cihazda yorumun çalıştığı kanıtı değildi; bu gerçek başarısızlık düzeltme kapsamıdır.

Temel main de920ec56a3d2052fb2d214cd66153bc77331c69;280 dosyanın Git blob SHA'sı yerelle eşleştirildi. Çalışma dalı fix/26.62-inline-reply. com.atmacanext.v258 / versionCode110 /26.62-inline-reply-input /Room6. Buse ve diğer görevler korunur.

Alt yanıt alanı önce seçilir; tıklama/odak erişilebilirlik eyleminin kabul edilmesi gerçek klavye odağı sayılmaz. Alanın ölçülen metin bölgesine fiziksel dokunuş yapılır. Odak beklenirken üç yazma hakkı tüketilmez. İkinci alternatif, ana gönderinin Yanıt toolbar'ıdır; alt yorumun toolbar'ı değildir. Yerel TR/EN alan rolü ekleri ve aynı kutunun çakışan semantik kopyaları desteklenir. Büyük yazar üst düğümü tüm gönderiyi kaplıyorsa onun küçük gerçek @handle çocuğu metin bandı için seçilir.

Android13+ erişilebilirlik FLAG_INPUT_METHOD_EDITOR etkinleştirilir; kullanıcının klavyesi değiştirilmez. Sadece odaklı, görünür, etkin X yorum kutusu ve X metin EditorInfo'suna bağlanılır; diğer paketler, sayısal/şifre alanları reddedilir. Tam giriş metni offset0 ve4096'dan kısa doğrulanır; eski taslak tümü seçilip commitText ile değiştirilir. Eski/sınırlı bağlantı yazma veya gönderim kanıtı değildir. Native yol yoksa SET_TEXT ve PASTE kullanılır; X'in PASTE işlem listesinde reklam vermemesi artık yapıştırmayı engellemez. SET_TEXT true döndüğü halde yeni karede metin boşsa ikinci deneme PASTE yolunu kullanır. Tam metin, yeni erişilebilirlik karesi veya aynı odaklı alanın canlı giriş bağlantısından okunmadan Yanıtla'ya basılmaz. Gönderim ve pozitif sonuç kanıtı, kalıcı pending/tek gönderim koruması26.61'den korunur.

QUOTE_REPLY_TARGET, QUOTE_REPLY_WRITE ve REPLY_INPUT tanı kayıtları hedef kanıtını, gerçek giriş yolunu ve odağı kaydeder; yorum metni loga basılmaz. Metin/odak/seçim olayları da taze okumayı tetikler. Görselden kurulan test düzeni gerçek cihaz ağacı dökümü diye sunulmaz.

21 yeni ReplyTextInput26_62Test regresyonu: gerçek odak öncesi yazmama; native boş alan/taslak değiştirme/Türkçe-emoji; etkisiz true SET_TEXT sonrası PASTE; eksik native okuma; çift eklememe; paket/şifre guard'ları; görselin alt alanı/medya/GIF ayrımı; büyük yazar bounds; alan rolü/kopyaları; yeni metin kanıtı ile gönderim. Önceki toolbar öncelik testi yeni alt-alan önceliğine güncellendi. Beklenen toplam643. git diff --check ve iki tarihsel SQLite5→6 veri koruma kontrolü geçti. Yeni CI/test/lint/APK sonucu henüz yok. Yeni26.62 fiziksel cihaz testi yapılmadı. Aynı9bb1488d50538b0b69427f7b49f5cc809b6b715eeb81a1867e33fe930074b7a5 sertifikalı güncelleme CI geçince hazırlanacak; özel anahtar depoya/pakete eklenmez. contents:read CI korunur; önceki otomatik onay reddi olan Releases workflow'u etkinleştirilmez.

Android resmi API referansları: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#getInputMethod() ; https://developer.android.com/reference/android/accessibilityservice/InputMethod.AccessibilityInputConnection .

## Önceki26.61 hazırlık/teslim ve daha eski kayıtlar

## 26.61 — Yorum Alıntısı tamamlandı, aynı imzalı APK hazır (6 Ekim 2026)

Yorum Alıntısı, Yeni Görev Akışı'nın üçüncü kategorisinde ve toplu görevlerde çalışacak şekilde motora bağlandı. Her hesap kendi aktif Alıntı Hedeflerini kullanır; görevde yazılacak yorum ve hedef başına 1–20 gönderi limiti seçilir. Hedefi olmayan hesap için görev oluşturulamaz. Görev düzenlemesinde ilerleme ve gönderilmiş yorum anahtarları korunur; yeni eklenen hesap başka hesabın kayıtlarını devralmaz. Bu özellik hedef profillerin son gönderilerine belirlenen yorum metnini yazar.

Hedef gönderiyi açıp orada kalma nedeni, yanıt alanı/etiket seçiminin dar olması ve gönderinin daha yorum gönderilmeden işlenmiş sayılmasıydı. Motor aynı taze ağaçta hedef yazar/metnini doğrular, yanıt alanını açar, metni aktarır ve tam metni yeniden okuduktan sonra bir kez gönderir. Eski bildirim, eski yorum veya yalnız alanın boşalması başarı sayılmaz. Gönderim öncesi gezinme 10 saniyede aynı hedef/ilerlemeyle Atmaca üzerinden kurtarılır. Gönderim sonucu belirsizse ilerleme ve bekleyen anahtar korunur; yorum ikinci kez körlemesine gönderilmez. Yeni kendi yorumunun veya yeni gönderildi bildiriminin kanıtı aranır.

PR #10 main'e alındı: `0041a460175396944ceddda1de35d714c3215e25`. PR CI 37498333393 ve main CI 37498898230 başarılı: 622 test, 0 başarısız/hata/atlanan; 29 yorum akışı + 13 görev bağlantısı yeni regresyonu geçti. Lint 0 hata/fatal, 21 uyarı. İki tarihsel SQLite 5→6 veri koruma kontrolü geçti. PR checkout/feature/main aynı `99c3be11424b4791f6bd141eec81703c2b6afae6` ağacını taşır. Test edilen kaynak ZIP'in 274 dosyası main Git blob SHA'larıyla eşleşti. [Gerçek main iş logu](validation/26.61-ci/main-job-log.txt) ve [teslim doğrulaması](validation/26.61-signing/summary.json) kaydedildi.

`AtmacaNext-26.61-Yorum-Alintisi.apk` önceki yerel26.59 ve teslim26.60-Guncelleme ile aynı `9bb1488d50538b0b69427f7b49f5cc809b6b715eeb81a1867e33fe930074b7a5` sertifikasıyla imzalandı. Mevcut Atmaca kaldırılmadan üzerine kurulabilir. com.atmacanext.v258 / versionCode109 / 26.61-quote-task-flow / Room6 korunur. APK 20903522 bayt, SHA256 `1e93ca711c439dc61a3bb56eaa8196e6df43c3c24e77ee45492b6fb65f0fb4d5`. Tam paket `AtmacaNext-26.61-Yorum-Alintisi-TAM-PAKET.zip`; doğru imzalı tek APK, tam test edilmiş kaynak, JUnit/lint raporları, gerçek CI logu, güncel notlar, kurulum/cihaz protokolü, manifest, imza ve checksum içerir. Tarihsel hazırlık notları kaynak ZIP'te kalır; bu dış son teslim kaydı esas alınır.

Derleme GitHub CI'da yapıldı; APK yerelde mevcut özel yedekle yeniden imzalandı. İmza dışındaki 151 APK ZIP girdisi CI APK'sıyla byte olarak aynı. v2/v3 imza, önceki26.60 sertifika eşleşmesi, manifest109, 16KB ZIP hizalaması, APK/tam paket CRC ve iç/dış APK eşitliği doğrulandı. Özel anahtar depoya veya teslim paketine eklenmedi. Buse değiştirilmedi. Fiziksel yeni Android/X testi yapılmadı; cihaz kabul adımları DEVICE_TEST_PROTOCOL.md'de. Hatasızlık veya her X sürümünde garanti iddia edilmez.

GitHub Releases yayını yapılmadı; AGENTS.md'deki önceki otomatik onay reddi nedeniyle contents:write yayın workflow'u etkinleştirilmedi. contents:read derleme CI'sı korunur. APK ve tam paket ayrı 26.61 adlarıyla kaydedildi ve bu sohbette teslim edilir; eski sürüm dosyaları değiştirilmez. Tam paket SHA256 `19faef4a3c924746131328bfe1c6c5c467028ca7e61da5b968b62156483722e2`; 21298942 bayt. İki dosyanın kayıt sonucu ve yerel kimlik aktarımı başarılıdır.

## 26.61 hazırlık kaydı ve önceki sürümler

# Atmaca Next 26.61 — Yorum Alıntısı görevi ve yanıt akışı

6 Ekim 2026. Temel main: 2d09fa76fdc3e8e3d181432add8c64d7fcc476af. Yerel çalışma dalı fix/26.61-quote-comment. com.atmacanext.v258 / versionCode109 / 26.61-quote-task-flow / Room6; Buse ve takipten çıkma özellikleri korunur.

Yorum Alıntısı, genel Yeni Görev Akışı ekranına üçüncü kategori olarak eklendi. Üstteki üç sekme telefonda yatay kaydırma gerektirmeden görünür. Toplu hesap seçiminde her hesabın kendi aktif Alıntı Hedefleri, normal hedeflerden ve diğer hesaplardan ayrılarak en fazla 5 farklı geçerli handle ile kaydedilir. Limit hedef başına 1–20 gönderidir; toplam limit hedef sayısı × limit × tekrar. Hedefi olmayan seçili hesap için görev oluşturulamaz. Ortak editörde yorum metni, hedef sayısı, Limit/Tekrar/Aralık görünür. Düzenlemede mevcut ilerleme ve gönderim anahtarları korunur; yeni eklenen hesap başka hesabın hedef/gönderim anahtarlarını devralmaz. Yeni kayıt topluca transaction içinde kaydedilir.

Motor düzeltmeleri: hedef gönderi, yorum gönderilmeden işlenmiş sayılmaz. Açılmayan veya geç yüklenen gönderi yeniden başlatmada atlanmaz. 2000 düğümlük aynı taze ağaç üzerinde yanıt alanı/düğmeleri seçilir; farklı UNKNOWN/inline/Compose yüzeylerinde hedef yazar + gerçek metin kanıtı korunur. Uzun gönderinin ayrı paragraf düğümleri yalnız ana gönderinin bandında birleştirilir; alt yorum metni hedef kanıtı olamaz. Kısa açık tweet_text içeriği okunur. Yanıt alanı önce odaklanır; ACTION_SET_TEXT çalışmazsa desteklenen ACTION_PASTE ile metin aktarılır. Taze ekranda tam metin okunmadan gönderilmez. Compose düğmesinin tıklanabilir olmayan etiket alt düğümü ve TR/EN düğme rolü ekleri desteklenir; alt yorumdaki Yanıtla gönderim düğmesi sayılmaz.

Gönderim öncesi ilerleme kalıcı kaydedilir; callback aynı görev/session üzerinde otomasyon iş parçacığına alınır. Tek gönderim dokunuşundan önce pending kaydedilir; dokunuş kabulü belirsizse yalnız sonuç okunur. Gönderilmiş bir yorum loading ekranında Back ile terk edilmez. Yanıt alanının boşalması tek başına başarı değildir: taze gönderildi bildirimi veya aynı yorum akışındaki yeni kendi yorumunun metni gerekir. Eski bildirim/önceden var olan yorum yeni başarı değildir. Aynı akışta en fazla üç sonuç okuma kaydırması vardır. Gönderimden önce takılma 10 saniyede Atmaca üzerinden aynı hedef ve ilerlemeyle kurtarılır; sonuç belirsiz bir gönderim yeniden uygulanmaz. Gönderilmemiş ama kaydı tamamlanmış manuel duraklatma sürdürülebilir; gerçekten gönderilmiş yorumun kaybolan pending durumu tekrar göndermeyi açmaz.

Değişen davranış dosyaları: TasksScreen.kt, TasksScreen26_43.kt, QuoteTaskSetupPolicy.kt, QuoteReplyFlowPolicy.kt, AutomationRuntime.kt, ReplyComposerEvidence.kt, XUiActions.kt, TweetContentEvidence.kt. app/build.gradle.kts sürümü, contents:read CI dosya adları ve verify_delivery.py yeni 42 regresyonun dahil edilmesine güncellendi. İmza özel anahtarı depoya eklenmez.

Doğrulama: git diff --check ve iki tarihsel SQLite5→6 koruma kontrolü yerelde geçti. QuoteTaskSetup26_61Test13 + QuoteReply26_61Test29 yeni regresyon; beklenen toplam622. Yeni CI/test/lint/APK sonucu henüz yok; önceki580 başarı26.60'a aittir. Fiziksel yeni Android/X testi yapılmadı. CI geçince önceki yerel26.59 ve teslim26.60 ile aynı 9bb1488d50538b0b69427f7b49f5cc809b6b715eeb81a1867e33fe930074b7a5 sertifikasıyla APK hazırlanacak; bu anahtar özel yedekte kalır. GitHub Releases workflow'u AGENTS.md'deki eski otomatik onay reddi nedeniyle etkinleştirilmez; eski sürümler değiştirilmez.

## Önceki 26.60 ve daha eski sürüm kayıtları

## 26.60 — aynı imzalı güncelleme APK'sı hazır (6 Ekim 2026)

İmza engeli çözüldü. Başarılı main CI 37450117619 çıktısı, önceki yerel 26.59'un özel yedekteki anahtarıyla yeniden imzalandı; uygulama kodu yeniden derlenmedi veya değiştirilmedi. Sertifika SHA256 `9bb1488d50538b0b69427f7b49f5cc809b6b715eeb81a1867e33fe930074b7a5` iki sürümde aynıdır. `AtmacaNext-26.60-Guncelleme.apk` önceki `AtmacaNext-26.59-Yerel-Test.apk` üzerine güncelleme olarak kurulabilir. Eski CI APK'ların farklı sertifikalarıyla uyum iddia edilmez. Buse değiştirilmedi. Uygulama kimliği com.atmacanext.v258, versionCode108, Room6 korunur.

APK: 20883042 bayt; SHA256 `e1722e6b0e042a32d0dcce7a301d13fa6e59512e1d400f4b158726a782ef1211`. Tam paket: `AtmacaNext-26.60-Guncelleme-TAM-PAKET.zip`. Paket, doğru imzalı tek APK, test edilen kaynak ZIP, 580 testin JUnit/lint raporları, gerçek main CI logu, güncel notlar, cihaz protokolü, manifest, imza ve checksumları içerir. Kaynak ZIP doğrudan test edilen `6aaa71ab25886a7e06c432c90203be9ca17b3490` commit'inden gelir; içindeki tarihsel hazırlık notlarından sonra bu dış güncel teslim kaydı esas alınır. Son doğrulama [validation/26.60-signing/summary.json](validation/26.60-signing/summary.json) içindedir.

580 test / 0 başarısız-hata-atlanan, lint 0 hata / 21 uyarı main CI sonucudur; yerelde XML toplamı yeniden doğrulandı. 264 kaynak dosyasının Git blob SHA'ları eşleşti. İmzalama sonrasında APK'nın imza dışındaki 151 ZIP girdisi main CI ile birebir aynı; v2/v3 imza, sertifika eşleşmesi, manifest, 16KB zip hizalaması ve ZIP CRC geçti. Fiziksel yeni Atmaca/X testi yapılmadı. Takip Etmeyenleri Çık toplu görevi, ilk 100 kişiyi koruma ve 10 saniyede aynı ilerlemeyle Atmaca'ya dönüp yeniden başlatma bu APK'dadır.

Kaynak/testler main ve feature/26.60-nonfollowers-recovery dalında; bu son kayıt sadece doğrulama/not dosyalarını günceller. İki teslim dosyası ayrı Guncelleme adıyla saklanır; eski CI/yerel APK'lar değiştirilmez. GitHub Releases yayını hâlâ yapılmadı; AGENTS.md'deki önceki otomatik onay reddi nedeniyle contents:write yayın workflow'u etkinleştirilmedi. Özel anahtar depo veya teslim paketine eklenmedi.

## 26.60 CI doğrulaması ve imza çözümünden önceki hazırlık kaydı

# Atmaca Next — 26.60: takip etmeyenleri çık ve ortak kurtarma

6 Ekim 2026. Temel GitHub main: 8b467a3baaa991abaa3202dca78ff9299ad1c2c0 (26.59). Uygulama kimliği com.atmacanext.v258; versionCode 108; versionName 26.60-nonfollowers-recovery; Room şeması 6.

Kullanıcı Buse 1.3'ün cihazda güzel çalıştığını doğruladı ve aynı görevi Atmaca'nın toplu görev ekranında Takipten çık'ın hemen altında istedi. Yeni UNFOLLOW_NON_FOLLOWERS görevi hesap seçimi, Limit, Tekrar, Aralık, tek/toplu başlatma ve hesap bazlı ortak döngüye bağlandı. İlk 100 farklı kişi korunur; 101. kişiden daha eskilere gidilir. Yalnız kişinin kendi Seni takip ediyor/Follows you etiketi olmayan, tam ve kimliği doğrulanan satırlar işlenir. Buse 1.3'ün örtüşen hızlı kaydırma, tek taze ağaç okuması, 16 ms/32 ms kararlı görünüm, 22 ms hareket + 120 ms sabit bırakma ve üç gerçek değişmeyen kaydırma ile liste sınırı mantığı taşındı. Yeni görev Buse'nin /70 zamanlamasını kullanır; diğer görevlerde Atmaca'nın mevcut kullanıcı hız ayarı korunur. Normal ve yeni takipten çıkma limiti tamamlanmadan COMPLETED olmaz.

Yorumcu takip günlük kanıtı: 6 Ekim 07:59:56, @saresirinnn, 4/35, cycle 1/1, screen HOME, stage RETURN_ENGAGEMENT, lastTarget kirmizituborg48. Kodda ana yorumlara dönüş 6 saniyede PAUSED yapılıyordu; AutomationStallPolicy PAUSED'ı izlemiyordu ve mevcut 10 saniyelik yol sadece aynı aşamayı tekrar okuyordu. Ayrıca her TWEET_DETAIL ekranı çocuk yorum sayıldığından geç yüklenen ana yorum yüzeyine fazladan Back basılabilirdi. Günlük tek başına HOME'a hangi dokunuşla gidildiğini kanıtlamaz; bu sınıflandırma kaynakta bulunan olası fazladan Back nedenidir.

Artık Back için çocuk başlık kimliği gerekir. Geçici gezinme hataları RECOVERING olarak 10 saniye denetiminde kalır; düzelmezse Atmaca'ya dönüş, aynı görev/session/ilerleme ve döngüyle aktif hesabın yeniden doğrulanması ve X'te yeniden başlatma yapılır. İşlenen kişiler, atlanan yorumlar, mevcut hedef indeksi, döngü başlangıç ilerlemesi ve kuyruk sahipliği korunur. 4/35 sıfırlanmaz. Genel watchdog tüm görev türlerinde bu yolu kullanır. Bekleyen gönderim/eylem tekrar uygulanmaz; yalnız sonucu doğrulanır. Kullanıcının pause/stop'u, cihaz duraklaması, X limiti, belirsiz gönderim ve planlı döngü beklemesi otomatik devam ettirilmez. Aynı ilerlemede en fazla üç gezinme yeniden başlatması vardır; yeni doğrulanmış başarı bütçeyi sıfırlar. Eski callback başka görev/session veya manuel pause/stop sonrası çalışamaz.

**26.60 CI doğrulaması tamamlandı: 580 test geçti; 0 başarısız/hata/atlanan.** Yeni 98 regresyonun (23 ortak kurtarma + 75 yeni görev/tarama/kuyruk/zamanlama) tamamı geçti. Lint 0 hata / 0 fatal / 21 uyarı. SQLite 5→6 koruma kontrolü, APK imza geçerliliği, uygulama kimliği/version108, manifest INTERNET/Startup sağlayıcısı yokluğu, APK/tam paket CRC ve paket içindeki APK byte eşitliği geçti. Fiziksel yeni Atmaca/X testi yapılmadı.

Uygulama kaynağı main'e alındı: 6aaa71ab25886a7e06c432c90203be9ca17b3490; çalışma dalı feature/26.60-nonfollowers-recovery aynı uygulama kaynağını taşır. PR #9 kapandı ve merged=true. Başarılı PR CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/37449294716 ; gerçek test checkout'u 6c04b2b86a32e6f35b1d0466c9bd4596d4824125. Her iki commit'in ağaç SHA'sı aynı: f1e55fd4840e55f392aad4c86322546666b9113c. Main'deki aynı kaynak için CI 37450117619 da başarılı tamamlandı: yine 580 test / 0 başarısız-hata-atlanan, lint 0 hata / 21 uyarı; APK/manifest/sertifika/ZIP CRC/byte eşitliği kontrolleri geçti. Gerçek main job logu validation/26.60-ci/main-job-log.txt içindedir. Main CI APK SHA256 7dba9deba91cc4384ab3ae5ad0f39dff22cebb6ee7d86848b8c96decc2ea66d4; main tam paket SHA256 7e797daa7767cceb4372f96270b3bbc8a4127d9d5ccb0fc0f7dffc19e858dad4. Main CI sertifikası bbbe7bcb56a580e24221a3370cdb413bdc39e0c0fe64001099ac346579be541a de yerel 26.59'dan farklıdır; aynı imzalı kurulum APK'sı engeli sürer. İlk PR derlemesinin eksik nodes parametreli iki çağrısı 6aaa71a'da düzeltildi; başarılı sonuç düzeltilmiş kaynağındır.

CI APK: AtmacaNext-26.60.apk, 20868832 byte, SHA256 537292be5a42fb5008e48e529c2b47b911cd6f9dd614bd585b15df138b9d847e. CI tam paket: AtmacaNext-26.60-TAM-PAKET.zip, 20939558 byte, SHA256 3d154a7110a207a1eae37782eb0c15cb47ee5bb797b8a52728fee1247aa5396a. APK ve kaynak/test/not/manifest/sertifika/doğrulama JSON'lu tam paket artifact 11405601510 içindedir; gerçek lint/JUnit raporları artifact 11406955173 içindedir. Özet ve gerçek iş logu validation/26.60-ci altında depoya kaydedildi.

**Üzerine kurulum engeli açık:** bu CI APK'nın sertifikası 57d25d1f1d3549efcaa39f8e629c490ff7ea99842c4d201ffb15e7fbceefb4ca; önceki yerel 26.59/Buse imzası 9bb1488d50538b0b69427f7b49f5cc809b6b715eeb81a1867e33fe930074b7a5 ile farklı. CI paketi mevcut yerel 26.59'a güncelleme olarak teslim edilmiş sayılmaz. Uygulamayı kaldırmak yerel kayıtları siler; kaldırma önerilmez. Özel anahtar ayrı özel yedekte kaldı, CI/depo/pakete konmadı. Yerel ortam unavailable ve download_file da 'requires a ready execution workspace' ile engellendi; aynı imzalı yerel APK'nın tamamlanması ve kalıcı APK/ZIP teslimi bu nedenle açık. Çalışma ortamı geri geldiğinde mevcut 26.59 anahtarıyla aynı main kaynağını derle, gerçek sertifikayı karşılaştır, APK ve tam paketi kalıcı kaydet.

Buse değiştirilmedi. contents:read CI korundu; contents:write Releases otomasyonu etkinleştirilmedi. GitHub Releases kalıcı yayını yapılmadı; süreli Actions artifacti kalıcı teslim olarak gösterilmedi.

## Önceki sürümlerin ayrıntılı kayıtları

## 26.59 tarihsel devir notu

Güncelleme: 5 Ekim 2026. Bu belge başka sohbet veya hesap üzerinden çalışmaya devam edecek kişinin **uygulamayı ve son değişiklikleri** devralması içindir. Anlık Actions durumu ve yeniden deneme kayıtları [BUILD_STATUS.md](BUILD_STATUS.md) içindedir.

## Güncel temel ve çalışma kuralı

- Güncel kaynak sürümü **26.59-unfollow-recovery**, versionCode **107**, uygulama kimliği **com.atmacanext.v258**, Room şeması **6**; minSdk 26 / targetSdk 36. Kaynak yama commit'i: `fec95b5b8dacd12f5ba0dc2745eb21121319d708`.
- En son **CI doğrulanmış** APK **26.58-batch-cycles**; kaynak `8af17d7a51e9565d5bf403bfde42698836c370ed`. **26.59 yerelde doğrulandı:** 482 test / 10 yeni regresyon geçti, lint 0 hata / 16 uyarı, APK ve tam paket kontrolü başarılı. Test edilen kaynak `21f162ea33490dc3fd22584459605ab41f9c2f4d`; uygulama yaması aynı `fec95b5`. GitHub CI üç denemede çalıştırıcı atayamadı; yerel başarı CI başarısı değildir. Fiziksel X testi henüz yok. Yerel APK imzası 26.58 CI APK ile farklıdır. Kanıt ve teslim bilgileri [BUILD_STATUS.md](BUILD_STATUS.md) / [doğrulama özeti](validation/26.59-local/summary.json) içindedir.
- Kaynak ve testler GitHub **main** üzerindedir. Son doğrulama güncel main'in ayrı klonunda **main** dalında yapıldı; önceki yama çalışma dalı **fix/26.59-unfollow-recovery** tarihsel kayıttır. Devralırken önce uzak main'i doğrula: daha önce çalışma alanı eski 26.55 kopyasına dönmüştü. Eski kopyaya yeni düzeltmeleri yeniden uygulamak güncel özellikleri kaybettirebilir.
- Kullanıcı yalnız sorunlu alanın düzeltilmesini istiyor. Uygulamayı baştan yazma, çalışan modülleri gereksiz değiştirme; tablet, hız, hesap doğrulaması, işlem sayacı ve önceki düzeltmeleri koru.

## Uygulamanın yapısı ve görev akışları

Android AccessibilityService, cihazdaki X uygulamasının ekranlarını okuyup eylemleri yürütür. Hesap senkronizasyonu açık X oturumlarını seçerek kimlik ve sayaçları doğrular; sonuçları Atmaca'ya kaydeder. Görev başlamadan seçilen X hesabı doğrulanır; yalnız hesap adına tıklanmış olması yeterli değildir.

Takip görevleri onaylı kullanıcı, yorumcu ve retweetçi takibi ile takibi bırakmayı kapsar. Etkileşim ve tweet/alıntı işlemlerinin önceki akışları da korunur. Görev ilerlemesi yalnız doğrulanan sonuçlardan hesaplanır; tıklama başarısı işlem başarısı değildir. Görev/oturum, kişi kimliği, döngü ve toplam ilerleme birbirine karıştırılmamalıdır.

- **AutomationRuntime.kt:** görev aşamaları, bekleyen işlem sonucu, sayaç, hesap doğrulaması ve kurtarma.
- **TaskOrchestrator.kt:** seçili hesapların görev kuyruğu, tur sırası, ortak bekleme, otomatik sonraki tur ve Atmaca'ya dönüş.
- **AccountSyncController.kt:** hesap ekleme/senkronizasyon aşamaları.
- **XNavigator.kt / ScreenDetector.kt:** ekran tanıma ve gezinme; telefonun alt menüsü ile tabletin yan menüsü desteklenmelidir.
- **BatchCyclePolicy.kt:** sonraki tura girecek görevler ve dakika/saniye geri sayımı.
- **UnfollowRecoveryPolicy.kt:** uygulanmamış takibi bırak denemesinin yeniden başlatılabileceği koşullar.

Dosyalar `app/src/main/java/com/atmacanext/app/` altındadır; BatchCyclePolicy `domain/engine/`, diğer motor dosyaları `automation/` içindedir. Kullanıcıya görünen durumlar `ui/screens/` altında gösterilir.

## Korunacak güncel davranışlar

| Alan | Kaynaktaki davranış / korunacak düzeltme |
| --- | --- |
| Toplu görev ve döngü (26.58) | 4 hesap × 2 döngüde önce 1→2→3→4 hesaplarının ilk turu yapılır. Ardından ortak aralık beklenir, kalan görevler aynı sırayla ikinci tura girer. İlk hesap tek başına beklemeye alınmaz. Son turdan sonra ek döngü beklemesi yoktur. |
| Geri sayım (26.58) | “Sonraki döngüye N dakika M saniye kaldı” her saniye güncellenir. Ortak bekleme son hesabın turu bitince başlar; farklı aralıklarda kalan görevlerin en uzun aralığı kullanılır. Süre bitince kuyruk otomatik tetiklenir. |
| Planlı bekleme ve kullanıcı kontrolü | WAITING_INTERVAL, 10 saniyelik hareketsizlik değildir. Manuel duraklatma/durdurma ve oturum değişikliği gecikmiş otomatik başlatmayı engeller. Duraklatılan döngü aynı bitiş zamanı üzerinden sürdürülür. |
| Onaylı kaynak değiştirme (26.54/26.57) | Listede uygun aday biterse aynı onaylı listeden ziyaret edilmemiş başka profil denenir. Liste boşsa veya yalnız kendi hesap satırı varsa önceki kaynak profile dönülür; normal takipçilerinde ziyaret edilmiş ilk kişi dışlanarak ikinci, sonra üçüncü kişi ekran sırasıyla denenir. Eksik kota tamamlandı sayılmaz. |
| Takip düğmeleri (26.53/26.56) | “Geri takip et” uygun takip eylemi olarak ele alınır. “İstek gönderildi/Beklemede” kalıcı duraksamaya yol açmamalıdır. Sonucu belirsiz bir eylem başarı sayılmaz veya körlemesine tekrar tıklanmaz. |
| Yorumcu/retweetçi gezinmesi (26.52/26.53) | Gereksiz hedef profilden çıkıp tekrar açma azaltılmıştır. İç içe yorum, takip düğmesi bulunmayan profil ve mesaj gibi farklı düğmeler için geri dönüş akışı korunur. Medya atlama kararları ilgili satıra ait olmalıdır; komşu satırın görseli nedeniyle uygun kişi atlanmamalıdır. |
| Yorum alıntısı (26.51) | Hedef gönderi doğrulanır, yorum metni doğru alanda yeniden okunur; gönderim tek kez yapılır. Belirsiz gönderim aynı yorumu yeniden yayımlamamalıdır. |
| Cihaz ve hız (26.54/26.55) | Tabletin soldaki menüsü ve farklı ekran sınırları dikkate alınır. 7× hız seçeneği korunur; hız artırımı ekran/hesap/sonuç doğrulamasını kaldırmaz. Her cihazda kusursuz çalıştığı henüz fiziksel olarak doğrulanmış değildir. |

## Son yama: 26.59 takibi bırak kurtarması

Kullanıcının ekran görüntüsünde `@atmaca2025` hesabı, `@azizsisman_` için onay penceresi açılmayınca **16/20, döngü 2/2** durumunda PAUSED'a geçiyordu. PAUSED hareketsizlik denetimine girmediğinden Atmaca'ya dönüp yeniden tetikleme yapılmıyordu.

Yama, bu özel durumda hemen PAUSED'a geçmek yerine sonucu okumaya devam eder. **Onay verilmemiş**, aynı kişi hâlâ **Takip ediliyor** halinde **en az 1 saniye kararlı** görülmüş ve denemeden itibaren **10 saniye geçmişse**, Atmaca'ya dönüş denenir ve X hesabı yeniden doğrulanarak aynı görev sürdürülür. Doğrulanmış 16/20 ilerleme, ikinci döngü, döngü başlangıç ilerlemesi ve işlenen/atlanan kişiler korunur. Sorunlu kişi tekrar işlenmez; yalnız uygulanmadığı kanıtlanan denemenin bütçesi geri verilir.

Onay verilmiş, kişi satırı kaybolmuş, düğmeler çelişkili veya sonuç belirsizse bu kural ilave işlem üretmez. Manuel pause/stop veya farklı session, dönüş callback'ini iptal eder. Atmaca dönüşü doğrulanamazsa log doğru sonucu yazar; dönüş başarılıymış gibi gösterilmez.

Davranış değişikliği **AutomationRuntime.kt** ve yeni **UnfollowRecoveryPolicy.kt** ile sınırlıdır; **UnfollowRecovery26_59Test.kt** içinde 10 regresyon eklendi. 26.58 tur/döngü ve diğer modüller yeniden yazılmadı.

## Doğrulama, kalan işler ve teslim

- 26.58: **472 test geçti**, lint 0 hata / 21 uyarı; APK ve paket kontrolü tamamlandı. Bu sonuç 26.59'un geçtiği anlamına gelmez.
- 26.59: **482 test / 10 yeni regresyon yerelde geçti**, 0 başarısız/hata/atlanan; lint 0 hata / 16 uyarı. Yerel APK/tam paket, imza/manifest/CRC/checksum ve SQLite 5→6 doğrulandı. GitHub CI çalıştırıcı atanamadığından başarısız; CI APK yok. Yerel APK ayrı Yerel-Test adıyla teslim edildi. Yeni yamayla fiziksel X testi yapılmadı.
- Öncelikli cihaz kontrolleri: 4 hesap/2 turda sıra ve sayaç; bekleme sonunda otomatik tetikleme; 16/20 takibi bırak takılmasında Atmaca dönüşü ve aynı ilerlemeden devam; manuel durdurmanın sonradan bozulmaması.
- Sonraki kontroller: ilk hesap senkronizasyonu; “Geri takip et/Beklemede”; boş onaylı listeden ikinci/üçüncü takipçiye geçiş; tablet yan menüsü; iç içe yorumdan doğru listeye dönüş. Geçmiş kullanıcı sorunları kod düzeltmesi bulunduğu için cihazda kesin çözülmüş sayılmamalıdır.
- Yeni çalışma: önce güncel kaynak ve gerçek hata kanıtını karşılaştır, yalnız ilgili alanı değiştir, anlamlı regresyonu çalıştır. Başarılı derlemede APK + kaynak/test/notlar içeren tam ZIP doğrulanıp teslim edilir.
- Kalıcı imza sorunu açıktır: CI debug sertifikası sürümler arasında değişmiştir. Sertifika eşleşmeden “üzerine güncelleme olur” denmez; uygulamayı kaldırmak yerel kayıtları silebilir. Önceki özel anahtar mevcut değildir.
- Releases yayın otomasyonu için AGENTS.md'deki otomatik onay reddi devam eder; taslak etkin değildir. Yayın izni ve gerçek yayın sonucu olmadan GitHub Releases teslimi tamamlandı denmez.

## Önceki sürümlerin ayrıntılı uygulama kayıtları

Aşağıdaki kayıtlar tarihseldir. Güncel uygulama özeti yukarıdadır; anlık derleme sonucu BUILD_STATUS.md üzerinden izlenir.

## 26.59 — yalnız açılmayan takibi bırak onayında kurtarma (5 Ekim 2026)

Güncel temel main10bbd57 /26.58; yerel eski26.55 temiz dal bırakılarak güncel main ayrıfix/26.59-unfollow-recovery dalına alındı.22:05 görselindeki22:00:42 hata @azizsisman_ onayı açılmadan16/20 cycle2/2 PAUSED; eski yol watchdog dışına çıkıyordu. Artık onay verilmemiş ve aynı satır Following halinde en az1 saniye stabil,10 saniye geçmişse Atmaca dönüşü ardından aynı task/session/toplam sayaç/döngü ile aktif hesap baştan doğrulanır. Başarısız kişi görevde dışlanır; yalnız uygulanmadığı kanıtlanan bu denemenin bütçesi geri verilir. Tamamlanan/atlanan kişiler ve döngü başlangıç ilerlemesi korunur. Onay verilmiş/belirsiz/kayıp/çelişkili satır veya gerçek geri dönüş bu kurtarma ile ek işlem üretmez. Manuel pause/stop/session değişimi dönüş callback'ini geçersiz kılar.26.58 ortak döngü ve saniyelik geri sayım/tablet/7× korunur.10 yeni test; toplam482 bekleniyor. CI bekleniyor, fiziksel yeni cihaz testi yapılmadı. versionCode107 /26.59-unfollow-recovery. İmza/Releases otomatik onay reddi kısıtları sürer.

## 26.58 — doğrulanmış döngü ve geri sayım teslimi (5 Ekim 2026)

- Başarılı APK kaynağı main8af17d7a51e9565d5bf403bfde42698836c370ed; CI https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/37356306942 .472 test/13 yeni BatchCycles26_58Test;0 başarısız/hata/atlanan. Önceki459 test de geçti. Lint0 hata/fatal,21 uyarı; SQLite5→6, APK imzası/manifest/paketleme geçti. İndirilen dış artifact digest/ZIP CRC/SHA256 listeleri/kaynak commit/paket içi-dışı APK eşitliği doğrulandı. Yeni fiziksel4 hesap/2 döngü cihaz testi yapılmadı.
- Yalnız görev döngüsü ve geri sayım değişti. Runtime kuyruk yönetiminde kendi başına Atmaca'ya dönüp beklemez; tur sonucu önce veritabanına kaydedilir, sıra X içinde sonraki hesaba geçer. Dört hesap için sıra1→2→3→4, ardından ortak tam dakika beklemesi, sonra kalan görevlerde aynı sıra1→2→3→4. Bekleme tüm turun sonundan başlar; farklı aralıklı seçimlerde kalan görevlerin en uzun aralığı kullanılır. Son tur bitince ek bekleme yok. Atlanan/başarısız/tüm tekrarları tamamlanan hesaplar diriltilmez; doğrulanmış toplam ilerleme korunur.
- Görev ekranı Sonraki döngüye N dakika M saniye kaldı mesajını her saniye gösterir. Görsel tick her saniye DB yazmaz. WAITING_INTERVAL aktif kuyruk durumudur ve10 saniye hareketsizlik kontrolü tarafından planlı bekleme sayılır. Duraklatma timer'ı iptal eder, Devam et aynı deadline ile sürdürür; durdurma/session değişimi gecikmiş tur callback'inin başlamasını engeller. Doğrudan runtime çağrısının eski tek görev beklemesi korunur. Aynı7× hız/tablet/takip sonucu/üst kaynak ikinci-üçüncü takipçi düzeltmeleri korunur.
- Değişen davranış dosyaları AutomationRuntime.kt,TaskOrchestrator.kt,BatchCyclePolicy.kt,OrchestrationModels.kt ve iki görev ekranındaki döngü durum etiketi.13 yeni test:4 hesap tur sınırı/aynı sıra/final tur/skip-fail/manualpause/single account/aktif bekleme/kalan toplam ilerleme/doğrudan runtime beklemesi/geri sayım dakika-saniye/erken sıfır ve negatif sınırı. versionCode106 /26.58-batch-cycles, com.atmacanext.v258 ve Room6 aynı. Kaynak main; çalışma dalı fix/26.58-batch-cycles yerel.
- AtmacaNext-26.58.apk: 20852444 bayt; SHA256 7194873fa015fa1dc5e0ebaa35f1fbed35252986e4019125ef9ed06e5478fadb. Başarılı CI dosyası aynen, yeniden imzalanmadan teslim edilir.
- AtmacaNext-26.58-TAM-PAKET.zip: 20806283 bayt; SHA256 70580153d9a8667feb8f0dd49a27a62de6ebff5b80b1102d5952c2be1d0d0801. Başarılı CI dosyası aynen, yeniden imzalanmadan teslim edilir.
- APK artifact11365415395,rapor11365161475;3 Ocak2027 süre sonu. Paket APK/kaynak/testXML/notlar/manifest/checksum içerir; paket notu sonunda gerçek472 test sonucu vardır.
- Sertifika26.57=b6e5169a959835eb8e10d137a01e6e72cf1712e75ae3dfcfbd558e78552b989e;26.58=bb89917d2046f37d76ad6113e208349f0409764d32c04b543710492d7cfecde4. Farklı:26.57 üzerine doğrudan kurulum uyumsuz. Önceki özel anahtar yok; uygulamayı kaldırmak yerel kayıtları siler.
- AGENTS.md otomatik onay reddi sürer:contents:write Releases workflow etkinleştirilmedi, kullanıcı somut yayın yetkisini onaylamadı. docs/archive-release.proposed.yml başarılı son koşuya güncellendi, etkin DEĞİL. Releases yayını tamamlanmadı. Eski sürüm APK/ZIP/yayınlar değiştirilmedi.

## 26.58 — yalnız toplu görev döngüsü (5 Ekim 2026)

Güncel26.57 üzerine dar yama. Kullanıcı4 hesap/2 döngüde ilk hesap turundan sonra Atmaca beklemesini bildirdi. Artık runtime kuyrukta yalnız tur sınırını bildirir; ilerleme veritabanına kaydedilip sıradaki hesap X içinde başlatılır. Tüm hesaplar ilk turu bitirince ortak tam dakika beklemesi başlar; kalan görevler aynı hesap sırasıyla ikinci tura alınır. Farklı aralıklarla seçilen görevlerde ortak bekleme kalan görevlerin en uzun aralığıdır. Tekrarı biten/atlanan/başarısız görev yeniden alınmaz. Manuel pause/deadline/stop korunur; planlı WAITING_INTERVAL kuyruk watchdog tarafından hareketsizlik sayılmaz. Yalnız döngü sahipliği/kuyruk tur geçişi ve ilgili durum etiketi değişti;26.57 takip/tablet/7×/sonuç düzeltmeleri korunur.Geri sayım her saniye dakika/saniye gösterir, ekran güncellemesi her saniye DB yazmaz.13 yeni test; CI bekleniyor, fiziksel yeni APK testi yapılmadı. versionCode106 /26.58-batch-cycles; com.atmacanext.v258/Room6 aynı. İmza/Releases otomatik onay reddi kısıtları sürer.

## 26.57 — doğrulanmış teslim sonucu (5 Ekim 2026)

- Başarılı APK kaynağı main366306183003f24228c299c37b22f8c11e6804dc; CI https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/37352233580 .459 test/4 yeni regresyon;0 başarısız/hata/atlanan. Lint0 hata/fatal,21 uyarı. SQLite5→6, APK imzası/manifest/paketleme ve indirilen arşivlerin CRC/digest/checksum/kaynak commit/paket içi-dışı APK eşitliği geçti. Fiziksel yeni X cihaz testi yapılmadı.
- Boş çocuk onaylı listesi: önceki kaynak profil kimliği hatırlanır; kontrollü Back ile bu profile dönülür, normal takipçileri açılır ve ziyaret edilmiş ilk kaynak dışlanarak ikinci, sonra üçüncü ekran sırasıyla seçilir. Uygun onaylı kaynak varsa önceki rastgele zincir sürer. Manuel dönüşte üst profil veya normal takipçi listesi görülünce aynı dönüş aşamasına geçilir; boş listede pull-to-refresh tekrarı yapılmaz. Eksik kota tamamlandı sayılmaz. Önceki10 saniye watchdog/7× hız/tablet/sayaç/session/bekleyen işlem korunur. Kaynak/navigasyon alanları görev sıfırlamasında temizlenir.
- versionCode105 /26.57-source-fallback, uygulama com.atmacanext.v258 ve Room6 aynı. Çalışma dalı fix/26.57-source-fallback yerel, kaynak/test main. İlk ara koşular37351792645/37351918494 başarılı olsa da teslim yalnız son kaynak/koşudan yapılır.
- APK20836060 bayt SHA2563545e2d983dee126927f2f5fc7dcfa71a3749568a5d5eed725d2272a3cdc6571. Tam paket20792045 bayt SHA256276d5f76908bbf4fc84dc28b48b6492033158abbee12113ad13050360ce0d1ad. Başarılı CI dosyaları yeniden imzalanmadan aynen verilir. APK artifact11363306378/rapor11362329739;3 Ocak2027 süre sonu.
- Sertifika26.57=b6e5169a959835eb8e10d137a01e6e72cf1712e75ae3dfcfbd558e78552b989e;26.56=565d5ddb623753e1e91b583b4abac88f1505695f148cc085241108052f82850e. Farklı imza:26.56 üzerine doğrudan kurulum uyumsuz. Önceki özel anahtar yok; kaldırmak yerel kayıtları siler.
- AGENTS.md otomatik onay reddi nedeniyle contents:write Releases workflow etkinleştirilmedi; somut kullanıcı yayın yetkisi onayı yok. Taslak docs/archive-release.proposed.yml başarılı son koşuya güncellendi, etkin değil. Releases yayını tamamlanmadı. Eski sürümler değiştirilmedi.

## 26.57 — boş onaylı listeden sıralı takipçi geçişi (5 Ekim 2026)

Güncel 26.56 temeli korunur. 1000033428.mp4 sonunda Gökçe Yalın onaylı listesinde yalnız kendi hesap satırı vardır. Yeni kaynak yoksa iki saniyelik taze liste beklemesinden sonra bir önceki kaynak profile kimliği doğrulanarak geri dönülür ve normal takipçilerindeki ziyaret edilmemiş kişiler ekran sırasıyla denenir. Aynı onaylı listede uygun kaynak varsa önceki rastgele zincir korunur. Eksik kota kaynak tükenince tamamlanmış sayılmaz. Sayaç/session/bekleyen işlem, tablet ve 7× hız korunur. Yeni alanlar görev sıfırlamasında temizlenir. versionCode105. CI bekleniyor; fiziksel yeni APK testi yapılmadı. Kalıcı imza ve AGENTS.md yayın onayı kısıtları sürer.

## 26.56 — doğrulanmış yama teslimi (5 Ekim 2026)

- Başarılı kaynak main commit63461e8a24e0a9d55bfbd08f73a09c403de5dc6d; CI https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/37348618113 .455 test/20 yeni regresyon;0 başarısız/hata/atlanan. Önceki435 test de geçti. Lint0 hata/fatal,21 uyarı; SQLite5→6/test/lint/assemble/APK imzası/manifest/paketleme geçti. Yeni fiziksel X testi yapılmadı.
- İlk kaynak e329343b8553a36525e82cf8889273bf96ff8314 /CI37348479767:455 testte1 başarısız. Eski SyncChainSpeed26_54Test reflection yolu8 alanlı pending constructor arıyordu; yeni unchangedFollowSince ile9 alan oldu. Regresyonun aynı pending nesnesini/ilerlemeyi koruma amacı değiştirilmeden constructor adaptasyonu yapıldı. Bu başarısız koşudan APK teslim edilmedi. Son başarılı koşu tüm455 testi kapsar.
- versionCode104 /26.56-follow-result-recovery; applicationIdcom.atmacanext.v258,Room6,minSdk26/targetSdk36; INTERNET ve Startup InitializationProvider yok. Kaynak/testler main'de; fix/26.56-follow-result-recovery yerel. Bu sonuçlar ayrı [skip ci] commit'i olarak yazılır; APK kaynağı63461e8a olarak kalır.
- AtmacaNext-26.56.apk20836064 bayt; SHA2565d855629868a4b6e33d9304429f0300890b27ba38313fc6bd46457aec085b61f. Tam paket20784801 bayt; SHA25620ced13bd808f64d809775798d14102e7258606299b7071bb2171c2161ccb5f4. Dış artifact digestleri/tüm ZIP CRC'leri/SHA256 listeleri/kaynak commit/manifest ve paket içi-dışı APK eşitliği doğrulandı; başarılı CI dosyaları yeniden derlenip imzalanmadan aynen teslim edilir. Pakette APK/kaynak/testXML/manifest/notlar/checksum var; CI notu sonunda gerçek455 test sonucu vardır.
- CI APK artifact11361247598; rapor11361417252;3 Ocak2027'de sona erer. docs/archive-release.proposed.yml son başarılı37348618113 koşusuna güncellendi; taslak etkin DEĞİL. Releases yayını tamamlanmadı: AGENTS.md'de kayıtlı otomatik onay incelemesi contents:write yayın workflow'unu reddetmiştir; somut kullanıcı yetki onayı yok. Eski yayın/APK/ZIP değiştirilmedi.
- Doğrudan güncelleme imza uyumu ayrıca karşılaştırıldı: APKv2 imza bloğundaki sertifika SHA256,26.55=151647105dcc01d423040745e254c598971ed36c34276370a403d00d1b39c4db;26.56=565d5ddb623753e1e91b583b4abac88f1505695f148cc085241108052f82850e. Sertifikalar farklıdır; önce teslim edilen26.55 APK üzerine doğrudan26.56 kurulumu uyumlu değildir. Önceki özel imza anahtarı elimizde yok; aynı imzayla update yapıldığı söylenmez. Uygulamayı kaldırmak yerel kayıtları siler; kullanıcıya açıklandı. Bu kaynak yaması uygulama/veritabanı yeniden yazımı değildir, fakat imza kısıtı yerinde APK güncellemesini engeller.
- Yeni fiziksel telefon/tablet/X testi YAPILMADI. Kullanıcı logu/video önceki cihaz davranışını kanıtlar;26.56 kabul senaryoları DEVICE_TEST_PROTOCOL.md içinde. Önceki tablet/7× hız/görev akışları korunur; sadece ilgili sonuç/ara geçiş/hareketsizlik yolları değişir. Manual pause/stop ve gerçek limit korumaları korunur.

## 26.56 — yalnız takip sonucu ve 10 saniyelik hareketsizlik güncellemesi (5 Ekim 2026)

- Güncel temel main2a8b15047ca0d97c461762068a9f3223aa162e75 /26.55; önceki tablet,7× hız ve görev değişiklikleri korunur. Kullanıcı yalnız ilgili alanların düzeltilmesini,10 saniye gereksiz hareketsizlikte motorun yeniden başlatılmasını istedi. Uygulama yeniden yazılmadı; mevcut ekran/veritabanı/hesap/görev yapısı değişmedi.
-20:04–20:12:54 logu:4 hesap sayaçları okunup20:06:16 Atmaca dönüşü doğrulanmış. @xhesaplar1 kaynak listesinde3/10 sonrası başka görünür kaynağa geçip5/10 olmuş; @neerajc111 sonucu7 saniyede UNKNOWN→PAUSED. Kullanıcı durdur/başlat yaptıktan sonra kalan5 tamamlandı. @ezeldestan ve @atmaca2025 tam10/10; sonraki hesap tam kimlikle doğrulandı. @bildirimhaber1 son doğrulanmış sayaç6/10; @rizuu015 sonucu kesinleşmedi ve PAUSED. Kullanıcının7 kişi bildirimiyle logdaki6 doğrulanmış ayrımı korunur. Yeniden başlatmadan sonra kalan4 tamamlandı;20:12:54 QUEUE_RETURN başarılı. Bunlar mevcut cihaz akışı kanıtıdır; yeni26.56 APK başarı iddiası değildir.
-1000033425.mp4 yerel eki incelendi:73,359sn/1080×2400.55sn Aira onaylı listesinde @meliscinm35 Geri Takip Et, @NeerajC111 Takip et.65sn Neeraj hâlâ Takip et;70sn Melis Takip ediliyor, Neeraj hâlâ Takip et. Kullanıcı manuel müdahaleyi bildirdi; hangi geçişin otomatik/manual olduğu video/logdan kesin ayrılmaz. Görselin hamnode/sürüm kanıtı olduğu iddia edilmez; Library üzerinden okunmadı.
- Onaylı takip seçicisi yeni availableFollowLabels ile Takip et/Geri Takip Et/Sen de takip et/Follow back eylemlerini kabul eder; Following/Requested içeren çelişkili kontrol yeni eylem sayılmaz. Genel plainFollowLabels değiştirilmedi; diğer görevlerin davranışı korunur. Kullanıcı adı, görünür gerçek kontrol ve mevcut satır/kimlik doğrulama şartları sürer.
- Takip sonucu: yeni Requested hemen başarı; Following stabil pencere sonrası başarı; önce gözlenen Following→Follow gerçek revert ve üç ardışık revert durdurma kuralı korunur. Hiç Following görülmemiş,7 saniye sonrası aynı kişinin görünür Follow/FollowBack durumunun en az1 saniye kesintisiz kaldığı no-effect sonucu başarı veya günlük limit sayılmadan kişiyi o görevde atlar ve sıradaki kişiye geçer. Gizli/kayıp/çelişkili satır no-effect veya sahte başarı değildir. UNKNOWN pendingAction artık atılmaz/PAUSED yapılmaz; taze okumayla sonucu bekler, yeniden takip tıklamaz.
- Ortak görev watchdog ve kuyruk geçiş eşiği10 saniye. Gerçek ilerleme yoksa accessibility actor motor adımını aynı task/session/sayaç/aşama/pendingAction ile yeniden başlatır; kabul edilmiş takip/yorum tekrar gönderilmez. Kullanıcı istediği üzere üç kurtarma sonrası otomatik duraklatma sınırı kaldırıldı; ilerleme yoksa her10 saniye kontrol yeniden başlatılır. Manuel Durdur/Duraklat, planlı aralık ve gerçek X limit/hata korumaları otomatik yeniden başlatılmaz. Pending sonucu görünmez kaldığında sayaç uydurulmadan tekrar okunur; eksik limit tamamlanmış sayılmaz.
- Hesap seçicisi açılışında ara PROFILE/FOLLOWERS_LIST frame'i beklenen switcher görülmeden Back üretmesin: AccountNavigationGate isteğe bağlı beklenen ekranı3 saniye kadar bekler; gerçek hedef ekran görülünce hemen kalkar. Hesap taraması ve runtime menü/seçici/profil geçişleri bu kapıyı kullanır. Süre sınırı ve tam hesap kimliği doğrulaması korunur.
-20 yeni FollowResultRecovery26_56Test; FollowBack/Following/Requested ayrımı, no-effect eşik/stabilite, kayıp/çelişkili sonuç, gerçek revert,10 saniye watchdog, pending watch, manuel pause/planlıwait ve ara hesap geçiş yüzeyleri. Beklenen toplam455; yeni gerçek CI sonucu bekleniyor. Yerel diff ve SQLite5→6 PASS. versionCode104 /26.56-follow-result-recovery; applicationIdcom.atmacanext.v258/Room6 aynı. Fiziksel yeni APK testi yapılmadı. Çalışma dalıfix/26.56-follow-result-recovery yerel; kaynak/notlar main'e kaydedilecek.
- Kalıcı debug imza sorunu ve AGENTS.md'deki otomatik onay reddi sürer. Releases contents:write taslağı etkinleştirilmez; yeni APK/tam paket CI'dan aynen doğrulanıp teslim edilir. Güncelleme uyumu garanti değildir; kaldırmak yerel kayıtları siler. Eski APK/ZIP/yayınlar değiştirilmez.

## 26.55 — doğrulanmış teslim sonucu (5 Ekim 2026)

- Test edilen kaynak main commit: efde2b84dc211c515bbbfc5c83f41f80f989b785. Başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/37341767741 . 435 test; 0 başarısız/hata/atlanan. AdaptiveNavigation26_55Test içindeki18 yeni regresyon geçti; önceki417 test de geçti. Test/lint/assemble/APK imzası/manifest/paketleme ve gerçek SQLite 5→6 kontrolü başarılı. Lint0 hata/fatal,21 uyarı.
- APK manifesti com.atmacanext.v258, versionCode103 / 26.55-adaptive-navigation, minSdk26/targetSdk36; INTERNET izni ve Startup InitializationProvider yok. Bu güncel APK, 26.51–26.54 düzeltmeleri ve 7× hızın üzerine tablet uyarlamasını içerir; eski sürüm temel alınmadı.
- AtmacaNext-26.55.apk:20836060 bayt; SHA256 edafe9d542f1965356566b59f6a8e3d4ec00cbc7eb0345a5ec76c182697b75e8. AtmacaNext-26.55-TAM-PAKET.zip:20771717 bayt; SHA256 13d1030af69565aba7c0929b9555266e62c9dedb4de70fc2d367436c8bcdf2b8. Başarılı CI dosyaları aynen teslim edilir; yeniden derleme/imzalama yapılmadı.
- Dış artifact digest, tüm ZIP CRC'leri, SHA256 listeleri, kaynak arşivi commit'i, manifest sürümü ve paket içi/dışı APK eşitliği indirilen dosyalarda doğrulandı. Tam paket APK, kaynak ZIP,435 testin JUnit XML ZIP'i, manifest, NOT_DEFTERI.txt ve checksum listesini içerir. Paket notunun sonunda gerçek CI kaynağı/435 test sonucu var; hazırlık sırasında yazılmış bekleme ifadeleri tarihsel aşamadır.
- CI artifact11359141890; doğrulama raporu11358567767;3 Ocak2027'de sona erer. Kaynak/testler main'de; çalışma dalı fix/26.55-adaptive-navigation yerel. Bu sonuç notları ayrı [skip ci] commit'idir; APK kaynak commit'i efde2b84 olarak kalır. 26.54 ayrıca yeniden incelendi: kaynak b6297ce, CI37303366905 başarılı; indirilen gerçek raporda417 test/33 yeni test ve0 başarısız/hata/atlanan, digest/CRC doğrulandı.
- docs/archive-release.proposed.yml varsayılanı bu başarılı koşuya güncellendi; taslak etkin DEĞİL. GitHub Releases yayını tamamlanmadı. AGENTS.md'de kayıtlı otomatik onay incelemesi contents:write yetkili yayın workflow'unu reddetmiştir; kullanıcı bu somut kapsamı onaylamadı. Güncel sürüm/tablet talebi yayın yetkisi onayı sayılmadı; eski yayın/APK/ZIP'ler değiştirilmedi.
- Yeni APK ile fiziksel telefon/tablet/X testi YAPILMADI. Soldaki çubuğa ait görsel kaynak inceleme kanıtıdır; sentetik testler fiziksel cihaz sonucu değildir. DEVICE_TEST_PROTOCOL.md sol/sağ/dip çubuk, yatay/split pencere, dar drawer ve önceki görevler için kabul adımlarını içerir.
- Kalıcı debug imza uyumluluğu çözülmedi; CI imza kontrolü eski yüklü APK ile aynı sertifikayı kanıtlamaz. Güncelleme reddedilebilir; uygulamayı kaldırmak yerel hesap/hedef/görev kayıtlarını siler.

## 26.55 — güncel kaynak doğrulaması ve tablet gezinmesi (5 Ekim 2026)

- Kullanıcı başka ChatGPT hesabında eski sürümden devam edilmiş olabileceğini bildirdi. GitHub main API ve yerel HEAD karşılaştırıldı: b6297ceab6056ce8d140b1ee3698d1d94ad2b16e / 26.54 aynı; yerel kullanıcı değişikliği yoktu. Bu güncel temel korunmuştur. 26.54 CI 37303366905 başarılı; önceki iki derleme aynı processSnapshot değişken adı çakışması nedeniyle başarısızdı, sessionReadKey düzeltmesi son başarılı commit'tedir. Önceki kaynak hazırlama notlarındaki bekleme ifadeleri tarihsel aşamadır; 26.55 yeni CI sonucu ayrıca kaydedilecek.
- 1000033423.png konuşmada görüldü: 960×1536 tablet, solda Home/Arama/Grok/Bildirim/Mesaj çubuğu, içerik x≈130 sonrası. Görselde uygulama sürümü ve ham erişilebilirlik düğümleri görünmez. Kullanıcının isteğiyle görsel Library üzerinden okunmadı. DiscoverySearchSelector etiketli aramayı alt %35 bölgede arıyordu; Discovery kaydırmasının kökün %6/%12 x noktası sol çubuğa denk geliyordu. Cihaz adı veya sabit piksel yerine canlı erişilebilirlik semantiği/geometrisi kullanılır.
- Yeni AdaptiveNavigationEvidence: görünür/enabled/editable olmayan Home ve en az iki farklı gezinme rolü, hizalanma ve anlamlı dağılımla sol/sağ/dip çubuğunu kanıtlar. Arama önce bu çubuk içinde seçilir; etiket yoksa yalnız kanıtlanmış dikey çubukta Home altındaki tek boş ikon yuvası kullanılabilir. Belirsiz çoklu adayda tahmin yapılmaz. Mevcut resource-id ve kanıtlı alt gezinme yedeği korunur. Profilin üst arama büyüteci global arama sayılmaz.
- ListGesture tüm fallback kaydırmalarında gerçek display ile kırpılmış alanı, kanıtlı gezinme çubuğunu dışarıda bırakarak kullanır. Böylece yorum/profil sol medya-güvenli şeridi artık içerik alanının soluna bağlıdır. Telefonun alt gezinmesi de kaydırma alanından ayrılır; sağ çubuk ve offset/split-window kenarları desteklenir. Çubuk kanıtı yoksa alan icat edilmez.
- Hesap menüsü avatar bölgesi içerik paneline göre bulunur; semantik menü ilk tercihtir. Hesap seçici konum yedeği, tam @handle ve en az iki drawer menü etiketi içeren en küçük gerçek üst kapsayıcıya bağlanır; tablette tam pencerenin sağındaki arka plana dokunulmaz. Sonraki ekran ve hesap kimliği doğrulaması hâlâ zorunludur.
- Önceki 26.51–26.54 yorum alıntısı, yalnız yorumla eşleşen medya atlama, ana yorumlara dönüş, no-follow/Mesaj gönder atlama, Requested/Beklemede sonucu, hedefte fazladan Back yasağı, tam hesap doğrulaması, toplu final Atmaca dönüşü, aynı listeden yeni kaynak, bounded hata kurtarma ve ortak hareketsizlik korumaları korunur. En hızlı · 7× ve kaydedilmiş manuel hız tercihleri korunur; gerçek X yüklenme süresi 7× garanti edilmez.
- AdaptiveNavigation26_55Test: 18 sentetik regresyon; görsele benzeyen sol çubuk, sağ çubuk, dört ölçek, yatay pencere, içerik şeridi, dip çubuğu, etiketsiz/çoklu/gizli/disabled/editable aday, profil büyüteci, yetersiz çubuk kanıtı, gönderi metni, offset pencere, bilinmeyen düzen ve yinelenen semantik. Bunlar gerçek cihaz dökümü değildir. Önceki 417 test ile beklenen toplam435; gerçek sonuç bekleniyor. Tarihsel 26.53 test sayısının 33 yazılması düzeltildi: gerçek yeni regresyon32, toplam384.
- versionCode103 / 26.55-adaptive-navigation, applicationId com.atmacanext.v258 ve Room6 aynı. Çalışma dalı fix/26.55-adaptive-navigation yerel. Yerel diff ve SQLite 5→6 kontrolü PASS. Fiziksel telefon/tablet/X testi yapılmadı; her X erişilebilirlik sürümünde başarı iddiası yok. Cihaz protokolüne sol/sağ/dip ve hesap paneli senaryoları eklendi.
- Kaynak/test/notlar main'e kaydedilecek; CI APK ve tam paket ayrıca doğrulanıp teslim edilecek. Kalıcı debug imza sorunu sürer; güncelleme reddedilebilir, kaldırmak yerel kayıtları siler. AGENTS.md contents:write otomatik yayın engeli sürer; taslak etkinleştirilmez, eski APK/ZIP/yayınlar değiştirilmez.

## 26.54 — hesap seçicisi, aynı listeden kaynak zinciri, hata kurtarma ve 7× hız (5 Ekim 2026)

- Temel main: 2a4976816cf22f26ab9d1acee9a1707b372c193c (26.53 teslim notları). Kullanıcı 14:04–14:09:51 logunu verdi. Hesap taramasında ilk @atmaca2025 seçimi sonrası VERIFY_DRAWER/ACCOUNT_SWITCHER aşamasında yaklaşık 19–26 saniye arasında tekrar tekrar home bağlantısı çağrılıyor. Kullanıcı manuel müdahaleden sonra taramanın ilerlediğini bildirdi; müdahalenin tam zamanı logdan ayrı belirlenemez. Sonra beş hesap başarıyla kaydediliyor ve 14:06:22 Atmaca dönüşü doğrulanıyor.
- Aynı logdaki ilk görev @xhesaplar1 15/15 tamamlanıyor; QUEUE_HANDOFF ile @ezeldestan seçilip tam kimlik doğrulanıyor. İkinci görev 14:09:51.057'de 10/15; ardından dikey liste eylemi reddediliyor, SNAPSHOT_ERROR IllegalArgumentException ve PAUSED oluşuyor. Bu durma kuyrukta ikinci hesaba geçilememesi değildir; kaydırma/ekran hatası sonrası kurtarma yolunun motoru durdurmasıdır. Eski log hata yığını/mesajı içermediğinden IllegalArgumentException'ın tam kaynağı kesin kanıtlanamaz.
- Kaynakta ListGesture ve GestureClick StrokeDescription oluşturmayı try dışında yapıyor; sanal/off-screen satırların negatif merkezleri de harekete taşınabiliyor. Android'in resmi StrokeDescription belgesinde hareket sınırlarının negatif olmaması şartı var: https://developer.android.com/reference/android/accessibilityservice/GestureDescription.StrokeDescription . ListGestureGeometry, gerçek ekran/display sınırlarını ve kullanıcı satırlarını kırpar; negatif/tamamı ekran dışı/boş/NaN/sonsuz koordinat engellenir. Constructor da hata yakalama içine alındı. Stale liste node eylemi ACTION_REJECTED olur, görünür alan hareketi veya taze okuma denenir. Bunlar gözlenen hatayı üretebilen kaynak yollarıdır; yeni telefonda başarı kanıtı olarak sunulmaz.
- Hesap ekleme ve görev hesabı değiştirmede closeAccountSwitcher kullanılır: cache yenilenir, yalnız güncel X hesabı seçici yüzeyi iki kez doğrulanınca görünür Geri veya sheet kapatma uygulanır. Güncel ekran HOME ise Geri yok; home deep-link tekrar döngüsü kaldırıldı. SETTLE, hesap kimliği/sayaçlarını menüden doğrulama ve toplam/aşama sınırları korunur. Ayar hızının hesap taramasında etkisiz kalmasına yol açan nextActionAt sabit süresi düzeltildi; bekleme deadline ve tick aynı ölçeklenmiş süreyi kullanır, tekrar ölçeklenmez. AccountNavigationGate, hızlı polling aynı ekranı görürken kabul edilmiş menü/seçici tıklamasını 1,5 saniyeye kadar tekrar göndermez; gerçek ekran değiştiğinde gate hemen kalkar. Menü açılıp ikinci dokunuşla kapanma riski böyle engellenir.
- Onaylı listede yeni Takip et kalmadığında aynı açık liste LOCATE_SOURCE_ROW aşamasına geçer; Geri, own-profile restart veya hesap değişimi yapılmaz. Öncelik o anki görünür kullanıcı adlarından rastgele başka profil; yeni profil tam kimlikle doğrulanır → takipçileri → Onaylı sekmesi → kalan limit. Takip ediliyor/Beklemede/no-button kişiler kaynak olabilir, takip düğmesi kullanıcı adı kanıtı değildir. Kendi hesap/ziyaret edilmiş/kapalı/disabled/ekran dışı satır dışlanır. Görünür kaynak yoksa yalnız aynı liste içinde yukarı kaydırılır. Sonsuz profil döngüsü ziyaret kümesiyle engellenir; gerçekten yeni kaynak yoksa eksik sayaç başarı yazılmadan korunur.
- SnapshotRetryPolicy tüm görevler ve hesap taramasında geçici RuntimeException için üç taze okuma denemesi yapar; dördüncü kesintisiz hata açık duraklama/iptal verir. onSnapshotReadFailure mevcut stage/account/session/verified counter ve pendingAction'ı korur; belirsiz takip/yorum/beğeni yeniden gönderilmez, yalnız sonuç tekrar okunur. SNAPHOT_ERROR artık en fazla altı stack frame içerir; kullanıcı metni/özel veri/credential loglanmaz. Her görev türü ve tarama cache/refresh taze root yolunu kullanır. Kısa SystemUI/klavye kökü 2,5 saniye beklenir; Android izin ekranı veya gerçekten başka uygulamaya geçiş bu grace kapsamına alınmaz.
- Ortak 20 saniye watchdog, ekran UNKNOWN/known gürültüsünü veya değişmeyen görüntüye gönderilen kaydırmayı ilerleme saymaz. listScrolls yalnız yeni viewport gözleminde artar. Kurtarma aynı accessibility actor üzerinde aynı task/session kontrolüyle yapılır; stop/resume, Atmaca'ya dönüş ve hesap baştan seçimi yok. Pending işlem korunur. Onaylı liste işleminde hareketsizlik varsa aynı listeden kaynak zinciri aranır. Değişmeyen ilerlemede üç yerinde kurtarma sonrası açık duraklama vardır. Kullanıcının duraklat/durdur, ekran kilidi, gerçek belirsiz sonuç ve rate-limit kuralları korunur. Toplu kuyruk geçiş koruması ve final Atmaca dönüşü 26.53'ten korunur.
- Kullanıcı ek isteği: hesap ekleme ve tüm görevler 7× hızlı, ayarlarda en hızlı seçenek. AutomationSpeedPreset: işlem 71 ms (nominal beklemeler tam /7), hesap 257 ms, görevler arası 214 ms. Yeni sürümde eski hız tercihi etkin 7× presetine geçer; kullanıcı bu sürümde çalışma süresini yeniden kaydederse yeni tercihi kalıcı korunur. Ayarlarda En hızlı · 7× düğmesi üç alanı birlikte doldurur; Kaydet ile uygulanır. Yeni kurulum/upgrade varsayılanı zaten 7×. Hız; takip, unfollow, yorumcu/retweetçi, alıntı/etkileşim, hesap tarama/değiştirme için ortak kullanılır. Takip başarı sonucu en az iki güncel gözlem ve hızla ölçekli 250–2000 ms kararlı pencere ister. X yüklenme/ağ süresi /7 hızlanmış kabul edilmez; tüm görevin gerçek elapsed süresine yedi kat garanti verilmez. Görev/döngü limiti, per-account günlük limit ve 120 dk hedef yaş kuralları değiştirilmedi.
- Yeni SyncChainSpeed26_54Test: 33 regresyon; seçili ilk hesap sheet'i, HOME Back engeli, hesap settle, negatif/sanal/kısmi satırlar ve display/yatay/dikey geometri, stale native scroll fallback, görünür/rastgele/ziyaret/kendi/ilişki kontrolü kaynak ayrımı, gerçek controller'da ikinci hesap 10/15 korunması, bounded retry, tüm desteklenen görevlerde gerçek controller read recovery ve yeni hesap state temizliği, pending takip nesnesinin aynen korunması, kullanıcı pause, kısa sistem kökü, watchdog noise, upgrade/sonraki manuel hız tercihi, tam /7 timing ve hızlı sonuçta revert ayrımı. Sentetik node örnekleri gerçek cihaz ağacı diye sunulmaz. Mevcut watchdog regresyonu ekran gürültüsünün ilerleme sayılmaması şartına güncellendi.
- Değişen dosyalar: AccountSyncController, AtmacaAccessibilityService, AutomationRuntime, ListGesture/GestureClick/ListViewportController, AutomationStallPolicy/AutomationForegroundService, AutomationTuning/TaskOrchestrator/VerifiedFollowPolicy, SettingsStore ve üç ayar yüzeyi. Yeni yardımcılar AccountNavigationGate, AccountSelectionReturnPolicy, ListGestureGeometry, SnapshotRetryPolicy, ForegroundReadPolicy, VerifiedSourcePolicy, AutomationSpeedPreset ve yeni test dosyası. versionCode102 / 26.54-sync-chain-speed; applicationId com.atmacanext.v258, Room6 ve toolchain korunur. Çalışma dalı fix/26.54-sync-chain-speed yereldir; kaynak/testler main'e kaydedilecek.
- Yerel git diff --check ve gerçek SQLite 5→6 kontrolü PASS. Android SDK/Gradle yerelde yok; gerçek test/lint/APK sonucu GitHub Actions'tan alınacak, henüz bekleniyor. 26.54 yeni APK ile fiziksel Android/X testi YAPILMADI. Kabul: manuel müdahale olmadan beş hesabı tara; beş hesaplı onaylı takipte ikinci hesabın 10/15 az listesinden aynı listede başka kaynak → 15/15 → üçüncü hesap; tüm görevlerde gerçek hareketsizlik ve geçici ekran hatası; 7× Kaydet ve tekrar açılışta korunma.
- Kalıcı debug imza çözülmedi; eski APK üzerine kurulum reddedilebilir, uygulamayı kaldırmak yerel kayıtları siler. AGENTS.md'deki contents:write workflow etkinleştirmesi açık onay engeli hâlâ geçerli; bu hata/hız talebi yayın yetkisi onayı değildir. Etkin build workflow yalnız contents:read; arşiv yayın taslağı etkinleştirilmez. Eski sürümler silinmez/değiştirilmez.

## 26.53 — doğrulanmış teslim sonucu (5 Ekim 2026)

- Test edilen kaynak main commit: bb7f9e4ffcf5071be632e6b5df0e827d8b6cf6f2. Başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/37299334945 . İndirilen JUnit XML'lerinde 384 test; 0 başarısız, hata veya atlanan. BatchCommentReturn26_53Test içindeki 32 yeni regresyonun tamamı geçti. Paket içindeki JUnit sonuçları aynı 384 testtir. Önceki 352 test de geçti. İlk 378-test koşusunun tek başarısız controller bağlantı testi ve düzeltmesi aşağıdaki kaynak notunda kayıtlıdır.
- SQLite 5→6 kontrolü, test/lint/assemble, APK imzası, manifest ve paketleme başarılı. Lint 0 hata/fatal, 21 uyarı. Manifest com.atmacanext.v258 / versionCode101 / 26.53-batch-comment-return / minSdk26 / targetSdk36; INTERNET izni ve Startup InitializationProvider yok. Yeni APK ile fiziksel Android/X test sonucu değildir.
- AtmacaNext-26.53.apk: 20819680 bayt; SHA256 bf740883e9fb1000efbb66719b7f3e7ab4c5e3a6c0950289bd558c35d15d0e0c. AtmacaNext-26.53-TAM-PAKET.zip: 20715949 bayt; SHA256 07530966f7697aa3557eb0b319428d443dc06269f7ad09a3663fb51f6a4b7cfd. Başarılı CI dosyaları aynen teslim edilir; yeniden derleme veya imzalama yapılmadı.
- Dış artifact hash'leri GitHub metadata digest'iyle eşleşir. Dış/ iç ZIP ve APK CRC'leri, tüm iç/dış SHA256 listeleri ve paket içi/dışı APK eşitliği doğrulandı. Kaynak ZIP git comment'i test edilen commit ile eşleşir; altı ana değişen kaynak/test dosyası ayrıca git show ile byte byte karşılaştırıldı. Tam paket kaynak ZIP, APK, test XML ZIP, manifest, NOT_DEFTERI.txt ve iç checksum içerir. Paket notunun sonunda gerçek kaynak commit'i ve 384-test CI sonucu vardır; baştaki CI bekleme notu kaynak hazırlama aşamasıdır. Repo SHA256SUMS.txt CI dist dosyalarını tanımlar; repo kökündeki daha güncel not metnini değil, paket içindeki CI footer'lı NOT_DEFTERI.txt dosyasını hash'ler. Tam ZIP'in kendi hash'i dış listededir.
- APK/tam paket artifact 11341930167; rapor artifact 11341835247; son kullanma 3 Ocak 2027. Actions kayıtları sürelidir. Kaynak/testler main'de; çalışma dalı fix/26.53-batch-comment-return yerelde. Sonuç notları ve checksum'lar daha sonraki yalnız teslim [skip ci] commit'inde main'e kaydedilir; APK'nın kaynağı yukarıdaki bb7f9e4ffcf5071be632e6b5df0e827d8b6cf6f2 olarak kalır.
- docs/archive-release.proposed.yml, başarılı 37299334945 koşusunu varsayılan kullanacak şekilde güncellendi. Taslak etkin DEĞİL; GitHub Releases yayını tamamlanmadı. AGENTS.md'deki contents:write iş akışı etkinleştirmesi için somut onay beklenir. Bu hata düzeltme talebi yayın yetkisi onayı sayılmadı; eski APK/ZIP/yayınlar değiştirilmedi.
- Fiziksel yeni 26.53 testi YAPILMADI. Kullanıcının 1000033391.mp4 videosu önceki çalışmanın kanıtıdır; sürüm numarası görünmez. İlk kabul: iki hesap/Limit1/Tekrar1; ilk işin Gönderi/listesinden ACCOUNT_RETURN ile hesap menüsüne, ikinci tam @handle ve iş, final QUEUE_RETURN ve Atmaca görünürlüğü. Mesaj gönder/no-follow yorumunda COMMENT_SKIP_NO_FOLLOW→COMMENT_RETURN PARENT; alt yanıt aksiyonu yok ve ana listedeki sonraki metin yorumundan devam. Onaylı listede yeni Beklemede sonucu FOLLOW_REQUEST ile bir sayılır ve sonraki kişi/hesap. Ayrıntılı protokol DEVICE_TEST_PROTOCOL.md içinde.
- Kalıcı debug imza uyumluluğu çözülmedi. CI apksigner APK'nın kendi imzasını doğrular; eski kurulumla eşit sertifika kanıtlamaz. Güncelleme reddedilebilir; uygulamayı kaldırmak yerel hesap/hedef/görev kayıtlarını siler. Anahtar/kimlik bilgisi eklenmedi.

## 26.53 — toplu hesap geçişi, alt yorum koruması ve Beklemede sonucu (5 Ekim 2026)

- Kullanıcı 1000033391.mp4 (228,66 sn, 1080×2400) videosunu gönderdi. İlk hesap görevi bittikten sonra X'te kalındığını, yaklaşık 2:45–2:50 arasında kendisinin Atmaca'ya döndüğünü bildirdi. Tüm görevlerde hesap geçişi ve final Atmaca dönüşünün kontrolünü; açılan yorumun alt yanıtlarına girilmemesini; Takip et yoksa/Mesaj gönder vb. varsa geri dönüp aynı ana yorumlarda devam edilmesini istedi. Onaylı kullanıcı takipte yeni isteğin Beklemede durumunda durulmamasını istedi. Bu talep, 26.52'deki düğme yoksa PAUSED davranışını değiştirir; açılmayan metin yorumu, belirsiz gönderim ve doğrulanmamış takip sonucu için mevcut korumalar devam eder.
- Video incelemesi: yaklaşık 146 sn yorumcu başlığında Takip et, 148–160 sn Takip ediliyor ve aynı Gönderi ekranı. 162 sn son uygulamalar, 164–168 sn ana yorum listesi, 170–174 sn Atmaca görev ekranı. 171 sn ekranı: toplu görev 2 hesap/2 alt iş, toplam 1/2; @xhesaplar1 tamamlanmış, @bildirimhaber1 sırada, VERIFY_ACCOUNT ve X ön plandan çıktı mesajı. 178–190 sn HOME→çekmece→hesap seçici→ikinci hesabın kendi profili; 218 sn Sessiz İstila @sessizistila58 başlığında Mesaj gönder ve aşağıda üç farklı alt yanıt. Videoda sürüm numarası veya yeni ham erişilebilirlik ağacı yok; onaylı takip/Beklemede çalışması bu videoda görünmez, kullanıcı bildirimidir. Kullanıcının müdahale zamanı kendi beyanıyla ayrıca kaydedildi; kare incelemesinin yaklaşık zamanı farklı olabilir.
- Kaynak nedeni: verifyTargetAccount, önceki görevin Gönderi/listesinden ve doğrulanmayan profilden X home bağlantısını her okumada yeniden çağırıyordu. Bağlantı kabul edildiği hâlde görünür ekran değişmezse bu dalda ne gerçek Back ne de süre/deneme sınırı vardı. İkinci alt işin seçilmiş olması, X hesabının değişmiş olduğunu kanıtlamaz. Yeni ortak AccountVerificationReturnPolicy: eski Gönderi/etkileşim/takipçi/takip edilen/onaylı liste/profil/oluşturucu yüzeyinden X içindeki Geri ile menüye dönüş; adımlar arası en az 1 sn yerleşme, en fazla 6 Back veya 12 sn ardından sınırlı hesap kurtarma. HOME/çekmece/hesap seçicide fazladan Back yok; UNKNOWN ancak gerçek Back kontrolüyle geri döner. Aktif tam @handle çekmecede ve kendi profilde doğrulanmadan görev aksiyonu yapılmaz. Aynı yol bütün desteklenen görev türlerinin başlangıç, tekrar ve hesap geçişinde çalışır.
- TaskOrchestrator: tamamlanan runtime'ın son doğrulanmış sayacı ve durumu, sonraki start/stop öncesinde doğrudan kaydedilir; bağımsız StateFlow kayıt okuyucusunun anlık tamamlanmayı atlamasına bırakılmaz. Terminal kuyruk satırına ait gecikmiş runtime okumaları geçişi iptal edemez. İlk hesabın işi tamamlanınca sıradaki seçili alt iş X içinde başlatılır; sadece finalde Atmaca dönüşü yapılır. Final dönüşün callback sonucu QUEUE_RETURN ile kaydedilir; eski kuyruğun dönüş callback'i yeni aktif kuyruğu etkileyemez. Mevcut ana iş parçacığı/son uygulama kartı/dönüş bildirimi yolları korunur; X'i zorla kapatma eklenmedi.
- Alt yorum koruması: CommentDetailEvidence, birleşik veya ayrı @handle+süre biçimindeki ilk alt yanıt başlığını ve Alakalı/Relevant gibi ayırıcıyı gerçek sınır kabul eder. Üst sağ fallback bu sınırı aşamaz. Üst yazar başka kişiyse fallback ile o kişi takip edilemez. Aynı bağlı seçici hem dokunmada hem takip sonucu okumada kullanılır; alt yanıtın Takip et/Takip ediliyor düğmesi açılan yorumcunun sonucu olamaz.
- Takip et yoksa, Mesaj gönder/Abone ol vb. varsa veya uygun başlık kontrolü bulunmuyorsa, açık yorumun yerleşmesine 1,2 sn pay verilip yalnız seçilmiş yorum anahtarı işlenmiş/atlanan olarak kaydedilir; başarı sayacı artmaz. Yeni CommenterReturnPolicy ile ana yorumlara geri dönülür; önceki görünür diğer metin yorumları kaydırmadan tüketilir. Ana yorum/ hedef profil zaten görülüyorsa fazladan Back yok. En fazla üç aralıklı Back, belirsiz dönüşte korunmuş ilerleme ve açık PAUSED nedeni vardır. Seçilen yorumun anahtarı tek başına, onun alt yanıt ekranından ana yorumlara dönüş kanıtı olamaz; kaydedilmiş gerçek ana yazar ve eski satır kanıtları kullanılır.
- Beklemede: VerifiedFollowPolicy bu sonucu zaten SUCCESS sayıyordu, fakat XUiActions.isRelationshipActionNode yalnız Follow/Following etiketlerini kabul ederek Requested/Beklemede düğümünü okumadan eliyordu. Yeni RelationshipActionEvidence ve gerçek rowHasAny/relaxed seçicisi, Requested etiketlerini ve görünür disabled/nonclickable ilişki durumlarını yalnız sonuç okumada kabul eder; yeni Follow eyleminde enabled/gerçek kontrol şartı kalır. Sekme/gizli satır/başka kişinin düğmesi sonuç değildir. Yeni isteğin Beklemede sonucu bir doğrulanmış işlem sayılır; onay beklemeden sıradaki uygun kişi, limitte sıradaki görev/hesap. Önceden Beklemede olan kişi tekrar takip edilmez. Üç gerçek Following→Follow geri dönüşündeki mevcut hesap durdurma kuralı değiştirilmedi. Link bazlı doğrudan takipte Requested sonucu da aynı biçimde kabul edilir.
- Son gözden geçirme: AtmacaRepository, düşük sayaçlı eski okumaları zaten engelliyordu fakat aynı sayaçta eski PAUSED/RUNNING okuması COMPLETED kaydını geri çevirebiliyordu. TaskProgressWritePolicy, tamamlanmış satıra yalnız tamamlanmış ve düşmeyen ilerleme kabul eder; kullanıcı görevi açıkça sıfırladığında yeni QUEUED kayıt yeniden çalışabilir. İlk CI 37298782697 (9ddf03686176cc593ba81f4a01fd16eeab8482fe) 378 testte 1 başarısız verdi: gerçek controller ile tüm görev türü testi FOLLOW başlatırken Android stub Uri.parse sonucu null olduğu için durdu. Üretim bağlantı kontrolü standart java.net.URI ile aynı izinli http/https X/Twitter hostlarına taşındı; JVM/Android aynı doğrulama kodunu kullanır. Altı yeni bağlantı ve kalıcı sonuç testi eklendi. Bu başarısız koşudan APK teslim edilmedi.
- Yeni BatchCommentReturn26_53Test: 32 regresyon; Mesaj gönder/no-button/diğer kontroller ve yakın alt yanıtlar, ayrı süre başlığı, yanlış üst yazar, doğru üst takip, disabled Requested/Following, Requested açıklaması, gizli/sekme/disabled Follow olumsuz örnekleri, isteğin revert zincirini kesmesi, eski görevin tüm X yüzeylerinden menüye dönüşü ve sınırları, ana yorum/ hedefte ekstra Back yasağı, alt yorumun tek anahtarıyla sahte dönüşün engellenmesi, tüm mevcut görev türleri için iki hesaplı sıra/final ve controller'ın iki dönüş yığınını temizlemesi, tüm link görevlerinin gerçek başlangıcı, doğru/bozuk/yabancı URL'ler ve gecikmiş kayıt/kalıcı tamamlanma/sıfırlama. Video düzenine benzeyen node örnekleri sentezdir; gerçek cihaz node dökümü olarak sunulmaz.
- versionCode101 / 26.53-batch-comment-return; applicationId com.atmacanext.v258 ve Room v6 korunur. Temel main 70e8004cf009915a861c926aee145e1af9202735 (26.52 teslim notları). Çalışma dalı fix/26.53-batch-comment-return yerelde. Değişen kaynaklar AutomationRuntime, TaskOrchestrator, CommentDetailEvidence, DiscoveryViewportEvidence, XUiActions, XUiVocabulary; yeni AccountVerificationReturnPolicy, CommenterReturnPolicy, RelationshipActionEvidence, TaskProgressWritePolicy ve regresyon dosyası; AtmacaRepository kayıt kapısı. Sürüm, salt-okuma build workflow ve ayrıntılı notlar güncellenir.
- Yerel git diff --check ve gerçek SQLite 5→6 kontrolü geçti. 26.53 test/lint/APK/imza/manifest başarılı; 384 testin gerçek sonuçları yukarıdaki doğrulanmış teslim başlığındadır. Yeni APK ile fiziksel Android/X testi YAPILMADI. DEVICE_TEST_PROTOCOL.md içinde yeni kabul senaryoları var.
- Kalıcı debug imza uyumluluğu çözülmedi; eski APK üzerine kurulum reddedilebilir, kaldırmak yerel hesap/hedef/görev kayıtlarını siler. Kaynak/testler main'e kaydedildi. Releases contents:write workflow için AGENTS.md'deki somut onay koşulu devam eder; bu yeni düzeltme talebi yetki onayı değildir. Taslak etkinleştirilmez ve eski APK/ZIP'ler değiştirilmez.

## 26.52 — doğrulanmış teslim sonucu (5 Ekim 2026)

- Test edilen kaynak main commit: 7e5b34890d78b7d8dd272014543ba74beb5b1660. Başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/37293523368 . İndirilen JUnit XML'lerinde 352 birim test; 0 başarısız, hata veya atlanan. CommentMediaReturn26_52Test içindeki 18 yeni regresyonun tamamı geçti. Paket içindeki test XML'leri aynı 352 sonucu içerir. İlk 26.52 koşusu 37293145203 de başarılıydı; son koşu, aynı yazarın medya ve metin yorumlarını seçilmiş yorum anahtarıyla ayıran ek düzeltmeyi de kapsar.
- SQLite 5→6 geçiş kontrolü, test/lint/assemble, APK imzası, manifest ve paketleme adımları başarılı. Lint 0 hata/0 fatal, 21 uyarı. APK manifesti com.atmacanext.v258, versionCode100 / 26.52-comment-media-return, minSdk26/targetSdk36; INTERNET izni ve Startup InitializationProvider yok. Bu sonuç fiziksel X cihaz testi değildir.
- AtmacaNext-26.52.apk: 20803296 bayt; SHA256 58d7d97b3b03085a6ab1ad3c5826806693220f5f0e4da9e6acaf69ca5c43cdbf. AtmacaNext-26.52-TAM-PAKET.zip: 20686235 bayt; SHA256 f0ea0760f2c42ca1c1086908e648bba708273ca0cbe72563627fe2536de1ff11. APK yeniden derlenmedi veya imzalanmadı; başarılı CI dosyaları aynen teslim edilir.
- Tam paket APK, kaynak ZIP, test XML ZIP, manifest, NOT_DEFTERI.txt ve iç SHA256 listesini içerir. Kaynak ZIP git comment'i test edilen commit ile eşleşir. Dış artifact, APK ve tüm iç ZIP CRC'leri, SHA256 listeleri ve paket içi/dışı APK eşitliği indirilen dosyalarda doğrulandı. Paket notunun sonunda gerçek CI commit'i ve 352 test sonucu bulunur; kaynak hazırlanırken yazılmış bekleme notları tarihsel aşamayı anlatır. Repo kökündeki SHA256SUMS.txt gerçek CI dist dosyalarının hash'lerini içerir; paketin kendi hash'i dış listededir.
- APK/tam paket artifact 11337423963; doğrulama raporu artifact 11338060227; son kullanma 3 Ocak 2027. Bunlar süreli Actions kayıtlarıdır. GitHub Releases yayını bu oturumda tamamlanmadı. Kaynak ve testler main'de; çalışma dalı fix/26.52-comment-media-return yerelde. Bu doğrulanmış sonuç notları daha sonraki yalnız teslim commit'inde [skip ci] ile main'e kaydedilir; APK'nın kaynak commit'i yukarıdaki 7e5b34890d78b7d8dd272014543ba74beb5b1660 olarak kalır.
- docs/archive-release.proposed.yml, başarılı 37293523368 koşusunu varsayılan kullanacak şekilde güncellendi; taslak etkin DEĞİL. AGENTS.md'deki contents:write workflow etkinleştirmesi için somut kullanıcı onayı hâlâ beklenir. Yeni hata bildirimi ve bu düzeltmeye devam talebi yayın yetkisi onayı sayılmaz; eski Releases veya APK'lar değiştirilmedi.
- Yeni 26.52 APK ile fiziksel Android/X testi YAPILMADI. 1000033389.mp4 önceki çalışmanın kanıtıdır; sürüm numarası görünmez. İlk kabul: metin/resim/metin/GIF/metin sırası ve Limit5; yalnız medya satırlarının atlanması, aynı yazarın ayrı metin yorumunun işlenmesi, eksik limitte aynı hedef profilinde sonraki gönderi, retweetçi liste→gönderi→hedef profil dönüşü ve ikinci hesaba temiz geçiş. DEVICE_TEST_PROTOCOL.md senaryoları korunur.
- Kalıcı debug imza uyumluluğu çözülmedi. CI apksigner yeni APK'nın kendi imzasını doğrular; eski yüklü APK ile aynı sertifikayı kanıtlamaz. Güncelleme reddedilebilir; uygulamayı kaldırmak yerel hesap/hedef/görev kayıtlarını siler. Anahtar veya kimlik bilgisi eklenmedi.

## 26.52 — yorumcu medya atlaması ve hedef profilinde devam (5 Ekim 2026)

- Kullanıcı 1000033389.mp4 (93,96 sn, 1080×2400) videosunu gönderdi. Yorumcu takipte yalnız resim/GIF/video paylaşan yorumların atlanmasını, limit dolmadığında hedef profilinden tamamen çıkılıp yeniden hedef aranmamasını istedi. Retweetçi takipte de gereksiz hedef çıkışı olduğunu bildirdi. Bu yönlendirme 26.51'in Yorum Alıntısı çalışmasının ardından ek düzeltmedir; önceki değişiklikler korunur.
- Video: yaklaşık 30–35 sn AVG yorum detayı; 37–50 sn aynı ana yorum görünümü; 53 sn Saye, Ender, Leao1905, Sessiz İstila ve Egeliyik gibi metin yorumcuları görünür. Sonraki kaydırmada bu metin satırlarının çoğunun açılmadan geçildiği görülür; 70–75 sn Murat, 90–94 sn YusufHerki detayı açılır. Videoda Atmaca sürüm numarası/görev sayacı ve retweetçi çalışması görünmez; yeni APK cihaz başarısı veya tam skip nedeninin erişilebilirlik kanıtı diye sunulmaz. Retweetçi belirtisi kullanıcı bildirimidir. Yeni ham node raporu verilmedi.
- Kaynak incelemesi: ReplyMediaEvidence, satırla yalnız kesişen önceki görseli, geniş medya atasını, küçük ve genel Fotoğraf etiketli avatarı veya metinde geçen video/fotoğraf/GIF sözcüğünü medya sanabiliyordu. Medya düğümünün başlangıcı gerçek yorum başlıkları arasındaki banda ait olmalı; avatar ve küçük composer kontrolleri ayrı tutulur, gövde metni medya değildir. Son yorum bandı inline giriş/end işaretinden önce biter. Yalnız medyalı yorumun anahtarı atlanır; aynı kişinin başka metin yorumunu medya satırı yüzünden engellemez. Açma/alternatif dokunuşlar yalnız seçilmiş yorum anahtarına bağlanır; aynı yazarın önceki medyalı satırına yanlışlıkla yeniden dokunulmaz.
- Görünür uygun metin yorumcuları kaydırmadan önce sırayla tüketilir. Yorumcuya tıklama başarısızlığı artık skippedHandles kaydı yapmaz: üç sınırlı dokunuş, ardından PAUSED ve korunmuş ilerleme. Kabul edilmiş dokunuş ana yorum ekranında kalırsa aynı yorumun metin alanına iki alternatif dokunuş denenir. Açılmış alt ekran doğrulanmadan ana hedefin Takip et düğmesine basılmaz. Takip düğmesi/kimlik/yükleme doğrulanamazsa kullanıcı sessizce atlanmaz; duraklama nedeni gösterilir. Zaten Takip ediliyor/Beklemede, kendi hesap/hedef ve işlenmiş kişiler doğal olarak yeni takip değildir. Kısaltılmış ve tam kimliği okunamayan metin yorumu başka hesaba tahminle dokunmadan PAUSED olur.
- Back sonrası CommenterViewportPolicy bekleyişi artık runtime'da gerçek kaydırmadan önce uygulanır. Bir bekleme, başarısız kaydırma/liste sonu diye üçe sayılıp gönderinin erken terk edilmesine yol açmaz. ListGesture içindeki ikinci guard sayımı kaldırıldı; yorum sonu ve güvenli sol şerit davranışı korunur.
- nextDiscoveryTweet, mevcut taze ekranı okumadan Geri'ye basıyordu. RETURN_DISCOVERY_TARGET da altı saniye/iki denemede yeniden hedef aramasına dönüyordu. Yeni DiscoveryReturnPolicy: hedef görünüyorsa doğrudan SCAN; yalnız Gönderi/Etkileşim alt ekranında aralıklı Geri; profil/Home/UNKNOWN'da kör Geri yok. Hedefe varıldıktan sonra fazladan Geri yok, mevcut profil konumu/ilerleme korunur. Belirsiz dönüşte hedef yeniden aranmaz; PAUSED. Yorumcu ve retweetçi aynı düzeltmeyi kullanır. NavigationSurfaceEvidence, eski TWEET_DETAIL sınıflandırması kalmış olsa da gerçek profil yüzeyinde Geri'yi engeller.
- Ana yorumlara dönüş doğrulanamadı diye gönderi otomatik terk edilmez. Hedef profil zaten açılmışsa doğrudan aynı hedefte sonraki uygun gönderi taranır. Orijinal hedef gönderisi için en az 120 dakika kuralı, görev limiti ve yalnız doğrulanmış takip sayımı korunur.
- Yeni CommentMediaReturn26_52Test: gerçek medya/metin/komşu görsel/atasal alan/avatar/composer ayrımı, videodaki altı metin başlığının okunması, aynı yazarın medya/metin yorum anahtarları, retweetçi liste→gönderi→profil geri yığını, profil/home/belirsiz ekranlarda kör Geri yasağı, eski ekran etiketiyle gerçek profil korunması, 120 dk kuralı ve ikinci hesapta retry/skip/return state temizliği. 18 yeni test ve toplam 352 test geçti; gerçek sonuçlar yukarıdaki doğrulanmış teslim başlığındadır.
- versionCode100 / 26.52-comment-media-return; applicationId com.atmacanext.v258 ve Room v6 korunur. Çalışma dalı fix/26.52-comment-media-return yereldir; kaynak/testler main'e kaydedildi. Değişen dosyalar AutomationRuntime, XTweetInspector, ReplyMediaEvidence, ListGesture, NavigationSurfaceEvidence; yeni DiscoveryReturnPolicy ve regresyon dosyası; sürüm/workflow/notlar.
- Yerel git diff --check ve gerçek SQLite 5→6 kontrolü geçti. Derleme/birim test fiziksel X testinden farklıdır. 26.52 ile yeni fiziksel cihaz testi YAPILMADI. İlk cihaz kontrolü: metin/görsel/metin/GIF/metin sıralı yorumlar ve Limit5; yalnız medya satırları atlanmalı. Bir gönderide limit dolmazsa ana hedefte sonraki gönderi; retweetçi listesi→gönderi→hedef profili ve burada durulması; ikinci hesaba geçiş ayrıca denenmeli.
- Kalıcı debug imza sorunu değişmedi; önceki kurulumla güncelleme uyumu garanti değildir, kaldırmak yerel kayıtları siler. 26.51 Releases workflow onay sorusuna yanıt henüz verilmedi; yeni hata bildirimi yayın yetkisi onayı sayılmaz. contents:write workflow etkinleştirilmeyecek; APK ve tam ZIP hazırlanıp kaynak/notlar GitHub'a kaydedilecek, süreli artifact kalıcı Releases diye sunulmayacak.

## 26.51 — doğrulanmış teslim sonucu (5 Ekim 2026)

- Test edilen kaynak main commit: 7df763bcbe58fc79878d391b641ebdcf757b11aa. Başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/37289341028 . 334 birim test; 0 başarısız, hata veya atlanan. Bunların 23'ü yeni Yorum Alıntısı/form/hedef/kuyruk/kayıt regresyonudur. Gerçek SQLite üzerinde iki eski v5 tablo biçiminin geçiş kontrolü geçti. Test/lint/assemble/imza/manifest/paketleme adımları başarılı; lint 0 hata/0 fatal ve 21 uyarı bildirir. Fiziksel cihaz başarı kanıtı değildir.
- AtmacaNext-26.51.apk: 20803300 bayt; applicationId com.atmacanext.v258; versionCode99 / 26.51-reply-flow-recovery; SHA256 13871547313f28ce434689d3faeb2f18d3099a3617cc237d16453f15503cc51b.
- AtmacaNext-26.51-TAM-PAKET.zip: 20667282 bayt; SHA256 a5bb6595aa23a83c855b7d1dfa96e8442a68d8d2b49144c5bf6c8f0075772f9d. Kaynak ZIP, APK, birim test XML'leri, manifest, NOT_DEFTERI ve paket içi checksum içerir. Kaynak ZIP git comment'i yukarıdaki commit ile eşleşir. Dış artifact, APK, kaynak/test/tam ZIP CRC ve bütün SHA256 değerleri ile paket içi/dışı APK eşitliği indirilen dosyalarda doğrulandı. Repo kökündeki SHA256SUMS.txt bu CI'nin dist dosyalarını tanımlar; kaynak depodaki notun CI footer eklenmiş sürümü dist içinde bulunur.
- APK/tam paket artifact 11336061712; tam doğrulama raporu artifact 11335608249; son kullanma 3 Ocak 2027. Actions artifact süreli kayıttır; GitHub Releases yayını bu oturumda tamamlanmadı. Kaynak/testler main'de, çalışma dalı fix/26.51-reply-flow yerelde. Sonuç notları ve yayın taslağı daha sonraki yalnız teslim commit'inde [skip ci] ile kaydedilir; APK kaynağı bu başlıktaki 7df763bcbe58fc79878d391b641ebdcf757b11aa olarak kalır.
- Release taslağı docs/archive-release.proposed.yml, bu başarılı run ID'sini kullanacak şekilde güncellendi; elle başka başarılı main run seçmek de mümkün. scripts/archive_release.py yalnız başarılı main APK'sını yeniden derlemeden/imzalamadan arşivler; tekil build etiketi kullanır, mevcut yayımlanmış APK'yı değiştirmez; dış SHA256 listesine tam paket hash'i de eklenir. Taslak etkin DEĞİL. AGENTS.md'deki somut contents:write workflow etkinleştirme onayı beklenir.
- Fiziksel Android/X testi YAPILMADI. İlk kontrol: tek hesap/tek hedef/Limit1/Tekrar1. Yorum alanı açılmalı, yazılan metin taze okumada doğrulanmalı, yalnız bir gönderim ve doğrulanmış 1/1 olmalı. Sonra iki hedef ve iki hesap. Gönderim belirsizse PAUSED; otomatik yeniden gönderim yapılmaz. Telefon geri bildirimi ve Hata Raporu ZIP olmadan X üzerinde kesin çözüm iddiası yok.
- Kalıcı release/debug imza uyumluluğu çözülmedi; CI apksigner doğrulaması yeni APK'nın kendi imzasını doğrular, eski yüklü APK'nın imzasıyla eşitliği kanıtlamaz. Güncelleme eski debug imza nedeniyle reddedilebilir; uygulamayı kaldırmak yerel hesap/hedef/görev kayıtlarını siler. Anahtar/kimlik bilgileri eklenmedi.

## 26.51 — Yorum Alıntısı gönderi üzerinde kalma düzeltmesi (5 Ekim 2026)

- Temel GitHub main: b9022ab0b02a268e9dd8a4ba1e0353dbcbe863dd (26.50). Kullanıcı son cihazda Yorum Alıntısı görevinin hedef gönderiyi açıp orada kaldığını bildirdi. Bu oturumda yeni cihaz videosu veya ham erişilebilirlik ağacı verilmedi; aşağıdaki nedenler kaynak incelemesidir, cihazdaki kesin neden/başarı olarak sunulmaz.
- Son main CI 34571512499 derlenemedi: AccountSettingsScreen.kt ve AccountSettingsScreen26_42.kt içindeki StatusLabel overload çakışması. Yeni yardımcıya benzersiz, private ad verildi; Ayarlar sürümü BuildConfig'ten okunur.
- COMMENT_QUOTE_TARGETS, TaskQueuePlanner'ın desteklediği türlere hiç eklenmemişti. Eklendi. TargetAccount mapper QUOTE kind değerini hem kayıtta hem okumada kaybediyordu; iki yönde düzeltildi. Room 5→6 geçişi, yalnız owner:QUOTE:handle UUID'si ile kanıtlanan eski yanlış STANDARD kayıtlarını onarır; normal hedefleri dönüştürmez.
- Yeni ReplyComposerEvidence + XUiActions yolları: yalnız görünür/etkin gerçek düğüm; geniş reply/container ID'si veya tıklanabilir ataya çıkma yok. Üst gönderinin Yanıtla kontrolü ve alttaki Yanıtını gönder girişi ayrı seçilir; kabul edilmiş tıklama 1,5 saniyede form açmazsa alternatif kontrol bir kez denenir. Hem ayrı COMPOSER hem gönderi içindeki TWEET_DETAIL editörü desteklenir.
- Hedef gönderinin yazar ve metni doğrulanmadan yorum yazılmaz. ACTION_SET_TEXT'in kabulü yazma başarısı değildir: sonraki taze erişilebilirlik okumada tam metin eşleşmelidir. Görünür etkin Yanıtla/Gönder kontrolüne bir kez basılır. Yalnız gönderi ekranının görünmesi gönderim başarısı sayılmaz: gönderim bildirimi, yeni kendi yorum satırı veya aynı hedefte 650 ms kararlı form kapanması/temizlenmesi aranır. 10 saniyede sonuç belirsizse tekrar gönderim yerine PAUSED.
- COMMENT_QUOTE_TARGETS taze root/cache yenileme yoluna eklendi. QUOTE_REPLY, QUOTE_REPLY_OPEN, QUOTE_REPLY_WRITE, QUOTE_REPLY_SUBMIT kayıtları form, seçici, deneme ve doğrulama aşamasını gösterir; yorum metni tanılama kaydına yazılmaz.
- Gönderim öncesi quotePendingKey Room'a kaydedilmeden tıklama yapılmaz. Yeni süreçte belirsiz gönderim varsa aynı yorum yeniden gönderilmez; X'teki sonuç kullanıcı tarafından kontrol edilmelidir. quotePostedKeys ile doğrulanan gönderiler duraklatma/yeniden başlatma/tekrar döngüsünde yeniden işlenmez. Eski asenkron kayıt bu gönderim kilidini veya daha yüksek ilerlemeyi geri alamaz.
- Görev başına quoteTargets hedef listesi kaydedilir; çalışma sırasında hedef düzenlemesi yarım görevin hesabını/limitini değiştirmez. Limit her hedef için uygulanır: 5 hedef, Limit 1, Tekrar 1 = toplam 5 doğrulanmış yorum. Eksik hedef/gönderi başarı sayılmaz, eksik limitte ilerleme korunarak duraklar. Yeni gönderi yeniden açma kurtarması bu görevde 120 dakika filtresi uygulamaz; yorumcu/retweetçi takip yaş kuralı korunur.
- Yorum Alıntısı ekranı seçili hesapları tek grup olarak seri başlatır; tarama/görev kilidi, duraklat/devam/durdur, Türkçe durum, ilerleme ve telefon için kaydırılabilir form/listeler eklendi. Çoklu hesap görevi tek transaction ile kaydedilir; hedefi eksik hesap için oluşturma kapalıdır. Takip/Etkileşim dış sekmeleri doğru başlangıç filtresine bağlandı.
- Ara hesap sonunda Atmaca'ya erken dönüş kaldırıldı. Son kuyrukta QUEUE_DECISION → QUEUE_HANDOFF / QUEUE_DONE kayıtları yazılır. finishQueue kendi geçiş coroutine'ini iptal edip kayıt/dönüşü yarıda kesmez. Kullanıcının mevcut kuyruğu ikinci bir start çağrısıyla iptal edilemez; start kilidi ve session/index kontrolü eklendi. X zorla kapatılmaz; son seçili görevde Atmaca öne gelir.
- Kimlik: versionCode 99 / 26.51-reply-flow-recovery; applicationId com.atmacanext.v258; Room v6. Android araç zinciri ve imzalama ayarları korunur.
- Değişen ana dosyalar: AutomationRuntime, AtmacaAccessibilityService, XUiActions, DiscoveryTweetOpenRecovery, TaskOrchestrator, TaskQueuePlanner, Models, Entities, Dao, Mappers, AtmacaRepository, AtmacaDatabase, TasksScreen/26_42/26_43, AccountSettingsScreen26_42; yeni ReplyComposerEvidence ve QuoteTargetProgressPolicy. Yeni regresyon dosyaları QuoteReplyRegressionTest, QuotePersistenceRegressionTest, QuoteQueueRegressionTest.
- İlk 26.51 CI 37287905394 (a323307916458496cc454c56491704533f01caa9), uygulama derlenmeden setup-android@v3 varsayılan eski tools paketinde durdu: Failed to find package tools. Resmi setup-android README ile doğrulandı; v4, cmdline-tools 15859902, yalnız platform-tools ve sessiz lisans kaydı kullanıldı. Bu başarısız koşu birim test/derleme sonucu değildir. Düzeltilmiş araç zinciri ile CI 37288168240 (8de43244bdb760aa12738448caec3773bc742b96) test/lint/APK/imza/manifest/paketleme adımlarını başarıyla tamamladı; son veritabanı düzenlemesiyle CI 37289341028 de başarılı oldu.
- Son teslim kontrolünde Room v5 taze kurulumunun DEFAULT STANDARD ile 4→5 geçişinin DEFAULT 'STANDARD' biçimleri tek v6 tablo biçiminde birleştirildi; hedef satırları aynen kopyalanır. scripts/verify_quote_migration.py, kaynakta kullanılan gerçek geçiş SQL'ini her iki eski biçimde SQLite üzerinde çalıştırdı: kimlikler/hesap/hedef/aktiflik/zaman ve görev ilerlemesi korundu; yalnız kanıtlanan QUOTE satırı onarıldı. CI öncesi bu geçiş kontrolü zorunludur. Yerel geçiş ve git diff --check geçti; son CI 37289341028 ile test/lint/assemble de geçti.
- Çalışma dalı: fix/26.51-reply-flow (yerel). Kaynak/testler main'e kaydedildi; paket kaynak commit'ini ve CI sonuçlarını NOT_DEFTERI.txt içine otomatik ekler. 26.51 APK/tam ZIP/SHA256SUMS salt-okuma yetkili mevcut build workflow tarafından üretilecek.
- Fiziksel Android/X testi YAPILMADI. İlk kabul: tek hesap + tek hedef + Limit 1 ile gönderi → yorum alanı → taze tam metin → bir gönderim → bir doğrulanmış sonuç. Ardından iki hedef ve iki hesap. Yeni telefon kanıtı olmadan tüm X arayüzleri için kesin çözüm iddia edilmez.
- Kalıcı imza çözülmedi: CI debug anahtarı önceki APK'dan farklı olabilir; kaldırma kayıtları siler. GitHub Releases yetkisi değiştirilmedi. AGENTS.md içindeki önceki contents:write onay engeli nedeniyle archive-release.yml etkinleştirilmedi; başarılı APK ve ZIP hazır olduktan sonra bu somut yayın adımı için kullanıcı onayı gerekir. Actions artifact kalıcı Releases teslimi diye sunulmaz.

## 26.33 — Boşuna Tıklama profilinde yanlış bildirim penceresi

- 1000027181.mp4 (35. saniye) normal profil gösteriyor; açık pencere yok. 23:41:42 PROFILE / NOTIFICATION_PROMPT ardından 23:41:43 PAUSED kaydı, taramadan önce durulduğunu kanıtlıyor. Bio içindeki “bildirimleri açın” eski contains("bildirimleri aç") koşulunu tetikliyordu. 26.31/26.32 için ileri sürülen medya kaydırması bu durmanın nedeni değildi; önceki kesin teşhis geri çekildi.
- PopupClassifier bildirim/rehber/izin/promosyon metnine ek olarak gerçek dialog veya görünür, etkin kapatma kontrolü arar. Profil ve gönderi metni tek başına görev durdurmaz. Gerçek pencerelerin güvenli işlenmesi korunur; hesaba özel istisna eklenmedi.
- Beş regresyon testi: cihazdaki bio, TR/EN metinler, gerçek Compose bildirim istemi, kapatma düğmesiz native dialog, gizli/tıklanmaz kapatma metni. Hız ve kaydırma davranışlarına bu sürümde dokunulmadı.
- versionCode81 / 26.33-popup-surface-evidence. Yeni APK için CI sonucu bekleniyor; fiziksel cihaz testi yapılmadı. Releases yetkisi ve imza ayarları değiştirilmedi.

## 26.32 — doğrulanmış teslim sonucu

- Kaynak main commit `f273facc153586cf411dc09c416c414db24e870f`; başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34400761367 . 269 test; 0 başarısız, hata veya atlanan. Test/lint/assemble, APK imzası, manifest ve paketleme adımları geçti.
- `AtmacaNext-26.32.apk`: 20.623.076 bayt; versionCode80 / 26.32-progressive-discovery-scroll; SHA256 `4795b56b2b62c4a2f93d23ab4806d44247d3dab1df715613eb1310a341ff7deb`.
- `AtmacaNext-26.32-TAM-PAKET.zip`: 20.408.750 bayt; SHA256 `8df46797c6f795140bfa63faf8b98e29c2e2646114989221787d19537025f7e0`. ZIP CRC ve paket içi/dışı APK eşitliği doğrulandı. APK artifact `10123472499`; rapor artifact `10123473367`; son kullanma 8 Aralık 2026.
- Fiziksel X cihaz testi bu yeni APK ile YAPILMADI. 26.31 cihaz geri bildirimi önceki sürümde kalan profil/yorum kaydırma sorunlarının kanıtıdır. 26.32 için beklenen yeni kanıtlar `COMMENT_SCROLL` kaydı, değişen `DISCOVERY_SCAN` yaşları ve Ayarlar'daki hız değerine göre kısalan adım süreleridir.
- Kalıcı debug imza ve Releases `contents:write` sınırlaması değişmedi. APK ile tam paket sohbet teslimindedir.


## 26.32 — profil kaydırma tekrarı ve yorum sonrası zorunlu ilerleme

- Kullanıcının 26.31 fiziksel geri bildirimi: yorumcu takip düğmesi artık çalışıyor; ancak `@bosunatiklama` gibi büyük medya taşıyan hedef profilde tarama yine ilerlemeyebiliyor. Ayrıca bir üst yorumcu işlendiğinde yorum listesinin aşağı kaydırılması bazı denemelerde gerçekleşmiyor. Yeni cihaz logu verilmediği için bu iki belirti kullanıcı gözlemi olarak kaydedildi.
- Hedef profil kaydırması güçlendirildi: hareket %88'den %20'ye uzatıldı, süre 620 ms yapıldı ve X hareketi kabul edip listeyi değiştirmezse medya alanının dışında kalan %6/%12 sol şeritler arasında dönüşümlü tekrarlandı. Profil sonu kararı iki değişmeyen okumadan altıya çıkarıldı; gerçek erişilebilirlik imzası değişince sayaç yine sıfırlanır.
- Yorumcu gönderisine girilip takip edildikten veya takip düğmesi olmadığı için atlandıktan sonra ana yorumlara dönüldüğünde, yeni aday seçilmeden önce bir zorunlu dikey kaydırma yapılır. `COMMENT_SCROLL accepted=... scroll=...` kaydı cihazdaki hareket isteğini açıkça gösterir. İlk görünür yorumun işlenmesi korunur; sonraki işlem alt yorumlardan sürer.
- Kullanıcının ek isteğiyle Ayarlar > Çalışma/Görev Güvenliği içindeki işlem aralığı gerçek küresel hız kontrolüne çevrildi. `Tüm işlemlerin hızı` 100–15000 ms: 500 normal, 250 yaklaşık 2 kat, 100 yaklaşık 5 kat hızlıdır. Runtime'ın sabit tıklama, kaydırma, bekleme ve doğrulama yoklamaları aynı oranda ölçeklenir; 60 ms erişilebilirlik tabanı korunur. Takipten çıkma sonuç sabitlemesi ve kaydırması, onaylı takip, yorumcu, retweetçi ve diğer görevler kapsamdadır. Hesap geçişi 500–15000 ms, toplu görevler arası 250–60000 ms ayrıca kullanıcı denetimindedir.
- Yeni kaydırma şeridi dönüşüm testi eklendi. versionCode80 / 26.32-progressive-discovery-scroll. CI ve bu APK ile fiziksel cihaz testi henüz yapılmadı.

## 26.31 — doğrulanmış teslim sonucu

- Kaynak main commit `e01a49e99550a82168ce307c03cfc40b4479d5bd`; başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34394873143 . 266 test; 0 başarısız, hata veya atlanan. Test/lint/assemble, APK imzası, manifest ve paketleme adımları geçti.
- `AtmacaNext-26.31.apk`: 20.623.076 bayt; versionCode79 / 26.31-discovery-media-scroll; SHA256 `986eec67a0d34e330d92a32b4fe8cb7d582b1c22bb6a33da268b4ee749d9372`.
- `AtmacaNext-26.31-TAM-PAKET.zip`: 20.401.991 bayt; SHA256 `9d97a2b4165b85f619483870d6fca7dc1dac7f7e25c4febfa22e97b0e7144bc6`. ZIP CRC ve paket içi/dışı APK eşitliği doğrulandı. APK artifact `10121220178`; rapor artifact `10121220723`; son kullanma 8 Aralık 2026.
- Fiziksel X cihaz testi bu yeni APK ile YAPILMADI. 22:17 ekran görüntüsü 26.30'daki profil üzerinde kalma sorununun kanıtıdır. 26.31'de beklenen sonuç, büyük video/GIF/fotoğraf olsa da sol güvenli kenardan profil listesinin ilerlemesi ve en az 120 dakikalık gönderinin açılmasıdır.
- Kalıcı debug imza ve Releases `contents:write` sınırlaması değişmedi. APK ile tam paket sohbet teslimindedir.

## 26.31 — medya içeren hedef profilde ve toplu görevde ilerleme

- 9 Eylül 2026 22:17 Boşuna Tıklama ekran görüntüsü incelendi. Profil doğru açılıyor; ilk görünür gönderi 6 dakikalık ve büyük inline video içeriyor. Yorumcu/retweetçi için en az 120 dakikalık gönderiye ulaşmak üzere kaydırma gerekir. Mevcut ListGesture ekranın merkezinde başladığı için video oynatıcı dikey hareketi tüketebiliyor; Android gesture tamamlandı cevabı gerçek liste hareketini kanıtlamıyor. Bu aynı hedefte tek ve toplu görevlerin profil üzerinde kalmasını açıklayan kod boşluğudur; yeni görev state sıfırlaması mevcut testlerle zaten doğrulanmıştır.
- Discovery takip kaydırması artık ekran merkezini kullanmaz: x=%6 sol güvenli kenar, y=%82'den %28'e dikey yol. Böylece inline video/GIF/fotoğraf ve sağ-alt oluştur düğmesi dışında kalır. Geri kaydırma aynı yolun tersidir. Profilin yatay Gönderiler/Yanıtlar/Medya pager'ına ACTION_SCROLL_FORWARD gönderilmez.
- Bu hareket yalnız COMMENTER_FOLLOW/RETWEETER_FOLLOW/QUOTER_FOLLOW taramasına uygulanır. Takipten çıkma, onaylı takip ve diğer görevlerin kaydırması değiştirilmedi. İki yeni politika testi sol şerit ve tam ters yönü doğrular; önceki ardışık iki hesap/görev sıfırlama regresyonu korunur.
- versionCode79 / 26.31-discovery-media-scroll. CI ve yeni fiziksel cihaz testi henüz yapılmadı.

## 26.30 — doğrulanmış teslim sonucu

- Kaynak main commit 18b96d0643795e0d4fe6caf403b91ad722f49f7e; başarılı CI https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34389850082 . 264 test; 0 başarısız, hata veya atlanan. Test/lint/assemble, imza, manifest ve paketleme başarılı.
- AtmacaNext-26.30.apk: 20.623.072 bayt; versionCode78 / 26.30-comment-header-follow; SHA256 284bd2634f7391cb52af9183de72e25c2e9946465924b15017c0d21835371b52.
- AtmacaNext-26.30-TAM-PAKET.zip: 20.395.722 bayt; SHA256 1e67333ff4237ebafca81b7f7a278dce7465371e0476bca7cb1ea73ea88c17a8. ZIP CRC ve paket içi/dışı APK eşitliği doğrulandı. APK/tam paket artifact10119288714; rapor artifact10119289638; son kullanma 8 Aralık 2026.
- Fiziksel X cihaz testi bu yeni APK ile YAPILMADI. 21:22 cihaz kaydı önceki 26.29 hatasının kanıtıdır. 26.30'da beklenen kanıt `COMMENT_FOLLOW ... author=@null ... available=true` ardından `COMMENT_CLICK ... mode=top-right`; düğme yoksa sıradaki yoruma dönüş.
- Kalıcı debug imza ve Releases contents:write sınırlaması değişmedi. APK ile tam paket sohbet teslimindedir.

## 26.30 — görünür üst-sağ yorumcu Takip et düğmesi

- 9 Eylül 2026 21:22–21:25 fiziksel cihaz logu ve 1000027144.jpg/1000027142.mp4 incelendi. Hedef gönderi ve yorumcular açılıyor. Her açılan yorumda üst-sağ `Takip et` erişilebilirlik ağacında `Rect(813,334–959,380)` olarak görünmesine rağmen `detailAuthor=@null`; `COMMENT_CLICK` hiç oluşmuyor ve runtime 5–6 saniye sonra başka yoruma dönüyor. Kesin neden tıklama değil, X'in açılan yorum sahibinin ad/@handle düğümlerini bu ekranda yayımlamaması nedeniyle kimlik önkoşulunun geçilememesidir.
- OPEN_ENGAGER_PROFILE artık açılmış Gönderi ekranında üst başlığın hemen altındaki, ekranın sağ %40'ında kalan tek düz `Takip et` eylemini kimlik düğümü olmasa da kabul eder ve doğrudan bu düğüm sınırına basar. `Takip et` yoksa 1,5 saniyelik yerleşmeden sonra sıradaki yoruma döner. Alt yorumlardaki takip düğmeleri üst başlık bandı dışında bırakılır.
- Takip sonucu aynı üst-sağ bölgede `Takip ediliyor` veya `Beklemede` görülerek doğrulanır; düz `Takip et` hâlâ görünüyorsa başarı sayılmaz. Exact kullanıcı adı bulunan eski güvenli yol öncelikli olarak korunur.
- İki yeni fiziksel-ekran regresyonu: kimliksiz üst-sağ takip düğmesini seçme ve aşağıdaki yorum takip düğmesini reddetme. versionCode78 / 26.30-comment-header-follow. CI ve yeni fiziksel cihaz testi henüz yapılmadı.

## 26.29 — doğrulanmış teslim sonucu

- Kaynak main commit cee20e7d3e23ce06c473bedbf4032c11e473b563; başarılı CI https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34387120893 . 262 test; 0 başarısız, hata veya atlanan. Test/lint/assemble, APK imzası, manifest ve paketleme adımları geçti.
- AtmacaNext-26.29.apk: 20.623.080 bayt; versionCode77 / 26.29-engagement-click-recovery; SHA256 c97ef28090542a9a688ba617598f9caff74c749b2a2e8e63721acdaf210b9052.
- AtmacaNext-26.29-TAM-PAKET.zip: 20.391.088 bayt; SHA256 b830d8ec2a78ad84d7b483ff7eb6812330389ebf07d77448eacae35d911137a9. ZIP CRC ve paket içi/dışı APK eşitliği doğrulandı. APK/tam paket artifact10118231520; rapor artifact10118232084; son kullanma 8 Aralık 2026.
- Fiziksel X cihaz testi YAPILMADI. 262 test sentetik/birim doğrulamasıdır; yorumcu Takip et ve ikinci hesap retweetçi gönderi açma sonucu telefonda ayrıca denenmelidir. Yeni kanıtlar COMMENT_CLICK ile sağ aksiyon dokunuşu ve DISCOVERY_CLICK retry=1/2 kayıtlarıdır.
- Debug imza kalıcı olmayabilir; eski APK üzerine kurulum reddedilebilir ve kaldırmak yerel verileri siler. Releases contents:write iş akışı etkinleştirilmedi; APK ve tam paket Actions ile sohbet teslimindedir.

## 26.29 — yorumcu takip ve ikinci hesap retweetçi gönderi açma düzeltmesi

- 9 Eylül 2026 20:43 logu ile 26.28 cihaz geri bildirimi incelendi. Hesap senkronizasyonu dört hesabı tamamlıyor; yorumcu görevinde hedef araması başlıyor. Kullanıcı, açılan yorumcu gönderisinde görünür Takip et düğmesine basılmadığını ve retweetçi akışının ilk hesapta çalışıp ikinci hesapta hedef profilde kaldığını bildirdi. Paylaşılan kayıt DISCOVERY_CLICK/COMMENT_CLICK satırlarından önce kesildiği için cihazdaki dokunma sonucu doğrudan kanıtlanmış sayılmaz.
- Yorumcu başlığında exact kişi ve düz Takip et doğrulaması korunur. Compose düğmeyi satır genişliğinde semantik alan olarak yayımlarsa ACTION_CLICK sonrası fallback artık alanın ortasına değil sağdaki gerçek ilişki aksiyonu bölgesine dokunur. Kart/üst ata tıklanmaz; Takip ediliyor/Beklemede hâlâ aday değildir ve sonuç aynı kullanıcı başlığında doğrulanmadan başarı sayılmaz.
- Retweetçi keşfinde ilk dokunuş metni yalnız genişletip hedef profilde bırakırsa aynı yazar, en az 120 dakika ve aynı anahtar/uzun metin başlangıcı doğrulaması korunur. İki tekrar artık aynı üst noktaya değil, genişlemiş metnin merkez ve alt-sol gövde noktalarına gider. Toplam üç dokunuş sınırı, farklı/genç/belirsiz gönderi reddi ve açılmayan gönderide geri basmadan sonraki gönderiye geçiş korunur.
- Yeni regresyon testi iki tekrarın farklı gövde noktaları kullandığını doğrular. Önceki gerçek runtime iki hesap sıfırlama testi korunur. versionCode77 / 26.29-engagement-click-recovery. Yerel test/lint/APK sonucu henüz aşağıda güncellenecek; fiziksel X cihaz testi YAPILMADI.
- Kalıcı debug imza ve GitHub Releases contents:write sınırlaması değişmedi. Kaynak/test/notlar main'e kaydedilecek; başarılı CI sonrası APK ve tam paket sonucu ayrıca yazılacak.

## 26.28 — doğrulanmış teslim sonucu

- Kaynak main commit b6c4d8dae6f76868bc92878fe42cbc06dc2677b2; başarılı CI https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34376974840 . 261 test; 0 başarısız, hata veya atlanan. Sekiz yeni gönderi açma regresyon testi dahil XML toplamları indirilen paketten doğrulandı. Test/lint/assemble, imza ve manifest kontrolleri geçti.
- AtmacaNext-26.28.apk: 20623072 bayt; versionCode76 / 26.28-tweet-open-recovery; SHA256 c8d3997baf343e5bc933b5de9f1a9d9a32f2009a787da0038bb132809d384043.
- AtmacaNext-26.28-TAM-PAKET.zip: 20387252 bayt; SHA256 53eda5b9c26fd1a1e2a1868148c202f21fc08e18e4ed2e55a9e905822f9a096c. ZIP CRC, paket içi/dışı APK eşitliği, sürüm ve CI SHA256 kaydı doğrulandı. APK/tam paket artifact10114398135; rapor artifact10114399107.
- Kod/testler main'de; yerel fix/26-28-tweet-open aynı kaynak ağacına sahiptir (1343af9c8e4e1b7fa14b37708b1541041acc9e56). Sonuç notu [skip ci] belge commit'idir; APK yeniden derlenmedi. Eski yerel 26.23 değişiklikleri korunmuştur.
- Fiziksel X cihaz testi YAPILMADI. Videoda profil içi metin genişlemesi görülür; yeni sürümün telefonda gerçekten ayrıntıyı açması henüz doğrulanmadı. Birim test başarısı cihaz başarısı diye sunulmaz. İlk cihaz kontrolü: aynı 2 saatlik gönderi -> metin genişlerse sınırlı aynı gönderi tekrarı -> Gönderi ayrıntısı; açılmazsa geri basmadan sonraki gönderi. DISCOVERY_CLICK / DISCOVERY_OPEN_RETRY / DISCOVERY_OPEN_SKIP kayıtları yeni kanıttır.
- Debug imza farklı olabilir; uygulamayı kaldırmak yerel verileri siler. AGENTS.md'de kayıtlı önceki yayın yetki reddi korunur; contents:write Releases workflow'u etkinleştirilmedi, kalıcı Releases yayını yapılamadı. Dosyalar sohbet üzerinden ayrıca teslim edilir.

## 26.28 — 14:34 gönderi açılmama incelemesi

- Temel main f35924e7cccd5a6117caf75a44c5c2bff15e6619 (26.27). 1000027080.mp4 ve 1000027081.mp4 incelendi. Pusholder akışındaki 2 saatlik Bakan Akın Gürlek gönderisinin metni profilde genişliyor; detay ekranı açılmıyor. 14:34:44 OPEN_ENGAGEMENT sonrasında PROFILE kalıyor. Bu kayıt yorumcuya ulaşmadan önceki arızayı gösterir; yorum takip başarısı kanıtı değildir.
- XTweetInspector yalnız metnin merkezine gesture gönderiyordu. Artık tam metin düğümünün ACTION_CLICK eylemi denenir; gesture metnin üst bölümüne gider, altındaki Daha fazlasını göster alanından uzak tutulur. Kart atası veya medya açılmaz.
- DiscoveryTweetOpenRecovery: ilk dokunuştan sonra hedef PROFILE olarak kaldıysa 1,5 saniye yerleşme; aynı yazar/yaş ve metin anahtarı ya da uzun değişmemiş başlangıçla genişleyen gönderiyi yeniden bulma. Başka yazarlı/genç gönderi, kısa ortak başlangıç ve farklı alıntı reddedilir; birden fazla eşleşmede dokunulmaz. Toplam en fazla üç dokunuş, ardından geri basmadan taramaya devam. Eski/yeni metin anahtarları sonuçta birlikte saklanır. Açılma doğrulanmadan başarı/takip sayılmaz.
- DISCOVERY_CLICK / DISCOVERY_OPEN_RETRY / DISCOVERY_OPEN_SKIP kayıtları gerçek düğme sınırlarını, dokunuş sonucunu ve tekrar sayısını verir. Kullanıcı metni ham olarak yeni kayıtlara eklenmez. Diğer takip, saat ve hesap geçiş düzeltmeleri korunmuştur.
- 8 yeni regresyon testi: videodaki metin genişlemesi, aynı yazarlı farklı alıntı, yanlış yazar/119 dk, eksik/kısa metin, satır sonları, yerleşme süresi, üç deneme sınırı, kaybolan/belirsiz aday. CI sonucu bekleniyor; fiziksel X testi YAPILMADI. Önceki 253 test başarısı bu cihaz vakasının çözüldüğünü kanıtlamamıştır.
- Sürüm code76 / 26.28-tweet-open-recovery. Değişenler AutomationRuntime, XTweetInspector, GestureClick; yeni DiscoveryTweetOpenRecovery ve testi; sürüm/paket adları, README ve notlar. Çalışma dalı fix/26-28-tweet-open; kaynak/testler main'e kaydedilecek.
- Kalıcı debug imza sorunu sürer; kaldırma yerel kayıtları siler. AGENTS.md'deki önceden reddedilmiş Releases yayın otomasyonu etkinleştirilmedi. Kalıcı GitHub Releases teslimi henüz yapılamadı.

## 26.27 — doğrulanmış derleme sonucu

- Kaynak main commit e491fdaca0dd394a630826588a552d63128c4e59. Başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34343654939 . 253 test; 0 başarısız, hata veya atlanan. Yeni 8 video regresyon testi dahil test XML toplamları paket içinden doğrulandı. Test/lint/assemble ve imza/manifest kontrolleri geçti.
- AtmacaNext-26.27.apk: 20.606.684 bayt; versionCode75 / 26.27-video-recovery; SHA256 44a549466dd2a66e31ae4ea33d20a3e8bc4c16c6b0707b738a7f3a0d9c7fae97.
- AtmacaNext-26.27-TAM-PAKET.zip: 20.375.884 bayt; SHA256 a83b089aeef063d1fad914a67d03eca1e1b9f7b9eb559d4005f81c8ad8c27ff9. ZIP CRC, paket içi/dışı APK eşitliği ve CI hash kaydı doğrulandı. Paket kaynak, test raporları, manifest ve notları içerir.
- APK/tam paket artifact10100871276; rapor artifact10100871898. Actions son kullanımı 8 Aralık 2026. Kaynak/testler main'de; yerel fix/video-review-26-27 aynı kaynak ağacına sahiptir (96fd8b155a674dc24c9e7f78cef4067f620c2c42). Bu sonuç kaydı yalnız [skip ci] belgedir; APK yeniden derlenmedi.
- Fiziksel X cihaz testi YAPILMADI. 26.26 başlık/kimlik düzeltmeleri korunmuştur; yeni testler sentetik erişilebilirlik düzenleri ve tarih sınırlarını doğrular. Cihazda kesin çözüm iddia edilmez.
- Debug imza kalıcı değildir; eski APK üzerine kurulum reddedilebilir, kaldırma yerel verileri siler. AGENTS.md'deki önceden reddedilmiş contents:write Releases otomasyonu etkinleştirilmedi; kalıcı GitHub Releases teslimi tamamlanmadı. APK ve tam paket sohbet dosyaları olarak ayrıca teslim edilir.

## 26.27 — video ve retweetçi liste kurtarma

- Temel main ff1728542d6336f20124049acf3e4b6475b7048f (26.26). 1000027075.mp4 hedef Boşuna Tıklama profilinde beklemeyi; 1000027074.mp4 ise 1 saatlik gönderi görünse de sonunda 2 saatlik gönderinin açılmasını gösteriyor. 09:04 logunda 420/480 dakikalık gönderiler açılıyor, seçili retweetçi listesi FOLLOWING_LIST olarak değişince terk ediliyor. 1 saatlik gönderiye takip uygulandığı bu kanıtla doğrulanmadı.
- 26.26 yorum başlığı, kişiselleştirilmiş takip etiketi, hedef profil ve hesap sıfırlama düzeltmeleri korundu. Takip düğmesi olmayan doğrulanmış yorumcu 1,5 saniyelik yerleşmeden sonra atlanır. Profil takip alanı sayaçların altındaki düğmeyi de Gönderiler sekmesine kadar kabul eder; gönderilerin içindeki düğmeler hariçtir.
- Seçili yeniden gönderenler sekmesi ilişki listesi sınıflamasından önce gelir. Başlık kaybolursa en fazla iki yukarı kaydırmayla yeniden doğrulanır. Hedefe dönüş görünür Geri düğmesiyle sınırlıdır; iki deneme/6 saniye sonrası aramaya dönülür. Çift Atmaca dönüş isteği kaldırıldı, görev kuyruğu dönüşü yönetir.
- Ortak görev döngüsü yalnız görünür Geri ve yükleme etiketlerinden oluşan boş ekranı 1,8 saniye sonra kapatır; en fazla iki deneme. Normal gönderi, profil, izin ve yazma ekranları bu kurala girmez. Doğrulanmamış işlem sonucu korunur, yeniden işlem gönderilmez. Yüklenmeyen yorum/gönderi atlanır; ilerleme korunur.
- Gönderi dokunuşundan önce aynı aday ikinci taze okumada doğrulanır. Çelişkili aynı satır yaşında genç olan kullanılır. Açılan ayrıntının okunabilir tam tarihi de 120 dakika sınırına göre denetlenir.
- Değişenler: AutomationRuntime, XUiActions, ScreenDetector, EngagementListEvidence, FeedRowEvidence; yeni NavigationSurfaceEvidence, TweetAgeEvidence, DiscoveryVideo27Test; sürüm/paket adları ve notlar. versionCode75 / 26.27-video-recovery.
- CI test/lint/assemble sonucu bekleniyor. Fiziksel X testi YAPILMADI; ekran videosu erişilebilirlik düğüm ağacının yerine geçmez. Kesin cihaz başarısı iddia edilmez. Kaynak/testler main'e kaydedilecek.
- Eski yerel 26.23 çalışması korunmuştur; son ana dalın üstüne yazılmamıştır. Güncel çalışma dalı fix/video-review-26-27. Debug imza farklı olabilir; uygulamayı kaldırmak yerel verileri siler. Önceden reddedilmiş Releases workflow'u etkinleştirilmedi.

## 26.26 — doğrulanmış derleme ve teslim sonucu

- Kaynak main commit: 1c79622383b371426ee6ebe7c4b857afc9af4696. Başarılı GitHub Actions: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34334622307 . 245 test; 0 başarısız, hata veya atlanan. Yeni DiscoveryHandoff26_26Test sınıfındaki 11 testin tamamı geçti. Test XML toplamları indirilen raporlardan doğrulandı. Test/lint/assemble, imza ve manifest kontrolleri başarılı.
- AtmacaNext-26.26.apk: 20.606.684 bayt; versionCode74 / 26.26-discovery-screen-handoff; SHA256 b28c06e2c98aa419c1dd36d34df66e279584db9571b793410a19d04e1c182602.
- AtmacaNext-26.26-TAM-PAKET.zip: 20.360.292 bayt; SHA256 424e49cc99af4d48e6b41748620dfb7c134f974d87c06fc60394e7e1a5e53ae0. ZIP CRC, paket içi/dışı APK eşitliği, manifest sürümü ve CI SHA256 kaydı doğrulandı. APK/tam paket artifact10097288867; rapor artifact10097289747. Son kullanma 8 Aralık 2026.
- Kaynak ve testler GitHub main'de. Yerel inceleme dalı fix/26.26-discovery-validation aynı kaynak commit'inden açıldı. İlk yerel 8f2eead commit'i ile GitHub 1c796223 kaynak ağacı aynıdır (45cb724c71a688b775650761b4489c7e4d298efa). Bu sonuç kaydı [skip ci] belge güncellemesidir; APK yeniden derlenmedi.
- Fiziksel X cihaz testi YAPILMADI. Yeni testler, gerçek runtime hesap sıfırlaması ile sentetik ekran/başlık düzenlerini sınar. Kullanıcının telefonunda iki hatanın kesin çözüldüğü iddia edilmez. İlk cihaz doğrulaması: yorumcu görevinde açılan kişinin sağ üst Takip et -> Takip ediliyor/Beklemede -> ana yorumlara dönüş; iki hesaplı retweetçi görevinde ikinci hesap doğrulaması -> kendi hedefi -> en az iki saatlik gönderi -> seçili yeniden gönderenler -> limitte durma.
- Yeni hata raporunda FLOW / DISCOVERY_STATE / COMMENT_FOLLOW / COMMENT_CLICK / COMMENT_EVIDENCE / SNAPSHOT_ERROR kayıtları ile uygulamanın hata raporu ZIP'i kullanılmalı; yalnız ACCOUNT_SYNC çıktısı görev aşamasını kanıtlamaz. Verilen logdaki Atmaca'ya dönüş doğrulama hatası ayrı açık sorun olarak korunuyor.
- Kalıcı debug imza sorunu çözülmedi. Üzerine güncelleme reddedilebilir; kaldırmak yerel verileri siler. Reddedilmiş contents:write yayın workflow'u etkinleştirilmedi. Mevcut GitHub bağlantısında Release oluşturma/varlık yükleme eylemi bulunmadığından kalıcı GitHub Releases yayını tamamlanmadı. APK/tam paket başarılı Actions çıktısındadır; sohbet dosyaları ayrıca teslim ediliyor.

## 26.26 — 9 Eylül yorum takip / ikinci hesap incelemesi

- Temel main 8ca695385a3409017cf8a79d291ecbebee9aed58; kaynak sürüm 26.25 / code73. Kullanıcı yorumun ayrı Gönderi ekranında Takip et görünmesine rağmen basılmadığını; çoklu retweetçi görevinde ikinci hesabın hedef profilde kaldığını bildirdi. Ekran görüntüsü görüldü. Verilen log 11:18–11:19 başarılı 4 hesap senkronizasyonu ve Atmaca dönüş doğrulama hatası içeriyor; aktif yorum/retweet görevine ait aşama veya ham düğüm kaydı içermiyor. Cihazdaki kesin kök neden bu kanıtla saptanmış değildir. Önceki notlardaki kesin cihaz nedeni iddiaları bu çalışma için kanıt sayılmadı.
- Kodda doğrulanan boşluklar: görünmez eski composer/sayfa düğümleri ScreenDetector kararını etkiliyor; tüm ekranı kaplayan yinelenmiş Gönderi başlığı gerçek başlık yerine seçilebiliyor; profil Gönderiler sekmesi varken satır/sayaç yüklenmediyse profil tanınmıyor; arama sonucu gesture kabulü sonrası step3 yalnız bekliyor; snapshot RuntimeException worker HandlerThread'i sonlandırabiliyor.
- Düzeltmeler: yalnız görünür ekran sınıflaması ve kimlik adayları; başlıkta en küçük gerçek semantik alan, TR/EN heading eki; takip düğmesinin sağ başlık bandı, boyutu ve sonraki yorum sınırı; profil sekmeleri + üstte tam kullanıcı adı ile yüklenen hedefin doğrulanması; yanlış açık profil hedefin repostuyla doğrulanamaz; aramada 2,5 saniye yerleşme sonrası yalnız aynı sorgu duruyorsa en fazla üç sonuç dokunuşu; hesap/döngü başlangıcında arama/gecikme/kaydırma alanlarının tam sıfırlanması; snapshot hatası açık kayıt ve duraklama, worker korunur.
- FLOW, DISCOVERY_STATE, COMMENT_FOLLOW, COMMENT_CLICK ve sansürlenmiş COMMENT_EVIDENCE kayıtları hangi aşama/kimlik/düğme kontrolünün başarısız olduğunu gösterecek. UI düğüm sayısı artık 50'ye kesilmez. Hesap senkronizasyonu/Atmaca dönüş tasarımı değiştirilmedi; logdaki dönüş sorunu açık kalır.
- 11 yeni regresyon testi: yinelenen başlık, TR/EN başlık rolü, büyük ortak kapsayıcı, yakındaki alt yorum, yanlış yazar, yüklenen profil, gizli eski hesap/composer, başka profil repostu, sorgu/bio reddi, sınırlı arama tekrarı, gerçek runtime'da ardışık iki hesap başlangıcı. Testler sentetik erişilebilirlik kanıtlarıdır; fiziksel X testi değildir.
- Değişen kaynaklar: CommentDetailEvidence, ScreenDetector, FeedRowEvidence, DiscoveryProfileEvidence, DiscoverySearchRecovery (yeni), AutomationRuntime, XIdentityDetector, XUiActions, AtmacaAccessibilityService; test DiscoveryHandoff26_26Test; sürüm/paket adları ve notlar.
- versionCode74 / 26.26-discovery-screen-handoff. Yerelde Gradle/Android SDK yok. CI test/lint/APK henüz çalıştırılmadı. Fiziksel telefon/X testi YAPILMADI. Kaynaklar main'e kaydedilecek; sonuç ve gerçek commit sonradan bu bölüme eklenecek.
- Kalıcı debug imza sorunu sürer; önceki APK üzerine kurulum reddedilebilir, uygulamayı kaldırmak yerel verileri siler. Reddedilmiş contents:write yayın workflow'u etkinleştirilmedi. GitHub Releases teslimi henüz yapılmadı.

## 26.25 — yorum takip düğmesi ve hedef geçişi; doğrulanmış teslim

- 1000027063.mp4 incelendi. Yorumcu akışı yorum sahibinin ayrı Gönderi ekranını açıyor ve sağ üstte görünür `Takip et` bulunmasına rağmen işlem yapmadan bekliyordu. Kaynak neden: ilişki seçicisi yalnız tam `Takip et`/`Follow` etiketini kabul ediyor, X'in kişi adını içeren erişilebilirlik etiketini reddediyordu; düğüm tıklaması da yalnız koordinat hareketine dayanıyordu.
- Güvenli eşleştirme `Fth adlı kullanıcıyı takip et` ve `Follow Fth` gibi kişiselleştirilmiş düz takip etiketlerini kapsıyor; `Takip ediliyor`, `Beklemede` ve geri-takip biçimleri hâlâ reddediliyor. Yorum detayındaki tam sağ üst düğmede önce doğrudan ACTION_CLICK, gerekirse aynı düğüm sınırında gesture uygulanıyor; kart üst atasına tıklanmıyor.
- Retweetçi hedef geçişinde X profil başlığındaki @kullanıcı adını geçici olarak yayımlamazsa ikinci hedefte bekleme oluşuyordu. Artık tam header handle öncelikli; eksikse yalnız açık Gönderiler yüzeyi ile beklenen hedefin yazdığı görünür gönderi birlikte kanıt sayılıyor. Yeni hedefe geçerken etkileşim denemeleri, yorumcu dönüş durumu, liste imzası ve kaydırma sayaçları sıfırlanıyor.
- Kaynak main commit: 3b6b573e144a6106eb6efbc9f99b829053dce7ad. Başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34327358272 . 234 test; 0 başarısız, hata veya atlanan. Test/lint/assemble, imza, manifest ve paketleme tamamlandı.
- AtmacaNext-26.25.apk: 20.606.700 bayt; versionCode73 / 26.25-comment-follow-click-target-handoff; SHA256 cd99f0431485ad6c0eb50e2dca8fdf36e824233176af96389d56c2ef756e4e0e. TAM-PAKET SHA256 80baa715e8b94a694f237eba79a6c94726ebbc862b7609354b5a15f20bd7e89f; ZIP CRC ve kayıtlı APK hash eşitliği doğrulandı.
- APK/tam paket artifact 10094418153; test/lint rapor artifact 10094418769. Actions çıktılarının son kullanımı 8 Aralık 2026. Fiziksel telefonda 26.25 henüz çalıştırılmadı; düzeltmenin cihaz sonucu kullanıcı testiyle doğrulanmalıdır.
- Kalıcı debug imza sorunu çözülmedi. Önceki APK üzerine kurulum reddedilebilir; uygulamayı kaldırmak yerel verileri siler.

## 26.22 — doğrulanmış APK ve teslim sonucu

- Nihai APK kaynak main commit: ee92236b46712409db247d57b4677276c9ffe5fc. Başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34316308189 . Bu sonuç kaydı [skip ci] belge güncellemesidir; APK tekrar derlenmedi.
- 227 test; 0 başarısız, hata veya atlanan. Test/lint/assemble, apksigner, manifest ve paketleme başarılı. İndirilen test XML toplamları, ZIP CRC, manifest code70 / 26.22-comment-detail-return ve paket içi/dışı APK eşitliği ayrıca doğrulandı.
- AtmacaNext-26.22.apk: 20.590.304 bayt; SHA256 14b2caa756a8d793913044510e40a2f19332633ea54d95b518296592d4e76061. TAM-PAKET SHA256 93679ed455d5b075efe88ce6d1d4ab9e9cc73ec6395dd955530621568792d605.
- APK/tam paket artifact 10090265481; test/lint rapor artifact 10090266053. Önceki 26.22 ara derlemeleri teslim sürümü değildir; yalnız bu commit/run APK'sı teslim edildi.
- Son inceleme: alıntı girişini arama ve yeniden gönderenler sekmesi deneme bütçeleri ayrıldı; takip düğmesi yokken performStep döngüsüne girip hesap kurtarmasını tetikleme engellendi.
- Kod/test/notlar main'de. Yeni çalışma dalı oluşturulmadı. Yerel kaynak ağacı ve yayınlanan kaynak ağacı 57bf9cc8a949ef081e2448a1d32311b7e774d225 ile eşleşti (sonuç belgesi öncesi).
- Fiziksel X cihaz testi YAPILMADI. 26.20 retweetçi çalışması kullanıcı bildirimi; yeni 26.22 için gerçek cihaz doğrulaması beklenir. Birim testler ekran uyumluluğunun garantisi değildir.
- Kalıcı debug imza çözülmedi; eski sürüme güncelleme reddedilebilir, kaldırma yerel verileri siler. Önceden reddedilen contents:write Releases otomasyonu etkinleştirilmedi; kalıcı Releases yayını yapılmadı. Mevcut Actions dosyaları 8 Aralık 2026'da sona erer.

## 26.22 — yorum gönderisinden takip ve doğrulanmış geri dönüş

- Temel main: 0701d87f1bf591c111434025b3e6930298e038e5 (26.21); 26.20 eab9b0e ile fark incelendi. Kullanıcı 26.20 retweetçi akışını başarılı, 26.21'i başarısız bildirdi.
- 1000027022.mp4 (56,53 sn) ve 1000027023.jpg incelendi. Yorum dokunuşu profil değil yorumun ayrı Gönderi ekranını açıyor; Erol Emin/Arya başlığındaki Takip et kullanılmadan bekleniyor, ardından Bom Report alt yorumuna giriliyor. Runtime yalnız PROFILE kabul ediyor, timeout sonrası herhangi TWEET_DETAIL'i ana liste sanıyordu.
- CommentDetailEvidence yeni saf seçici: açılmış gönderinin başlığındaki tam yorumcu kullanıcı adı ve aynı başlık bandındaki sağ takip düğmesi; yanlış kullanıcı, alt yorum düğmesi, gizli/devre dışı düğme reddi. Başlıktaki yeni Takip ediliyor/Beklemede sonucu doğrulanır; önceden takip edilen/beklemedeki kişi sayılmadan atlanır. Profil yolu korunur.
- Runtime ana yorum görünümünün imzasını/gönderi anahtarlarını saklar. Geri sonrası aynı parent doğrulanmadan başka yorum işlenmez; alt yorum zinciri engellenir. Belirsiz takip sonucu duraklar ve yerine başka kişiye takip yapılmaz. Tek hedef, en az 120 dk, toplam/döngü limitleri korunur.
- Retweetçi: 26.21'in 8 tarama/2 kararlı okuma eşiği yerine 26.20'nin 12 tarama payı ve 3 kararlı okuma; görünür medya geometrisi ilerleme kanıtına dahil, gizli eski sayfalar hariç. Açık Gönderi Etkileşimleri başlığı arkada kalan inline reply kanıtından önce gelir. Seçili yeniden gönderenler sekmesi kontrolü korunur. Yeni retweetçi cihaz videosu yok; bu bölüm kaynak incelemesine dayalıdır, kesin cihaz nedeni iddia edilmez.
- Değişenler: AutomationRuntime, XUiActions, ScreenDetector, yeni CommentDetailEvidence/DiscoveryViewportEvidence ve 15 regresyon testi; versionCode70 / 26.22-comment-detail-return; mevcut contents:read CI paket adları. Hesap ekleme/onaylı takip/takipten çıkma akışları yeniden tasarlanmadı.
- Yerelde Gradle/Android SDK yok; derleme ve test CI'da yapılacak, sonuç henüz bekleniyor. Fiziksel yeni APK/X testi YAPILMADI. Kaynak/testler/notlar main'e kaydedilecek.
- Kalıcı imza ve önceden reddedilmiş Releases workflow yetkisi değiştirilmedi. Debug imza farklı olabilir; uygulamayı kaldırmak yerel verileri siler. Kalıcı Releases teslimi mevcut yetki engeli nedeniyle tamamlanmış sayılmaz.

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

- Nihai kaynak main commit: 939e4c65ea686f3c65679e828d5a7f213eb85dc5; CI 34313479402 başarılı. 212 test; 0 hata, başarısız, atlanan. Lint, assemble, imza, manifest ve paketleme geçti.
- APK: AtmacaNext-26.21.apk, code69 / 26.21-comment-profile-loop; SHA256 f065bf978261a1db1defff02d958a2f73923d65043b141d44abdf7ebabb02041. TAM-PAKET SHA256 2dd10bf5f8727c9ab7399c7a2330477cbbe03f1663bef8a70c19447eb17c22fe. Artifact 10089281698; rapor artifact 10089282179. Fiziksel X cihaz testi henüz yapılmadı.
- 1000027014.mp4, 1000027015.jpg ve 9 Eylül 07:29 cihaz logu incelendi. 26.20 tweet ayrıntısını ilk anda doğru tanıyor; Compose toolbar/back semantiği sonraki tazelemede kaybolunca alttaki editable `Yanıtını gönder` nedeniyle tekrar COMPOSER oluyor. Görünür gerçek tweet/yorum satırları artık kaydırılmış ayrıntı kanıtıdır.
- Yorum başlığının merkezine dokunmak yorumcunun profili yerine yorumun kendi tweet ayrıntısını açıyordu. Birleşik `Ad @kullanıcı · süre` başlığının profil olan sol ad/kullanıcı bölgesine dokunulur. İsim sabit değildir; hedef ve görev hesabı dışlandıktan sonra görünür yorumcular yukarıdan aşağı dinamik işlenir. Takip ediliyor/Beklemede ise işlem yapılmadan geri dönülür; yeni Beklemede limitten bir işlem sayılır.
- Yorum kalmazsa ayrıntı aşağı taranır; ekran sonu kararlıysa hedef profile dönülüp sıradaki en az iki saatlik gönderi açılır. Retweetçi görevinde `Alıntıları görüntüle` yoksa en fazla sekiz tarama veya iki kararlı ekran sonunda aynı geçiş yapılır. Limit dolana ya da uygun gönderi kalmayana kadar döngü korunur.
- versionCode69 / 26.21-comment-profile-loop. Yeni regresyon testi eklendi; CI ve fiziksel cihaz testi henüz yapılmadı.
- İlk CI 34312711274 ürün kodundan değil yeni test düzeneğinin tüm düğümleri aynı koordinatta oluşturmasından dolayı tek testte başarısız oldu; gerçek ekran bantlarını temsil eden ayrı koordinatlar verilerek test düzeltildi ve yeniden çalıştırılacak.
- İkinci CI 34313057927 aynı regresyonun satır gruplama kanıtını sınıflandırmada kullanamadığını gösterdi. Kaydırılmış ayrıntı için görünür, süre taşıyan en az iki farklı yazar başlığı doğrudan erken ekran kanıtına eklendi.
## 26.25 — yorumcu Takip et tıklaması ve sonraki retweet hedefi

- 1000027063.mp4 (61,16 sn) fiziksel 26.24 cihaz kaydı incelendi. Yorumcu Fth'nin ayrı Gönderi ekranı ve sağ üstteki Takip et düğmesi açılıyor fakat işlem yapılmadan bekleniyor. Cihazın düğme açıklamasına kişi adını ekleyebilen biçimi (`... takip et` / `Follow ...`) 26.24'ün dar düz etiket seçicisinde kabul edilmiyordu.
- Kişiye özel düz takip açıklamaları, Takip ediliyor/Beklemede ile çelişmediği sürece kabul edilir. Seçilen gerçek düğümde önce ACTION_CLICK denenir; yalnız bu düğüm başarısızsa aynı düğüm sınırına gesture gönderilir, yorum kartı atasına çıkılmaz.
- Retweetçi görevinde sonraki hedefin profil üst kullanıcı adı geçici olarak erişilemez olduğunda motor bekliyordu. Gönderiler yüzeyi ve hedefin kendi yazar satırı birlikte görünüyorsa hedef doğrulanır; hedef değişiminde yorum/etkileşim/gezinme sayaçları sıfırlanır. Yanlış hedefin tweeti doğrulama sayılmaz.
- İki cihaz regresyon testi eklendi. versionCode73 / 26.25-comment-follow-click-target-handoff. CI ve fiziksel 26.25 cihaz testi henüz yapılmadı.

