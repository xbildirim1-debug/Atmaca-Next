# Buse 1.2 — 100 kişi ve kararlı hızlı kaydırma

- Temiz yerel testDebugUnitTest, lintDebug ve assembleDebug başarılı. 532 test, 0 başarısız/hata/atlanan; lint 0 hata/fatal, 12 uyarı, 1 öneri.
- İlk 100 farklı kişi korunur; 101 ve sonrasındaki uygun satırlar işlenir. UI, erişilebilirlik açıklaması, sınır testleri ve cihaz protokolü güncellendi.
- Kaydırma 22 ms hareket + aynı temasın devamıyla 120 ms sabit tutup bırakma kullanır. Bırakma tamamlanana kadar iş parçacığı yeni harekete geçmez. Sonrasında güncel kişi/konum ağacı 32 ms arayla okunur; 64 ms kararlı geometri olmadan sayım, yeni kaydırma veya satıra basma yapılmaz. 2 saniyede oturmayan ekran belirsiz sayılır.
- 8 yeni regresyon; hareketli ve eksik ara ekranlar, tekrar kaydırma, 100 kişilik örtüşen pencere sayımı ve geçici ayrık ekranın erken duraklatmaması doğrulandı. Mevcut 524 test korundu; sınır beklentileri 100/101 güncellendi.
- APK: com.buse.mobile / versionCode 3 / versionName 1.2-fast100. Önceki Buse sürümleriyle aynı imza; üzerine güncelleme kurulabilir. Hesap/görev veritabanı ve kayıt biçimi aynı.
- Kullanıcının 1000033462.mp4 videosu incelendi: 125–129 saniyede liste açılıp birkaç hızlı hareket görülüyor, 130–145 saniyede görünüm çoğunlukla sabit kalıyor; 146'da başka görünür kişiler var. Video hata mesajı veya erişilebilirlik günlüğü içermiyor; kesin cihaz hata nedeni kanıtlanmış sayılmaz. Kodda hareket tamamlanınca liste kararlılığı beklenmeden yeniden kaydırma ve ara ekranla sıra kesilmesi duraklaması bulunuyordu.
- Yeni APK ile fiziksel Android/X kabul testi yapılmadı; geçmiş video yeni sürümün kabul testi değildir.
- APK SHA256: a86500b49ec3f979e70be8bfda1f4f6c5628688ea31c8cc0e3460ee8afc20977
- Sertifika SHA256: 9bb1488d50538b0b69427f7b49f5cc809b6b715eeb81a1867e33fe930074b7a5
- Atmaca değiştirilmedi; kaynak ayrı buse/1.2-fast100 dalının Buse/ klasöründedir. Raporlar validation/1.2-local/ içindedir.
