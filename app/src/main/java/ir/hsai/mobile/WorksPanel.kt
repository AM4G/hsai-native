package ir.hsai.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * پنجرهٔ «کارها» — معادل کامل پنل «مرکز کارها» در وب‌سایت (ChatWorkPanels.jsx).
 *
 * شامل دسته‌بندی‌های مشاوره، ساختن برگه، حساب و پشتیبانی و ابزارهای تخصصی.
 */
fun Destination.symbol(): ImageVector = when (this) {
    Destination.CHAT -> NativeIcons.ChatBubbleOutline
    Destination.HISTORY -> NativeIcons.History
    Destination.DOCUMENTS -> NativeIcons.FileSignature
    Destination.SCANNER -> NativeIcons.DocumentScanner
    Destination.ACCOUNT -> NativeIcons.PersonOutline
}

fun Destination.description(): String = when (this) {
    Destination.CHAT -> "پرسیدن سؤال حقوقی و گرفتن پاسخ مستند"
    Destination.HISTORY -> "گفتگوهای پیشین و ادامهٔ آن‌ها"
    Destination.DOCUMENTS -> "ساختن و ویرایش اوراق قضایی"
    Destination.SCANNER -> "تحلیل عکس و تصویر مدرک"
    Destination.ACCOUNT -> "حساب، اشتراک، قفل برنامه و تنظیمات"
}

private data class CapabilityItem(
    val id: String,
    val group: String,
    val title: String,
    val note: String,
    val icon: ImageVector,
    val draft: String = "",
    val destination: Screen
)

@Composable
private fun CapabilityCard(item: CapabilityItem, onClick: () -> Unit) {
    val dark = LocalSiteDark.current
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp)
            .semantics { contentDescription = item.title + "؛ " + item.note },
        shape = SiteShape.Xl,
        color = Soft.card(),
        border = BorderStroke(1.dp, Soft.border())
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(38.dp).background(
                    if (dark) Color(0x1AFFFFFF) else Palette.Brass100,
                    RoundedCornerShape(10.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, null, Modifier.size(18.dp), tint = if (dark) Palette.Brass300 else Palette.Brass800)
            }
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                Text(item.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(NativeIcons.NorthWest, null, Modifier.size(16.dp), tint = Palette.Brass400)
        }
    }
}

/** یک ردیف کار در پنجرهٔ «کارها»؛ حد لمسی ۴۸dp و برچسب دسترس‌پذیری کامل. */
@Composable
private fun WorkRow(title: String, note: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .semantics { contentDescription = title + "؛ " + note },
        shape = SiteShape.Xl,
        color = Soft.well(),
        border = BorderStroke(1.dp, Soft.border())
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(NativeIcons.NorthWest, null, Modifier.size(16.dp), tint = Palette.Brass400)
        }
    }
}

