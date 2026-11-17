package ir.hsai.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * پنجرهٔ «کارها» — دسترسی به ابزارهای جانبی و تخصصی برنامه.
 *
 * ناوبری درون‌برنامه‌ای به مهلت‌ها، بانک قوانین، محاسبه‌گرها،
 * طرح‌ها، پشتیبانی و تنظیمات با بستن دیالوگ و باز کردن صفحهٔ مربوطه.
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

/** یک ردیف کار در پنجرهٔ «کارها»؛ حد لمسی ۴۸dp و برچسب دسترس‌پذیری کامل. */
@Composable
private fun WorkRow(title: String, note: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .semantics { contentDescription = title + "؛ " + note },
        shape = SiteShape.Xl,
        color = Soft.well(),
        border = androidx.compose.foundation.BorderStroke(1.dp, Soft.border())
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
            Icon(NativeIcons.ExpandMore, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WorksPanel(s: AppUiState, vm: AppViewModel) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "مقصدهای اصلی همیشه در منو هستند (گفتگو، گفتگوهای من، اوراق، تحلیل مدرک، حساب). این بخش دسترسی به سایر ابزارهاست.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        WorkRow("مهلت‌های من", "تقویم شمسی، سررسیدها و یادآور ۷، ۳ و ۱ روز پیش", NativeIcons.CalendarClock) {
            vm.closeDialog(); vm.navigate(Screen.DEADLINES)
        }
        WorkRow("بانک قوانین", "جستجو در متن رسمی قوانین، آرای وحدت رویه و واژه‌نامه", NativeIcons.MenuBook) {
            vm.closeDialog(); vm.navigate(Screen.LAWS)
        }
        WorkRow("محاسبه‌گرها", "۱۴ ابزار محاسبه مالی و قضایی (هزینه دادرسی، دیه، ارث و...)", NativeIcons.Calculate) {
            vm.closeDialog(); vm.navigate(Screen.CALCULATORS)
        }
        WorkRow("طرح‌ها و پرداخت", "مشاهدهٔ سهمیه، تمدید و تاریخچهٔ پرداخت", NativeIcons.Crown) {
            vm.closeDialog(); vm.navigate(Screen.PLANS)
        }
        WorkRow("پشتیبانی", "پیام به پشتیبانی و پیگیری درخواست‌ها", NativeIcons.SupportAgent) {
            vm.closeDialog(); vm.navigate(Screen.TICKETS)
        }
        WorkRow("تنظیمات", "قفل برنامه، اندازهٔ متن، داده‌های گوشی", NativeIcons.Settings) {
            vm.closeDialog(); vm.navigate(Screen.SETTINGS)
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
