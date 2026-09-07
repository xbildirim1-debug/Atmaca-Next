# Atmaca Next — sohbetler arası devir notu

Son güncelleme: 7 Eylül 2026. Bu not ve kaynak kod GitHub'da tutulur; başka ChatGPT hesabından devam ederken önce bu dosyayı oku. Önceki sohbet dosyalarına erişebildiğini varsayma.

## Kullanıcının çalışma tercihi

Kullanıcı sohbet dolduğunda başka ChatGPT hesabından devam ediyor. Yapılan bütün kod değişiklikleri, testler ve güncel durum bu depoda kalmalı. İşi yalnız sohbet mesajında veya geçici çalışma dizininde bırakma. Sonraki düzeltmelerde bu notu güncelle; hangi dalda/commit'te olduklarını ve ana dala aktarılıp aktarılmadıklarını açık yaz. Şifre, token ve özel imzalama anahtarı ekleme.

## Depo ve sürüm

- Depo: https://github.com/xbildirim1-debug/Atmaca-Next
- Ana dal: main.
- Güncel düzeltme: 26.5-account-sync-home, versionCode 53.
- Android paket adı: com.atmacanext.v258; namespace: com.atmacanext.app.
- Önceki sürüm: 26.4-reliable-navigation, versionCode 52.
- 26.5 kod commit'i: 782349ccd7f7a4c9b7808e144623d9fa5c62dbd4.
- Düzeltme dalı: fix/account-sync-home-26-5.
- İnceleme kaydı: https://github.com/xbildirim1-debug/Atmaca-Next/pull/1
- Bu devir notunu içeren commit, test edilmiş düzeltmenin devamıdır. main bu commit'e normal ileri taşıma ile güncellenir; kod yalnız düzeltme dalında bırakılmaz. PR durum etiketi yerine main geçmişini esas al.

## Son bildirilen hata ve kanıt

Kullanıcı 1000026716.mp4 videosunu ve 7 Eylül 2026 loglarını gönderdi. Video incelendi: X ana akışta kalıyor, hesap menüsü açılmıyor. Ham video bu depoya yüklenmedi.

Log özeti:

```text
12:33:12.343 screen=HOME popup=NONE
12:33:12.374 ACCOUNT_SYNC DRAWER | Hesap menüsü açılıyor
12:33:13.137 screen=FOLLOWING_LIST popup=NONE
12:33:13–28 stage=DRAWER screen=FOLLOWING_LIST saved=0 skipped=0
12:33:32.802 X ekranına ulaşılamadı
12:33:33.050 Atmaca Next'e dönüş doğrulandı
12:33:47.920 DRAWER | Hesap menüsü açılıyor
12:33:48.183 screen=FOLLOWING_LIST
12:34:07.964 Zaman aşımı stage=DRAWER target=null screen=FOLLOWING_LIST
```

Kök neden: ana akıştaki Following/Takip edilenler sekmesi ile gönderilerdeki kullanıcı adları, ilişki listesi kurallarını tetikleyebiliyor. HOME'dan DRAWER aşamasına geçtikten sonraki snapshot FOLLOWING_LIST olarak sınıflanınca yalnız HOME için çalışan menü tıklaması yürümüyor. Ham seçili sekme denetimi de aynı ana akış sekmesini gerçek takip listesi sanabiliyor. Bu bir hesap şifresi veya oturum açma hatası değil.

## 26.5'te değişen dosyalar

