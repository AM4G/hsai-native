# دور ۳۲ — اجرای هفت پیشنهاد اپ (فقط سورس)

این سند فقط تغییرهای **سورس** را ثبت می‌کند. هیچ APK/AAB ساخته نشده، هیچ استقرار تازه‌ای
انجام نشده و هیچ فایلی آپلود نشده است. دلیل: در این محیط Gradle/AGP/Android SDK نصب نیست
(`ANDROID_HOME` خالی) و build واقعی ممکن نبود؛ بنابراین به‌جای ادعای «ساخته شد»، کد تحویل
شده و مراحل تأیید در همین سند آمده است.

## ۱) ناوبری به پنج مقصد

- `Destination` تازه در `AppViewModel.kt`: **CHAT / HISTORY / DOCUMENTS / SCANNER / ACCOUNT**.
- «بانک قوانین»، «محاسبه‌گرها» و «مهلت‌های قانونی» از منو، ریل، نوار بالا و بنر حذف شدند
  (`Destination.legacy`).
- جای آن‌ها یک درِ واحد «کارها» آمد (`WorksPanel.kt`): مهلت‌ها همان‌جا با تقویم شمسی،
  قوانین و محاسبه‌گرها با باز شدن صفحهٔ کامل سایت در مرورگر.
- `Screen.LAWS/CALCULATORS/DEADLINES` عمداً از `enum` حذف **نشدند**: آزمون‌های موجود
  (`NativeUiTest.kt`) همان صفحه‌ها را رندر می‌کنند و حذفشان بدون کامپایلر قابل تأیید نبود.
  هیچ مسیر کاربری به آن‌ها نمی‌رسد؛ `Destination.isLegacy` این را تضمین و آزمون می‌کند.

## ۲) مهمان: دو پیام رایگان

- `GuestChat.kt`: شناسهٔ نصب پایدار (`DeviceIdentity`)، نگاشت پاسخ `/public/chat`، برچسب سهمیه.
- `ApiClient.guestChat` / `guestQuota` + `rawRequest` (پاسخ مسیرهای عمومی پاکت `{ok,data}` ندارد).
- `ApiRoutes`: `/public/chat` (POST) و `/public/quota` (GET) مجاز شدند؛ سه مسیر عمومی
  با `isPublic` مشخص‌اند.
- `sendChat` دیگر مهمان را به صفحهٔ ورود پرت نمی‌کند؛ `sendGuestChat` مسیر عادی است.
  تصویر برای مهمان پذیرفته نمی‌شود (سرور هم نمی‌پذیرد) و پیام روشن داده می‌شود.
- نوار «۲ از ۲ پیام رایگان» بالای ورودی چت؛ با تمام‌شدن سهمیه، دکمهٔ ورود همان‌جا.

## ۳) App Links و پرداخت

- `DeepLinks.kt`: یک درگاه برای `hsai://` و `https://hsai.ir/...`؛ میزبان دیگر رد می‌شود،
  `normalize` هر دو را به یک شکل می‌آورد.
- `AndroidManifest.xml`: intent-filter با `autoVerify` برای
  `/payment/callback`, `/app/payment/callback`, `/app/auth`, `/app/deadlines`.
- `public/.well-known/assetlinks.json` + `scripts/assetlinks.mjs` (ساخت از اثر انگشت واقعی).
  **اثر انگشت‌ها هنوز جای‌نگهدارند** — تا جایگزینی، اندروید پیوند را در مرورگر باز می‌کند
  (رفتار امروز) و هیچ خطایی دیده نمی‌شود.
- `PaymentChannels.kt`: کانال بانکی فعال، مسیر Play Billing مشخص ولی
  `FLAG_PLAY_BILLING_ENABLED = false` تا محصول اشتراک در Play Console ساخته شود.
  متن «تمدید خودکار نیست + مسیر بازگشت وجه» به صفحهٔ طرح‌ها اضافه شد.

## ۴) دسترس‌پذیری و حریم صفحه

- `FLAG_SECURE` روی صفحه‌های سند/تحلیل/پیش‌نمایش تصویر و پنجره‌های `documentView`,
  `documentEdit`, `historyPreview` (`SecureScreens`).
- برچسب `contentDescription` برای دکمهٔ «کارها» در نوار بالا، دکمهٔ «کارهای دیگر» در خانه،
  و ردیف‌های پنجرهٔ کارها (هر ردیف: عنوان + توضیح).
