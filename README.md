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

# Atmaca Next — 26.61

Yorum Alıntısı, Yeni Görev Akışı ekranına eklendi; her seçili hesabın Hesaplar > Alıntı Hedefleri listesini kullanır. Limit hedef başına 1–20 son gönderidir. Üç görev sekmesi telefon ekranında görünür.

Hedef twitten yanıt alanına geçiş, ayrı paragraf metni, Compose düğmesi ve odak/yapıştırma yolu düzeltildi. Tam yorum metni taze ekranda okunmadan gönderilmez. Gönderi açılması başarı değildir; gönderim tek kez yapılır ve yeni yorum/bildirim doğrulanır. Gönderim öncesi takılma, aynı hedef korunarak 10 saniye kurtarmasına girer.

42 yeni regresyon / toplam622 bekleniyor. Yeni CI ve aynı imzalı APK hazırlanıyor; fiziksel yeni cihaz testi yapılmadı. Buse ve26.60 özellikleri korunur. [Devir notu](DEVIR_NOTU.md) / [derleme durumu](BUILD_STATUS.md).

## Önceki sürümlerin tarihsel kayıtları

# Atmaca Next — 26.60

**Takip Etmeyenleri Çık**, Yeni Görev Akışı > Takip bölümünde **Takipten çık** seçeneğinin hemen altındadır. Birden çok hesapla, Limit/Tekrar/Aralık ayarlarıyla toplu çalışır. Buse 1.3'te doğrulanan tarama taşındı: ilk 100 kişi korunur, 101'den daha eski kişilere gidilir; seni takip edenler ve belirsiz satırlar korunur.

Yorumdan dönüş ve diğer geçici gezinme hataları artık 10 saniye kuralından çıkmaz. Atmaca'ya dönülüp aynı görev, ilerleme ve döngüyle hesap yeniden doğrulanır; manuel durdurma ve bekleyen eylem tekrar edilmez. Ayrıntılar [DEVIR_NOTU.md](DEVIR_NOTU.md), test/derleme durumu [BUILD_STATUS.md](BUILD_STATUS.md).

**580 test geçti**, lint 0 hata / 21 uyarı; CI APK ve tam paket doğrulandı. [Gerçek doğrulama](validation/26.60-ci/summary.json). Fiziksel yeni Atmaca/X testi yapılmadı. Kod main'de, Buse değiştirilmedi.

**Aynı imzalı güncelleme APK'sı hazır:** `AtmacaNext-26.60-Guncelleme.apk`, önceki yerel `AtmacaNext-26.59-Yerel-Test.apk` ile aynı sertifikayı taşır ve onun üzerine kurulabilir. 580 testten geçen main CI uygulaması aynı anahtarla yeniden imzalandı; 151 imza dışı APK girdisi değişmedi. APK ve kaynak/rapor/not içeren tam paket ayrı Guncelleme adıyla teslim edilir. [İmza ve paket doğrulaması](validation/26.60-signing/summary.json). Fiziksel yeni cihaz testi ve GitHub Releases yayını yapılmadı.

---
Önceki sürümlerin tarihsel notları:

## 26.59 tarihsel notları

Uygulamanın mimarisi, toplu döngü sırası, geri sayım, son takibi bırak kurtarması ve kalan cihaz kontrolleri [uygulama devir notunda](DEVIR_NOTU.md) açıklanmıştır. Anlık APK/test durumu [BUILD_STATUS.md](BUILD_STATUS.md) içindedir. 26.59 yerelde doğrulandı: **482 test geçti**, lint 0 hata / 16 uyarı ve APK/tam paket kontrolü başarılı. GitHub CI çalıştırıcı atayamadı; son CI doğrulanmış APK 26.58. Yerel 26.59 APK imzası 26.58 CI APK ile farklıdır; uygulamayı kaldırmak kayıtları siler. Fiziksel X testi bekliyor. [Gerçek raporlar](validation/26.59-local/summary.json) / [cihaz kabul adımları](DEVICE_TEST_PROTOCOL.md).

Aşağıdaki sürüm kayıtları önceki hazırlık aşamalarının tarihsel notlarıdır; güncel doğrulama yukarıda ve BUILD_STATUS.md içindedir.

## 26.59 — yalnız açılmayan takibi bırak onayında kurtarma (5 Ekim 2026)

