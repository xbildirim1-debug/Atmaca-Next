# 26.1 doğrulama durumu — 6 Eylül 2026

## Gerçekleştirilen kontroller

- Kullanıcının gönderdiği kaynak paket incelendi; hesap kontrolörü, ekran seçicileri, Room kayıtları ve dönüş yolu gözden geçirildi.
- XML dosyaları standart XML ayrıştırıcısıyla açıldı; biçim hatası bulunmadı.
- GitHub Actions YAML yapısı ayrıştırıldı; push / pull_request / workflow_dispatch ve test/lint/build adımları mevcut.
- Kaynaktaki yerel import adayları kontrol edildi; çözümlenemeyen paket/sınıf adayı bulunmadı. Bu kontrol Kotlin derlemesi değildir.
- Manifestte INTERNET izni yok. Şifre düğümleri ortak erişilebilirlik taramasında atlanır. Boşta X ağacı işlenmez.
- `git diff --check` başarılı.

## Gerçekleştirilemeyen kontroller

- Gradle bulunmadı (`gradle --version`: komut bulunamadı); Android SDK/Kotlin derleyicisi bulunamadı. Araç zinciri indirme denemesi ağ zaman aşımına uğradı.
- Kotlin/Compose derlemesi, eklenen 6 sayaç testi, mevcut testler ve Android lint çalıştırılmadı.
- GitHub README oluşturma denemesi HTTP 403 `Resource not accessible by integration` döndürdü. Depoya yazılmadı, CI başlatılmadı.
- Yeni APK üretilmedi. Kaynak ZIP'e gönderilen eski APK eklenmedi; eski ikili yeni sürüm olarak sunulmaz.
- Arayüz cihaz/emülatörde render edilmedi. X'le uçtan uca doğrulama yapılmadı.

## Cihazda kabul akışı

1. Erişilebilirlik kapalı: izin yönlendirmesi gösterilmeli.
2. Tek oturum: iki sayaç da okunmalı, doğru kullanıcı adı bir kez kaydedilmeli, Atmaca'ya dönülmeli.
3. İki ve on oturum: her oturum kaydedilmeli; kaydırılan seçicide yukarı/aşağı arama denenmeli.
4. Yeniden tarama: aynı kullanıcı adları çoğalmamalı; sayılar güncellenmeli.
5. Hesaba geç: X'te hedef kendi profilinin kimliği doğrulanmalı; yanlış/eksik kimlik başarı sayılmamalı.
6. Sayaç eksik/geç yüklendi: beklenmeli veya açıklayıcı hata verilmeli; eski sayı yeni okunmuş gibi kullanılmamalı.
7. Durdur/ekran kilidi/hizmet kopması: sonradan gelen kayıt callback'i X gezinmesini yeniden başlatmamalı.
8. İşlem sonunda Atmaca ön plana gelmeli. Android 14+ için X sürecinin kapandığı iddia edilmemeli.
9. Küçük ekran ve büyük yazı boyutunda Hesaplar / Görevler / Ayarlar kontrol edilmeli.

## Sonraki adım

GitHub bağlantısına New deposunda dosya yazma erişimi sağlandıktan sonra kaynaklar yüklenip Actions çalıştırılmalı. İlk başarılı derlemeden sonra test APK'si gerçek telefonda bu akışla denenmeli.
