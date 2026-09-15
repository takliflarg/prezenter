# Prezenter aloqa protokoli

Telefon (Android, WebSocket **client**) va kompyuter (Windows, WebSocket
**server**) bir xil Wi-Fi tarmog'ida ulanadi. Barcha xabarlar UTF-8 JSON
matn frame sifatida yuboriladi.

## Transport

- Server: `ws://<kompyuter-ip>:9091/prezenter`
- Discovery: kompyuter `_prezenter._tcp.local.` mDNS xizmatini e'lon qiladi
  (TXT yozuvida `name`, `version`, `pairPort` maydonlari bilan).
- Pairing: har bir ulanish `pairToken` bilan tasdiqlanadi (QR kod yoki
  mDNS orqali topilgan qurilmani kompyuterda "Ruxsat berish" tugmasi
  bosilgandan keyin).

## Xabar konvertlari

### Klient -> Server

```json
{ "type": "pair", "token": "BASE64_PAIR_TOKEN", "deviceName": "Ali telefon" }
```

`token` maydoni ixtiyoriy (`null` bo'lishi mumkin) - bu tarmoqdan (mDNS/NSD)
avtomatik topilgan, lekin hali hech qachon tasdiqlanmagan kompyuterga
ulanishga urinishni bildiradi. Bu holda server darhol rad etmaydi -
kompyuter foydalanuvchisiga "Ruxsat berilsinmi?" dialogini ko'rsatadi va
javobni (agar tasdiqlansa, yangi doimiy `token` bilan) keyinroq alohida
`pairResult` xabarida qaytaradi (pastga qarang).

```json
{ "type": "command", "action": "NEXT" }
{ "type": "command", "action": "PREV" }
{ "type": "command", "action": "START_FROM_BEGINNING" }
{ "type": "command", "action": "START_FROM_CURRENT" }
{ "type": "command", "action": "STOP" }
{ "type": "command", "action": "VOLUME_UP" }
{ "type": "command", "action": "VOLUME_DOWN" }
{ "type": "command", "action": "MUTE_TOGGLE" }
```

```json
{ "type": "laser", "action": "ON" }
{ "type": "laser", "action": "OFF" }
{ "type": "laser", "action": "MOVE", "x": 0.42, "y": 0.71 }
```

`x`/`y` — 0.0-1.0 oralig'ida, ekranning nisbiy koordinatasi (chap-yuqori =
`0,0`, o'ng-past = `1,1`). Telefon trackpad ustida barmog'ini surganda
delta emas, mutlaq nisbiy pozitsiya yuboriladi — shu bois qaysi ekran
o'lchami bo'lishidan qat'iy nazar nuqta to'g'ri joyga tushadi.

```json
{ "type": "ping" }
```

### Server -> Klient

```json
{ "type": "pairResult", "success": true, "message": "Ulanish tasdiqlandi", "token": "BASE64_NEW_PERMANENT_TOKEN" }
```

`token` faqat tokensiz (tarmoqdan avtomatik topilgan) qurilma PC'da yangi
tasdiqlanganda keladi - klient buni saqlab qo'yishi kerak (keyingi
safar shu tokenni yuborsa, qayta tasdiqlashsiz ulanadi). QR orqali
pairing qilinganda alohida token qaytarilmaydi - QR kodning o'zidagi
bir martalik token doimiy tokenga aylanadi.

```json
{ "type": "status", "connected": true, "computerName": "DESKTOP-ALI", "muted": false, "volume": 62 }
```

```json
{ "type": "error", "message": "Noma'lum buyruq" }
```

```json
{ "type": "pong" }
```

## Klaviatura xaritasi (Windows tomonida simulyatsiya qilinadi)

| Action                | PowerPoint prezentatsiya rejimida | Google Slides (brauzer) |
|------------------------|-----------------------------------|--------------------------|
| `NEXT`                 | `Page Down` / `Right`             | `Right` / `Space`        |
| `PREV`                 | `Page Up` / `Left`                | `Left`                   |
| `START_FROM_BEGINNING` | `F5`                               | `Ctrl+F5` (dan.slide sahifasidan Present)|
| `START_FROM_CURRENT`   | `Shift+F5`                         | Joriy slayd tanlab, `Ctrl+Shift+F5`* |
| `STOP`                 | `Esc`                              | `Esc`                    |

\* Google Slides brauzerda "hozirgi slayddan boshlash" uchun rasmiy klaviatura
qisqa yo'li yo'q, shuning uchun ilova avval joriy tanlangan slaydni
o'zgartirmasdan `Present`/`Ctrl+F5` bosadi va real hayotda odatda foydalanuvchi
avval kerakli slaydni "Present from" strelkasi (menyudagi) orqali tanlaydi.
Kelajakda Google Slides API integratsiyasi bilan bu aniqroq boshqarilishi mumkin.

## Ovoz tugmalarining ikki xil rejimi (Android)

1. **Ilova old planda ochiq bo'lganda** — foydalanuvchi sozlamada tanlagan
   rejim bo'yicha: yoki odatiy tizim ovozini boshqaradi, yoki slaydni
   oldinga/orqaga suradi (`NEXT`/`PREV`).
2. **Ekran o'chiq yoki ilova fonda bo'lganda** — doimo slayd
   navigatsiyasi uchun ishlatiladi ("Taqdimot rejimi" yoqilgan bo'lsa).
   Bu ishlashi uchun Android ilova "soxta" `MediaSession`ni faol holatda
   ushlab turadi (ovoz chiqarmaydigan, lekin `PLAYING` holatidagi media
   sessiyasi) — faqat shu holatda Android tizimi tovush tugmalarini
   ekran qulflangan holatda ham ilovaga uzatadi. Batafsili
   `android/app/.../service/PresenterService.kt` faylida.
