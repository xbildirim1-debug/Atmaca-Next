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
