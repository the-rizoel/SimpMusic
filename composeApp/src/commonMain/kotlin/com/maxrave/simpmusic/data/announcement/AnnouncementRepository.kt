package com.maxrave.simpmusic.data.announcement

import com.maxrave.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

class AnnouncementRepository(
    private val feedUrl: String = DEFAULT_FEED_URL,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    suspend fun getActiveAnnouncement(
        currentAppVersion: String,
        dismissedIds: Set<String>,
    ): Announcement? {
        return runCatching {
            val client =
                HttpClient(CIO) {
                    install(HttpTimeout) {
                        requestTimeoutMillis = 8_000
                        connectTimeoutMillis = 6_000
                        socketTimeoutMillis = 8_000
                    }
                }
            try {
                val response = client.get(feedUrl).bodyAsText()
                val feed = json.decodeFromString<AnnouncementFeed>(response)
                if (!feed.enabled) {
                    null
                } else {
                    val now = Clock.System.now()
                    feed.announcements
                        .asSequence()
                        .filter { it.enabled }
                        .filter { it.display != Announcement.DISPLAY_SILENT }
                        .filter { it.isInsideDateWindow(now) }
                        .filter { it.matchesAppVersion(currentAppVersion) }
                        .filter { announcement ->
                            val isBlockingForceUpdate = announcement.isBlockingForceUpdate(currentAppVersion)
                            isBlockingForceUpdate || !announcement.showOnce || announcement.id !in dismissedIds
                        }
                        .sortedWith(
                            compareByDescending<Announcement> { it.isBlockingForceUpdate(currentAppVersion) }
                                .thenByDescending { it.priority },
                        ).firstOrNull()
                }
            } finally {
                client.close()
            }
        }.onFailure {
            Logger.w("AnnouncementRepository", "Failed to fetch announcements: ${it.message}")
        }.getOrNull()
    }

    private fun Announcement.isInsideDateWindow(now: Instant): Boolean {
        val from = showFrom?.safeInstantOrNull()
        val until = showUntil?.safeInstantOrNull()
        if (from != null && now < from) return false
        if (until != null && now > until) return false
        return true
    }

    private fun Announcement.matchesAppVersion(currentAppVersion: String): Boolean {
        if (minAppVersion != null && currentAppVersion.isOlderThanVersion(minAppVersion)) return false
        if (maxAppVersion != null && currentAppVersion.isNewerThanVersion(maxAppVersion)) return false
        return true
    }

    private fun String.safeInstantOrNull(): Instant? =
        runCatching { Instant.parse(this) }
            .recoverCatching { Instant.parse("${this}T00:00:00Z") }
            .getOrNull()

    companion object {
        const val DEFAULT_FEED_URL = "https://wavvy.rizoel.in/announcements.json"
    }
}