- `AppLock.kt` + `LockScreen`: قفل برنامه با قفل خود دستگاه (اثر انگشت/رمز صفحه)،
  پیش‌فرض خاموش، قابل روشن‌کردن از تنظیمات، قفل دوباره با رفتن به پس‌زمینه.

## ۵) یادآور مهلت ۷/۳/۱ روز + همگام‌سازی بدون FCM

- `DeadlineMilestones.schedule` سه کار یکتا با نام `deadline-<id>-<روز>` ثبت می‌کند؛
  گام‌های گذشته بی‌صدا رد می‌شوند و تعداد گام‌های ثبت‌شده به کاربر گفته می‌شود.
- `DeadlineSyncWorker` هر ۱۲ ساعت فهرست مهلت‌های باز را می‌گیرد و یادآورها را
  به‌روزرسانی می‌کند (بدون FCM، بدون سرور اضافه)؛ با خروج از حساب پاک می‌شود.
- همان قاعدهٔ حریم قبلی: اعلان عمومی روی صفحهٔ قفل فقط «وارد برنامه شوید» است.
- `DeadlineReminder` (تک‌گام، نسخهٔ قبل) دست‌نخورده مانده و `cancel` هر دو را پاک می‌کند.

## ۶) R8، Baseline Profile، minSdk

- `app/build.gradle`: `minSdk 26` (از ۲۹)، `minifyEnabled true`، `shrinkResources true`،
  `versionCode 5`، `versionName 2.1.0-native`، وابستگی `profileinstaller`.
- `app/proguard-rules.pro`: قاعده‌ها با دلیل — WorkManager (نام کلاس کارگرها در دیتابیس
  ذخیره می‌شود و با obfuscate شدن، یادآورهای در انتظار پس از آپدیت اجرا نمی‌شوند)،
  OkHttp/TLS، Compose، و حذف `Log.v/d` در نسخهٔ نهایی.
- `app/src/main/baseline-prof.txt`: مسیر شروع سرد (Activity، ViewModel، شبکه، تاریخ).

## ۷) مشاهده‌پذیری

- هدرهای `X-Hsai-App`, `X-Hsai-App-Build`, `User-Agent` روی همهٔ درخواست‌های اپ
  (`AppInfo`، از `BuildConfig`).
- `CrashReporter.kt`: استک روی گوشی صف می‌شود (سقف ۵ فایل) و با اولین اتصال به
  `POST /api/v1/app/crash` می‌رود؛ هیچ پیام/تصویر/ایمیل/توکن در بدنه نیست.
- سرور: `server/routes/appCrash.js` (بدون نشست، محدودیت ۲۰ گزارش در ساعت برای هر IP،
  فقط لاگ ساخت‌یافته و بدون ذخیره در دیتابیس) + سوار شدن روی `/api/v1/app`.
- صفحهٔ تنظیمات: نسخه، ساخت، منبع نصب و توضیح اینکه چه چیزی در گزارش خرابی می‌رود.

## آزمون‌ها

- `app/src/test/java/ir/hsai/mobile/Round32LogicTest.kt` (۲۰ آزمون منطق خالص):
  مسیرهای عمومی، نگاشت سهمیهٔ مهمان، پیوندهای ورودی (شامل رد میزبان بیگانه)،
  پنج مقصد ناوبری، هدر نسخه، کانال پرداخت.
- `ApiRoutes.kt` با `kotlinc 2.0.21` جداگانه کامپایل شد (بدون خطا). بقیهٔ فایل‌ها فقط
  از نظر نحوی بازبینی شدند؛ **کامپایل کامل و اجرای آزمون‌ها در این محیط ممکن نبود**.

## کارهای باقی‌مانده (بیرون از سورس)

1. `ANDROID_SHA256_RELEASE` و اثر انگشت Play را در `assetlinks.json` بگذارید (یا
   `node scripts/assetlinks.mjs` را در CI اجرا کنید) و سپس
   `https://hsai.ir/.well-known/assetlinks.json` را تأیید کنید.
2. `./gradlew :app:assembleRelease` و سپس آزمون دودی نسخهٔ R8 (شروع سرد، چت، ورود،
   بارگذاری عکس، یادآور). R8 در این دور بدون build واقعی روشن شده است.
3. برای Play Billing: ساخت محصول اشتراک، افزودن `com.android.billingclient`،
   اتصال `PaymentChannels.startStorePurchase` و روشن‌کردن پرچم.
4. اگر حذف کامل `Screen.LAWS/CALCULATORS/DEADLINES` خواسته شد: آزمون‌های
   `NativeUiTest.kt` هم‌زمان باید بازنویسی شوند (سه صفحه، ۱۵ فراخوانی).
