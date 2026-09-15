# Prezenter

Android telefon bilan kompyuterdagi prezentatsiyani (PowerPoint, Google
Slides) bir xil Wi-Fi tarmog'i orqali boshqarish tizimi.

## Loyihaning ikki qismi

| Papka       | Nima                                              | Platforma        |
|-------------|----------------------------------------------------|------------------|
| `desktop/`  | `PrezenterServer` - fonda ishlaydigan Windows dasturi | .NET 8 / WPF     |
| `android/`  | `Prezenter` - Android ilova                        | Kotlin / Compose |
| `docs/`     | Aloqa protokoli spetsifikatsiyasi                  | -                |

To'liq JSON xabar formati uchun [`docs/PROTOCOL.md`](docs/PROTOCOL.md) ga qarang.

## Amalga oshirilgan funksiyalar

- **Wi-Fi orqali ulanish** - kompyuter WebSocket serverini ochadi
  (`ws://<ip>:9091/prezenter/`) va `_prezenter._tcp.local.` mDNS xizmati
  sifatida o'zini e'lon qiladi.
- **Pairing** - birinchi ulanishda QR kod skanerlanadi (`MainWindow`da
  ko'rsatiladi); token telefonda saqlanadi, shu bois keyingi safar
  bir xil tarmoqqa ulanganda kompyuter ro'yxatda avtomatik chiqadi va
  QR qayta kerak bo'lmaydi.
- **Ekran o'chiq holatda tovush tugmalari bilan slayd boshqarish** -
  "Taqdimot rejimi" yoqilganda Android ilova fon xizmati (`PresenterService`)
  orqali "soxta" `MediaSession`ni faol ushlab turadi, shu bois tizim
  tovush tugmalarini ekran qulflangan holatda ham ilovaga uzatadi
  (batafsili izoh kod ichida, `PresenterService.kt`).
- **Prezentatsiya boshqaruvi**: boshidan boshlash (`F5`), joriy
  slayddan (kattaytirib) boshlash (`Shift+F5`), oldinga/orqaga
  (`Page Up`/`Page Down`), to'xtatish (`Esc`).
- **Ovoz**: balandlashtirish, pasaytirish, bitta tugma bilan to'liq
  mute qilish - kompyuterning tizim (master) ovoz darajasi WASAPI
  orqali boshqariladi.
- **Lazer ko'rsatkich (taklif qilingan qo'shimcha funksiya)** - jismoniy
  lazer o'rniga, kompyuterda har doim tepada turuvchi shaffof, "click-through"
  oyna ochiladi va telefon ekranidagi trackpad maydonida barmoq surilganda
  o'sha oynada qizil nuqta harakatlanadi. Proektorga ham, monitorga ham
  ishlaydi, qo'shimcha uskuna kerak emas.

## Ishga tushirish

### Windows (`desktop/PrezenterServer`)

```powershell
cd desktop
dotnet restore
dotnet run --project PrezenterServer
```

> **Eslatma:** `HttpListener` `http://+:9091/...` manzilida tinglaydi, bu
> odatda administrator huquqi yoki oldindan URL ACL rezervatsiyasini talab
> qiladi. Administrator terminalda bir marta ishga tushirish eng oson yo'l,
> yoki doimiy ruxsat berish uchun:
> ```powershell
> netsh http add urlacl url=http://+:9091/prezenter/ user=Everyone
> ```

Dastur ishga tushgach oynada QR kod chiqadi - shuni Android ilovada
"QR kodni skanerlash" tugmasi orqali skanerlang.

### Android (`android/`)

Eng oson yo'l - **Android Studio'da** `android/` papkasini ochish: IDE
Gradle wrapper'ni o'zi generatsiya qiladi va sinxronlaydi (bu repo
`gradlew`/wrapper jar faylini o'z ichiga olmaydi, chunki bu sessiyada
Android SDK/tarmoq cheklangan muhitda ishlandi).

Terminaldan qurish uchun, avval lokal Gradle va Android SDK o'rnatilgan
bo'lishi kerak:

```bash
cd android
gradle wrapper --gradle-version 8.7   # bir martalik: wrapper generatsiya qiladi
./gradlew assembleDebug
```

Ilovani telefonga o'rnating, kompyuter bilan bir xil Wi-Fi tarmog'iga
ulaning va QR kodni skanerlang.

## Muhim texnik eslatma

Bu sessiyada **`.NET SDK` va **Android SDK** o'rnatilmagan muhitda
ishlandi, shuning uchun ikkala loyiha ham compile qilib **sinovdan
o'tkazilmagan**. Kod arxitekturasi va API ishlatilishi (WPF/HttpListener,
NAudio CoreAudioApi, Makaretu mDNS, QRCoder / Android MediaSessionCompat,
CameraX + ML Kit, NSD, OkHttp WebSocket) hujjatlashtirilgan rasmiy
API'larga asoslangan, lekin haqiqiy qurilmada birinchi marta ishga
tushirishda kichik tuzatishlar (paket versiyalari, ruxsatlar va h.k.)
kerak bo'lishi mumkin.

## Kelajakda qo'shish mumkin bo'lgan narsalar

- Google Slides uchun rasmiy Google Slides API integratsiyasi (hozircha
  brauzer klaviatura qisqa yo'llariga tayanadi - "joriy slayddan boshlash"
  uchun 100% aniq emas, `docs/PROTOCOL.md` dagi izohga qarang).
- WebSocket ulanishini TLS/parol bilan qo'shimcha shifrlash (hozir faqat
  bir martalik token + lokal tarmoq ishonchiga tayanadi).
- Bir nechta telefonni bitta kompyuterga bir vaqtda ulash va navbat/huquq
  boshqaruvi.
- iOS versiyasi (agar kerak bo'lsa, umumiy protokol allaqachon platformadan
  mustaqil).
