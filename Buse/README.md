# Buse 1.1 — yalnız hız

Pembe ağırlıklı, bağımsız Android uygulaması. Yalnız **Hesaplar** ve **Görevler** ekranları vardır.

- **Takipten Çıkma:** Atmaca 26.59'un hesap doğrulama, onay ve sonuç doğrulama akışını kullanır.
- **Takip Etmeyenleri Çıkma:** Kendi hesabının Takip ediliyor listesini baştan açar. İlk 200 farklı kullanıcı korunur. 201. kişiden daha aşağıya doğru devam eder. Kişinin kendi satırında “Seni takip ediyor” / “Follows you” varsa bırakır. Etiketi olmayan, tam ve aynı kimlikle iki kez okunmuş satırlardaki kişiler çıkarılır.
- 200 kişi, kaydırma sayısı değildir. Kayan ekranlar örtüşür; aynı kullanıcı iki kez sayılmaz. Sıra kesilirse görev duraklar. Liste 200 kişiden kısaysa hiç kimse çıkarılmaz. Liste sonunda yeni kişilere dönülmez. Eksik/kırpılmış/belirsiz satırlar işlenmez.
- Buse 1.0’a göre **10× hızlı süre ayarı** varsayılandır. Kaydırma 220→22 ms, kaydırma sonrası bekleme 250→25 ms ve satır tekrar okuması 200→20 ms. X ağ ve ekran yükleme süresine hız garantisi verilmez; kişi/onay/sonuç, 200 kişi sınırı ve liste ucu kanıtları korunur.
- En fazla 10 X hesabı. Her tur için 1–35 kişi; tur sayısı ve turlar arası bekleme seçilebilir. Her iki görevde X’in işlem sınırları ve uygulamanın deneme/sonuç doğrulama koruması geçerlidir.

## Bağımsızlık

| Alan | Buse | Atmaca Next |
| --- | --- | --- |
| Uygulama kimliği | `com.buse.mobile` | `com.atmacanext.v258` |
| Kod alanı | `com.buse.app` | `com.atmacanext.app` |
| Veritabanı | `buse_next.db` | `atmaca_next.db` |
| Ayarlar | `buse_settings` | `atmaca_settings` |
| Erişilebilirlik hizmeti | `BuseAccessibilityService` | `AtmacaAccessibilityService` |

Buse, Atmaca'nın üzerine kurulmaz. Atmaca'nın hesapları ve kayıtları Buse'ye taşınmaz. Atmaca kaynağı `8b467a3baaa991abaa3202dca78ff9299ad1c2c0` temel alınarak ayrı projeye kopyalanmıştır; mevcut Atmaca projesi değiştirilmeyecektir.

## Kullanım

1. Buse APK'sını Atmaca'yı kaldırmadan kur.
2. X'te kullanacağın hesaplara giriş yap.
3. Buse → Hesaplar → Hesap Ekle. Android erişilebilirlik ekranında **Buse** hizmetini etkinleştir. Buse'ye dönüp Hesap Ekle ile X hesaplarını tara.
4. Görevler'den iki görevden birini seç, hesabı/hesapları ve limiti belirle; Kaydet → Başlat.
5. Ekran açık ve kilitsiz kalmalı. Aynı X ekranını paylaşan Atmaca ve Buse görevlerini aynı anda çalıştırma.

## Derleme

JDK 17, Android SDK platform 37.1, build-tools 36.0.0 ve Gradle 9.5.0 kullanılır. SDK yolu `ANDROID_HOME` veya `local.properties` ile ayarlanır.

```sh
./gradlew testDebugUnitTest lintDebug :app:assembleDebug
```

İlk derlemede internet ve ilgili SDK/Gradle bağımlılıkları gerekir. APK `app/build/outputs/apk/debug/app-debug.apk` altında oluşur. İmzalama anahtarı kaynak paketine dahil değildir.

Ortak motorun kullanılmayan eski görevleri kaynakta korunmuştur; Buse arayüzü, görev kaydı ve çalıştırma kuyruğu yalnız iki çıkış türünü kabul eder. API veya INTERNET izni yoktur. Cihaz/X kabul adımları `DEVICE_TEST_PROTOCOL.md` içindedir. Gerçek yerel test sonucu `BUILD_STATUS.md` ve paket notlarında bulunur.