@Composable
fun WorksPanel(s: AppUiState, vm: AppViewModel) {
    var activeGroup by rememberSaveable { mutableStateOf("all") }

    val capabilities = remember {
        listOf(
            CapabilityItem(
                id = "ask",
                group = "consult",
                title = "پرسش حقوقی",
                note = "هر سؤال، پاسخ روشن و گام‌به‌گام",
                icon = NativeIcons.ChatBubbleOutline,
                destination = Screen.CHAT
            ),
            CapabilityItem(
                id = "notice",
                group = "consult",
                title = "خواندن ابلاغیه ثنا یا پیامک",
                note = "عکس بفرست، مهلت و قدم بعدی را بگیر",
                icon = NativeIcons.ScanSearch,
                draft = "این ابلاغیه/پیامک دادگاه را بخوان و بگو چه مهلتی دارم و قدم بعدی چیست.",
                destination = Screen.SCANNER
            ),
            CapabilityItem(
                id = "contract-check",
                group = "consult",
                title = "بررسی قرارداد",
                note = "بندهای پرخطر و تعهد یک‌طرفه",
                icon = NativeIcons.FileSignature,
                draft = "این قرارداد را بررسی کن؛ بندهای پرخطر، تعهدات یک‌طرفه و جای خالی‌اش را بگو.",
                destination = Screen.SCANNER
            ),
            CapabilityItem(
                id = "petition",
                group = "doc",
                title = "دادخواست",
                note = "آمادهٔ ثبت در دفاتر خدمات قضایی",
                icon = NativeIcons.FileSignature,
                draft = "برای من دادخواست تنظیم کن. موضوع دعوا: … / خواهان: … / خوانده: … / خواسته و بهای آن: … / دلایل: …",
                destination = Screen.DOCUMENTS
            ),
            CapabilityItem(
                id = "complaint",
                group = "doc",
                title = "شکواییه",
                note = "شروع تعقیب کیفری",
                icon = NativeIcons.FileSignature,
                draft = "برای من شکواییه تنظیم کن. موضوع شکایت: … / شاکی: … / مشتکی‌عنه: … / تاریخ و محل وقوع: … / دلایل: …",
                destination = Screen.DOCUMENTS
            ),
            CapabilityItem(
                id = "statement",
                group = "doc",
                title = "اظهارنامه",
                note = "اخطار رسمی پیش از دادخواست",
                icon = NativeIcons.FileSignature,
                draft = "برای من اظهارنامه تنظیم کن. موضوع: … / مخاطب: … / خواسته: … / مهلت اعلامی: …",
                destination = Screen.DOCUMENTS
            ),
            CapabilityItem(
                id = "defense",
                group = "doc",
                title = "لایحهٔ دفاعیه",
                note = "پاسخ به ادعای طرف مقابل",
                icon = NativeIcons.FileSignature,
                draft = "برای من لایحهٔ دفاعیه بنویس. موضوع پرونده: … / ادعای طرف مقابل: … / دفاعیات و مستندات من: …",
                destination = Screen.DOCUMENTS
            ),
            CapabilityItem(
                id = "contract-doc",
                group = "doc",
                title = "قرارداد",
                note = "اجاره، فروش، کار، مشارکت…",
                icon = NativeIcons.FileSignature,
                draft = "برای من قرارداد تنظیم کن. نوع قرارداد: … / طرفین: … / موضوع: … / مبلغ و مدت: … / شروط مهم: …",
                destination = Screen.DOCUMENTS
            ),
            CapabilityItem(
                id = "subscribe",
                group = "account",
                title = "طرح‌ها و پرداخت",
                note = "مشاهدهٔ سهمیه، تمدید و تاریخچهٔ پرداخت",
                icon = NativeIcons.Crown,
                destination = Screen.PLANS
            ),
            CapabilityItem(
                id = "ticket",
                group = "account",
                title = "پشتیبانی",
                note = "پیام به پشتیبانی و پیگیری درخواست‌ها",
                icon = NativeIcons.SupportAgent,
                destination = Screen.TICKETS
            ),
            CapabilityItem(
                id = "account",
                group = "account",
                title = "تنظیمات حساب",
                note = "هویت، امنیت، رمز عبور",
                icon = NativeIcons.PersonOutline,
                destination = Screen.ACCOUNT
            )
        )
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // سربرگ شبیه ChatWorkPanels.jsx
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = SiteShape.Card,
            color = Soft.card(),
            border = BorderStroke(1.dp, Soft.border())
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "همهٔ کارهای حقوقی، یک‌جا در چت",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "لازم نیست از صفحهٔ دیگری استفاده کنید؛ هر کار را از همین‌جا شروع کنید — نتیجه و سابقه در همان گفتگو می‌ماند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // فیلتر دسته‌بندی‌ها عین وب‌سایت
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "all" to "همهٔ دسته‌ها",
                "consult" to "مشاوره و بررسی",
                "doc" to "ساختن برگه",
                "account" to "حساب و پشتیبانی",
                "tools" to "ابزارهای کمکی"
            ).forEach { (id, label) ->
                FilterChip(
                    selected = activeGroup == id,
                    onClick = { activeGroup = id },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Soft.card(),
                        labelColor = Soft.inkMuted(),
                        selectedContainerColor = if (LocalSiteDark.current) Palette.Brass400 else Palette.Navy950,
                        selectedLabelColor = if (LocalSiteDark.current) Palette.Navy950 else Palette.Brass300
                    )
                )
            }
        }

        // نمایش قابلیت‌های مرتبط با تب
        val visibleCapabilities = capabilities.filter { activeGroup == "all" || it.group == activeGroup }
        if (visibleCapabilities.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibleCapabilities.forEach { cap ->
                    CapabilityCard(cap) {
                        if (cap.draft.isNotBlank()) vm.setDraft(cap.draft)
                        vm.closeDialog()
                        vm.navigate(cap.destination)
                    }
                }
            }
        }

        // ابزارهای پیشرفته: مهلت‌ها، قوانین، محاسبه‌گرها، تنظیمات
        if (activeGroup == "all" || activeGroup == "tools") {
            if (activeGroup == "all") {
                HorizontalDivider(color = Soft.hairline(), modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    "ابزارهای کمکی و پیشرفته",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkRow("مهلت‌های من", "تقویم شمسی، سررسیدها و یادآور ۷، ۳ و ۱ روز پیش", NativeIcons.CalendarClock) {
                    vm.closeDialog(); vm.navigate(Screen.DEADLINES)
                }
                WorkRow("بانک قوانین", "جستجو در متن رسمی قوانین، آرای وحدت رویه و واژه‌نامه", NativeIcons.MenuBook) {
                    vm.closeDialog(); vm.navigate(Screen.LAWS)
                }
                WorkRow("محاسبه‌گرها", "۱۴ ابزار محاسبه مالی و قضایی (هزینه دادرسی، دیه، ارث و...)", NativeIcons.Calculate) {
                    vm.closeDialog(); vm.navigate(Screen.CALCULATORS)
                }
                WorkRow("تنظیمات", "قفل برنامه، اندازهٔ متن، داده‌های گوشی", NativeIcons.Settings) {
                    vm.closeDialog(); vm.navigate(Screen.SETTINGS)
                }
            }
        }
    }
}

/**
 * صفحهٔ قفل برنامه. عمداً هیچ محتوایی از پرونده‌ها روی این صفحه نیست (حتی پیش‌نمایش
 * گفتگوی قبلی) و تنها راه عبور، تأیید هویت خود دستگاه است.
 */
@Composable
fun LockScreen(note: String, onUnlock: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandEmblem(56.dp)
        Spacer(Modifier.height(18.dp))
        Text("حق‌سنج قفل است", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "برای دیدن گفتگوها، اوراق و تصاویر پرونده، هویت خود را با اثر انگشت یا رمز صفحه تأیید کنید.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Action("باز کردن با قفل دستگاه", false) { onUnlock() }
        if (note.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
