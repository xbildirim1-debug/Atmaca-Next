# X Selector Matrix — Checkpoint 10

| Hedef | Kabul edilen kanıt | Ek doğrulama | Belirsizlikte davranış |
|---|---|---|---|
| X paketi | `com.twitter.android` | active root package | işlem yapma |
| Kendi profil | `Edit profile/Profili düzenle` veya @handle + Followers + Following profil kanıtı | beklenen @handle | yeniden HOME -> profile |
| Followers listesi | Followers/Takipçiler/Takipçi header | explicit @handle + action/list kanıtı | recovery |
| Following listesi | Following/Takip edilen header | explicit @handle + action/list kanıtı | recovery |
| Verified Followers | Verified Followers/Onaylı Takipçiler/Doğrulanmış Takipçiler | explicit @handle + action/list kanıtı | kaynağı atla |
| Follow hedefi | Follow, Follow back, Takip et, Geri takip et, Sen de takip et, Sende takip et | aynı row = tam 1 explicit @handle | tıklama yok |
| Unfollow hedefi | Following, Takip ediliyor, Takip ediyor | aynı row = tam 1 explicit @handle | tıklama yok |
| Unfollow onayı | Unfollow/Takipten çık/Takibi bırak | exact clickable label | PAUSED |
| Hesap satırı | exact hedef @handle veya güçlü username resource-id | seçim sonrası aktif profil @handle doğrulaması | recovery/PAUSED |
| Profil Followers/Following sayacı | label + görünür sayı veya güçlü count resource-id | profile screen | tıklama yok |
| Account switch trigger | exact account label/resource-id | zaten ACCOUNT_DRAWER ekranında | belirsizse tıklama yok |
| Bio/Doğum tarihi/Daha fazla | yasak hedef | merkezi TargetVerifier | her zaman reddet |

## Kullanıcı adı kuralı
Genel listelerde yalnızca `@name` biçimindeki explicit handle kabul edilir. `Following`, `Profile`, `Followers` gibi normal X UI kelimeleri kullanıcı adı sayılmaz. Bare handle yalnızca resource-id açıkça `username`, `screen_name`, `screenname` veya `handle` sinyali verdiğinde profil kimliği için kullanılabilir.
