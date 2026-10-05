# Buse 1.0 cihaz kabulü

Yerel test/lint/APK derlemesi, gerçek telefonda X ekranı kontrolünün yerine geçmez. Buse 1.0 bu çalışma sırasında fiziksel telefonda denenmedi.

1. Atmaca yüklüyken Buse'yi kur. İki ayrı simge ve paket olduğunu, Atmaca hesap/görev kayıtlarının korunduğunu kontrol et.
2. Buse erişilebilirlik hizmetini aç. Hesap Ekle ile X'teki hesapları içe aktar; hesap değiştirmede doğru @kullanıcı adını doğrula.
3. Küçük limitli normal Takipten Çıkma görevini başlat; X onayı ve Takip et durumundan sonra sayaç artmalı. Duraklat/Durdur ve ekran kilidi otomatik işlemleri durdurmalı.
4. 100'den az takip edilen bulunan hesapta Takip Etmeyenleri Çıkma çalıştır. Sonuç sıfır işlem olmalı.
5. 100'den fazla kişide liste başından ilk 100 kullanıcıyı işlem öncesi kaydet. 100. ve 101. kişi aynı ekrandayken 100. kişi korunmalı; yalnız 101 ve sonrasında etiketi olmayan tam satırlar aday olmalı.
6. Verilen örneğe benzer ardışık satırlarda Seni takip ediyor yazan kişi kalmalı; komşu satırın etiketi başka kişiye taşınmamalı. Üstten kırpılan etiket yok kabul edilmemeli.
7. Listenin alt ucunda yeni kişilere geri dönülmemeli. Talep edilen limitten az aday varsa yalnız gerçekleşen sayı yazılmalı.
8. Sonuç bekleme, X yüklenmesi, onay açılmaması ve hesabın değişmesi sırasında ek kişiye yanlış işlem gönderilmemeli.
9. Birden fazla hesap / iki turda ilk tur tüm hesaplarda sırasıyla bitmeli; aradan sonra ikinci tur aynı sırayla başlamalı.
10. En hızlı 7× işlem ayarında ekran yenilemeleri doğru okunmalı; gerçek hız ayrıca gözlemlenmeli.

Gerçek X erişilebilirlik ağacı bu çalışmada alınmadı. Kişi satırı gruplanmasını gizleyen farklı X sürümünde Buse belirsiz satıra işlem yapmaz; bu durumda o telefondaki güncel ekran ağacı ile selector doğrulaması gerekir.

## 1.2 hızlı kaydırma kabulü

- İlk 100 kullanıcı sınırı oluşurken her kısa kaydırmanın sonunda liste sabitlenmeli; atlanan kullanıcı veya kişi sırası kopması nedeniyle erken duraklama olmamalı.
- Kullanıcının 1000033462.mp4 videosundaki 125–130 saniye akışını aynı hesapta yeniden dene. İlk 100 kişi sayılmalı; 101 ve sonrası uygun satırlar işlenmeli.
- Kayıtta ara ekranlar hareket ederken hiçbir ilişki düğmesine basılmadığını ve iki komşu penceredeki ortak kişilerin bir kez sayıldığını doğrula.
- Bu adımlar yeni APK ile henüz fiziksel cihazda uygulanmadı.
