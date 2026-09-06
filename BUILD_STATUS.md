# Atmaca Next V23 Build Status

- versionCode: 23
- versionName: 0.23.0-startup-fix
- applicationId: com.atmacanext.app
- minSdk: 26
- targetSdk: 36
- compileSdk: 37.1
- Android Gradle Plugin: 9.3.0 (yerleşik Kotlin)
- Gradle: 9.5.0
- JDK: 17
- `:app:assembleDebug`: PASS
- Birim testleri: PASS (67/67)
- `lintDebug`: PASS
- APK ZIP bütünlüğü: PASS
- APK Signature Scheme v2 doğrulaması: PASS
- Birleşik manifestte `androidx.startup.InitializationProvider`: YOK
- `AtmacaNextApplication` üzerinden isteğe bağlı WorkManager yapılandırması: AKTİF
- GitHub Actions: `.github/workflows/android-build.yml`

Not: Üretilen dosya geliştirme/debug anahtarıyla imzalıdır. Play Store veya kalıcı
dağıtım için kullanıcıya ait release keystore ile ayrıca imzalanmalıdır.
