@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    val destination: Screen,
    val badge: String = "",
    val badgeTone: String = ""
)

/** بج رنگی قابلیت — عین لایهٔ ۲۰۲۶ وب (ChatWorkPanels.jsx): بنفش/کهربایی/گلبهی/آبی. */
@Composable
private fun WorkBadge(text: String, tone: String) {
    val dark = LocalSiteDark.current
    val bg: Color
    val fg: Color
    when (tone) {
        "purple" -> { bg = if (dark) Color(0x29A78BFA) else Color(0xFFF3E8FF); fg = if (dark) Color(0xFFC4B5FD) else Color(0xFF6D28D9) }
        "amber" -> { bg = if (dark) Color(0x29FBBF24) else Color(0xFFFEF9C3); fg = if (dark) Color(0xFFFCD34D) else Color(0xFFA16207) }
        "rose" -> { bg = if (dark) Color(0x29FB7185) else Color(0xFFFFE4E6); fg = if (dark) Color(0xFFFDA4AF) else Color(0xFFBE123C) }
        "blue" -> { bg = if (dark) Color(0x2960A5FA) else Color(0xFFDBEAFE); fg = if (dark) Color(0xFF93C5FD) else Color(0xFF1D4ED8) }
        else -> { bg = if (dark) Color(0x2934D399) else Color(0xFFD1FAE5); fg = if (dark) Color(0xFF6EE7B7) else Color(0xFF047857) }
    }
    Surface(color = bg, shape = SiteShape.Chip) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = fg,
            maxLines = 1, softWrap = false)
    }
}

