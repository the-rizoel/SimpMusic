package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.maxrave.simpmusic.data.announcement.Announcement
import com.maxrave.simpmusic.data.announcement.AnnouncementAction
import com.maxrave.simpmusic.ui.theme.typo

@Composable
@ExperimentalMaterial3Api
fun AnnouncementDialog(
    announcement: Announcement,
    currentAppVersion: String,
    onDismiss: () -> Unit,
    onActionClick: (AnnouncementAction) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val isBlockingForceUpdate = announcement.isBlockingForceUpdate(currentAppVersion)
    val visibleActions =
        (
            if (isBlockingForceUpdate && announcement.actions.isEmpty()) {
                listOf(
                    AnnouncementAction(
                        id = "update",
                        text = "Download update",
                        url = "https://wavvy.rizoel.in/download",
                        style = AnnouncementAction.STYLE_PRIMARY,
                        dismissOnClick = false,
                    ),
                )
            } else {
                announcement.actions
            }
        ).sortedBy { it.order }.take(3)
    val dismissible = announcement.dismissible && !isBlockingForceUpdate

    AlertDialog(
        properties =
            DialogProperties(
                dismissOnBackPress = dismissible,
                dismissOnClickOutside = dismissible,
            ),
        onDismissRequest = {
            if (dismissible) onDismiss.invoke()
        },
        title = {
            Text(
                text = announcement.title,
                style = typo().labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                announcement.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = announcement.title,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(announcement.imageAspectRatio.toFloatRatio())
                                .padding(bottom = 2.dp),
                    )
                }
                Text(
                    text = announcement.message,
                    textAlign = TextAlign.Center,
                    style = typo().bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (visibleActions.isEmpty()) {
                    if (dismissible) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Later", style = typo().bodySmall)
                        }
                    }
                } else {
                    visibleActions.forEach { action ->
                        AnnouncementActionButton(
                            action = action,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (action.url?.isNotBlank() == true) {
                                uriHandler.openUri(action.url)
                            }
                            onActionClick.invoke(action)
                        }
                    }
                    if (dismissible && visibleActions.none { it.action == AnnouncementAction.ACTION_DISMISS }) {
                        Spacer(Modifier.height(2.dp))
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Later", style = typo().bodySmall)
                        }
                    }
                }
            }
        },
        dismissButton = {},
    )
}

@Composable
private fun AnnouncementActionButton(
    action: AnnouncementAction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    when (action.style) {
        AnnouncementAction.STYLE_PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                shape = RoundedCornerShape(100.dp),
            ) {
                Text(action.text, style = typo().bodySmall)
            }
        }

        AnnouncementAction.STYLE_TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = modifier,
            ) {
                Text(action.text, style = typo().bodySmall)
            }
        }

        else -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                shape = RoundedCornerShape(100.dp),
            ) {
                Text(action.text, style = typo().bodySmall)
            }
        }
    }
}

private fun String?.toFloatRatio(): Float {
    val value = this ?: return 16f / 9f
    val parts = value.split(":")
    if (parts.size == 2) {
        val width = parts[0].toFloatOrNull()
        val height = parts[1].toFloatOrNull()
        if (width != null && height != null && height > 0f) return width / height
    }
    return value.toFloatOrNull()?.takeIf { it > 0f } ?: (16f / 9f)
}
