package ir.hsai.mobile

import android.content.Context
import android.os.Build
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * هویت نسخهٔ اپ — یک‌جا، تا هدرها، گزارش خرابی و صفحهٔ تنظیمات یک عدد را نشان دهند.
 * `BuildConfig` در آزمون‌های JVM هم تولید می‌شود، پس این مقدار در تست هم قابل اتکاست.
 */
object AppInfo {
    const val PLATFORM: String = "android"
    val VERSION_NAME: String = BuildConfig.VERSION_NAME
    val VERSION_CODE: Int = BuildConfig.VERSION_CODE
    /** مقدار هدر `X-Hsai-App`؛ سرور با همین می‌فهمد درخواست از کدام نسخهٔ اپ آمده است. */
    val HEADER: String = "hsai-native/" + VERSION_NAME + " (" + VERSION_CODE + ")"
    val USER_AGENT: String = "HsaiNative/" + VERSION_NAME
}

/**
 * گزارش خرابیِ خودمان — بدون سرویس واسط.
 *
 * چرا: خطای کرش در اپ بومی هیچ‌جا دیده نمی‌شد؛ کاربر فقط «بسته شد» می‌دید و ما هیچ سرنخی
 * نداشتیم. اینجا همان استک روی گوشی در صف می‌ماند و با اولین اتصال به `/api/v1/app/crash`
 * فرستاده می‌شود. اگر مسیر روی سرور نباشد، گزارش در صف می‌ماند و چیزی به کاربر نشان
 * داده نمی‌شود (شکست گزارش‌دهی هرگز نباید خودش علت خرابی شود).
 *
 * چه چیزی نمی‌فرستیم: هیچ پیام گفتگو، تصویر، ایمیل، شماره، توکن یا شناسهٔ کاربر.
 * فقط نام کلاس خطا، استک، نسخهٔ اپ، مدل دستگاه و نسخهٔ اندروید.
 */
class CrashReporter(private val context: Context) {
    private val dir: File get() = File(context.filesDir, "crash-pending").apply { if (!exists()) mkdirs() }

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { record(thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    /** نوشتن گزارش روی دیسک و صف‌کردن ارسال. همهٔ خطاها بلعیده می‌شوند. */
    fun record(thread: Thread, error: Throwable) {
        if (dir.listFiles()?.size ?: 0 >= MAX_PENDING) return
        val payload = JSONObject()
            .put("message", (error.javaClass.name + ": " + (error.message ?: "")).take(300))
            .put("stack", android.util.Log.getStackTraceString(error).take(MAX_STACK))
            .put("thread", thread.name.take(80))
            .put("app_version", AppInfo.VERSION_NAME)
            .put("app_build", AppInfo.VERSION_CODE)
            .put("platform", AppInfo.PLATFORM)
            .put("device", (Build.MANUFACTURER + " " + Build.MODEL).take(80))
            .put("os", "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")")
            .put("at", System.currentTimeMillis())
        val file = File(dir, "crash-" + System.currentTimeMillis() + "-" + (0..999999).random() + ".json")
        file.writeText(payload.toString())
        enqueue()
    }

    fun pending(): List<File> = dir.listFiles()?.sortedBy { it.name }?.take(MAX_PENDING).orEmpty()

    fun enqueue() {
        val request = OneTimeWorkRequestBuilder<CrashUploadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag(TAG).build()
        WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val TAG = "hsai-crash"
        const val MAX_PENDING = 5
        const val MAX_STACK = 4000
    }
}

class CrashUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val reporter = CrashReporter(applicationContext)
        val api = ApiClient()
        var retry = false
        reporter.pending().forEach { file ->
            val payload = runCatching { JSONObject(file.readText()) }.getOrNull()
            if (payload == null) { file.delete(); return@forEach }
            try {
                api.request("/app/crash", "POST", payload, authenticated = false)
                file.delete()
            } catch (e: ApiException) {
                // ۴xx یعنی مسیر یا بدنه پذیرفته نشد؛ فرستادن دوباره فایده‌ای ندارد.
                if (e.status in 400..499) file.delete() else retry = true
            } catch (_: Exception) { retry = true }
        }
        if (retry) Result.retry() else Result.success()
    }
}
