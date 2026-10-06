# 26.61 — Yorum Alıntısı cihaz kabulü (henüz yapılmadı)

1. Önceki yerel26.59 veya Guncelleme26.60 üzerine aynı sertifikalı APK kurulur; kaldırma yapılmaz. Hesaplar, görevler ve hedeflerin korunması kontrol edilir.
2. Görevler ve Yeni Görev Akışı içinde Takip / Etkileşim / Yorum Alıntısı görünür. Yorum Alıntısı seçildiğinde yorum metni ve her hesabın hedef sayısı görünür; hedefi olmayan hesapla kayıt engellenir.
3. İki hesapta farklı aktif Alıntı Hedefleri tanımlanır. Limit1 / iki hedef / tekrar1 ile her hesap kendi iki hedefinin birer son gönderisine yorum bırakır. 1/2 tamamlandı sayılmaz; hesap ve yorum doğru olmalıdır.
4. Hedefin uzun gönderisi, ayrı paragraf metni, çok kısa gönderisi ve inline yanıt alanı denenir. Metin tamamı ile alana aktarılır; üst veya inline Yanıtla tek kez kullanılır; alt yorumun Yanıtla düğmesine basılmaz.
5. Ekranı geç yükleten koşulda yorum gönderilmeden10 saniye geçerse Atmaca'ya dönüş ve aynı ilerleme/hedefe devam kontrol edilir. Gönderilmeyen seçili twit yeniden taramada atlanmamalıdır.
6. Gönderimden sonra yükleme/okuma gecikmesi oluşturulur: otomatik tekrar gönderim ve Back yoktur. Aynı metin bir kez yayımlanır; başarı yalnız yeni yorum veya taze gönderildi bildirimiyle sayılır. Eski bildirim ve boşalan alan tek başına sayılmaz.
7. Gönderim öncesi ve sonrası manuel Duraklat/Durdur denenir; eski callback gönderim yapmamalıdır. Mevcut göreve yeni hesap eklenince diğer hesabın işlenen/pending anahtarları taşınmamalıdır.
8. Takip Etmeyenleri Çık ilk100 koruması,20 limit,10 saniye kurtarma, diğer takip görevleri ve toplu döngü sırası korunmalıdır.

Fiziksel sonuç henüz yok; CI birim testleri cihaz başarısı olarak gösterilmez.

## Önceki cihaz protokolleri

# 26.60 cihaz kabul adımları

- Yeni Görev Akışı > Takip: Takip Etmeyenleri Çık, Takipten çık'ın hemen altında görünmeli. Aynı ekranda birden çok hesap, limit 20, tekrar 2 ve aralık seçilebilmeli.
- Yeni görev ilk 100 farklı kişiyi korumalı; 101 ve daha eskilerde kendi Seni takip ediyor/Follows you etiketi olanlara dokunmamalı. 1/20 sonrasında sürmeli, 20 doğrulanmış sonuçta tur bitmeli.
- Dört hesap/iki tur: tüm hesapların ilk turu, ortak dakika/saniye beklemesi, aynı sırada ikinci tur. Eksik limit tamamlanmış gibi gösterilmemeli.
- Yorumcu takip 4/35: ana yorumlara dönüşte HOME görüldüğünde 10 saniye ilerleme yoksa Atmaca'ya dönülmeli; aynı taskId/session/progress/cycle ve aynı hedefle X'te yeniden başlatılmalı. İşlenen kişiler tekrar sayılmamalı.
- Ana yorumun başlığı görünür fakat satırlar geç yüklenirken fazladan Geri basılmamalı. COMMENT_RETURN başlık/çocuk/ana yorum kimliği ve süreyi, TASK_RESTART_RETURN gerçek dönüş sonucunu kaydeder.
- Geçici ekran/gezinme hatalarında onaylı, yorumcu, retweetçi, yeni/normal çıkma ve yayın görevleri ortak 10 saniye yolunu kullanmalı. Bekleyen eylem, manuel pause/stop, ekran kilidi, X limiti ve planlı bekleme otomatik yeniden işlem üretmemeli.
- Atmaca'ya dönüş sırasında Duraklat/Durdur'a basıldığında gecikmiş callback X'i tekrar başlatmamalı. Aynı ilerlemede üç başarısız yeniden başlatmadan sonra sayaçlar korunarak durmalı.

