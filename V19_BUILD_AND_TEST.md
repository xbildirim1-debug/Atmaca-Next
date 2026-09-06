# Atmaca Next V19 Modern — Build/Test Gate

Identity: `com.atmacanext.app` (CP15 master identity retained)
X target: `com.twitter.android`
Version: `19 / 0.19.0-cekmece-fix-modern`

Applied:
- V19 real TR drawer label (`Gezinti çekmecesini göster`) and gesture fallback
- 3.5s outside-X suppression after X deep links; 2.8s duplicate-launch guard
- drawer + exact target handle verification without profile loop
- X account import/sync up to 10 accounts, exact @handle switching, profile stat sync
- `Hesaba geç` per account and `X'ten hesapları ekle`
- gesture-capable Accessibility config
- Verified source chain no longer marks task COMPLETED merely because 8 sources were visited; a high safety cap pauses instead
- existing CP15 multi-account/multi-task, Room, recovery, tablet components preserved

Required build gate:
`gradle clean test lint assembleDebug --stacktrace`
Then `zipalign -c` and `apksigner verify` on release artifact.
Physical-device gate: account import, A→B→A switch, Following deeplink must not be immediately overwritten by Home, verified follow, 100-depth unfollow, second-account handoff.
