# 27.65 GitHub ve yerel doğrulama

Başarılı main CI: [38580353744](https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/38580353744), uygulama/test/derleme kaynak commit 8631c5e96faa4ad2009449425d299b1a03ccd20a. 726 test / 0 başarısız-hata-atlanan; lint 0 Error/Fatal, 55 Warning ve 8 Hint; APK/manifest/imza/CRC ve tam paket içindeki aynı APK kontrolü geçti. İlk CI'nin 384 MiB metaspace hatası 1024 MiB derleme sınırıyla giderildi. Uygulama kodu bu ayar için değiştirilmedi.

Yerel aynı imzalı teslim: 726 test; lint 0 Error/Fatal, 50 Warning ve 8 Hint. AtmacaNext-27.65.apk SHA256 d1fb5d7a321e6de2b7f76e5d5735cf54ee94446986348346ff9b588a0b636c9c; 22373850 bayt. Sertifika b52d8a15d0dbeb9e16ee7629aa44c026a991c4b8374eaf61134d1556f0da21fa, kullanıcının 27.64 APK'sıyla aynı. Genel CI sertifikası 553016671b50b5f0edf1aa9fe5b211d4f2cf876d0836515110dfc76d15164d02 farklıdır; CI artifact APK'sı kullanıcıya üzerine kurulacak dosya olarak sunulmaz.

Kaynak/testler main ve fix/27.65-profile-return-gesture üzerinde. Son uygulama devir kaydı DEVIR.txt ve DEVIR_NOTU.md başındadır. Fiziksel X/Android/tablet testi yapılmadı. GitHub Releases yayını için önceki contents:write otomatik onay reddi sürer; yayın workflow'u etkinleştirilmedi.
