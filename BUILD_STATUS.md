# Atmaca Next 26.51 derleme durumu

- Temel kaynak: b9022ab0b02a268e9dd8a4ba1e0353dbcbe863dd (26.50).
- versionCode: 99; versionName: 26.51-reply-flow-recovery.
- applicationId: com.atmacanext.v258; minSdk: 26; targetSdk: 36; compileSdk: 37.1.
- AGP: 9.3.2; Gradle: 9.5.0; JDK: 17; Room schema: 6.
- 26.50 son main derlemesi başarısız: StatusLabel çakışması (CI 34571512499).
- CI 37288168240 (8de43244bdb760aa12738448caec3773bc742b96): test/lint/assemble/imza/manifest/paketleme başarılı. Son veritabanı normalleştirmesi ve gerçek SQLite geçiş kontrolü için yeni CI bekleniyor.
- Fiziksel X cihaz testi yapılmadı.
- Workflow: .github/workflows/android-build.yml (contents:read).
- Kalıcı release imzası ve Releases yayın yetkisi bu çalışmada değiştirilmedi.

İlk 26.51 CI 37287905394 eski tools SDK paketi bulunamadığı için kurulum aşamasında durdu; testler çalışmadı. SDK action v4 ve 15859902 ile düzeltildi; sonraki CI başarılı oldu.
