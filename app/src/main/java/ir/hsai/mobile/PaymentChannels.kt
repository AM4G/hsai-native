package ir.hsai.mobile

import android.content.Context
import android.os.Build

/**
 * کانال پرداخت اشتراک.
 *
 * واقعیت امروز: اشتراک از درگاه بانکی خودمان (زرین‌پال) و در مرورگر امن دستگاه پرداخت
 * می‌شود؛ این تنها مسیری است که الان کار می‌کند و همین در اپ باقی می‌ماند.
 *
 * چرا کد فروشگاه هم اینجاست: اگر اپ در Google Play منتشر شود، بند ۴ سیاست پرداخت گوگل
 * می‌گوید محتوای دیجیتالِ درون‌اپ باید با Play Billing فروخته شود، وگرنه اپ رد می‌شود.
 * راه‌حل معمول: نسخهٔ «Play» با فروش درون‌اپ، و نسخهٔ «مستقیم» با درگاه بانکی — با یک
 * کد مشترک. این فایل همان تصمیم را می‌گیرد و جای اتصال SDK را مشخص می‌کند؛ خودِ SDK
 * (com.android.billingclient) عمداً به پروژه اضافه نشده، چون تا زمانی که محصول در
 * Play Console ساخته نشود فقط حجم و پیچیدگی اضافه می‌کند.
 *
 * قاعدهٔ سخت: هیچ‌جا «ارتقای ساختگی» انجام نمی‌شود. اگر کانال فروشگاه در دسترس نباشد،
 * کاربر همان مسیر بانکی را می‌بیند و متن روشن می‌گیرد — نه دکمه‌ای که کار نمی‌کند.
 */
enum class PaymentChannel { BANK_GATEWAY, PLAY_BILLING }

object PaymentChannels {
    /**
     * آیا این نصب از Google Play آمده است؟ (nasb از فروشگاه، نه نصب مستقیم APK)
     * روی اندروید ۱۱+ `getInstallSourceInfo` و پایین‌تر `getInstallerPackageName`.
     */
    fun installedFromPlay(context: Context): Boolean {
        val pm = context.packageManager ?: return false
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { pm.getInstallSourceInfo(context.packageName).installingPackageName }.getOrNull()
        } else {
            @Suppress("DEPRECATION")
            runCatching { pm.getInstallerPackageName(context.packageName) }.getOrNull()
        }
        return installer == PLAY_STORE_PACKAGE
    }

    /** آیا Play Billing از نظر محیطی (نه از نظر SDK) قابل استفاده است؟ */
    fun playBillingUsable(context: Context): Boolean {
        if (!FLAG_PLAY_BILLING_ENABLED) return false
        if (!installedFromPlay(context)) return false
        return runCatching {
            context.packageManager.getPackageInfo(GOOGLE_PLAY_SERVICES, 0)
        }.isSuccess
    }

    fun current(context: Context): PaymentChannel =
        if (playBillingUsable(context)) PaymentChannel.PLAY_BILLING else PaymentChannel.BANK_GATEWAY

    /** توضیح یک‌خطی برای صفحهٔ طرح‌ها؛ در اپ فارسی و بدون اصطلاح انگلیسی. */
    fun explain(context: Context): String = when (current(context)) {
        PaymentChannel.PLAY_BILLING ->
            "پرداخت از طریق Google Play انجام می‌شود و بازگشت خودکار به اپ است."
        PaymentChannel.BANK_GATEWAY ->
            "پرداخت از درگاه بانکی امن انجام می‌شود و پس از آن خودکار به اپ برمی‌گردید."
    }

    /** متن حقوقی که باید کنار دکمهٔ پرداخت باشد (حق انصراف و بازگشت وجه). */
    fun refundNotice(): String =
        "پیش از پرداخت، مبلغ و دورهٔ اشتراک در همین صفحه نمایش داده می‌شود. " +
            "اشتراک خودکار تمدید نمی‌شود؛ برای تمدید دوباره خودتان پرداخت می‌کنید. " +
            "اگر پس از پرداخت، سرویس را دریافت نکردید، از بخش پشتیبانی با شمارهٔ پیگیری پرداخت پیام بدهید؛ " +
            "بازگشت وجه در چارچوب شرایط استفاده و مقررات حمایت از مصرف‌کننده بررسی می‌شود."

    /**
     * نقطهٔ اتصال خرید در فروشگاه. تا وقتی SDK اضافه نشده، پیام روشن برمی‌گرداند و
     * هیچ مبلغی از کاربر کم نمی‌شود.
     */
    fun startStorePurchase(planId: String): Result<Unit> =
        if (!FLAG_PLAY_BILLING_ENABLED)
            Result.failure(ApiException("خرید درون‌اپ فروشگاه برای این نسخه فعال نیست؛ از درگاه بانکی همین صفحه پرداخت کنید.", 0, "STORE_DISABLED"))
        else
            Result.failure(ApiException("اتصال به سرویس پرداخت فروشگاه در این نسخه تکمیل نشده است.", 0, "STORE_NOT_WIRED"))

    /** نگاشت کد طرح اپ به شناسهٔ محصول فروشگاه (قرارداد آیندهٔ Play Console). */
    fun storeProductId(planId: String, yearly: Boolean): String =
        if (yearly) "hsai_sub_yearly_$planId" else "hsai_sub_monthly_$planId"

    private const val PLAY_STORE_PACKAGE = "com.android.vending"
    private const val GOOGLE_PLAY_SERVICES = "com.google.android.gms"
    /**
     * تا زمانی که محصول اشتراک در Play Console ساخته و `billingclient` اضافه نشود،
     * این پرچم باید `false` بماند؛ وگرنه کاربر دکمه‌ای می‌بیند که کار نمی‌کند.
     */
    const val FLAG_PLAY_BILLING_ENABLED: Boolean = false

    /** فقط برای آزمون: کانال را بدون بررسی محیط انتخاب می‌کند. */
    fun describe(channel: PaymentChannel): String = when (channel) {
        PaymentChannel.PLAY_BILLING -> "فروشگاه برنامه"
        PaymentChannel.BANK_GATEWAY -> "درگاه بانکی"
    }
}