1. `app/src/main/java/com/atmacanext/app/automation/HomeTimelineEvidence.kt`: yeni. Görünür Sana özel/For you ile Following sekme çiftinden veya home_timeline kimliğinden ana akış kanıtı üretir; süslenmiş sekme etiketlerini destekler.
2. `ScreenDetector.kt` (aynı klasör): menü/seçici katmanları ve ana akış kanıtı, ham seçili ilişki sekmesi ve liste sezgilerinden önce değerlendirilir. Tek parametreli mevcut detect çağrısı korunur. Gerçek takip listesi ve profil ayrımları korunur.
3. `AccountSyncController.kt`: hesap menüsü ve seçici tıklamalarında 1200 ms animasyon beklemesi; tıklama sonucu logu; DRAWER/SWITCHER/VERIFY_DRAWER aşamalarında beklenmeyen ekran için sınırlı toparlanma. Var olan 3 denemelik bütçe ve hesap süre sınırları korunur.
4. `XNavigator.kt`: genel avatar/profile_image/user_image kimlikleri ve genel profil fotoğrafı etiketleri sınırsız menü seçiminden çıkarıldı. Özel menü etiketleri ve üst araç çubuğu sınırlarıyla avatar seçimi korundu; görünürlük kontrolü eklendi. Önceden var olan oran tabanlı son çare bu değişiklikte kaldırılmadı.
5. `app/src/test/java/com/atmacanext/app/automation/AccountSyncHomeRegressionTest.kt`: 9 yeni regresyon testi.
6. `app/build.gradle.kts`: versionCode 53, versionName 26.5-account-sync-home.
7. `.github/workflows/android-build.yml`: APK/artifact adları 26.5'e güncellendi.

## Test sonucu ve sınırı

Başarılı çalışma: https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/34108017552

- Test edilmiş kaynak commit'i: 782349ccd7f7a4c9b7808e144623d9fa5c62dbd4.
- 95 birim testi: 0 başarısız, 0 hata, 0 atlanan.
- Bunların 9'u yeni hesap ekleme regresyon testidir.
- Türkçe/İngilizce ana akış, seçili Following akışı, gönderi yazarları yüklenirken ekran ayrımı, ham seçili sekme, süslenmiş sekmeler, akış üzerindeki menü/seçici, gerçek ilişki listesi, gizli ana akış ve profil örnekleri kontrol edildi.
- testDebugUnitTest, lintDebug, assembleDebug ve apksigner verify başarılı.
- İndirilen APK manifesti ve SHA256 dosyası doğrulandı.
- Gerçek Android cihazında X oturumlarıyla uçtan uca test YAPILMADI. Birim testi ve başarılı APK derlemesi, kullanıcının telefonundaki tüm akışların çalıştığını kanıtlamaz. Yeni kullanıcı videosu/logu henüz alınmadı.
- Görevlerin tamamının düzeldiği veya hesapların başarıyla eklendiği iddia edilmemeli.

## Kullanıcıya teslim edilen APK

- Dosya: AtmacaNext-26.5.apk; 20.524.760 bayt.
- SHA256: aa1b6763b711951315af526151231d7f367597bfcb6e6feeab01adaa4380400c
- İlgili GitHub Actions APK artifact ID: 10013278541, adı AtmacaNext-26.5-APK.
- Test raporu artifact ID: 10013279421, adı validation-reports.
- Yukarıdaki çalışma sayfasının Artifacts bölümünde bulunur. Artifact ZIP'inin içinde APK, SHA256 ve manifest.xml vardır.
- Kullanıcıya APK doğrudan sohbetten de teslim edildi. APK ikili dosyası git kaynak ağacına eklenmedi; GitHub Actions çıktısındadır.
- Bu artifact'in mevcut son kullanma tarihi 6 Aralık 2026. Süresi dolduğunda aynı kaynaktan yeniden derlenebilir, ancak aşağıdaki imza sorunu nedeniyle byte/imza eşitliği varsayılmamalı.

## İmzalama: somut açık sorun

Mevcut CI geçici Android debug anahtarı kullanıyor. 26.4 ile kullanıcıya teslim edilen 26.5'in sertifikaları karşılaştırıldı ve FARKLI oldukları doğrulandı:

- 26.4 sertifika SHA256: 06e9ab2a6841f9d37363c1afecebe4336e96f1f95e0acee76aa2a3eef1d6c17c
- Teslim edilen 26.5 sertifika SHA256: f3022244f909fd47d13313c078b590eaeb6c3caa59fafa6a5f2067ac46f341e5

