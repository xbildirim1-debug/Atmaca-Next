# Atmaca Next 27.66 gerçek doğrulama

27.66 TESLİM VE GERÇEK DOĞRULAMA — 14 Ekim 2026
Kaynak/testler: main ve feature/27.66-pilot.
APK uygulama kaynak commit: e805c5d041d2d6b49137118bb68d50bb8e63d219.
Başarılı main CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/38634197777
770 test geçti (44 yeni regresyon); 0 başarısız/hata/atlanan.
Lint 0 Error/Fatal, 58 Warning, 8 Hint. APK ve CI paket kontrolü geçti.
Son APK asıl anahtarla yeniden imzalandı; dex, kaynaklar ve manifestin tamamı
test edilen CI APK'sıyla aynı baytlarda olduğu tek tek karşılaştırıldı.
Paket com.atmacanext.v258, 27.66 / 214, min26/target36, Room6.
APK SHA256: c243a988b96c17d5780915b57735c8c3ff86a5132e9a93e5640d04649fd05bfa
İmza SHA256: b52d8a15d0dbeb9e16ee7629aa44c026a991c4b8374eaf61134d1556f0da21fa
27.64/27.65 üzerindeki imzayla aynı; uygulamayı kaldırmadan güncellenebilir.
APK CRC, imza v2/v3 ve 16 KiB hizalama geçti. Özel anahtar pakete konmadı.
İki tarihsel SQLite 5→6 düzeni veri/ilerleme koruma kontrolü yerelde geçti.
Yeni APK fiziksel Android/X/tablette çalıştırılmadı; canlı API anahtarıyla
500 metin üretimi yapılmadı. Toplu üretim gerçek JSON ve denetlenen
üretim/saklama portlarıyla, iptal ve kısmi sonuçlar dahil birim test edildi.
Kullanıcının hesap ekleme tamam geri bildirimi korundu; AccountSync aynı.
İlk CI'da eksik AccessibilityNodeInfo importu, ikinci CI'da Türkçe İ
nedeniyle iki tekrar sayımı bulundu ve düzeltildi; yukarıdaki son CI geçti.
Releases workflow'u önceki contents:write otomatik onay reddi nedeniyle
etkinleştirilmedi. APK, kaynak, gerçek raporlar ve notlar ayrı dosya teslimidir.


Değişiklikler DEVIR.txt içinde. Test edilen uygulama kaynaklarına bu doğrulama kaydından sonra dokunulmadı. Ham main-job-log.txt ve BUILD_VERIFICATION.json CI sonucunu, UPDATE_VERIFICATION.json teslim APK imzasını/bayt eşleşmesini kaydeder.
