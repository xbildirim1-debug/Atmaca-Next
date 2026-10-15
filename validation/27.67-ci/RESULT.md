# 27.67 gerçek doğrulama

27.67 TESLİM VE GERÇEK DOĞRULAMA — 15 Ekim 2026
Kaynak/testler: main ve feature/27.67-engagement-pilot.
APK uygulama kaynak commit: a64ed332febffd333705fe031d6d1c0f1113f97d.
Başarılı main CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/38688139343
817 test geçti (47 yeni regresyon); 0 başarısız/hata/atlanan.
Lint: 0 Error/Fatal, 56 Warning, 8 Hint. APK ve CI paket kontrolü geçti.
Son APK asıl b52d8a15… anahtarıyla imzalandı; test edilen CI APK'sının
imza dışındaki tüm girdileriyle byte olarak eşit olduğu karşılaştırıldı.
Paket com.atmacanext.v258, 27.67 / 215, min26/target36, Room6.
APK SHA256: 63216fac9173043f125138533f335cce7474ac8c59078a19a0df8604b5e39ae4
İmza SHA256: b52d8a15d0dbeb9e16ee7629aa44c026a991c4b8374eaf61134d1556f0da21fa
Mevcut 27.64–27.66 üzerine uygulama kaldırılmadan kurulabilir.
APK CRC, v2/v3 imza, 16 KiB hizalama ve iki tarihsel SQLite5→6 kontrolü geçti.
Yeni APK fiziksel Android/X/tablette çalıştırılmadı. Canlı API anahtarıyla
resim anlama veya 500 metin üretimi denenmedi; gerçek API için ayarlardaki
kullanıcı anahtarı gerekir. Havuz taşıma kuralları, yanlış adet/yetki reddi,
iptal/kısmi üretim ve JSON görüntü gövdesi birim testlerde kontrol edildi.
Hesap ekleme ve takip etmeyenleri çıkarma akışları bu sürümde değiştirilmedi.
Kaynakta başlangıç eşiği en az iki saatlik ilk uygun tweet olarak korundu.
Video yeni APK'nın fiziksel cihaz doğrulaması sayılmaz.
Releases workflow'u önceki contents:write otomatik onay reddi nedeniyle
etkinleştirilmedi. APK, tam kaynak/test/not paketi kalıcı dosya teslimidir.
Özel anahtarlar depoya veya pakete eklenmedi. CI debug imzası teslim APK'sı değildir.


Bu commit yalnız teslim notu/evidans içerir; uygulama ve testler başarılı CI kaynağıyla aynıdır.
