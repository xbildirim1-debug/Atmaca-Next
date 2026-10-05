# Buse 1.0 — doğrulanmış yerel derleme

- Son yerel Gradle çalışması başarılı: testDebugUnitTest, lintDebug ve assembleDebug.
- JUnit: **524 test**, 0 başarısız/hata/atlanan. Yeni Buse regresyonları: **42 test**.
- Lint: **0 hata/fatal**, 12 uyarı, 1 öneri. Rapor `validation/1.0-local/` içindedir.
- APK: com.buse.mobile / versionCode 1 / versionName 1.0 / minSdk 26 / targetSdk 36. Uygulama adı Buse. INTERNET izni yok. İmza ve APK ZIP CRC doğrulandı.
- APK SHA256: `63d700071bb9ef519e0be1b52401d39777fc4758be4f3737756522d6eec95623`.
- Sertifika SHA256: `9bb1488d50538b0b69427f7b49f5cc809b6b715eeb81a1867e33fe930074b7a5`. Yerel Android debug anahtarıyla imzalıdır; imza anahtarı kaynakta ve tam pakette yoktur. Gelecek güncelleme aynı anahtarla imzalanmalıdır.
- Atmaca HEAD `8b467a3baaa991abaa3202dca78ff9299ad1c2c0` ve çalışma ağacı değişmeden korundu.
- GitHub kaynağı ayrı `buse/1.0` dalının `Buse/` klasörüne kaydedilir; Atmaca main'e Buse eklenmez.
- **Fiziksel Android/X testi yapılmadı.** CI başarısı olarak sunulmaz. Gerçek X satır gruplanması ve 200 kişilik başlangıç cihazda kabul protokolüyle doğrulanmalıdır.
