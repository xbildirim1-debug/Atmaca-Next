# Buse çalışma devamlılığı

Önce README.md, DEVIR_NOTU.md ve BUILD_STATUS.md dosyalarını oku.

- Buse bağımsız Android uygulamasıdır. Atmaca-Next main ve mevcut Atmaca dosyaları değiştirilmez. Kaynaklar Atmaca-Next deposundaki ayrı `buse/1.1-fast10` dalının `Buse/` klasöründedir.
- Uygulama kimliği `com.buse.mobile`, kod alanı `com.buse.app`, pembe arayüz ve yalnız Hesaplar/Görevler gezinmesi korunur.
- Yalnız Takipten Çıkma ve Takip Etmeyenleri Çıkma oluşturulabilir. Diğer ortak motor görevleri Buse kuyruğuna alınmaz.
- Takip Etmeyenleri Çıkma: ilk 200 farklı kişi korunur, 201'den aşağıya devam edilir. Yukarı dönülmez. Aynı kişiye ait Seni takip ediyor/Follows you etiketi korunur; komşu veya kırpılmış satırdan yokluk sonucu çıkarılmaz.
- Buse 1.0’a göre 10× hızlı süre ayarı varsayılandır; X yüklenme süresi hız garantisi değildir. Onay ve sonuç kontrollerini kaldırarak hızlandırma.
- Gerçek Android/X testini JUnit, lint veya APK derlemesiyle karıştırma. Yapılmamış cihaz testini yapılmış gösterme.
- Yeni sürümde APK, tam kaynak/test/not paketi ve gerçek sonuçları sakla; DEVIR_NOTU ve BUILD_STATUS güncel olmalı. İmza anahtarını GitHub'a veya kaynak/tam paket ZIP'e ekleme.
- Cihazda belirsiz satır görülürse o cihazın güncel ekran ağacını incele; belirsiz etiket yokluğundan takipten çıkma üretme.