Güncel temel main10bbd57 /26.58; yerel eski26.55 temiz dal bırakılarak güncel main ayrıfix/26.59-unfollow-recovery dalına alındı.22:05 görselindeki22:00:42 hata @azizsisman_ onayı açılmadan16/20 cycle2/2 PAUSED; eski yol watchdog dışına çıkıyordu. Artık onay verilmemiş ve aynı satır Following halinde en az1 saniye stabil,10 saniye geçmişse Atmaca dönüşü ardından aynı task/session/toplam sayaç/döngü ile aktif hesap baştan doğrulanır. Başarısız kişi görevde dışlanır; yalnız uygulanmadığı kanıtlanan bu denemenin bütçesi geri verilir. Tamamlanan/atlanan kişiler ve döngü başlangıç ilerlemesi korunur. Onay verilmiş/belirsiz/kayıp/çelişkili satır veya gerçek geri dönüş bu kurtarma ile ek işlem üretmez. Manuel pause/stop/session değişimi dönüş callback'ini geçersiz kılar.26.58 ortak döngü ve saniyelik geri sayım/tablet/7× korunur.10 yeni test; toplam482 bekleniyor. CI bekleniyor, fiziksel yeni cihaz testi yapılmadı. versionCode107 /26.59-unfollow-recovery. İmza/Releases otomatik onay reddi kısıtları sürer.

# Atmaca Next — 26.58

472 test geçti. Dört hesapta ilk tur biter, ortak döngü süresi beklenir, sonra ikinci tur aynı sırada başlar. Görev ekranında dakika/saniye geri sayımı her saniye güncellenir. Fiziksel yeni APK testi yapılmadı.26.57 ile imza farklıdır; üzerine doğrudan kurulum uyumsuz.

# Atmaca Next — 26.58

## 26.58 — yalnız toplu görev döngüsü (5 Ekim 2026)

Güncel26.57 üzerine dar yama. Kullanıcı4 hesap/2 döngüde ilk hesap turundan sonra Atmaca beklemesini bildirdi. Artık runtime kuyrukta yalnız tur sınırını bildirir; ilerleme veritabanına kaydedilip sıradaki hesap X içinde başlatılır. Tüm hesaplar ilk turu bitirince ortak tam dakika beklemesi başlar; kalan görevler aynı hesap sırasıyla ikinci tura alınır. Farklı aralıklarla seçilen görevlerde ortak bekleme kalan görevlerin en uzun aralığıdır. Tekrarı biten/atlanan/başarısız görev yeniden alınmaz. Manuel pause/deadline/stop korunur; planlı WAITING_INTERVAL kuyruk watchdog tarafından hareketsizlik sayılmaz. Yalnız döngü sahipliği/kuyruk tur geçişi ve ilgili durum etiketi değişti;26.57 takip/tablet/7×/sonuç düzeltmeleri korunur.Geri sayım her saniye dakika/saniye gösterir, ekran güncellemesi her saniye DB yazmaz.13 yeni test; CI bekleniyor, fiziksel yeni APK testi yapılmadı. versionCode106 /26.58-batch-cycles; com.atmacanext.v258/Room6 aynı. İmza/Releases otomatik onay reddi kısıtları sürer.

# Atmaca Next — 26.57

459 test geçti. Boş onaylı listede önceki kaynak profile dönüp ziyaret edilmemiş ikinci/üçüncü takipçi ekran sırasıyla denenir. Sayaç,10 saniye kurtarma,7× hız/tablet korunur. Fiziksel yeni cihaz testi yapılmadı.26.56 ile imza farklıdır; doğrudan kurulum uyumsuz.

# Atmaca Next — 26.57

## 26.57 — boş onaylı listeden sıralı takipçi geçişi (5 Ekim 2026)

Güncel 26.56 temeli korunur. 1000033428.mp4 sonunda Gökçe Yalın onaylı listesinde yalnız kendi hesap satırı vardır. Yeni kaynak yoksa iki saniyelik taze liste beklemesinden sonra bir önceki kaynak profile kimliği doğrulanarak geri dönülür ve normal takipçilerindeki ziyaret edilmemiş kişiler ekran sırasıyla denenir. Aynı onaylı listede uygun kaynak varsa önceki rastgele zincir korunur. Eksik kota kaynak tükenince tamamlanmış sayılmaz. Sayaç/session/bekleyen işlem, tablet ve 7× hız korunur. Yeni alanlar görev sıfırlamasında temizlenir. versionCode105. CI bekleniyor; fiziksel yeni APK testi yapılmadı. Kalıcı imza ve AGENTS.md yayın onayı kısıtları sürer.

# Atmaca Next — 26.56