Bu yeni Atmaca APK'sında bu cihaz adımları henüz uygulanmadı. Kullanıcı Buse 1.3'ü çalışır olarak doğruladı; bu yalnız Buse kanıtıdır.

---
Önceki cihaz protokolleri:

## 26.59 takibi bırak kurtarması ve döngü kabulü

Bu bölüm yeni 26.59 APK ile yapılacak fiziksel kontrollerdir; henüz yapılmadı. Birim test ve derleme sonucu cihaz doğrulaması yerine geçmez.

1. Ayarlar sürümü `26.59-unfollow-recovery`, APK versionCode 107 olmalı. Kurulu APK ile imza eşleşmeden üzerine güncelleme varsayılmamalı; uygulamayı kaldırmak yerel kayıtları siler.
2. Takibi bırak onayı açılmayan mevcut hata senaryosunda aynı kişi hâlâ Takip ediliyor olarak en az 1 saniye kararlı okunmalı. Denemeden itibaren 10 saniye sonra Atmaca dönüşü ve X hesabının yeniden doğrulanması görülmeli. `UNFOLLOW_RESTART` kaydı gerçek dönüş sonucunu yazmalı.
3. Örnekteki 16/20 ilerleme ve 2/2 döngü korunmalı; görev ilk döngüye dönmemeli. Onayı açılmayan kişi yeniden işlenmemeli, başarısız deneme başarı sayılmamalı. Kalan uygun kişilerle aynı görev devam etmeli.
4. Onay verilmiş, kişi satırı kaybolmuş veya Takip et/Takip ediliyor düğmeleri çelişkiliyse bu kurtarma ek işlem üretmemeli. Dönüş sırasında Duraklat/Durdur ve yeni oturum, eski callback'in görevi sonradan başlatmasını engellemeli.
5. Dört hesap ve iki döngüde ilk tur 1→2→3→4 sırasıyla bitmeli. Ortak dakika/saniye geri sayımı son hesabın turundan sonra başlamalı; süre sonunda kalan görevler aynı sırayla otomatik başlamalı. Planlı bekleme 10 saniyelik hareketsizlik kurtarmasına girmemeli; son turdan sonra ek bekleme olmamalı.
6. Telefon/tablet gezinmesi, hesap senkronizasyonu, Geri takip et/Beklemede, boş onaylı kaynakta ikinci/üçüncü takipçiye geçiş ve iç içe yorumdan dönüş için aşağıdaki önceki kabul senaryoları ayrıca doğrulanmalı. Cihaz/X sürümü ve Hata Raporu ZIP'i sonuçla birlikte saklanmalı.

## 26.56 dar kapsam kabulü

1. Ayarlar sürümü26.56 olmalı. Onaylı listede Geri Takip Et kişisi seçilip Following/Requested sonucu doğrulanmalı; önceden Following/Requested tekrar seçilmemeli.
2. @NeerajC111 örneğinde eylemden sonra hâlâ Follow/FollowBack varsa7 saniye ve1 saniye sürekli görünür durumdan sonra FOLLOW_NO_EFFECT; sayaç artışı yok, sıradaki uygun kişi. Kayıp/çelişkili satır başarı/no-effect sayılmamalı ve tekrar takip tıklanmamalı.
3. Son hesap6/10 veya7/10 iken sonuç okunamazsa10 saniye ilerlemesizlikte STALL_RECOVERY, aynı pending/sayaç korunarak okuma yeniden başlasın. Tekrarlanan kurtarma üç kezden sonra otomatik PAUSED yapmamalı. İlgili düğme daha sonra Following/Requested olunca tek kez sayılsın.
4. Kullanıcı Durdur/Duraklat ve planlı döngü beklemesinde otomatik yeniden başlama olmamalı. Rate-limit mesajı veya üç gerçek revert koruması korunmalı.
5. Hesap seçici açılışındaki ara PROFILE/FOLLOWERS_LIST, beklenen switcher görülmeden kapatılmamalı. Toplu kuyruk ve final Atmaca dönüşü; önceki tablet/7× ayarları tekrar kontrol edilmeli.

