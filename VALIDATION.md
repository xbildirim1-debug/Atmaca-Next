# Atmaca Next V25.12.17 — Doğrulama Notu

## Düzeltilen son iki sorun

- Tek hesap Limit 5 tamamlandıktan sonra X'te kalma: terminaldeki `launchXHome()` çağrısı kaldırıldı; Atmaca doğrudan öne alınıp X arka plan işlemi kapatılıyor.
- İki hesapta ilk hesabın sonunda kalma: hesap tamamlanma olayı Atmaca dönüşüne ve 3 saniyelik seri kuyruk geçişine bağlandı. Sonraki hesap X'i yeniden açarak başlıyor.

## Paket doğrulaması

- Paket: `com.atmacanext.v258`
- Sürüm kodu: `48`
- Sürüm adı: `25.12.17-queue-handoff-close-x`
- Uygulama etiketi: `Atmaca Next V25.12.17`
- Ana Activity: `com.atmacanext.app.MainActivity`
- APK boyutu: `17.905.257` bayt
- SHA-256: `f671818cbe6d4b6f9376451083658aeda6538a2fa21041abaf1f0a54b1df1712`
- APK imzası: v2 ve v3 geçerli; hata `0`, uyarı `0`
- İmza sertifikası SHA-1: `34F3F38CD4714BE915A1D75FCCFA0559796BFA60`
- İmza sertifikası SHA-256: `CCB5AB4624C2C3A53CD10A8DB3B3DA7723E50E171149CCFABB7558481451E6FC`
- DEX dosyası: `16`
- Sınıf: `29.908`
- Tanımlı metot: `146.349`
- Yinelenen sınıf: `0`

## Paket içi akış denetimi

- İmzalı APK yeniden çözümlendi; sürüm 48 ve `KILL_BACKGROUND_PROCESSES` izni doğrulandı.
- `AtmacaAccessibilityService.closeXBackground()` ve `ActivityManager.killBackgroundProcesses("com.twitter.android")` çağrısı imzalı DEX içinde doğrulandı.
- Atmaca'ya doğrudan intent yolu ve üretici ROM'u için yedek dönüş yolları imzalı DEX içinde doğrulandı.
- Günlük limit tamamlama, normal görev tamamlama, döngü arası bekleme ve hata yollarında X ana sayfası yerine Atmaca dönüşü doğrulandı.
- Kuyruk yöneticisinin başarılı ve başarısız hesap sonlarında `returnToAtmaca()` çağırdığı doğrulandı.
- Başarılı ilk hesaptan sonraki kuyruk ilerleme gecikmesi `3000 ms` olarak doğrulandı.
- ZIP/DEX yapısı yeniden okunabildi; gerekli servis, çalışma motoru, kuyruk yöneticisi ve ana Activity sınıfları bulundu.

## Gerçek cihaz deneme sırası

1. Tek hesap, Limit 5 ile başlayın. Beş doğrulanmış işlemden sonra Atmaca Next öne gelmeli ve X kapanmalıdır.
2. İki hesap, her hesap Limit 1 veya 2 ile başlayın. İlk hesap bitince kısa süre Atmaca görünmeli; ardından X yeniden açılıp ikinci seçili hesap doğrulanmalıdır.
3. İkinci hesap tamamlanınca X kapanmalı ve uygulama Atmaca Next'te kalmalıdır.
4. Loglarda birinci hesap için `QUEUE_TASK_DONE`, ardından ikinci hesabın `VERIFY_ACCOUNT` başlangıcı ve finalde `QUEUE_DONE`/Atmaca dönüşü aranmalıdır.

## Ortam sınırı

APK derleme, imza, manifest, DEX bütünlüğü ve statik akış denetimlerinden geçti. Fiziksel Android/X oturumu bulunmadığından son dokunmatik arayüz doğrulaması gerçek cihazda yapılmalıdır.
