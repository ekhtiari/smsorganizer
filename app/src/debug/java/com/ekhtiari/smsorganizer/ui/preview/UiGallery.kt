package com.ekhtiari.smsorganizer.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import com.ekhtiari.smsorganizer.R
import com.ekhtiari.smsorganizer.domain.Conversation
import com.ekhtiari.smsorganizer.domain.MessageCategory
import com.ekhtiari.smsorganizer.domain.SimInfo
import com.ekhtiari.smsorganizer.domain.SmsMessage
import com.ekhtiari.smsorganizer.ui.compose.ComposeContent
import com.ekhtiari.smsorganizer.ui.conversation.ConversationContent
import com.ekhtiari.smsorganizer.ui.conversation.ConversationUiState
import com.ekhtiari.smsorganizer.ui.inbox.InboxContent
import com.ekhtiari.smsorganizer.ui.inbox.InboxUiState
import com.ekhtiari.smsorganizer.ui.settings.SettingsContent
import com.ekhtiari.smsorganizer.ui.theme.SmsOrganizerTheme
import com.ekhtiari.smsorganizer.ui.update.UpdatePromptDialog

private val Now = System.currentTimeMillis()

private val sampleSims = listOf(
    SimInfo(1, 0, "Hamrah", "Irancell", null, true),
    SimInfo(2, 1, "MCI", "MCI", null, false),
)

private val sampleConversations = listOf(
    Conversation(
        threadId = 1,
        address = "+14155550101",
        contactName = "Sara Ahmadi",
        snippet = "Are we still on for coffee tomorrow?",
        date = Now - 8 * 60_000L,
        unreadCount = 2,
        subscriptionId = 1,
        category = MessageCategory.PERSONAL,
    ),
    Conversation(
        threadId = 2,
        address = "+989121112233",
        contactName = "سارا محمدی",
        snippet = "سلام، فردا ساعت پنج می‌بینمت؟",
        date = Now - 45 * 60_000L,
        unreadCount = 0,
        subscriptionId = 1,
        category = MessageCategory.PERSONAL,
    ),
    Conversation(
        threadId = 3,
        address = "Bank",
        contactName = "City Bank",
        snippet = "Your verification code is 482193",
        date = Now - 2 * 3_600_000L,
        unreadCount = 1,
        subscriptionId = 2,
        category = MessageCategory.OTP,
        otpCode = "482193",
    ),
    Conversation(
        threadId = 4,
        address = "Shop",
        contactName = "Market",
        snippet = "Weekend sale: 30% off your next order",
        date = Now - 26 * 3_600_000L,
        unreadCount = 0,
        subscriptionId = 1,
        category = MessageCategory.OFFERS,
    ),
    Conversation(
        threadId = 5,
        address = "Txn",
        contactName = "Payments",
        snippet = "You paid 250,000 IRR to the bakery",
        date = Now - 3 * 86_400_000L,
        unreadCount = 0,
        subscriptionId = 1,
        category = MessageCategory.TRANSACTIONS,
    ),
)

private val sampleMessages = listOf(
    SmsMessage(
        id = 1,
        threadId = 2,
        address = "+989121112233",
        body = "سلام، فردا ساعت پنج می‌بینمت؟",
        date = Now - 50 * 60_000L,
        type = SmsMessage.TYPE_INBOX,
        read = true,
        subscriptionId = 1,
        category = MessageCategory.PERSONAL,
    ),
    SmsMessage(
        id = 2,
        threadId = 2,
        address = "+989121112233",
        body = "Yes — I'll be there at 5. Can't wait.",
        date = Now - 48 * 60_000L,
        type = SmsMessage.TYPE_SENT,
        read = true,
        subscriptionId = 1,
    ),
    SmsMessage(
        id = 3,
        threadId = 2,
        address = "+989121112233",
        body = "Perfect. I'll grab a table by the window.",
        date = Now - 47 * 60_000L,
        type = SmsMessage.TYPE_SENT,
        read = true,
        subscriptionId = 1,
    ),
    SmsMessage(
        id = 4,
        threadId = 2,
        address = "+989121112233",
        body = "کد تأیید شما ۴۸۲۱۹۳ است. آن را به کسی ندهید.",
        date = Now - 20 * 60_000L,
        type = SmsMessage.TYPE_INBOX,
        read = true,
        subscriptionId = 1,
        category = MessageCategory.OTP,
        otpCode = "482193",
    ),
)

private val inboxState = InboxUiState(
    hasPermissions = true,
    isDefaultSms = true,
    sims = sampleSims,
)

private val conversationState = ConversationUiState(
    threadId = 2,
    address = "+989121112233",
    displayName = "سارا محمدی",
    draft = "می‌بینمت",
    selectedSimId = 1,
    sims = sampleSims,
)

@Composable
private fun Frame(
    dark: Boolean,
    rtl: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        SmsOrganizerTheme(darkTheme = dark, dynamicColor = false, content = content)
    }
}

