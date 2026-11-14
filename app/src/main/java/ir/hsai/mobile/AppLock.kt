package ir.hsai.mobile

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.view.WindowManager
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * پیش‌نمایش سند و عکس: محتوای پروندهٔ حقوقی نباید در «آخرین برنامه‌های باز» یا
 * اسکرین‌شات‌ها و ضبط صفحه بماند. `FLAG_SECURE` هر سه را هم‌زمان می‌بندد.
 */
object SecureScreens {
    fun set(activity: Activity?, secure: Boolean) {
        val window = activity?.window ?: return
        if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

/**
 * قفل برنامه با همان قفل دستگاه (اثر انگشت/چهره/رمز صفحه) — بدون کتابخانهٔ تازه.
 *
 * چرا اهمیت دارد: نشست در Keystore رمزنگاری شده، ولی گوشیِ باز و در دسترسِ شخص سوم
 * همان نشست را باز می‌گذارد. قفل برنامه همان کاری است که اپ‌های بانکی می‌کنند.
 *
 * قواعد:
 *  - پیش‌فرض خاموش است؛ روشن‌کردنش فقط وقتی معنا دارد که دستگاه قفل صفحه داشته باشد.
 *  - اگر کاربر بعداً قفل صفحه را بردارد، اپ قفل نمی‌شود (وگرنه راه ورود بسته می‌شد).
 *  - با رفتن به پس‌زمینه (`onStop`) دوباره قفل می‌شود؛ فقط «بازگشت از پیش‌نمایش تصویر» مستثنی است.
 */
class AppLock(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("hsai_lock", Context.MODE_PRIVATE)

    fun enabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        unlocked.set(!value)
    }

    private fun keyguard(): KeyguardManager? = app.getSystemService(KeyguardManager::class.java)

    /** آیا گوشی خودش قفل صفحه دارد؟ بدون آن «قفل برنامه» فقط یک پیام بی‌اثر بود. */
    fun deviceHasLock(): Boolean = keyguard()?.isDeviceSecure == true

    /** آیا الان باید صفحهٔ قفل نشان داده شود؟ */
    fun locked(): Boolean = enabled() && deviceHasLock() && !unlocked.get()

    fun markLocked() { unlocked.set(false) }

    fun markUnlocked() { unlocked.set(true) }

    /** پیام وضعیت برای تنظیمات؛ کوتاه و بدون اصطلاح فنی. */
    fun status(): String = when {
        !deviceHasLock() -> "گوشی شما قفل صفحه ندارد؛ ابتدا در تنظیمات اندروید رمز یا اثر انگشت بگذارید."
        enabled() -> "قفل برنامه روشن است؛ هر بار بازگشت به اپ، قفل دستگاه پرسیده می‌شود."
        else -> "قفل برنامه خاموش است (روشن‌کردنش پیشنهاد می‌شود)."
    }

    /**
     * درخواست تأیید هویت. [onResult] فقط با `true` یعنی باز شد؛ هر شکست یا لغو یعنی
     * صفحهٔ قفل می‌ماند و هیچ داده‌ای بارگذاری نمی‌شود.
     */
    fun prompt(activity: Activity, onResult: (Boolean) -> Unit) {
        if (!enabled() || !deviceHasLock()) { markUnlocked(); onResult(true); return }
        val executor = ContextCompat.getMainExecutor(activity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && biometricsReady(activity)) {
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle("ورود به حق‌سنج")
                .setSubtitle("برای دیدن پرونده‌ها و گفتگوها هویت خود را تأیید کنید.")
                .setAllowedAuthenticators(
                    android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
            val signal = android.os.CancellationSignal()
            prompt.authenticate(signal, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    markUnlocked(); onResult(true)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) { onResult(false) }
                override fun onAuthenticationFailed() { /* همان پنجره باز می‌ماند؛ تلاش بعدی */ }
            })
            return
        }
        // مسیر پشتیبان (اندروید ۹ و پایین‌تر، یا وقتی بیومتریک در دسترس نیست):
        // همان صفحهٔ قفل خود دستگاه با «تأیید رمز/الگو/پین».
        val intent = keyguard()?.createConfirmDeviceCredentialIntent("ورود به حق‌سنج", "هویت خود را تأیید کنید.")
        if (intent == null) { onResult(false); return }
        pendingCallback.set(onResult)
        activity.startActivityForResult(intent, REQUEST_CONFIRM)
    }

    /** فراخوانی از `MainActivity.onActivityResult`؛ `true` یعنی همین درخواست مصرف شد. */
    fun onActivityResult(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode != REQUEST_CONFIRM) return false
        val callback = pendingCallback.getAndSet(null)
        val ok = resultCode == Activity.RESULT_OK
        if (ok) markUnlocked()
        callback?.invoke(ok)
        return true
    }

    private fun biometricsReady(activity: Activity): Boolean = try {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            (activity.getSystemService(android.hardware.biometrics.BiometricManager::class.java))?.canAuthenticate(
                android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            ) == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS
    } catch (_: Exception) { false }

    private companion object {
        const val KEY_ENABLED = "app_lock_enabled"
        const val REQUEST_CONFIRM = 4711
        /** وضعیت «در همین اجرا باز شده»؛ با kill شدن فرایند به‌طور طبیعی صفر می‌شود. */
        val unlocked = AtomicBoolean(false)
        val pendingCallback = AtomicReference<((Boolean) -> Unit)?>(null)
    }
}
