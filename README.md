# Atmaca Next — 26.25

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

JDK 17, Gradle 9.5.0 ve Android SDK 37.1 gerektirir. Depodaki `android-build.yml` test, lint, APK ve imza kontrolünü çalıştırır. Başarılı çalıştırmanın **AtmacaNext-26.16-APK** çıktısını indirin.

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
