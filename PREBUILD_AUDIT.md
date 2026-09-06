# Atmaca Next V19 — Prebuild Audit

## Build matrix
- AGP: 9.3.0
- Gradle: 9.5.0
- JDK: 17
- compileSdk / targetSdk: 37
- SDK Build Tools: 36.0.0
- Kotlin: 2.3.21
- KSP: 2.3.9
- Compose BOM: 2026.08.00

AGP 9.x enables built-in Kotlin by default. This project keeps the existing `org.jetbrains.kotlin.android` + `android.kotlinOptions` configuration for the first V19 build, therefore `gradle.properties` explicitly opts out with both `android.builtInKotlin=false` and `android.newDsl=false`. This is a temporary compatibility bridge supported by AGP 9.x.

## V19 runtime gates
- Application id preserved: `com.atmacanext.app`
- X target only: `com.twitter.android`
- Version code: 19
- Turkish X drawer label: `Gezinti çekmecesini göster`
- Deep-link outside-X suppression: 3500 ms
- Re-launch dedupe: 2800 ms
- Gesture click fallback enabled
- Account sync/switch flow retained
- Verified follow / unfollow safety gates retained

## Local validation completed without Android SDK
- CP15 pure core behavior JAR: PASS
- 10-account queue evidence: PASS
- YAML syntax: PASS
- build_linux.sh shell syntax: PASS
- static V19 marker checks: PASS

## Remaining hosted build gate
`clean -> test -> lint -> assembleDebug -> apksigner verify`
