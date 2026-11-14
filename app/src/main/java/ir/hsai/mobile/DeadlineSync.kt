package ir.hsai.mobile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * یادآور مهلت در سه گام: ۷، ۳ و ۱ روز پیش.
 *
 * چرا سه گام: یک یادآور «یک روز قبل» برای مهلتی که ۳۰ روز بعد است تقریباً هیچ فایده‌ای ندارد
 * (کاربر عملاً یک روز فرصت دارد). اپ‌های تقویمی و بانکی همین الگو را دارند و آنچه مهم است
 * «فرصت اقدام» است، نه فقط «خبر دادن».
 *
 * چرا بدون FCM: برای این سه گام نیازی به سرور و توکن نیست؛ همان چیزی که امروز کار می‌کند
 * (WorkManager + اعلان محلی) کافی است. اعلان عمومی روی صفحهٔ قفل فقط «وارد برنامه شوید»
 * نشان می‌دهد و عنوان پرونده پنهان می‌ماند (همان تصمیم دورهای قبل، همان کانال).
 */
object DeadlineMilestones {
    val OFFSETS: List<Int> = listOf(7, 3, 1)

    /** ساعت اعلان در روز مقصد (۹ صبح محلی). */
    private const val HOUR = 9

    /**
     * برای هر گام یک کار یکتا با نام `deadline-<id>-<روز>` ثبت می‌شود؛ گام‌های گذشته
     * ثبت نمی‌شوند و بی‌صدا رد می‌شوند تا «ثبت یادآور» همیشه موفق باشد.
     */
    fun schedule(context: Context, id: Long, title: String, date: String, owner: String): Int {
        val current = SecureSession(context).read()?.optJSONObject("user")?.str("id")
        require(ReminderPrivacy.sameOwner(owner, current)) { "برای یادآور، نشست ذخیره‌شده لازم است؛ دوباره وارد شوید یا فایل تقویم بگیرید." }
        val due = LocalDate.parse(date)
        var scheduled = 0
        OFFSETS.forEach { lead ->
            val at = due.minusDays(lead.toLong()).atTime(HOUR, 0).atZone(ZoneId.systemDefault()).toInstant()
            val delay = Duration.between(Instant.now(), at).toMillis()
            if (delay <= 0) return@forEach
            val work = OneTimeWorkRequestBuilder<DeadlineMilestoneWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("hsai-deadline").addTag(ReminderMigration.TAG)
                .setInputData(workDataOf("id" to id, "title" to title, "date" to date, "owner" to owner, "lead" to lead))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("deadline-$id-$lead", ExistingWorkPolicy.REPLACE, work)
            scheduled++
        }
        return scheduled
    }

    fun cancel(context: Context, id: Long) {
        OFFSETS.forEach { lead -> WorkManager.getInstance(context).cancelUniqueWork("deadline-$id-$lead") }
        // کار نسخهٔ قبل (یک‌روزه، نام قدیمی) هم پاک می‌شود تا اعلان تکراری نیاید.
        WorkManager.getInstance(context).cancelUniqueWork("deadline-$id")
    }
}

class DeadlineMilestoneWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return Result.success()
        val owner = inputData.getString("owner").orEmpty()
        val current = SecureSession(context).read()?.optJSONObject("user")?.str("id")
        // اگر حساب عوض شده باشد، اعلان پروندهٔ حساب قبلی نباید روی این گوشی بالا بیاید.
        if (!ReminderPrivacy.sameOwner(owner, current)) return Result.success()
        val id = inputData.getLong("id", 0).toInt()
        val lead = inputData.getInt("lead", 1)
        val title = inputData.getString("title").orEmpty()
        val date = inputData.getString("date").orEmpty()
        val channel = "legal_deadlines"
        manager.createNotificationChannel(NotificationChannel(channel, "مهلت‌های حقوقی", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).setAction(DeepLinks.ACTION_OPEN_DEADLINES),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val publicNotice = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("یادآور حق‌سنج")
            .setContentText("برای مشاهده وارد برنامه شوید.")
            .build()
        val headline = when (lead) {
            7 -> "۷ روز تا مهلت شما"
            3 -> "۳ روز تا مهلت شما"
            else -> "فردا آخرین روز مهلت شما"
        }
        val body = headline + "\n" + title + "\nتاریخ ثبت‌شده: " + dateFa(date) + " (" + date + ")" +
            "\nاین یادآور جایگزین پیگیری پرونده نیست؛ تاریخ را با مرجع قضایی تطبیق دهید."
        val notification = NotificationCompat.Builder(context, channel)
            .addExtras(android.os.Bundle().apply { putBoolean(ReminderMigration.MARKER, true) })
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotice)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(headline)
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        return try {
            manager.notify(LEAD_BASE + id * 10 + lead, notification)
            Result.success()
        } catch (_: SecurityException) { Result.success() }
    }

    private companion object { const val LEAD_BASE = 7_100_000 }
}