Aynı paket adıyla 26.5, önceki 26.4'ün üzerine güncelleme olarak kurulamaz. Eski Atmaca'yı kaldırmak uygulama içi verileri siler; kullanıcı bu konuda bilgilendirildi. Önceki imzalama özel anahtarı bu oturumda bulunmadı. Eski APK'dan özel anahtar çıkarılamaz. Sonraki üretim güncellemelerinde güvenli, kalıcı imzalama kurulmalı; keystore veya parolalar herkese açık depoya yazılmamalı. main'e taşıma sonrası yeniden derlenen APK'nın da bu teslim edilen APK ile aynı sertifikada olduğu varsayılmamalı.

## Derleme ve çalışma mimarisi

- Java 17; Gradle 9.5.0; compileSdk 37.1; targetSdk 36; minSdk 26; build-tools 36.0.0.
- Workflow: `.github/workflows/android-build.yml`; main push, pull_request ve workflow_dispatch tetikler.
- Komut: `gradle --no-daemon testDebugUnitTest lintDebug :app:assembleDebug --stacktrace`.
- Gradle wrapper JAR mevcut değil; CI setup-gradle kullanır.
- Yerel X Android uygulaması ve AccessibilityService kullanılır. X API/Selenium ile değiştirme.
- APK INTERNET izni istemez; internet bağlantısını X kullanır.
- Hesap import/switch: AccountSyncController; ekranlar: ScreenDetector; menü gezinmesi: XNavigator; oturum kanıtı: AccountSwitcherInspector.
- Hesap tarama kimlik ve sayaç doğrulaması yapar; eksik bilgi başarılı kayıt sayılmaz. Mevcut en fazla 10 hesap sınırı korunur.
- Tablet ve farklı ekran boyutları gereksinimi devam eder; yeni sabit piksel koordinatları ekleme.
- README ve eski V19/V25/V26 notları tarihsel içerik taşır. Örneğin eski 100 kullanıcı derinliği talebi ile README'deki 26.3'ün görünür satırdan başlama davranışı farklıdır. Bu hesap ekleme düzeltmesi takipten çıkma davranışını değiştirmedi; eski gereksinimi kendiliğinden geri yükleme.

## Sonraki asistanın başlayacağı yer

1. Bu dosyayı, README'yi, güncel main commit'ini ve app/build.gradle.kts sürümünü oku. Başka bir sohbetin daha yeni commit eklemiş olabileceğini kontrol et.
2. Kullanıcıdan gelen yeni 26.5 cihaz testini esas al; henüz gelmemişse test edilmiş gibi konuşma.
3. Beklenen import akışı: HOME → DRAWER → ACCOUNT_DRAWER → SWITCHER → ACCOUNT_SWITCHER → HARVEST → SELECT → VERIFY_DRAWER → SAVING; sonra sıradaki hesap ve sonunda Atmaca'ya dönüş.
4. Sorun sürerse ACCOUNT_SYNC ve NAV loglarını, ilgili ekranın erişilebilirlik kanıtını incele. Kullanıcı adını/takip sayaçlarını okumadan hesaba başarı yazma; açılmayan menüye başarılı deme.
5. Hesap ekleme doğrulandıktan sonra çoklu hesap görev sırası, görev sonu dönüş ve diğer bildirilen görev sorunlarını ayrı kanıtlarla değerlendir. Bu değişiklik bunların tamamını doğrulamaz.
6. Mevcut telefon uygulaması kararlı olmadan daha önce ertelenen çoklu emülatör mimarisine geçme.
7. Her yeni düzeltmeyi test et, GitHub'a kaydet, bu notu son testler, APK ve kalan sorunlarla güncelle. Kullanıcı aynı talimatları başka hesapta tekrar etmek zorunda kalmasın.
