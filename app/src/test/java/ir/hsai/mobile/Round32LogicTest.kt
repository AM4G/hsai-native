package ir.hsai.mobile

import android.net.Uri
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * آزمون‌های منطق خالص دور ۳۲ (بدون رابط کاربری):
 *  • مسیرهای عمومی مهمان و گزارش خرابی در [ApiRoutes]
 *  • نگاشت پاسخ `/public/chat` و سهمیهٔ مهمان در [GuestChat]
 *  • یک درگاه پیوند ورودی در [DeepLinks]
 *  • پنج مقصد ناوبری و حذف سه صفحهٔ قدیمی از ناوبری در [Destination]
 *  • هدر نسخهٔ اپ در [AppInfo]
 *
 * این‌ها عمداً از UI جدا هستند: اگر روزی چیدمان عوض شود، این قراردادها باید ثابت بمانند.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class Round32LogicTest {

    // ── مسیرها ────────────────────────────────────────────────────────────────
    @Test fun guestAndCrashRoutesAreAllowed() {
        assertTrue(ApiRoutes.allowed("/public/chat", "POST"))
        assertTrue(ApiRoutes.allowed("/public/quota?device_id=abc", "GET"))
        assertTrue(ApiRoutes.allowed("/app/crash", "POST"))
        assertTrue(ApiRoutes.isPublic("/public/chat"))
        assertFalse(ApiRoutes.isPublic("/chat"))
    }

    @Test fun guestRoutesRejectWrongMethods() {
        assertFalse(ApiRoutes.allowed("/public/chat", "GET"))
        assertFalse(ApiRoutes.allowed("/public/quota", "POST"))
        assertFalse(ApiRoutes.allowed("/app/crash", "GET"))
        // مسیرهای خصوصی هم بدون نشست مجاز نمی‌شوند؛ فقط سه مسیر بالا عمومی‌اند.
        assertFalse(ApiRoutes.isPublic("/me"))
        assertFalse(ApiRoutes.allowed("/public/../me", "POST"))
    }

    // ── سهمیهٔ مهمان ──────────────────────────────────────────────────────────
    @Test fun guestQuotaIsMappedFromServerSnapshot() {
        val raw = JSONObject().put("guest", JSONObject().put("enabled", true).put("limit", 2).put("used", 1).put("remaining", 1))
        val quota = GuestChat.quota(raw)
        assertEquals(1, quota.optInt("remaining"))
        assertEquals(2, quota.optInt("limit"))
        assertTrue(GuestChat.quotaLabel(quota).contains("۱"))
    }

    @Test fun guestQuotaFallsBackWhenRemainingIsMissing() {
        val raw = JSONObject().put("guest", JSONObject().put("limit", 3).put("used", 2))
        assertEquals(1, GuestChat.quota(raw).optInt("remaining"))
    }

    @Test fun exhaustedGuestQuotaDoesNotShowNegativeRemaining() {
        val raw = JSONObject().put("guest", JSONObject().put("limit", 2).put("used", 9).put("remaining", -5))
        assertEquals(0, GuestChat.quota(raw).optInt("remaining"))
    }

    @Test fun disabledGuestModeIsExplainedNotHidden() {
        val raw = JSONObject().put("guest", JSONObject().put("enabled", false).put("limit", 0).put("used", 0).put("remaining", 0))
        assertTrue(GuestChat.quotaLabel(GuestChat.quota(raw)).contains("وارد شوید"))
    }

    @Test fun guestLimitCountsAsExhausted() {
        assertTrue(GuestChat.exhausted(402, ""))
        assertTrue(GuestChat.exhausted(429, ""))
        assertTrue(GuestChat.exhausted(400, "NEEDS_ACCOUNT"))
        assertFalse(GuestChat.exhausted(500, "INTERNAL"))
    }

    @Test fun guestHistoryIsTrimmedToLastMessages() {
        val messages = listOf(
            ChatMessage("user", "اول"),
            ChatMessage("assistant", "دوم"),
            ChatMessage("user", "سوم"),
            ChatMessage("assistant", "چهارم"),
            ChatMessage("user", "پنجم")
        )
        val history = GuestChat.history(messages, max = 3)
        assertEquals(3, history.length())
        assertEquals("سوم", history.getJSONObject(0).optString("content"))
        assertEquals("user", history.getJSONObject(2).optString("role"))
    }

    @Test fun guestReplyIsReadFromRawPublicPayload() {
        // پاسخ `/public/chat` پاکت `{ok,data}` ندارد؛ متن در `content` است.
        val raw = JSONObject()
            .put("content", "پاسخ آزمایشی مهمان")
            .put("suggestions", org.json.JSONArray().put("پیشنهاد بعدی"))
            .put("verification", JSONObject().put("mode", "guest_trial"))
            .put("guest", JSONObject().put("limit", 2).put("used", 1).put("remaining", 1))
        val message = GuestChat.message(raw)
        assertEquals("پاسخ آزمایشی مهمان", message.text)
        assertEquals("assistant", message.role)
        assertEquals(1, GuestChat.quota(raw).optInt("remaining"))
    }

    // ── پیوندهای ورودی ────────────────────────────────────────────────────────
    @Test fun paymentAppLinkIsRecognised() {
        assertEquals(DeepLinks.Kind.PAYMENT, DeepLinks.kind(Uri.parse("https://hsai.ir/payment/callback?Authority=A1&Status=OK")))
        assertEquals(DeepLinks.Kind.PAYMENT, DeepLinks.kind(Uri.parse("hsai://payment/callback?Status=OK")))
        assertEquals("OK", DeepLinks.paymentStatus(Uri.parse("https://hsai.ir/payment/callback?Status=OK")))
        assertEquals("A1", DeepLinks.paymentAuthority(Uri.parse("https://hsai.ir/payment/callback?Authority=A1")))
    }

    @Test fun foreignHostsAreRejected() {
        assertEquals(DeepLinks.Kind.NONE, DeepLinks.kind(Uri.parse("https://evil.example/payment/callback?Status=OK")))
        assertEquals(DeepLinks.Kind.NONE, DeepLinks.kind(Uri.parse("http://hsai.ir.example.com/payment/callback")))
        assertEquals(DeepLinks.Kind.NONE, DeepLinks.kind(Uri.parse("hsai://unknown/thing")))
        assertNull(DeepLinks.normalize(Uri.parse("https://evil.example/payment/callback")))
    }

    @Test fun appLinkIsNormalisedToTheSchemeTheAppAlreadyUnderstands() {
        val normalised = DeepLinks.normalize(Uri.parse("https://hsai.ir/payment/callback?Authority=A1&Status=OK"))
        assertNotNull(normalised)
        assertEquals("hsai", normalised!!.scheme)
        assertEquals("payment", normalised.host)
        assertEquals("A1", normalised.getQueryParameter("Authority"))
    }

    @Test fun authAndDeadlineLinksBothWork() {
        assertEquals(DeepLinks.Kind.AUTH, DeepLinks.kind(Uri.parse("https://hsai.ir/app/auth?nonce=abc")))
        assertEquals(DeepLinks.Kind.DEADLINES, DeepLinks.kind(Uri.parse("hsai://deadlines")))
        assertEquals("https://hsai.ir/payment/callback", DeepLinks.paymentCallbackUrl())
    }

    // ── ناوبری ────────────────────────────────────────────────────────────────
    @Test fun navigationHasExactlyFiveDestinations() {
        assertEquals(5, Destination.entries.size)
        assertEquals(
            setOf(Screen.CHAT, Screen.HISTORY, Screen.DOCUMENTS, Screen.SCANNER, Screen.ACCOUNT),
            Destination.entries.map { it.screen }.toSet()
        )
        assertNotNull(Destination.of(Screen.CHAT))
        assertNull(Destination.of(Screen.LAWS))
    }

    @Test fun removedScreensAreNoLongerDestinations() {
        assertTrue(Destination.isLegacy(Screen.LAWS))
        assertTrue(Destination.isLegacy(Screen.CALCULATORS))
        assertTrue(Destination.isLegacy(Screen.DEADLINES))
        assertFalse(Destination.isLegacy(Screen.CHAT))
        assertFalse(Destination.isLegacy(Screen.PLANS))
    }

    // ── نسخه در هدر ───────────────────────────────────────────────────────────
    @Test fun appHeaderCarriesVersionAndBuild() {
        assertTrue(AppInfo.HEADER.startsWith("hsai-native/"))
        assertTrue(AppInfo.HEADER.contains(AppInfo.VERSION_NAME))
        assertTrue(AppInfo.HEADER.contains(AppInfo.VERSION_CODE.toString()))
        assertEquals("android", AppInfo.PLATFORM)
    }

    // ── کانال پرداخت ──────────────────────────────────────────────────────────
    @Test fun storeBillingStaysDisabledUntilProductsExist() {
        assertFalse(PaymentChannels.FLAG_PLAY_BILLING_ENABLED)
        assertTrue(PaymentChannels.startStorePurchase("basic").isFailure)
        assertEquals("hsai_sub_yearly_basic", PaymentChannels.storeProductId("basic", true))
        assertTrue(PaymentChannels.refundNotice().contains("تمدید نمی‌شود"))
    }
}
