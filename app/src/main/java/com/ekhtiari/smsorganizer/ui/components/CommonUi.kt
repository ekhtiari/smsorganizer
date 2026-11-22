package com.ekhtiari.smsorganizer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ekhtiari.smsorganizer.domain.MessageCategory
import java.text.DateFormat
import java.util.Date

private val AvatarPalette = listOf(
    Color(0xFFC4553A),
    Color(0xFFD97706),
    Color(0xFF0F766E),
    Color(0xFF7C3AED),
    Color(0xFFBE185D),
    Color(0xFF1D4ED8),
    Color(0xFFB45309),
    Color(0xFF047857),
    Color(0xFF9333EA),
    Color(0xFF0369A1),
)

/** Letters for a contact. Null when the label is a phone number or empty, so the avatar can show a person icon. */
fun contactInitials(name: String): String? {
    val trimmed = name.trim()
    if (trimmed.isEmpty() || trimmed.none { it.isLetter() }) return null
    val parts = trimmed.split(Regex("\\s+"))
        .filter { part -> part.firstOrNull()?.isLetter() == true }
    val raw = when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}"
        parts.size == 1 -> parts.first().first().toString()
        else -> trimmed.first { it.isLetter() }.toString()
    }
    return raw.uppercase()
}

fun avatarColor(name: String): Color {
    val key = name.trim().lowercase()
    if (key.isEmpty()) return AvatarPalette.first()
    val index = (key.hashCode() and Int.MAX_VALUE) % AvatarPalette.size
    return AvatarPalette[index]
}

fun categoryIcon(category: MessageCategory): ImageVector = when (category) {
    MessageCategory.PERSONAL -> Icons.Outlined.Person
    MessageCategory.TRANSACTIONS -> Icons.Outlined.AccountBalance
    MessageCategory.OFFERS -> Icons.Outlined.LocalOffer
    MessageCategory.OTP -> Icons.Outlined.Password
}

class UiHaptics(private val haptic: HapticFeedback) {
    fun tick() = haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    fun click() = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    fun confirm() = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberUiHaptics(): UiHaptics {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { UiHaptics(haptic) }
}

@Composable
fun AvatarCircle(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val initials = remember(name) { contactInitials(name) }
    val background = remember(name) { avatarColor(name) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (initials == null) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.52f),
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.36f).sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
fun CategoryPill(category: MessageCategory, modifier: Modifier = Modifier) {
    val (background, foreground) = when (category) {
        MessageCategory.PERSONAL ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        MessageCategory.TRANSACTIONS ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        MessageCategory.OFFERS ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        MessageCategory.OTP ->
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) to MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            maxLines = 1,
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    hint: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun TonalIcon(
    icon: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun warmFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
)

fun formatMessageTime(epochMs: Long): String {
    val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    return df.format(Date(epochMs))
}

fun formatRelativeTime(epochMs: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - epochMs
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    return when {
        diff < minute -> "Just now"
        diff < hour -> "${diff / minute}m"
        diff < day -> "${diff / hour}h"
        diff < 7 * day -> "${diff / day}d"
        else -> DateFormat.getDateInstance(DateFormat.SHORT).format(Date(epochMs))
    }
}

fun categoryChipLabel(category: MessageCategory): String = category.displayName
