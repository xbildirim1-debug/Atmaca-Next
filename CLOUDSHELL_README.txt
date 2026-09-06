ATMACA NEXT V19 — GOOGLE CLOUD SHELL DERLEME

Bu paket GitHub gerektirmez. Google Cloud Shell icinde Android SDK ve Gradle'i indirir,
clean + test + lint + assembleDebug calistirir, APK imzasini apksigner ile dogrular ve:

  $HOME/AtmacaNext-V19.apk

olarak cikti verir.

TELEFONDAN EN KISA YOL
1) shell.cloud.google.com ac.
2) Bu ZIP'i Cloud Shell'e Upload ile yukle.
3) Terminalde:

   unzip -q AtmacaNext-V19-CloudShell-BuildReady.zip -d AtmacaV19
   cd AtmacaV19/AtmacaNext_V19_FinalSource
   bash CLOUDSHELL_BUILD.sh

4) Basarili olursa:

   cloudshell download "$HOME/AtmacaNext-V19.apk"

NOTLAR
- Ilk build internet hizina gore Android SDK, Gradle ve Maven bagimliliklarini indirir.
- Proje kimligi: com.atmacanext.app
- X hedef paketi: com.twitter.android
- versionCode: 19
- Debug APK Android SDK tarafindan imzalanir ve zipalign edilir.