26.56: yalnız Onaylı takipte Geri Takip Et/sonuç doğrulaması, ara hesap seçici ekranı ve10 saniye hareketsizlik kurtarması güncellendi. Sayaç/aşama/bekleyen işlem korunarak motor adımı yeniden başlar; CI37348618113 başarılı:455 test/20 yeni regresyon, lint/APK imzası/paket doğrulaması geçti. Önceki26.55 APK ile sertifika farklı; doğrudan üzerine kurulum uyumlu değil. Fiziksel yeni APK testi yapılmadı. Diğer26.55 davranışları korunur.

26.55: güncel 26.54 main korunarak sol/sağ/alt gezinme çubuğu canlı semantik ve sınırlarla bulunur. Tablet kaydırması içerik panelinde kalır; hesap seçici gerçek drawer paneline bağlanır. Önceki görev düzeltmeleri ve En hızlı · 7× korunur. CI 37341767741 başarılı:435 test (18 yeni), lint ve APK/imza/paketleme geçti. APK kaynağı efde2b84dc211c515bbbfc5c83f41f80f989b785. Fiziksel cihaz testi yapılmadı.

26.54: hesap seçicisinde ilk hesap durması, ikinci hesapta kaydırma hatası ve onaylı liste sonu düzeltildi. Limit dolmazsa aynı listeden rastgele başka kaynak → takipçileri → Onaylı döngüsü sürer. Geçici ekran hataları aynı aşamadan yeniden okunur; ortak hareketsizlik denetimi tüm görevleri kapsar. Varsayılan 7× hız ve Ayarlar’da En hızlı · 7× seçeneği eklendi. CI 37303366905 başarılı; ayrıntılar DEVIR_NOTU.md içinde.


26.53: tüm görevlerde önceki X ekranından hesap menüsüne sınırlı Back ile dönülür; sonraki hesap tam kimlikle doğrulanır. Açılan yorumun alt yanıtlarına girilmez; Takip et yoksa ana yorumlara dönüp sıradaki görünür yorumla devam edilir. Yeni takip isteği Beklemede olduğunda onay beklenmeden işlem sayılır.

26.53 için [CI 37299334945](https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/37299334945) başarılı: 384 test (32 yeni regresyon), lint, APK imzası ve paketleme geçti. APK kaynağı `bb7f9e4ffcf5071be632e6b5df0e827d8b6cf6f2`. Ayrıntılar [DEVIR_NOTU.md](DEVIR_NOTU.md) ve [BUILD_STATUS.md](BUILD_STATUS.md) içindedir. Yeni APK henüz fiziksel cihazda doğrulanmadı; Releases workflow onayı bekleniyor.

26.52'den korunan davranış: yorumcu takipte yalnız o yorum satırına bağlı resim/GIF/video medya olarak atlanır. Görünür metin yorumları sırayla işlenir; belirsiz kimlik/tıklama sessiz atlama yerine ilerleme korunarak duraklar. Yorumcu ve retweetçi takip, hedef profile geri dönünce yeniden Geri/arama yapmadan aynı konumdan taramaya devam eder.

26.52 için [CI 37293523368](https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/37293523368) başarılı: 352 test (18 yeni regresyon), lint, APK imzası ve paketleme geçti. Test edilen APK kaynağı `7e5b34890d78b7d8dd272014543ba74beb5b1660`. Ayrıntılar [DEVIR_NOTU.md](DEVIR_NOTU.md), [BUILD_STATUS.md](BUILD_STATUS.md) ve paket içindeki NOT_DEFTERI.txt içinde tutulur. Yeni APK henüz fiziksel X cihazında doğrulanmadı; Releases yayını için depoda belirtilen workflow onayı bekleniyor.

26.51'den korunan davranış: Yorum Alıntısı, hedef gönderiyi doğruladıktan sonra X'in ayrı yorum ekranını veya gönderi içindeki yorum alanını kullanır. Metin yeni okumada doğrulanır, gönderim bir kez yapılır ve sonuç belirsizse aynı yorum tekrar gönderilmez.

Alıntı hedefleri ayrı kaydedilir. Limit her hedef içindir: 5 hedef × Limit 1 = 5 yorum. Seçili hesaplar tek grupta sırayla çalışır; kuyruk tamamlanınca Atmaca Next öne gelir.

26.51 için [CI 37289341028](https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/37289341028) başarılı: 334 test, lint, APK imzası ve paketleme geçti. Test edilen APK kaynağı `7df763bcbe58fc79878d391b641ebdcf757b11aa`. Ayrıntılar [DEVIR_NOTU.md](DEVIR_NOTU.md), [BUILD_STATUS.md](BUILD_STATUS.md) ve paket içindeki NOT_DEFTERI.txt içinde tutulur. Yeni APK henüz fiziksel X cihazında doğrulanmadı; Releases yayını için depoda belirtilen workflow onayı bekleniyor.

