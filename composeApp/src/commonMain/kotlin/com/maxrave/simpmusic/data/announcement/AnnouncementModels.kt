package com.maxrave.simpmusic.data.announcement

import kotlinx.serialization.Serializable

@Serializable
data class AnnouncementFeed(
    val version: Int = 1,
    val enabled: Boolean = true,
    val announcements: List<Announcement> = emptyList(),
)

@Serializable
data class Announcement(
    val id: String,
    val enabled: Boolean = true,
    val type: String = TYPE_INFO,
    val display: String = DISPLAY_DIALOG,
    val priority: Int = 0,
    val showOnce: Boolean = true,
    val dismissible: Boolean = true,
    val cooldownHours: Int? = null,
    val title: String,
    val message: String,
    val imageUrl: String? = null,
    val imageAspectRatio: String? = null,
    val showFrom: String? = null,
    val showUntil: String? = null,
    val minAppVersion: String? = null,
    val maxAppVersion: String? = null,
    val forceUpdate: AnnouncementForceUpdate? = null,
    val actions: List<AnnouncementAction> = emptyList(),
) {
    fun isBlockingForceUpdate(currentAppVersion: String): Boolean =
        forceUpdate?.enabled == true &&
            forceUpdate.blockApp &&
            currentAppVersion.isOlderThanVersion(forceUpdate.minRequiredVersion)

    companion object {
        const val TYPE_INFO = "info"
        const val TYPE_SUCCESS = "success"
        const val TYPE_WARNING = "warning"
        const val TYPE_ERROR = "error"
        const val TYPE_FORCE_UPDATE = "force_update"
        const val TYPE_MAINTENANCE = "maintenance"
        const val TYPE_PROMO = "promo"

        const val DISPLAY_DIALOG = "dialog"
        const val DISPLAY_BANNER = "banner"
        const val DISPLAY_SILENT = "silent"
    }
}

@Serializable
data class AnnouncementForceUpdate(
    val enabled: Boolean = false,
    val minRequiredVersion: String = "0.0.0",
    val blockApp: Boolean = false,
)

@Serializable
data class AnnouncementAction(
    val id: String? = null,
    val text: String,
    val url: String? = null,
    val action: String? = null,
    val style: String = STYLE_SECONDARY,
    val dismissOnClick: Boolean = true,
    val order: Int = 0,
) {
    companion object {
        const val STYLE_PRIMARY = "primary"
        const val STYLE_SECONDARY = "secondary"
        const val STYLE_TEXT = "text"

        const val ACTION_DISMISS = "dismiss"
    }
}

internal fun String.isOlderThanVersion(other: String): Boolean = compareVersions(this, other) < 0

internal fun String.isNewerThanVersion(other: String): Boolean = compareVersions(this, other) > 0

private fun compareVersions(
    current: String,
    required: String,
): Int {
    val currentParts = current.normalizedVersionParts()
    val requiredParts = required.normalizedVersionParts()
    val max = maxOf(currentParts.size, requiredParts.size)

    for (index in 0 until max) {
        val left = currentParts.getOrNull(index) ?: 0
        val right = requiredParts.getOrNull(index) ?: 0
        if (left != right) return left.compareTo(right)
    }
    return 0
}

private fun String.normalizedVersionParts(): List<Int> =
    substringBefore("-")
        .split(".")
        .map { part -> part.filter { it.isDigit() }.toIntOrNull() ?: 0 }
