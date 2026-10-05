# Atmaca Next 26.51 derleme durumu

- APK kaynak commit: 7df763bcbe58fc79878d391b641ebdcf757b11aa (main).
- Başarılı CI: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/37289341028 .
- versionCode99 / 26.51-reply-flow-recovery; applicationId com.atmacanext.v258; Room schema6.
- minSdk26; targetSdk36; compileSdk37.1; AGP9.3.2; Gradle9.5.0; JDK17.
- 334 test: 0 başarısız, 0 hata, 0 atlanan. 23 yeni Yorum Alıntısı regresyonu.
- İki eski v5 tablo biçimiyle gerçek SQLite geçişi başarılı; hedef/görev ilerlemesi korundu.
- Test/lint/assemble/APK imzası/manifest/paketleme başarılı. Lint: 0 hata, 0 fatal, 21 uyarı.
- APK SHA256: 13871547313f28ce434689d3faeb2f18d3099a3617cc237d16453f15503cc51b.
- Tam paket SHA256: a5bb6595aa23a83c855b7d1dfa96e8442a68d8d2b49144c5bf6c8f0075772f9d. İndirilen dosyaların CRC/hash ve APK eşitlik kontrolü başarılı.
- 26.50 son main CI 34571512499 StatusLabel çakışmasıyla başarısızdı; düzeltildi.
- İlk 26.51 CI 37287905394 eski tools SDK paketinde durdu; setup-android v4 ve 15859902 ile düzeltildi. CI 37288168240 ve son 37289341028 başarılıdır.
- Fiziksel X cihaz testi yapılmadı. Debug imzasının önceki kurulumla uyumu doğrulanmadı.
- Build workflow contents:read. Releases workflow taslağı hazır; AGENTS.md uyarınca contents:write etkinleştirme onayı bekler. Henüz kalıcı Releases yayını yapılmadı.
- Devir/test/teslim ayrıntıları DEVIR_NOTU.md ve NOT_DEFTERI.txt içindedir.