## Geçmiş sürüm notları — 26.33

26.33: profil açıklamasındaki “bildirimleri açın” artık bildirim penceresi
sayılmaz. Pencere sınıflandırması gerçek dialog/kapatma kontrolü gerektirir.

26.32: hedef profilde medya kaydırmayı yutarsa iki ayrı sol güvenli şeritte daha
uzun kaydırmayı tekrarlar ve iki başarısız okumada hedefi terk etmez. Yorumcu
takibinden/atlamasından sonra sıradaki görünür üst yoruma takılmak yerine alt
yorumlara bir zorunlu kaydırma yapar ve `COMMENT_SCROLL` kanıtını kaydeder.
Ayarlar'daki `Tüm işlemlerin hızı` 71–15000 ms aralığında takipten çıkma,
onaylı takip, yorumcu ve retweetçi dahil tüm tıklama/kaydırma/doğrulama
adımlarını ölçekler; hesap ve toplu görev geçişleri ayrıca ayarlanır.

26.31: yorumcu/retweetçi hedef profilindeki video, GIF veya fotoğraf orta ekran
kaydırmasını yutsa bile gönderi akışını sol güvenli kenar şeridinden dikey
kaydırır. Toplu görevde yeni görev aynı temiz tarama durumuyla başlar.

26.30: X açılan yorumun kullanıcı adını erişilebilirlik ağacından gizlese bile,
Gönderi ekranının üst-sağ başlık bölgesindeki tek Takip et düğmesine basar.
Düğme yoksa alt yorumların düğmelerine dokunmadan sıradaki yoruma döner; sonucu
aynı üst-sağ alanda Takip ediliyor/Beklemede olarak doğrular.

26.29: yorumcu başlığındaki satır-genişliğinde Takip et semantiğini gerçek sağ
aksiyon bölgesinden tıklar. Retweetçi akışında gönderi metni profilde genişleyip
detay açılmadığında aynı doğrulanmış gönderiyi farklı güvenli gövde noktalarından
sınırlı olarak yeniden dener. 26.28'in aynı gönderi doğrulaması korunur.

26.28: gönderi metni profilde genişleyip detay açılmadığında aynı gönderiyi
taze metinle yeniden açma; en fazla üç dokunuş, ardından geri basmadan taramaya devam.
Gönderiye dokunulması başarı sayılmaz. Fiziksel cihaz doğrulaması beklenir.

26.27: seçili retweetçi listesini koruma, boş yükleme ekranından sınırlı dönüş,
takip düğmesi olmayan yorumcuyu atlama ve ayrıntıdaki tam saatle 120 dakika kontrolü.
26.26 hedef/yorum başlığı düzeltmeleri korunmuştur. Cihaz doğrulaması beklenir.

26.26: görünür ekran/yorum başlığı doğrulaması, hesap başına temiz arama ve açılmayan arama sonucuna sınırlı tekrar. Cihaz doğrulaması beklenir.

26.25: yorumcu gönderisinin sağ üstündeki kişiye özel Takip et açıklamasını kabul eder,
önce gerçek erişilebilirlik tıklamasını dener ve sonraki retweet hedefini gönderi yüzeyiyle doğrular.

26.22: açılan yorum gönderisinin kendi üst başlığındaki Takip et düğmesi, tam yorumcu
kimliğiyle işlenir; profil açılması da desteklenir. Geri dönüş ana yorum listesiyle
eşleştirilir, alt yorumlara zincirleme girilmez. Etkileşim başlığı satır içi yanıt
alanından önce tanınır; alıntı girişi yoksa 26.20 tarama payıyla sonraki gönderiye geçilir.
Derleme/test sonucu DEVIR_NOTU.md içinde; fiziksel X cihaz doğrulaması ayrıca gerekir.

26.21: yorum kartı yerine yorumcu adının profil alanını açma; kaydırılmış yorum ekranını
oluşturucu sanmama; Alıntıları görüntüle bulunmayan gönderiden sıradaki gönderiye geçme.
26.20: tweet ayrıntısındaki satır içi yanıt alanını oluşturucu sanmadan yorumları işleme;
26.19: ayrı Compose yazar/saat/metin alanlarından gönderi okuma, öneri kartı/profil ayrımı,
yorumcu profilinden kontrollü dönüş, retweetçi sonuç çelişkisi kontrolü ve eksik limitte duraklama.
26.18 cihazda başarısızdır. 26.19 cihaz doğrulaması beklenir; birim testler cihaz testi değildir.

