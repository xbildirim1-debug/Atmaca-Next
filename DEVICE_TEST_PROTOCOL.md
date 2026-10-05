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