Bu yeni APK ile fiziksel test yapılmadı.

## 26.55 telefon/tablet ve güncellik kabulü

1. Ayarlar sürümü 26.55-adaptive-navigation olmalı; eski 26.54/26.53 APK ile karıştırılmamalı. X sürümü, cihaz modeli, yön ve ekran ölçeğini loga ekle.
2. Görseldeki sol çubuk, sağ çubuk, telefon dip çubuğu ve yatay/split pencere: yorumcu/retweetçi hedef arama doğru Arama ikonunu açmalı; profil büyüteci yanlış global arama sayılmamalı.
3. Tablet içerik kaydırması sol gezinme çubuğuna değil akışa denk gelmeli. Resim/GIF/video kaydırmayı tüketirse ikinci içerik şeridi denenmeli; hedef profilden gereksiz çıkış olmamalı.
4. Dar tablet drawer panelinde hesabı tara/değiştir: arka plana yanlış dokunma olmadan seçici; ilk açık hesabın seçicisinden çıkış; tam kimlik/sayaç ve final Atmaca dönüşü. Beş hesapta ikinci iş10/15 sonrası kaynak zinciri devamı.
5. En hızlı · 7× Kaydet, yeniden başlat; önceki yorum/no-follow/Beklemede/kuyruk ve pending işlem korumalarını tekrar dene. Belirsiz ikon veya çoklu adayda başka kontrole tahmini dokunma olmamalı.

Fiziksel test henüz yapılmadı; görsel düzeni için kullanılan regresyon düğümleri sentetiktir.

## 26.54 kabul senaryoları

1. X ilk açık hesabını değiştirmeden hesap taraması: seçici kendiliğinden kapanmalı, beş hesap ve sayaçları alınmalı, Atmaca dönüşü doğrulanmalı. Manuel müdahale gerekirse log ve Hata Raporu alınmalı.
2. Beş hesap / Onaylı takip / Limit15: ikinci kaynakta yalnız10 yeni kişi varsa 10/15 sonrası aynı listeden başka kullanıcı adına gitmeli; onun takipçileri/Onaylı sekmesinde kalan5 tamamlanmalı; sonraki hesap kimliği doğrulanmalı. Beklemede yeni sonuç sayılmalı; önceden takipli/no-button kişi yalnız kaynak olarak kullanılabilmeli.
3. Liste sonundaki sanal/off-screen satırlar: IllegalArgumentException motoru kalıcı PAUSED yapmamalı. Yeni SCROLL_RETRY/GESTURE_RETRY/SNAPSHOT_ERROR frame bilgileri ile toparlanma doğrulanmalı. Kesintisiz dört okuma hatasında ilerleme korunarak duraklama beklenir.
4. Tüm görev türlerinde ikinci hesap, 20 saniye hareketsizlik ve aralıklı UNKNOWN/SystemUI: kısa kökte hesap baştan seçilmemeli; yerinde yeni okuma yapılmalı. Pending takip/yorum/gönderi yeniden tıklanmamalı. Kullanıcı duraklatma ve ekran kilidinde otomatik devam olmamalı.
5. Ayarlar En hızlı · 7× → Kaydet:71/257/214 görünmeli; uygulama yeniden açıldığında kalmalı. Daha yavaş süre seçip Kaydet de sonraki açılışta korunmalı. X gerçek yüklenme süresi ayrı ölçülmeli.

Bu yeni APK ile fiziksel test henüz yapılmadı.

## 26.53 toplu geçiş / alt yorum / Beklemede kabul testi