/**
 * همگام‌سازی دوره‌ای مهلت‌ها بدون FCM.
 *
 * مسئله‌ای که حل می‌کند: مهلتی که در سایت (یا روی دستگاه دیگر) ثبت شود، روی گوشی هیچ
 * یادآوری نداشت، چون یادآورها فقط لحظهٔ «ثبت در اپ» ساخته می‌شدند. این کارگر هر ۱۲ ساعت
 * یک‌بار فهرست مهلت‌های باز را می‌گیرد و گام‌های ۷/۳/۱ را می‌سازد (کارهای موجود با
 * `ExistingWorkPolicy.REPLACE` بازنویسی می‌شوند؛ پس تکرار اعلان رخ نمی‌دهد).
 *
 * محدودیت‌ها که عمداً رعایت شده‌اند:
 *  - بدون نشست ذخیره‌شده هیچ درخواستی زده نمی‌شود (اپ مهمان شبکه را بی‌دلیل بیدار نمی‌کند).
 *  - خطای شبکه = `Result.retry()` با backoff؛ خطای احراز هویت = `Result.success()` تا
 *    کار پشت سر هم شکست نخورد (کاربر باید خودش وارد شود).
 *  - متن پرونده در اعلان عمومی دیده نمی‌شود (همان قاعدهٔ ReminderPrivacy).
 */
class DeadlineSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val session = SecureSession(context).read() ?: return@withContext Result.success()
        val token = session.str("token")
        val owner = session.optJSONObject("user")?.str("id").orEmpty()
        if (token.isBlank() || owner.isBlank()) return@withContext Result.success()
        val api = ApiClient()
        api.token = token
        try {
            val data = api.request("/deadlines?page=1")
            val items = ApiContract.page(data, 1, 50).items
            items.forEach { item ->
                val id = item.safeLong("id") ?: return@forEach
                val status = item.str("status").lowercase()
                if (status in setOf("done", "completed", "canceled", "cancelled", "closed")) {
                    DeadlineMilestones.cancel(context, id); return@forEach
                }
                val title = item.str("title", item.str("case_title", "مهلت قانونی"))
                val date = item.str("due_date", item.str("deadline_date")).take(10)
                if (date.isBlank()) return@forEach
                // تاریخ نامعتبر (مثلاً خالی یا شمسی) نباید کل همگام‌سازی را بشکند.
                runCatching { DeadlineMilestones.schedule(context, id, title, date, owner) }
            }
            Result.success()
        } catch (e: ApiException) {
            if (e.status == 401 || e.status == 403) Result.success() else Result.retry()
        } catch (_: Exception) { Result.retry() }
    }
}

object DeadlineSync {
    private const val UNIQUE = "hsai-deadline-sync"

    /** ثبت همگام‌سازی دوره‌ای؛ فراخوانی در `MainActivity.onCreate` بی‌خطر و تکراری‌پذیر است. */
    fun init(context: Context) {
        val request = PeriodicWorkRequestBuilder<DeadlineSyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag("hsai-deadline").build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** یک‌بار پس از ورود یا ثبت مهلت؛ تأخیر کوتاه تا UI بی‌درنگ پاسخ دهد. */
    fun syncSoon(context: Context) {
        val request = OneTimeWorkRequestBuilder<DeadlineSyncWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag("hsai-deadline").build()
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE + "-now", ExistingWorkPolicy.REPLACE, request)
    }

    /** پس از خروج از حساب: کارها پاک می‌شوند تا اعلان حساب قبلی نیاید. */
    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE)
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE + "-now")
    }

    /** برای آزمون/خروجی تشخیصی. */
    fun summary(rows: List<JSONObject>): String {
        val open = rows.count { it.str("status").lowercase() !in setOf("done", "completed", "canceled", "cancelled", "closed") }
        return fa(open) + " مهلت باز"
    }
}
