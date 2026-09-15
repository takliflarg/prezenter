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

## Tayyor fayllarni yuklab olish (o'rnatish uchun)

Kodni o'zingiz qurishingiz shart emas - tayyor `.exe` va `.apk`
fayllar avtomatik ravishda **GitHub Actions** orqali quriladi va
repozitoriyning **Releases** bo'limiga joylanadi:

**[Repozitoriyning Releases sahifasi](../../releases)** - eng oxirgi
`latest-build` (yoki versiyalangan `vX.Y.Z`) relizidan quyidagilarni
yuklab oling:

- `Prezenter-Server-Windows-x64.exe` - Windows 11 (x64) kompyuterga
  o'rnatish shart emas, faylni ishga tushirish kifoya (o'z-o'zini
  ta'minlovchi, .NET runtime alohida o'rnatilmasa ham ishlaydi).
- `Prezenter-Android.apk` - Android telefonga o'rnatish uchun (avval
  "Noma'lum manbalardan o'rnatish"ga ruxsat bering).

Agar Releases bo'limida hali fayl bo'lmasa, repozitoriy Actions
bo'limidan **"Build release binaries"** workflow'ini qo'lda ishga
tushiring (`workflow_dispatch`) - bir necha daqiqada ikkala fayl ham
tayyor bo'ladi.

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

## Manbadan o'zingiz qurish (ixtiyoriy)

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

Gradle wrapper repoga qo'shilgan, shuning uchun Android SDK o'rnatilgan
bo'lsa kifoya:

```bash
cd android
./gradlew assembleDebug
```

Yoki Android Studio'da `android/` papkasini oching - avtomatik
sinxronlanadi. Ilovani telefonga o'rnating, kompyuter bilan bir xil
Wi-Fi tarmog'iga ulaning va QR kodni skanerlang.

## Bu loyiha qanday tekshirildi / muhim texnik eslatmalar

- **Windows (.NET/WPF) tomoni** - shu ishlov beruvchi muhitda `dotnet
  restore` va kod bazasi haqiqatda tekshirildi (paketlar to'g'ri
  tortiladi). Biroq WPF (`net8.0-windows`) loyihasini **to'liq
  publish** qilish uchun Microsoft'ning rasmiy Windows Desktop SDK
  komponenti kerak, u esa faqat Microsoft'ning rasmiy `.NET` yuklab
  olish serverlaridan keladi - bu sessiyaning tarmoq siyosati aynan
  o'sha serverlarni (`builds.dotnet.microsoft.com`, `ci.dot.net`)
  bloklagan. Shu sababli haqiqiy `.exe` faylni **shu yerda emas**,
  balki repoga qo'shilgan GitHub Actions workflow orqali (`windows-latest`
  runner'da, cheklanmagan tarmoq bilan) quramiz - natija Releases
  bo'limida paydo bo'ladi.
- **Android tomoni** - xuddi shunday sababga ko'ra (Android Gradle
  Plugin faqat Google'ning `dl.google.com` Maven repozitoriysida
  joylashgan, u ham shu sessiyada tashkilot xavfsizlik siyosati
  tomonidan bloklangan) APK shu yerda emas, GitHub Actions'da
  (`ubuntu-latest` + Android SDK) quriladi.
- Ikkala holatda ham men bloklangan xostlarni chetlab o'tishga
  urinmadim - buning o'rniga to'g'ri, takrorlanuvchi yechim sifatida
  CI orqali build pipeline sozladim, shunda har safar kod
  o'zgarganda tayyor fayllar avtomatik yangilanadi.
- Kod arxitekturasi va ishlatilgan API'lar (WPF/HttpListener, NAudio
  CoreAudioApi, Makaretu mDNS, QRCoder / Android MediaSessionCompat,
  CameraX + ML Kit, NSD, OkHttp WebSocket) hujjatlashtirilgan rasmiy
  API'larga asoslangan; birinchi CI build muvaffaqiyatsiz bo'lsa,
  Actions logidagi xatolik xabari bo'yicha tuzatish kerak bo'lishi
  mumkin (masalan paket versiyasi mos kelmasligi).

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