/** سربرگ دسته با شمارش قابلیت‌ها — مثل وب: نقطهٔ برنجی + عنوان + «N قابلیت». */
@Composable
private fun CategoryHeader(label: String, count: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(7.dp).background(Palette.Brass500, androidx.compose.foundation.shape.CircleShape))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.weight(1f))
        Text("${fa(count)} قابلیت", style = MaterialTheme.typography.labelSmall, color = Soft.inkMuted())
    }
}

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
                    if (dark) Color(0x14FFFFFF) else Palette.Slate100,
                    RoundedCornerShape(10.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, null, Modifier.size(18.dp), tint = if (dark) Palette.Slate200 else Palette.Navy700)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.title, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    if (item.badge.isNotBlank()) WorkBadge(item.badge, item.badgeTone)
                }
                Text(item.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(NativeIcons.ArrowLeft, null, Modifier.size(16.dp), tint = if (dark) Palette.Slate400 else Palette.Slate500)
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
            Icon(NativeIcons.ArrowLeft, null, Modifier.size(16.dp), tint = if (LocalSiteDark.current) Palette.Slate400 else Palette.Slate500)
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
                destination = Screen.SCANNER,
                badge = "تشخیص مهلت",
                badgeTone = "purple"
            ),
            CapabilityItem(
                id = "contract-check",
                group = "consult",
                title = "بررسی قرارداد",
                note = "بندهای پرخطر و تعهد یک‌طرفه",
                icon = NativeIcons.FileSignature,
                draft = "این قرارداد را بررسی کن؛ بندهای پرخطر، تعهدات یک‌طرفه و جای خالی‌اش را بگو.",
                destination = Screen.SCANNER,
                badge = "سپر امنیتی",
                badgeTone = "amber"
            ),
            CapabilityItem(
                id = "petition",
                group = "doc",
                title = "دادخواست",
                note = "آمادهٔ ثبت در دفاتر خدمات قضایی",
                icon = NativeIcons.FileSignature,
                draft = "برای من دادخواست تنظیم کن. موضوع دعوا: … / خواهان: … / خوانده: … / خواسته و بهای آن: … / دلایل: …",
                destination = Screen.DOCUMENTS,
                badge = "ثنا / A4",
                badgeTone = "amber"
            ),
            CapabilityItem(
                id = "complaint",
                group = "doc",
                title = "شکواییه",
                note = "شروع تعقیب کیفری",
                icon = NativeIcons.FileSignature,
                draft = "برای من شکواییه تنظیم کن. موضوع شکایت: … / شاکی: … / مشتکی‌عنه: … / تاریخ و محل وقوع: … / دلایل: …",
                destination = Screen.DOCUMENTS,
                badge = "دادسرا",
                badgeTone = "rose"
            ),
            CapabilityItem(
                id = "statement",
                group = "doc",
                title = "اظهارنامه",
                note = "اخطار رسمی پیش از دادخواست",
                icon = NativeIcons.FileSignature,
                draft = "برای من اظهارنامه تنظیم کن. موضوع: … / مخاطب: … / خواسته: … / مهلت اعلامی: …",
                destination = Screen.DOCUMENTS,
                badge = "اخطار رسمی",
                badgeTone = "blue"
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

    var panelTab by rememberSaveable { mutableStateOf("works") }
    var query by rememberSaveable { mutableStateOf("") }
    val dark = LocalSiteDark.current
    val openCap: (CapabilityItem) -> Unit = { cap ->
        if (cap.draft.isNotBlank()) vm.setDraft(cap.draft)
        vm.closeDialog()
        vm.navigate(cap.destination)
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // تب‌های وب: کارها / تیکت پشتیبانی (قرص سرمه‌ای با متن برنجی برای حالت فعال).
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("works" to "کارها", "tickets" to "تیکت پشتیبانی").forEach { (key, label) ->
                val on = panelTab == key
                Surface(
                    onClick = { panelTab = key }, shape = SiteShape.Chip,
                    color = if (on) Palette.Navy950 else Soft.card(),
                    contentColor = if (on) Palette.Brass300 else Soft.inkMuted(),
                    border = BorderStroke(1.dp, if (on) Palette.Navy950 else Soft.border())
                ) {
                    Text(label, Modifier.heightIn(min = 40.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        if (panelTab == "tickets") {
            Surface(Modifier.fillMaxWidth(), shape = SiteShape.Card, color = Soft.card(), border = BorderStroke(1.dp, Soft.border())) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(40.dp).background(Palette.Navy950, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(NativeIcons.LifeBuoy, null, Modifier.size(20.dp), tint = Palette.Brass300)
                        }
                        Column {
                            Text("پشتیبانی و تیکت‌ها", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                            Text("سؤال‌های حساب، پرداخت و خطاها را از همین‌جا پیگیری کنید.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    SoftButton(onClick = { vm.closeDialog(); vm.navigate(Screen.TICKETS) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(NativeIcons.LifeBuoy, null, Modifier.size(16.dp)); Text("تیکت‌های پشتیبانی")
                    }
                    SoftButton(onClick = { vm.openDialog("contact", "تماس با پشتیبانی") }, modifier = Modifier.fillMaxWidth()) {
                        Text("پیام به پشتیبانی (بدون حساب)")
                    }
                }
            }
            return@Column
        }

        // سربرگ وب: کاشی سرمه‌ای اسپارکل + عنوان + زیرنویس + جست‌وجوی سریع کارها.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = SiteShape.Card,
            color = Soft.card(),
            border = BorderStroke(1.dp, Soft.border())
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(40.dp).background(Palette.Navy950, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(NativeIcons.AutoAwesome, null, Modifier.size(20.dp), tint = Palette.Brass300)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "همهٔ کارهای حقوقی، یک‌جا در چت",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "هر کار را از همین‌جا انتخاب کنید؛ تنظیم برگه، خروجی Word/PDF و پاسخ مستند در همین گفتگو انجام می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("جست‌وجوی سریع کارها (دادخواست، ابلاغیه، چک، قرارداد…)", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(NativeIcons.Search, null, Modifier.size(18.dp)) },
                    shape = SiteShape.Xxl,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Soft.well(), focusedContainerColor = Soft.well(),
                        unfocusedBorderColor = Soft.border(), focusedBorderColor = Palette.Brass400,
                        cursorColor = Palette.Brass500
                    )
                )
            }
        }

        // فیلتر دسته‌بندی‌ها عین وب‌سایت — FlowRow تا چیپ «حساب و پشتیبانی» بریده نشود.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        selectedContainerColor = if (dark) Palette.Brass400 else Palette.Navy950,
                        selectedLabelColor = if (dark) Palette.Navy950 else Palette.Brass300
                    )
                )
            }
        }

        // نمایش قابلیت‌های مرتبط با تب + جست‌وجو؛ در حالت «همه» بخش‌بندی با شمارش مثل وب.
        val q = query.trim()
        val visibleCapabilities = capabilities.filter {
            (activeGroup == "all" || it.group == activeGroup) &&
                (q.isBlank() || it.title.contains(q) || it.note.contains(q))
        }
        if (visibleCapabilities.isEmpty()) {
            Text("کاری با این عبارت یافت نشد.", style = MaterialTheme.typography.bodySmall, color = Soft.inkMuted())
        } else if (activeGroup == "all" && q.isBlank()) {
            listOf("consult" to "مشاوره و بررسی", "doc" to "ساختن برگه", "account" to "حساب و پشتیبانی").forEach { (gid, glabel) ->
                val items = visibleCapabilities.filter { it.group == gid }
                if (items.isNotEmpty()) {
                    CategoryHeader(glabel, items.size)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items.forEach { cap -> CapabilityCard(cap) { openCap(cap) } }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibleCapabilities.forEach { cap -> CapabilityCard(cap) { openCap(cap) } }
            }
        }

        // ابزارهای پیشرفته: مهلت‌ها، قوانین، محاسبه‌گرها، تنظیمات
        if (activeGroup == "all" || activeGroup == "tools") {
            if (activeGroup == "all") {
                HorizontalDivider(color = Soft.hairline(), modifier = Modifier.padding(vertical = 4.dp))
                CategoryHeader("ابزارهای کمکی و پیشرفته", 4)
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