@Preview(name = "Inbox light", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun InboxLightPreview() = Frame(dark = false) {
    InboxContent(
        ui = inboxState,
        conversations = sampleConversations,
        onSearch = {},
        onCategory = {},
        onOpenThread = { _, _ -> },
        onCompose = {},
        onSettings = {},
        onArchive = {},
        onDelete = {},
        onCopyOtp = {},
        onRequestDefaultSms = {},
        onRequestPermissions = {},
        onOpenAppInfo = {},
        onOpenDefaultApps = {},
        onDismissPermission = {},
        onDismissDefaultSms = {},
    )
}

@Preview(name = "Inbox dark", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun InboxDarkPreview() = Frame(dark = true) { InboxLightPreviewBody() }

@Preview(name = "Inbox fa light", locale = "fa", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun InboxFaLightPreview() = Frame(dark = false, rtl = true) { InboxLightPreviewBody() }

@Preview(name = "Inbox fa dark", locale = "fa", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun InboxFaDarkPreview() = Frame(dark = true, rtl = true) { InboxLightPreviewBody() }

@Composable
internal fun InboxLightPreviewBody() {
    InboxContent(
        ui = inboxState,
        conversations = sampleConversations,
        onSearch = {},
        onCategory = {},
        onOpenThread = { _, _ -> },
        onCompose = {},
        onSettings = {},
        onArchive = {},
        onDelete = {},
        onCopyOtp = {},
        onRequestDefaultSms = {},
        onRequestPermissions = {},
        onOpenAppInfo = {},
        onOpenDefaultApps = {},
        onDismissPermission = {},
        onDismissDefaultSms = {},
    )
}

@Preview(name = "Inbox RTL", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun InboxRtlPreview() = Frame(dark = false, rtl = true) { InboxLightPreviewBody() }

@Preview(name = "Conversation light", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun ConversationLightPreview() = Frame(dark = false) { ConversationPreviewBody() }

@Preview(name = "Conversation dark", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun ConversationDarkPreview() = Frame(dark = true) { ConversationPreviewBody() }

@Preview(name = "Conversation RTL", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun ConversationRtlPreview() = Frame(dark = false, rtl = true) { ConversationPreviewBody() }

@Preview(name = "Conversation fa light", locale = "fa", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun ConversationFaLightPreview() = Frame(dark = false, rtl = true) { ConversationPreviewBody() }

@Preview(name = "Conversation fa dark", locale = "fa", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun ConversationFaDarkPreview() = Frame(dark = true, rtl = true) { ConversationPreviewBody() }

@Composable
internal fun ConversationPreviewBody() {
    ConversationContent(
        ui = conversationState,
        messages = sampleMessages,
        onBack = {},
        onDraftChange = {},
        onSelectSim = {},
        onSend = {},
        onDelete = {},
        onCopyOtp = {},
    )
}

@Preview(name = "Compose light", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun ComposeLightPreview() = Frame(dark = false) { ComposePreviewBody() }

@Preview(name = "Compose dark", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun ComposeDarkPreview() = Frame(dark = true) { ComposePreviewBody() }

@Preview(name = "Compose fa light", locale = "fa", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun ComposeFaLightPreview() = Frame(dark = false, rtl = true) { ComposePreviewBody() }

@Preview(name = "Compose fa dark", locale = "fa", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun ComposeFaDarkPreview() = Frame(dark = true, rtl = true) { ComposePreviewBody() }

@Composable
internal fun ComposePreviewBody() {
    ComposeContent(
        address = "+989121112233",
        onAddressChange = {},
        body = "سلام، ساعت پنج می‌رسم.",
        onBodyChange = {},
        sims = sampleSims,
        selectedSimId = 1,
        onSelectSim = {},
        error = null,
        onBack = {},
        onSend = {},
    )
}

@Preview(name = "Settings light", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun SettingsLightPreview() = Frame(dark = false) { SettingsPreviewBody() }

@Preview(name = "Settings dark", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun SettingsDarkPreview() = Frame(dark = true) { SettingsPreviewBody() }

@Preview(name = "Settings fa light", locale = "fa", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun SettingsFaLightPreview() = Frame(dark = false, rtl = true) { SettingsPreviewBody() }

@Preview(name = "Settings fa dark", locale = "fa", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun SettingsFaDarkPreview() = Frame(dark = true, rtl = true) { SettingsPreviewBody() }

@Composable
internal fun SettingsPreviewBody() {
    SettingsContent(
        isDefault = false,
        hasPerms = true,
        roleAvailable = true,
        simSummary = "SIM 1 · Irancell (subId=1)\nSIM 2 · MCI (subId=2)",
        versionLabel = stringResource(R.string.settings_version, "1.0.8", 8),
        updateStatus = stringResource(R.string.update_check_hint),
        checkingUpdate = false,
        onBack = {},
        onRequestDefaultSms = {},
        onRequestPermissions = {},
        onOpenAppInfo = {},
        onOpenDefaultApps = {},
        onReclassify = {},
        onCheckUpdate = {},
    )
}

@Preview(name = "Update light", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun UpdateLightPreview() = Frame(dark = false) { UpdatePreviewBody() }

@Preview(name = "Update dark", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun UpdateDarkPreview() = Frame(dark = true) { UpdatePreviewBody() }

@Preview(name = "Update fa light", locale = "fa", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun UpdateFaLightPreview() = Frame(dark = false, rtl = true) { UpdatePreviewBody() }

@Preview(name = "Update fa dark", locale = "fa", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun UpdateFaDarkPreview() = Frame(dark = true, rtl = true) { UpdatePreviewBody() }

@Composable
internal fun UpdatePreviewBody() {
    UpdatePromptDialog(
        versionName = "1.0.9",
        currentVersion = "1.0.8",
        directApk = true,
        onConfirm = {},
        onLater = {},
        onDismiss = {},
    )
}
