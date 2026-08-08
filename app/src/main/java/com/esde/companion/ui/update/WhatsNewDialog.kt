package com.esde.companion.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.ReleaseInfo

/** Shown once per version bump, the first startup after that version was installed. */
@Composable
fun WhatsNewDialog(
    release: ReleaseInfo,
    onDismiss: () -> Unit,
) {
    val releaseNotesModifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What's new in v${release.versionName}") },
        text = {
            Column(modifier = releaseNotesModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(release.releaseNotes.ifBlank { "No release notes provided." })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
    )
}
