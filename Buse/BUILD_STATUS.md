# Buse 1.1 — yalnız hız, doğrulanmış yerel derleme

- testDebugUnitTest, lintDebug ve assembleDebug başarılı. 524 test, 0 başarısız/hata/atlanan; lint 0 hata/fatal, 15 uyarı, 1 öneri.
- APK: com.buse.mobile / versionCode 2 / versionName 1.1-fast10. Buse 1.0 ile aynı imza; üzerine güncelleme kurulabilir. Uygulama kimliği, Room veritabanı ve görev biçimi aynı.
- Yalnız süreler değişti: kaydırma 220→22 ms, kaydırma sonrası bekleme 250→25 ms, satır tekrar okuması 200→20 ms; işlem/hesap/görev arası süreler 71/257/214→7/25/21 ms. Genel gecikmeler /70, ekran okuma alt sınırı 16 ms. Liste başı/sonu 1,5 saniye kanıtı ve hata/rate-limit süreleri korunur.
- İlk 200 kişi, kendi satırında takip etiketi, iki güncel okuma, onay ve sonuç kuralları aynı. Uygulama, hesaplar ve görevler aynı.
- SHA256: 365d9ec23242972fa3e6b4619ac2d65d764f8ac157cd2f2d149a563270190121
- Sertifika SHA256: 9bb1488d50538b0b69427f7b49f5cc809b6b715eeb81a1867e33fe930074b7a5
- Fiziksel X testi yapılmadı; 10× süre ayarı gerçek toplam işlem süresine veya X yüklenmesine 10× garanti değildir.
- Atmaca değiştirilmedi; kaynak ayrı buse/1.1-fast10 dalının Buse/ klasöründedir. Raporlar validation/1.1-local/ içindedir.