**Başka sohbet veya ChatGPT hesabından devam:** önce [DEVIR_NOTU.md](DEVIR_NOTU.md) dosyasını okuyun. Hesap eklemenin başarılı cihaz doğrulaması, 26.16 yerel hedef profil yönlendirmesi, test durumu ve APK bilgileri burada kayıtlıdır.

Gönderilen V25.12.17 kaynak paketi temel alınmıştır. Hesap taraması, hesap geçişi ve görev oluşturma/çalıştırma açıktır. Görevler yalnız kullanıcı başlattığında mevcut ekran otomasyon motoruyla çalışır. API ile içerik üretimi kapalıdır; içerik elle girilir.

## Kullanım

1. X Android uygulamasında hesaplarınızın oturumunu açın.
2. Atmaca Next > Ayarlar > Ekran okuma bölümünden erişilebilirlik hizmetini etkinleştirin.
3. Hesaplar > X hesaplarını tara. Telefonun kilidi açık kalmalı; tarama sırasında X'i elle değiştirmeyin.
4. En fazla 10 hesap, kullanıcı adı / takipçi / takip edilen bilgileriyle eklenir. Eksik sayaç başarılı kayıt sayılmaz.
5. Her karttaki **Hesaba geç**, X'in hesap seçicisinde ilgili oturumu seçer; hesap menüsündeki kimliği doğrular, sayaçları günceller ve Atmaca'ya döner.
6. Durdur, sonraki gezinmeyi keser. Daha önce tamamlanan kayıtlar korunur. Yeniden tarama kullanıcı adına göre günceller.

## Sınırlar

- X API, Selenium veya ağdan hesap verisi alma yoktur. Bu APK'nin INTERNET izni bulunmaz. X'in kendi internet bağlantısına ihtiyacı vardır.
- Android AccessibilityService ekran metnini okur ve düğmelere basar. Profil açmak için X uygulamasına yönlendirilen Android bağlantıları kullanılır; HTTP istemcisi değildir.
- OCR içermez: X erişilebilirlik ağacında paylaşmadığı bir alanı okuyamaz. Ekran dili için Türkçe/İngilizce etiketler bulunur; cihazdaki X sürümüyle fiziksel test gerekir.
- Sayaçlar ekranda göründüğü biçimdedir: “1,2 B” tam sayıya çevrilmez.
- Atmaca ön plana döner. Android 14+ başka uygulama sürecini kapatmaya izin vermez; X arka planda kalabilir. Eski Android sürümlerindeki kapatma isteği de “zorla durdurma” değildir. Oturum kapatılmaz.
- Etkin hesap rozeti son doğrulamayı gösterir; X'teki manuel değişiklikleri canlı izlemez.
- Önceden kayıtlı hesaplarla birlikte toplam sınır 10'dur; artık kullanılmayan kayıtlar elle kaldırılabilir. X'ten çıkış yapılmaz.
- Önceki APK farklı sertifikayla imzalıysa yeni APK üstüne kurulmaz. Mevcut uygulamayı silmek verileri siler; üretim güncellemeleri için aynı imzalama anahtarı gerekir. Bu depodaki Actions debug imzası test amaçlıdır.

## Geliştirme / APK

JDK 17, Gradle 9.5.0 ve Android SDK 37.1 gerektirir. Depodaki `android-build.yml` test, lint, APK ve imza kontrolünü çalıştırır. Başarılı çalıştırmanın **AtmacaNext-26.54-APK** çıktısını indirin.

```sh
gradle testDebugUnitTest lintDebug :app:assembleDebug
```

Android araç zinciri sürümleri gönderilen kaynak temel alınarak yapılandırılmış; AndroidX gereksinimleri için compileSdk 37.1 kullanılmıştır. targetSdk 36 ve minSdk 26 korunmuştur. Gradle wrapper JAR dosyası gönderilen kaynakta yoktu; CI Gradle'ı setup-gradle ile kurar.

## Doğrulama

- Sayaç regresyon testleri: aynı satırda iki farklı sayı, ayrı düğümler, Türkçe kısaltma, sıfır, eksik sayaç ve bio içindeki yanıltıcı sayılar.
- Kaydetmeden önce tek SAVING aşaması; iptal/yeniden başlat sonrası eski async callback'ler gezinme başlatamaz.
- Toplam 5 dakika ve aşama başına 20 saniye sınırı, en fazla üç ekran kurtarma denemesi.
- Fiziksel test: 1/2/10 hesap, kaydırılan hesap seçici, tekrar tarama, eksik sayaç, yanlış hesap, ekran kilidi, hizmet bağlantısının kesilmesi ve geri dönüş.

Eski VALIDATION/BUILD_STATUS dosyaları V25.12.17 paketinden gelen tarihsel notlardır; bu sürümün test sonucu olarak kullanılmamalıdır.

