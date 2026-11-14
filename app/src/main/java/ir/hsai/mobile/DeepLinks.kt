package ir.hsai.mobile

import android.content.Intent
import android.net.Uri

/**
 * یک درگاه ورود برای همهٔ پیوندهای اپ: هم `hsai://...` (که امروز کار می‌کند) و هم
 * پیوندهای رسمی `https://hsai.ir/...` (App Links) که پس از تأیید
 * `/.well-known/assetlinks.json` مستقیم در اپ باز می‌شوند و نه در مرورگر.
 *
 * چرا لازم است: درگاه پرداخت کاربر را به `https://hsai.ir/payment/callback` برمی‌گرداند.
 * بدون App Link این بازگشت در مرورگر می‌ماند و کاربر باید دستی به اپ برگردد و خودش
 * «بررسی پرداخت» را بزند — یعنی همان نقطه‌ای که بیشترین پرداخت نیمه‌کاره اتفاق می‌افتد.
 *
 * قاعدهٔ امنیتی: هر پیوند ناشناخته یا با میزبان دیگر نادیده گرفته می‌شود؛ هیچ پارامتری
 * از پیوند به‌عنوان دستور (مثلاً «ارتقای اشتراک») اجرا نمی‌شود، فقط «بررسی وضعیت پرداخت».
 */
object DeepLinks {
    const val HOST: String = "hsai.ir"
    const val SCHEME: String = "hsai"

    /** مسیرهایی که به‌عنوان App Link ثبت شده‌اند (باید با AndroidManifest یکی بمانند). */
    val PATHS: List<String> = listOf("/payment/callback", "/app/payment/callback", "/app/auth")

    enum class Kind { PAYMENT, AUTH, DEADLINES, NONE }

    fun kind(intent: Intent?): Kind {
        val data = intent?.data
        val byUri = data?.let(::kind)
        if (byUri != null && byUri != Kind.NONE) return byUri
        return when (intent?.action) {
            ACTION_OPEN_DEADLINES -> Kind.DEADLINES
            Intent.ACTION_VIEW -> Kind.NONE
            else -> Kind.NONE
        }
    }

    fun kind(uri: Uri?): Kind {
        if (uri == null) return Kind.NONE
        val scheme = uri.scheme?.lowercase().orEmpty()
        if (scheme == SCHEME) {
            return when (uri.host?.lowercase()) {
                "payment" -> Kind.PAYMENT
                "auth" -> Kind.AUTH
                "deadlines" -> Kind.DEADLINES
                else -> Kind.NONE
            }
        }
        if (scheme != "https" && scheme != "http") return Kind.NONE
        if (uri.host?.lowercase() != HOST) return Kind.NONE
        val path = uri.path.orEmpty().trimEnd('/')
        return when {
            path.endsWith("/payment/callback") || path == "/payment" -> Kind.PAYMENT
            path == "/app/payment/callback" -> Kind.PAYMENT
            path == "/app/auth" -> Kind.AUTH
            path == "/app/deadlines" -> Kind.DEADLINES
            else -> Kind.NONE
        }
    }

    /**
     * تبدیل پیوند https به همان شکل `hsai://`، تا بقیهٔ کد فقط یک شکل بشناسد.
     * نام میزبان عوض می‌شود و مسیر به «نوع» تبدیل می‌شود؛ پارامترها دست‌نخورده می‌مانند.
     */
    fun normalize(uri: Uri?): Uri? {
        if (uri == null) return null
        if (uri.scheme?.lowercase() == SCHEME) return uri
        return when (kind(uri)) {
            Kind.PAYMENT -> uri.buildUpon().scheme(SCHEME).authority("payment").path("/").build()
            Kind.AUTH -> uri.buildUpon().scheme(SCHEME).authority("auth").path("/").build()
            Kind.DEADLINES -> uri.buildUpon().scheme(SCHEME).authority("deadlines").path("/").build()
            Kind.NONE -> null
        }
    }

    /**
     * پارامترهای درگاه (زرین‌پال: `Authority` و `Status`) با نام‌های مختلف می‌آیند؛
     * این نگاشت یک‌جا انجام می‌شود تا `MainActivity` همیشه یک شکل ببیند.
     */
    fun paymentStatus(uri: Uri?): String = when {
        uri == null -> ""
        uri.getQueryParameter("Status") != null -> uri.getQueryParameter("Status").orEmpty()
        uri.getQueryParameter("status") != null -> uri.getQueryParameter("status").orEmpty()
        else -> ""
    }

    fun paymentAuthority(uri: Uri?): String = listOf("Authority", "authority", "authority_code")
        .firstNotNullOfOrNull { key -> uri?.getQueryParameter(key)?.takeIf { it.isNotBlank() } }
        .orEmpty()

    /** پیوند رسمی که برای بازگشت از درگاه به کاربر/درگاه داده می‌شود. */
    fun paymentCallbackUrl(): String = "https://" + HOST + "/payment/callback"

    const val ACTION_OPEN_DEADLINES: String = "ir.hsai.mobile.OPEN_DEADLINES"
}
