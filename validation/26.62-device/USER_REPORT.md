# 26.62 — kullanıcı aynı takılmayı bildirdi

7 Ekim 2026, 08:46:54 Europe/Istanbul. Son verilen APK AtmacaNext-26.62-Yorum-Duzeltmesi.apk / versionCode 110 / 26.62-inline-reply-input. Kullanıcı: “Yorum alıntısının kodlarını yaz bana hata var aynı yerde kaldı”. Bu mesajda yeni runtime logu, cihaz ağacı veya sürüm ekranı yok; rapor son verilen sürüm bağlamında kaydedildi. Kullanıcının sonucu başarısızdır; Yorum Alıntısı cihazda çözüldü diye değerlendirilmez.

Önceki 643 test başarısı ve APK imza/paket kontrolleri geçerli tarihsel otomatik kontrollerdir; bu gerçek X akışının başarı kanıtı değildir. Önceki “fiziksel telefonda henüz denenmedi” teslim notu, teslim anındaki durumu anlatır. Güncel kullanıcı sonucu takılmanın sürmesidir.

Kaynak incelemesinde açık: ReplyInputConnection.replaceAll() komutları çağırdıktan sonra true döner. ReplyTextTransfer.write() her denemede önce bu bağlantıyı dener; true dönen ama metni değiştirmeyen bağlantı bütün denemeleri INPUT_CONNECTION yolunda tüketir ve SET_TEXT/PASTE satırlarına ulaşmaz. Metin doğrulaması yoksa FILL_COMPOSER gönderime geçmez; üç yazma denemesi sonrası WAIT ve 10 saniye dolunca RESTART kararı oluşur. Bu koşullu davranış koddan doğrulanabilir. Kullanıcının takılmasının bu noktada mı, önceki hedef/kutu tanıma aşamasında mı olduğu yeni log olmadan kesinleştirilemez.

Mevcut 21 metin giriş testi etkisiz SET_TEXT kabulünü kapsar; native bağlantının art arda true döndürüp metni değiştirmediği durumda alternatif yönteme geçişi kapsamaz. Bu eksik burada giderilmedi. Bu tur kullanıcının istediği mevcut kod dökümü çıkarıldı; uygulama kaynakları, sürüm ve APK değişmedi. Test çalıştırılmadı.

APK uygulama kaynağı f26d76c1c8676e2613fe8326d9d3442652f7cf01 / tree 10dac51fb14714a251e7268388dce3085fe0610f. Kod dökümü için kullanılan 14 kaynak dosyanın Git blob SHA'sı bu ağaçla eşleşti. Ana dal başlangıcı e74dc59a19f8805232cb926a23de9d60fed1ba0d. fix/26.62-inline-reply dalı 937ff8429112717e49a6ef42ea9466f41006fd5d, PR #11 birleşmiş durumda. Bu kayıt ve kod dökümü main'e yalnız belge olarak eklenir. Buse değişmedi.