## 26.2 değişiklikleri

- Hesap geçişinden sonra profil bağlantısına bağımlılık kaldırıldı: menüde tam kullanıcı adı ve iki sayaç doğrulanır.
- Okunamayan hesap atlanır, diğer oturumlar denenir. Aşama başına 20 saniye ve hesap başına 45 saniye sınırı vardır.
- Ekran olayları bekleyen otomasyon kontrolünü sürekli erteleyemez.
- Görevler sekmesi gerçek oluşturma/düzenleme ve seri çalıştırma ekranına bağlandı. Tarama ve görev aynı anda çalıştırılamaz.
- Ayarlarda işlem/geçiş/görev bekleme süreleri, hata sonrası devam ve kayıt saklama süresi; keşif görevleri için hesap başına hedef listesi.
- Kayıtlarda hesap taraması filtresi, aşama/ekran/hedef bilgisi ve TXT dışa aktarma.
- Kullanıcının videosunda dört oturum ve ilk seçimden sonra X ana sayfasında kalma görüldü. Yeni sürüm için fiziksel cihaz doğrulaması gereklidir; birim testler ekran uyumluluğunu kanıtlamaz.

## 26.3 düzeltmeleri

- Kalıcı erişilebilirlik izni ile Android hizmet bağlantısı ayrıldı. İzin açıkken tekrar ayarlara yönlendirme yok; bağlantı için en fazla 10 saniye beklenir. Android tarafından gerçekten kaldırılan izni uygulama kendi kendine veremez.
- Takipten çıkma artık 100 kullanıcı derinliğini beklemeden doğrulanan Takip edilenler listesindeki görünür kullanıcılardan başlar. Kullanıcı adı, düğme ve işlem sonucu doğrulaması korunur.
- Servis seçili sekme bilgisini ham erişilebilirlik ağacından okur; sekme açıklamasındaki ek metin liste yüklenmesini engellemez. Seçili sekme, komşu doğrulanmış takipçi başlığından önceliklidir.
- Tarama aşamaları arası gecikme azaltıldı. Hesap değiştiği kesin olarak görüldüğünde sabit bekleme sonuna kadar beklenmez.
- Dönüş mevcut Android uygulama penceresini öne alır, açılışı ana iş parçacığında tekrar dener ve Activity yaşam döngüsüyle doğrular. X sürecini öldürme kaldırıldı. Yeni X işlemi başlarsa eski dönüş denemeleri iptal edilir.
- Yeni APK için fiziksel telefon doğrulaması yapılmadı; birim testler cihazdaki X arayüzü uyumluluğunu kanıtlamaz.

## Hesap başına hedef sayfalar

Hesaplar kartındaki **Hedef hesap ekle** ile en fazla üç kullanıcı adı kaydedilir. Yorumcu ve retweetçi görevleri görevi çalıştıran hesabın kendi hedeflerini kullanır. Alan boşaltılıp kaydedilirse hedef kaldırılır; kayıt tek işlemle yapılır.

Hedef profilin en son beş gönderisi taranır; önceki 90 dakika alt sınırı kaldırılmıştır. Ekranda sabitlenmiş/reklam işaretli gönderiler atlanır. Yorum satırında takip düğmesi yoksa erişilebilirlikte açıkça görülen yazar alanından profil açılır; tam kullanıcı adı ve takip sonucu doğrulanır. Yazar bilgisi X tarafından paylaşılmıyorsa metin içindeki @bahsetmelerden hedef üretilmez. Hedef kaydetmek görev başlatmaz; kullanıcı Görevler'den başlatır.

## 26.4 — ikinci hesapta uyarı nedeniyle durma

Kullanıcının logunda ilk hesap başarıyla kaydedildi, ikinci seçimde UNKNOWN_DIALOG nedeniyle tüm tarama kesildi. Metin içindeki “ok” gibi alt dizeler artık ileti penceresi kanıtı sayılmaz; gerçek kontroller/pencere işaretleri kullanılır. Uyarılarda onay verme yoktur: bilinen isteğe bağlı istemler olumsuz düğmeyle, diğerleri geri eylemiyle en fazla iki kez kapatılır; çözülemeyen hesap atlanır. Deneme ve sınırlı arayüz kanıtı kayda eklenir. Doğrudan Atmaca dönüşü doğrulanmazsa son uygulamalarda yalnız tam “Atmaca Next” başlıklı kart açılmaya çalışılır. Bu da başarısızsa izin mevcut olduğunda dönüş bildirimi gösterilir. Gerçek telefonda doğrulama henüz yapılmadı.