1. İki hesap, Limit1/Tekrar1; sırayla Takipten çık, Onaylı takip, Yorumcu, Retweetçi ve link görevlerini ayrı çalıştır. İlk işin son X ekranında ACCOUNT_RETURN görülmeli; menü→seçici→ikinci kendi profilindeki tam @handle; sonra ikinci iş. Manuel Back/Atmaca müdahalesi gerekmeden devam etmeli. Tam kuyruk sonunda QUEUE_DONE ve başarılı QUEUE_RETURN; Atmaca öne gelmeli.
2. Hedef yorum listesinden altında kendi yanıtları olan bir yorum aç. Yalnız o yorumcunun başlığı işlenmeli; alt yanıt düğmeleri açılmamalı ve başarı sayılmamalı.
3. Üstte Mesaj gönder/Abone ol veya hiç Takip et olmayan yorum: COMMENT_SKIP_NO_FOLLOW, bir kontrollü dönüş, COMMENT_RETURN PARENT; aynı ana listedeki sonraki görünür metin yorumundan devam, sayaç artışı yok. Hedef profil zaten açılmışsa ekstra Back yok.
4. Onaylı listede bir yeni takip isteği Beklemede olsun: FOLLOW_REQUEST, bir sayaç artışı ve hemen sonraki kişi. Önceden Beklemede olan kişi tekrar işlenmemeli. Disabled Requested status, bir sonraki satırın düğmesi ve sekme adları karıştırılmamalı.
5. Aynı senaryolar farklı ekran ölçeklerinde ve ikinci hesapta. Ayarlar sürümünü, cihaz/X sürümünü ve Hata Raporu ZIP'ini sakla.

Yeni 26.53 APK ile bu fiziksel testler henüz yapılmadı. 1000033391.mp4 hata kanıtıdır; ham node dökümü değildir.

## 26.52 yorumcu/retweetçi kabul testi

1. Tek hedefte metin, resim, metin, GIF, metin yorum sırası. Metinler görünür sırayla takip edilmeli; yalnız medyalı satır atlanmalı. Avatar ve metinde geçen video/fotoğraf sözcüğü atlama nedeni olmamalı.
2. Açma dokunuşu ekranı değiştirmezse COMMENT_OPEN_RETRY; kişi sessizce atlanmamalı. Takip sonucu belirsizse PAUSED ve korunan sayaç.
3. Bir gönderi limiti dolduramazsa Gönderi→hedef profil; DISCOVERY_RETURN SCAN sonrasında fazladan BACK veya yeniden DISCOVERY_SEARCH olmamalı, sonraki uygun gönderiden devam etmeli.
4. Retweetçi için liste→Gönderi→hedef profil, iki ayrı doğrulanmış dönüş; hedef profilinden çıkış yok.
5. İki hesap arasında eski medya/retry/return state yeni hesaba taşınmamalı.

Yeni APK ile bu fiziksel testler henüz yapılmadı. 1000033389.mp4 önceki çalışmanın hata kanıtıdır.

## 26.51 Yorum Alıntısı kabul testi

1. Tek hesap, tek alıntı hedefi, Limit 1, Tekrar 1 ve kısa ayırt edici yorum.
2. Hedef gönderi açılır; yazar/metin doğru okunur. QUOTE_REPLY ile yorum alanı bulunur; metin yeni ekran okumada doğrulanır. QUOTE_REPLY_SUBMIT yalnız bir kez, ardından ilerleme 1/1.
3. İki alıntı hedefi, Limit 1: her hedefte bir yorum; toplam 2/2. İlk hedef bitti diye görev tamamlanmaz.
4. İki hesap seçili tek görev: ilk hesap hedeflerini bitirir, QUEUE_HANDOFF ile ikinci hesaba geçer. Yalnız finalde QUEUE_DONE ve Atmaca dönüşü.
5. Ayrı yorum oluşturucu ve gönderi içindeki Yanıtını gönder alanını ayrı kontrol et. Hedefte metin genişleyip detay açılmazsa aynı yeni gönderi sınırlı tekrar edilir.
6. Metin yazılırken veya gönderim doğrulanırken duraklat/durdur. Doğrulanmamış gönderim otomatik tekrar edilmez. Tekrar döngüsünde aynı doğrulanmış gönderi yeniden yorumlanmaz.
7. Eksik hedef/gönderi veya yanıtsız gönderim: açık PAUSED nedeni ve korunmuş ilerleme; sahte COMPLETED yok.

Bu fiziksel testler bu oturumda henüz yapılmadı. Derleme/birim test başarısı bunların geçtiği anlamına gelmez.

# Checkpoint 10 — X Gerçek Cihaz Test Protokolü

