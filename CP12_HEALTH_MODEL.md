# Checkpoint 12 — Hesap Sağlığı

Her X hesabı için görev ve gerçek Room loglarından dinamik sağlık skoru hesaplanır.

- Başlangıç: 100
- FAILED görev: -18
- ERROR olayı: -8
- rate-limit/cooldown: -7
- recovery: -3
- WARN: -2
- Doğrulanmış işlem ve tamamlanan görevler sınırlı pozitif kanıt sağlar.
- Skor 0–100 arasında tutulur; A–E derece ve SAĞLIKLI/İZLE/RİSKLİ/KRİTİK durumu üretilir.

Bu skor X'in kendi hesap güvenlik durumunu iddia etmez; yalnızca Atmaca otomasyonunun gözlediği operasyonel sağlıktır.