## APK, ZIP ve ayrıntılı not defteri

Her başarılı main derlemesinin APK ve tam ZIP paketini GitHub Releases bölümünde saklamak için yayın taslağı hazırlandı; otomatik onay incelemesi contents:write yetkili workflow etkinleştirmesini reddettiği için henüz etkin değil. Mevcut 26.6 APK/ZIP çıktısı [doğrulanmış derlemenin Artifacts bölümündedir](https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34111507110). Aşağıdakiler planlanan yayın paketinin içeriğidir. Her yayında NOT_DEFTERI.txt, SHA256SUMS.txt ve APK bulunur. Tam paket ZIP ayrıca kaynak kodun ZIP'ini, test raporlarını ve manifesti içerir. Ayrıntılı güncel çalışma notları [DEVIR_NOTU.md](DEVIR_NOTU.md) içindedir. Yayın adındaki build numarası APK'nın tam kaynağını belirtir; farklı build'lerin imzaları aynı kabul edilmemelidir.

## 26.7

Geri Takip Et sonucu takipten çıkma başarısı olarak doğrulanır; hesap ve döngü başına başlatılan işlem için ayrıca kesin limit kontrolü vardır. Sonuç belirsizse yeni kişiye geçmek yerine duraklar. Varsayılan bekleme 800 ms, sonuç sabitleme 650 ms. Görev ekranı koyu temaya uyar; yeni görevler yalnız Takip ve Etkileşim sekmelerindedir. Tweet/alıntı oluşturma kaldırıldı. Her Actions APK çıktısında tam paket ZIP ve NOT_DEFTERI.txt bulunur. Cihaz doğrulaması bekleniyor.

## 26.8 — Onaylı kullanıcı takip et

Görevler → Yeni görev → Takip → Onaylı kullanıcı takip et. Kendi profilinin takipçilerinden ilk kaynak açılır; seçili Verified Followers / Onaylı Takipçiler listesinde yalnız Takip et düğmeleri işlenir. Geri takip et ve Takip ediliyor atlanır. Kaynak biterse açık listeden başka profilin onaylı takipçileriyle devam edilir. Üç ardışık Takip ediliyor → Takip et dönüşünde başarı yazılmadan o hesap atlanır ve sıradaki hesap çalışır. Sonuç belirsizse duraklar. Kaynak/profil/sekme doğrulaması ve görev limitleri korunur. Cihaz testi bekleniyor.

## 26.14

Followers you know ile gerçek Followers ayrıldı. Followers listesinin en üstündeki güncel kullanıcı, takip durumundan bağımsız kaynak seçilir. Kullanıcı adı sabit değildir. Ardından onun Verified followers sekmesi açılır. Tam düzeltme ve teslim durumu NOT_DEFTERI.txt içindedir.

## 26.15

Yorumcu/retweetçi görevi, görev hesabının kendi profilini doğruladıktan sonra hedef profile ayrı ve gecikmeli bir aşamada gider. X ilk bağlantıyı geçiş sırasında yutarsa hedef kullanıcı adı ekranda doğrulanana kadar en fazla üç farklı yönlendirme isteği gönderilir; açık hesabın profilinde bekleyip görevi bitirmez. Her deneme `DISCOVERY_TARGET` kaydına hedef, deneme ve ekranla yazılır. Varsayılan işlemler arası süre 500 ms, hesap geçişi 1800 ms ve görevler arası süre 1500 ms'dir; kimlik ve sonuç doğrulamaları korunur.

Doğrulanmış 26.15 derlemesi: [GitHub Actions çalışması](https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34215844253), 177 test ve sıfır hata. Fiziksel X cihaz testi ayrıca gereklidir.

## 26.16

26.15 cihaz logu hedefin doğru `@pusholder` olarak okunduğunu fakat üç `https://x.com/pusholder` isteğinin de X tarafından yutulduğunu kanıtladı. İlk yönlendirme artık X'in yerel `twitter://user?screen_name=` şemasını kullanır. Olmazsa twitter.com ve x.com ayrı yolları denenir; her yol 2,5 saniye bekler. Tam hedef kullanıcı adı görünmeden gönderi taraması başlamaz.

Doğrulanmış 26.16 derlemesi: [GitHub Actions çalışması](https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34219134897), 178 test ve sıfır hata. Yerel profil rotasının fiziksel cihaz testi bekleniyor.

## 26.17 — X içi hedef profil araması

