# CP14 Release Checklist

- [x] CP13 İçerik Stüdyosu CP14 tabanına dahil
- [x] X/Twitter hedef paketi kontrol edildi
- [x] versionCode/versionName CP14'e yükseltildi
- [x] compileSdk/targetSdk 37
- [x] Java/Kotlin 17
- [x] R8 minify + resource shrink release yapılandırması
- [x] ProGuard kuralları mevcut
- [x] Manifest/XML statik parse kontrolü
- [x] ZIP bütünlük kontrolü
- [ ] Android SDK ortamında Gradle wrapper üret/doğrula
- [ ] `./gradlew test`
- [ ] `./gradlew lint`
- [ ] `./gradlew assembleDebug`
- [ ] Fiziksel cihazda X Accessibility selector testi
- [ ] 10 hesap uzun koşu testi
- [ ] `./gradlew assembleRelease` + signing
