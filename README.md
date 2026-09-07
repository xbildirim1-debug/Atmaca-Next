# Atmaca Next — 26.2

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

JDK 17, Gradle 9.5.0 ve Android SDK 37.1 gerektirir. Depodaki `android-build.yml` test, lint, APK ve imza kontrolünü çalıştırır. Başarılı çalıştırmanın **AtmacaNext-26.2-APK** çıktısını indirin.

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
