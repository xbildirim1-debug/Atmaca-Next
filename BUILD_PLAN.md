# Atmaca Next — Checkpoint 14 Final Release Candidate

CP14 artık CP13 İçerik Stüdyosu dahil güncel X/Twitter tabanının üzerine kuruludur.

## Release kapısı
- CP1–CP12 otomasyon, recovery, scheduler ve sağlık katmanları korunur.
- CP13 İçerik Stüdyosu release adayına dahildir.
- Hedef uygulama paketi `com.twitter.android`.
- Android: compile/target SDK 37, min SDK 26.
- AGP 9.3.0 + Gradle 9.5.0 + JDK 17 hedeflenir.
- Release build: R8 minify + resource shrink.
- Son kapı: gerçek Android SDK ortamında `assembleDebug`, `test`, `lint`, `assembleRelease` ve fiziksel cihaz testi.
