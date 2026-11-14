package ir.hsai.mobile

import android.content.Context
import org.json.JSONObject
import java.util.UUID

/**
 * شناسهٔ دستگاهی پایدار برای سهمیهٔ دو پیام رایگان مهمان.
 *
 * چرا لازم است: بدون شناسه‌ای که بین اجراهای اپ ثابت بماند، هر بار اجرا سهمیهٔ تازه می‌گرفت
 * (یا برعکس، هر بار از صفر شروع می‌کرد و پیام سوم هم رد نمی‌شد). این مقدار «شناسهٔ نصب» است،
 * نه شناسهٔ کاربر: هیچ نام، ایمیل، شماره یا IMEI در آن نیست و سرور آن را پیش از ذخیره هش می‌کند.
 *
 * قواعد سازگاری:
 *  - فقط UUID نسخهٔ ۴ تولید می‌شود؛ هیچ داده‌ای از دستگاه خوانده نمی‌شود.
 *  - مقدار با `clearLocalCache` پاک **نمی‌شود** (وگرنه سهمیه با یک ضربه ریست می‌شد)؛
 *    فقط با حذف دادهٔ برنامه از تنظیمات اندروید از بین می‌رود.
 *  - اگر مقدار ذخیره‌شده خراب باشد، بی‌صدا بازتولید می‌شود تا اپ قفل نشود.
 */
class DeviceIdentity(context: Context) {
    private val prefs = context.getSharedPreferences("hsai_device", Context.MODE_PRIVATE)

    fun id(): String {
        val stored = prefs.getString(KEY, null)
        if (stored != null && stored.length in 16..64 && stored.all { it.isLetterOrDigit() || it == '-' }) return stored
        val fresh = UUID.randomUUID().toString()
        prefs.edit().putString(KEY, fresh).apply()
        return fresh
    }

    /** فقط برای آزمون و برای «شروع تازهٔ عمدی»؛ در مسیر عادی UI فراخوانی نمی‌شود. */
    fun rotate(): String {
        prefs.edit().remove(KEY).apply()
        return id()
    }

    private companion object { const val KEY = "device_id" }
}

/**
 * نگاشت پاسخ `POST /api/v1/public/chat` (مهمان) به مدل‌های خود اپ.
 *
 * قرارداد سرور (همان مسیر وب‌سایت، بدون نشست):
 * ```
 * { ok:true, data:{ reply, citations[], guest:{enabled,limit,used,remaining},
 *                   quick_questions[], suggestions[], verification{} } }
 * ```
 * برای متن و استنادها از همان [ApiContract.chatMessage] استفاده می‌کنیم تا مهمان و کاربر
 * دقیقاً یک شکل پاسخ ببینند؛ فقط سهمیه و برچسب وضعیت مهمان اینجاست.
 */
object GuestChat {
    /** سقف پیش‌فرض سرور؛ اگر پاسخ عدد دیگری داد همان مبنا است. */
    const val DEFAULT_LIMIT: Int = 2

    /** فقط سهمیه؛ شیء خالی اگر سرور نفرستد (سپس UI چیزی نشان نمی‌دهد). */
    fun quota(raw: JSONObject): JSONObject {
        val g = raw.optJSONObject("guest") ?: return JSONObject()
        val limit = g.optInt("limit", DEFAULT_LIMIT)
        val used = g.optInt("used", 0).coerceAtLeast(0)
        val remaining = when {
            g.has("remaining") -> g.optInt("remaining", 0)
            else -> (limit - used)
        }.coerceIn(0, limit)
        return JSONObject()
            .put("enabled", g.optBoolean("enabled", true))
            .put("limit", limit)
            .put("used", used)
            .put("remaining", remaining)
    }

    /** پیام مهمان؛ همان مسیر مشترک پیام کاربر واردشده. */
    fun message(raw: JSONObject): ChatMessage = ApiContract.chatMessage(raw)

    /** متن سهمیه برای نوار بالای ورودی چت. */
    fun quotaLabel(quota: JSONObject): String {
        if (quota.length() == 0) return ""
        if (!quota.optBoolean("enabled", true)) return "این نسخه گفتگوی مهمان را فعال نکرده است؛ برای ادامه وارد شوید."
        val remaining = quota.optInt("remaining", 0)
        val limit = quota.optInt("limit", DEFAULT_LIMIT)
        return when {
            remaining > 0 -> "مهمان: " + fa(remaining) + " از " + fa(limit) + " پیام رایگان این دستگاه باقی مانده است."
            else -> "پیام‌های رایگان مهمان تمام شد؛ برای ادامه وارد حساب شوید."
        }
    }

    /**
     * تاریخچهٔ کوتاه برای مهمان. سرور برای مهمان تاریخچهٔ حساب ندارد و فقط همان دو پیام
     * را می‌بیند؛ کل مکالمهٔ این نشست را نمی‌فرستیم تا هم بار پاسخ کم بماند و هم چیزی
     * بیش از نیاز از متن کاربر جابه‌جا نشود.
     */
    fun history(messages: List<ChatMessage>, max: Int = 4): org.json.JSONArray {
        val out = org.json.JSONArray()
        messages.filter { it.text.isNotBlank() }.takeLast(max).forEach { m ->
            out.put(JSONObject().put("role", if (m.role == "user") "user" else "assistant").put("content", m.text))
        }
        return out
    }

    /** آیا این خطا یعنی «سهمیهٔ مهمان تمام شد»؟ */
    fun exhausted(status: Int, code: String): Boolean = status == 429 || status == 402 ||
        code.contains("QUOTA", true) || code.contains("GUEST", true) || code.contains("LIMIT", true) ||
        code.contains("NEEDS_ACCOUNT", true)
}
