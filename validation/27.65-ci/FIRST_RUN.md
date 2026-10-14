# 27.65 GitHub CI ilk kontrolü

Main kaynak commit 6b385e20a8bee790eb9839e6f661aa49c26f8430 için Actions 38579607230 uygulama/test derlemesini geçti; lintAnalyzeDebug `java.lang.OutOfMemoryError: Metaspace` ile durdu. Bu bir uygulama test hatası değildir; başarılı CI sonucu sayılmaz. 384 MiB metaspace sınırı 1024 MiB yapılarak yeni kontrol tetiklenir. Yerel 726 test / lint / aynı imzalı APK başarılıdır. Uygulama kaynakları ve teslim APK'sı bu bellek değişikliği için değiştirilmez.

https://github.com/xbildirim1-debug/Atmaca-Next/actions/runs/38579607230