Bu protokol gerçek APK üretildiğinde selector sertleştirmesini tahminle değil, kaydedilmiş accessibility kanıtıyla yapmak içindir.

## Test öncesi
1. X uygulamasında test edilecek bütün hesapların oturumları açık olmalı.
2. Atmaca Accessibility servisi açık olmalı ve Atmaca ekranında servis bağlantısı aktif görünmeli.
3. X dili kaydedilmeli (TR veya EN).
4. Cihaz modeli, Android sürümü ve X uygulama sürümü hata raporuyla birlikte saklanmalı.
5. İlk turda düşük görev limiti kullanılarak ekran geçişleri doğrulanmalı; daha sonra 35 limite çıkılmalı.

## Senaryo A — hesap doğrulama/değiştirme
- X Home açılır.
- Drawer açılır.
- Aktif profile girilir ve @handle okunur.
- Hedef farklı hesapsa account switcher açılır.
- Hedef satır yalnızca exact @handle ile seçilir.
- Switcher kapandıktan sonra HOME/PROFILE/DRAWER geçişi görülür.
- Stabilizasyon sonrası profil yeniden açılır ve yeni @handle kesin doğrulanır.
- Yanlış @handle görülürse hiçbir Follow/Unfollow aksiyonu yapılmamalıdır.

## Senaryo B — onaylı kullanıcı takibi
- Kendi profilindeki Followers sayacı açılır.
- Listenin en başına dönüldüğü iki stabil kontrolle doğrulanır.
- En yeni işlenmemiş takipçinin @handle satırı açılır.
- Kaynak profilinde Followers sayacı açılır.
- `Onaylı Takipçiler / Doğrulanmış Takipçiler / Verified Followers` sekmesi bulunur.
- Follow/Follow back/Sen de takip et hedefi aynı satırda tek bir @handle ile eşleştirilir.
- Click sonrası aynı @handle satırı yeniden bulunur ve durum `Following/Takip ediliyor` olmadan sayaç artırılmaz.

## Senaryo C — takipten çıkma
- Kendi profilindeki Following/Takip edilen sayacı açılır.
- En az 100 farklı @handle görülene kadar veya gerçek liste sonu iki stabil scroll ile kanıtlanana kadar aşağı inilmelidir.
- `Following/Takip ediliyor/Takip ediyor` düğmesi tek @handle içeren satırda bulunur.
- Unfollow onay penceresinde yalnızca exact `Unfollow/Takipten çık/Takibi bırak` kabul edilir.
- Aynı @handle satırı `Follow/Takip et` durumuna dönmeden sayaç artırılmaz.

## Senaryo D — yanlış tıklama engelleri
Aşağıdaki profiller ayrı ayrı denenmelidir:
- Uzun bio + Daha fazla
- Doğum tarihi bulunan profil
- Çeviri bağlantısı bulunan bio
- Gizli hesap
- Follow request/pending durumundaki hesap

Beklenti: Atmaca bu alanları işlem hedefi olarak asla seçmemeli.

## Senaryo E — popup ve ağ hataları
- İnternet kısa süre kesilir.
- X retry ekranı oluşursa yalnızca exact Retry/Tekrar dene kullanılmalı.
- Rate-limit ekranında görev cooldown'a girmeli.
- Bilinmeyen dialogda riskli affirmative butona basılmamalı; gerekirse PAUSED olmalı.

## Senaryo F — 10 hesap kuyruğu
- En az 10 X hesabı kuyrukta sıralanır.
- 1. hesap bittiğinde runtime transient state temizlenir.
- 2. hesaba switch edilir ve @handle yeniden doğrulanır.
- Bir hesap PAUSED/FAILED olduğunda uygulamanın process'i kapanmamalıdır.
- Politika izin veriyorsa problemli hesap atlanıp sonraki hesaba geçilir.

## Her hata sonrası alınacak çıktı
Atmaca -> Hata Raporu ZIP Oluştur.
Özellikle şu dosyalar incelenir:
- runtime.txt
- accessibility.txt
- automation_logs.csv
- x_accessibility_probe.txt
- x_accessibility_probe_previous.txt

Probe'da kullanıcı içeriği redacted ve @handle değerleri hashlenmiş olmalıdır.
