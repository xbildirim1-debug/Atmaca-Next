# Atmaca Next 26.1 — derleme doğrulaması

- Depo: https://github.com/xbildirim1-debug/Atmaca-Next
- Başarılı çalışma: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34043164712
- APK kaynak commit'i: 92c61c901028e9ad2edd062081244a321239a9d2
- Tarih: 6 Eylül 2026

## Tamamlanan kontroller

- Kotlin/Compose derlemesi başarılı.
- JUnit raporu: 67 test, 0 hata, 0 başarısız, 0 atlanan.
- Android lint başarılı. Manifestte yalnız kaldırma amacıyla bulunan InitializationProvider kaydının MissingClass uyarısı, bu kayıt özelinde açıklanmıştır. CI, nihai manifestte bu bileşenin gerçekten bulunmadığını ayrıca doğrular.
- Debug APK oluşturuldu; apksigner doğrulaması başarılı.
- Nihai manifestte INTERNET izni yok.
- İndirilen ZIP bütünlüğü ve APK SHA-256 değeri doğrulandı.
- Sürüm adı: 26.1-accounts-preview; sürüm kodu: 49.
- Paket kimliği: com.atmacanext.v258.
- compileSdk 37.1; targetSdk 36; minSdk 26.
- APK boyutu: 20.459.224 bayt.
- SHA-256: c7b6aeae7174bdcc6cd86b3f5db5c34220e2fe24c82fd3a3906be4af1501cc1c

## İlk CI çalışmaları sırasında giderilenler

1. AndroidX kütüphanelerinin SDK gereksinimi için 37.1 platformu ve minor API DSL kullanıldı.
2. Eski kuyruk testi, mevcut görev türü sırasını bekleyecek şekilde güncellendi. Kuyruk motorunun davranışı değiştirilmedi; hesap sırası ve filtreleme doğrulamaları korundu.
3. Manifest kaldırma kaydının lint uyarısı yalnız bu kayıt için ele alındı; nihai paket kontrolü eklendi.

## Henüz doğrulanmayanlar

Fiziksel Android/X oturumuna veya emülatöre erişim olmadığından arayüz render kontrolü, kurulum/açılış testi ve X ile uçtan uca test yapılmadı. Derleme başarısı, cihazdaki X sürümüyle tam uyumluluk garantisi değildir.

Telefon testi: tek/iki/on hesap, kaydırılan hesap seçici, tekrar tarama, iki sayaç, eksik/yanlış kimlik, durdurma, ekran kilidi, hizmet kopması ve Atmaca'ya dönüş. Android 14+ üzerinde X arka planda kalabilir; zorla kapandığı iddia edilmez.

APK test amaçlı debug imzalıdır. Önceki sürümün sertifikası farklıysa üzerine kurulamayabilir. Kalıcı güncellemeler aynı imzalama anahtarını gerektirir.
