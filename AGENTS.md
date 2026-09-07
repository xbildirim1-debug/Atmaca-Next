# Atmaca Next çalışma devamlılığı

Bu depoda çalışmaya başlamadan önce `DEVIR_NOTU.md` ve `README.md` dosyalarını oku. Kullanıcı sohbet dolduğunda başka ChatGPT hesabına geçer; güncel kaynak, değişiklikler ve durum yalnız sohbet içinde kalmamalıdır.

- İş sonunda yapılan değişiklikleri, gerçek test sonuçlarını, APK kaynağını ve açık sorunları DEVIR_NOTU.md içinde güncelle.
- Kaynak kodu/testleri GitHub'da sakla; ana dal ve çalışma dalının durumunu açık belirt. Mevcut kullanıcı değişikliklerini ezme.
- APK derlemesi/birim testleri ile gerçek X cihaz testi farklıdır. Yapılmayan testi yapılmış gibi yazma.
- İmza değişikliği ve veri kaybı riski varsa açık belirt; özel anahtar veya kimlik bilgilerini depoya ekleme.
- Eski sürüm notlarını güncel davranışla karıştırma; önce güncel kaynak ve kullanıcı kanıtını incele.

## Her yeni sürüm için zorunlu teslim

Kullanıcı 7 Eylül 2026 tarihinde tüm değişikliklerin, ayrıntılı not defterinin ve her yeni APK ile ZIP paketinin GitHub'a eklenmesini istedi. Her sürümde DEVIR_NOTU.md güncelle; değişen dosyaları, gerekçeleri, testleri, cihaz doğrulamasını, kalan sorunları, kaynak commit ve imza durumunu yaz. Kaynak ve testleri main'e kaydet. Başarılı main APK derlemesinden sonra APK ve tam paket ZIP, NOT_DEFTERI.txt ve SHA256SUMS.txt GitHub Releases'e eklenmelidir. Otomatik yayın taslağı docs/archive-release.proposed.yml içinde hazırlanmıştır; henüz etkin değildir. Otomatik onay incelemesi contents:write yetkili iş akışının etkinleştirilmesini reddetti; kullanıcıdan bu somut kapsam için onay alınmadan etkinleştirme. Tam paket kaynak ZIP, APK, test raporları ve notları içerir. Yayının gerçekten tamamlandığını ve varlıkları kontrol et, kullanıcıya doğrudan GitHub indirme bağlantıları ver. Yalnız süreli Actions artifact bırakmak teslim sayılmaz. Eski sürümleri silme veya yayınlanmış APK'yı başka imzalı derlemeyle değiştirme; her build kendi etiketiyle saklanır. Kalıcı imza sorunu çözülene kadar bu riski belirt.
