# ─────────────────────────────────────────────────────────────────────────────
# حق‌سنج — قاعده‌های R8 (دور ۳۲)
#
# هدف: کوچک‌کردن و مبهم‌سازی، بدون شکستن چیزهایی که با نام پیدا می‌شوند.
# هر قاعده اینجا دلیل دارد؛ قاعدهٔ «نگه‌داشتن همه‌چیز» عمداً نیست.
# ─────────────────────────────────────────────────────────────────────────────

# OkHttp: بدون این‌ها در حالت R8 full-mode، اسامی کلاس‌های پلتفرم/Conscrypt حذف می‌شوند
# و اتصال TLS در نسخهٔ release با خطای زمان اجرا شکست می‌خورد.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepnames class okhttp3.internal.platform.** { *; }
-keepnames class org.conscrypt.** { *; }
-keepnames class org.bouncycastle.** { *; }

# Kotlin coroutines: کلاس‌های داخلی که با نام ساخته می‌شوند.
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# WorkManager: کارگرها با نام کلاس در دیتابیس ذخیره می‌شوند و پس از آپدیت اپ با
# reflectively ساخته می‌شوند. اگر نامشان obfuscate شود، کارهای در انتظار (یادآور مهلت)
# پس از آپدیت دیگر اجرا نمی‌شوند.
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
-keep class ir.hsai.mobile.DeadlineWorker { <init>(...); }
-keep class ir.hsai.mobile.DeadlineMilestoneWorker { <init>(...); }
-keep class ir.hsai.mobile.DeadlineSyncWorker { <init>(...); }
-keep class ir.hsai.mobile.CrashUploadWorker { <init>(...); }

# Compose: قاعده‌های لازم برای runtime و ابزارها (نسخه‌های جدید خودشان همراه دارند،
# این‌ها برای اطمینان روی نسخهٔ فعلی BOM هستند).
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ViewModel با نام ساخته می‌شود (by viewModels).
-keep class ir.hsai.mobile.AppViewModel { <init>(...); }

# خط‌های استک در گزارش خرابی خودمان خوانا بمانند (نام کلاس‌ها و متدها حفظ شود،
# ولی نام فیلدها می‌تواند obfuscate شود). بدون این، گزارش کرش بی‌فایده می‌شود.
-keepnames class ir.hsai.mobile.**

# حذف لاگ‌های اشکال‌زدایی در نسخهٔ نهایی (اطلاعات داخلی در لاگ دستگاه نمی‌ماند).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