- 8 Eylül 14:46 logu ve 1000026873.mp4: görev hesabı bildirimhaber1 doğrulanıyor; pusholder hedefi doğru okunmasına rağmen 26.16 native-user/twitter-web/x-web yollarının üçünde de kendi profilinde kalıyor. startActivity kabulü hedefin açılması değildir; Android/X seviyesindeki kesin neden bu kanıtla belirlenemez.
- Üç bağlantı sonucu hedef doğrulanmazsa veya bağlantı başlatılamazsa SEARCH_DISCOVERY_TARGET başlar. Kendi profilinden Geri ile alt gezinmeye, genel Ara sekmesine, arama alanına ve tam @hedef önerisine gider. Profilin üstteki kendi gönderilerinde arama simgesi kullanılmaz. Öneri gelmezse desteklenen cihazlarda IME araması bir kez gönderilir ve yalnız seçili Kişiler sekmesinde sonuç seçilir.
- Hedef profil başlığı doğrulanmadan gönderi taraması başlamaz. Sorgu alanı, kısmi kullanıcı adı, metin içi bahsetme ve takip düğmeleri sonuç sayılmaz. 45 saniyede hedef doğrulanmazsa görev tamamlanmış sayılmadan duraklar. DISCOVERY_SEARCH adım/eylem/kabul/ekran kayıtları eklendi.
- Değişiklikler: AutomationRuntime.kt, yeni DiscoverySearchSelector.kt ve DiscoverySearchSelectorTest.kt, sürüm code65 / 26.17-search-profile-navigation, mevcut salt-okuma build workflow paket adları.
- Tek hedef, en az 120 dakikalık hedef gönderileri, takip limitleri, Beklemede sayımı ve hesap senkronizasyonu korunur.
- Derleme/test sonucu henüz bekleniyor. Gerçek telefonda yeni arama yolu test edilmedi; video 26.16 hatasının kanıtıdır.
- Kalıcı imza çözülmedi. GitHub Releases contents:write workflow için önceki otomatik onay reddi nedeniyle yetki değişikliği yapılmadı; teslim arşivi durumu ayrıca kaydedilecek.

## 26.18 — Gönderiler listesinde dikey kaydırma ve metinden gönderi açma

- Kullanıcı 26.17 hedef aramasının fiziksel cihazda çalıştığını doğruladı. 1000026954.mp4 ve 8 Eylül 19:15 logu incelendi: hedeften sonra Yanıtlar, ardından Videolar sekmesine kayılıyor. Kodun genel ACTION_SCROLL_FORWARD seçimi profilin yatay pager'ını ilerletiyordu; istenen aşağı kaydırma gerçekleşmiyordu.
- Yorumcu/retweetçi keşif kaydırmaları artık yalnız dikey ListGesture kullanır. Profilde açık Gönderiler sekmesine ekstra giriş yok. 850 ms yerleşme süresiyle okunur. Çalışmayan üç deep-link denemesi başlangıçtan çıkarıldı; doğrudan cihazda doğrulanan X içi arama kullanılır.
- XTweetInspector tek satırdaki isim/@handle/2 sa başlığını okur. Kartın genel tıklaması kaldırıldı: gönderinin metin düğümüne gesture tap yapılır, video/medya/aksiyon düğmeleri seçilmez. Uygun gönderi metni bulunamazsa yanlış yere basmak yerine açık nedenle duraklar. Yorumcu yazarının birleşik başlığı da tıklanabilir. Birden fazla yazar başlığı içeren liste kapsayıcısı gönderi sayılmaz.
- Kaydırılmış yorum ekranı Yanıtını gönder + Alıntıları görüntüle kanıtıyla tanınır. Retweetçi akışı Alıntıları görüntüle görünene kadar aşağı ilerler, sonra seçili ve sayısı değişken yeniden gönderenler sekmesini doğrular.
- Görev toplam limiti dolunca durur; gönderinin adayları yetmezse aynı hedefte alttaki en az 120 dakikalık gönderiye geçer. Tek hedef, işlem/kimlik doğrulaması ve Beklemede sayımı korunur.
- DISCOVERY_SCAN görünür gönderi/saat/kaydırma; DISCOVERY_TWEET seçilen gönderi/metin kanıtını kaydeder.
- Yeni 9 regresyon testi: TR/EN birleşik başlık, yaş sınırı, gövde bahsetmesi reddi, metin/medya seçimi, gizli düğüm, kaydırılmış yorum ekranı ve 66 değişken sekme etiketi.
- code66 / 26.18-vertical-discovery-feed. CI sonucu henüz bekleniyor; fiziksel yeni sürüm testi yapılmadı. Kullanıcının main gönderimi ve APK derleme onayı geçerlidir. Kalıcı imza ve GitHub Releases workflow yetkisi değiştirilmedi.

