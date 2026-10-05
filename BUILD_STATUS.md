# Atmaca Next 26.52 derleme durumu

- Temel main: b9debe9759d90384415e81e5a8c70f157372bc4b (26.51, 334 test başarılı).
- versionCode100 / 26.52-comment-media-return; applicationId com.atmacanext.v258; Room v6.
- Yorumcu medya/atlama ve yorumcu-retweetçi hedefe dönüş düzeltmesi. Önceki 26.51 davranışı korunur.
- Test edilen kaynak main commit: 7e5b34890d78b7d8dd272014543ba74beb5b1660.
- Başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/37293523368 . Test/lint/assemble/APK imzası/manifest/paketleme geçti.
- İndirilen JUnit XML'leri: 352 test, 0 başarısız/hata/atlanan; 18 yeni CommentMediaReturn26_52Test regresyonu. Lint: 0 hata/fatal, 21 uyarı. SQLite 5→6 geçiş kontrolü geçti.
- APK: 20803296 bayt; SHA256 58d7d97b3b03085a6ab1ad3c5826806693220f5f0e4da9e6acaf69ca5c43cdbf.
- Tam paket: 20686235 bayt; SHA256 f0ea0760f2c42ca1c1086908e648bba708273ca0cbe72563627fe2536de1ff11. Kaynak commit, manifest kimliği, ZIP CRC, SHA256 listeleri ve paket içi/dışı APK eşitliği doğrulandı.
- Kaynak/testler main'de; fix/26.52-comment-media-return yerel çalışma dalı. Sonuç notları ayrı [skip ci] teslim commit'idir, yukarıdaki APK kaynağını değiştirmez. Dosyalar CI'den aynen alınır; yeniden derleme/imzalama yok.
- Actions artifact 11337423963 ve rapor 11338060227, 3 Ocak 2027'de sona erer. GitHub Releases yayını tamamlanmadı; docs/archive-release.proposed.yml etkin değildir ve bu başarılı run'a hazırlanmıştır.
- Kullanıcının önceki sürüm videosu incelendi; yeni 26.52 APK ile fiziksel X testi yapılmadı.
- Ayrıntılı devir/cihaz kanıtı DEVIR_NOTU.md içinde. Kalıcı debug imza ve Releases workflow onay koşulu değişmedi.
