package com.esde.companion.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.RetroAchievementsCredentials

/**
 * Bundles the two credential text fields and their change handlers into one parameter,
 * keeping [RetroAchievementsSettingsContent]/[RetroAchievementsCredentialsForm] under
 * detekt's LongParameterList threshold - same reasoning as UISettingsContent's
 * DimAmountControl.
 */
internal data class RetroAchievementsCredentialsInput(
    val username: String,
    val onUsernameChanged: (String) -> Unit,
    val webApiKey: String,
    val onWebApiKeyChanged: (String) -> Unit,
)

/** Bundles the Connect action's transient state and handler - same reasoning as [RetroAchievementsCredentialsInput]. */
internal data class RetroAchievementsConnectStatus(
    val isConnecting: Boolean,
    val connectError: String?,
    val onConnectClicked: () -> Unit,
)

/** Bundles the "Update on Screensaver" toggle - same reasoning as [RetroAchievementsCredentialsInput]. */
internal data class RetroAchievementsScreensaverToggle(
    val enabled: Boolean,
    val onEnabledChanged: (Boolean) -> Unit,
)

@Composable
internal fun RetroAchievementsSettingsContent(
    credentials: RetroAchievementsCredentials?,
    input: RetroAchievementsCredentialsInput,
    connectStatus: RetroAchievementsConnectStatus,
    onSignOutClicked: () -> Unit,
    screensaverToggle: RetroAchievementsScreensaverToggle,
) {
    val columnModifier =
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    Column(
        modifier = columnModifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        RetroAchievementsConnectionStatus(credentials, onSignOutClicked)
        RetroAchievementsCredentialsForm(
            input = input,
            connectStatus = connectStatus,
        )
        ToggleSettingRow(
            icon = Icons.Filled.Nightlight,
            title = "Update on Screensaver",
            description = "Switch the achievement page to the screensaver's game while it's active.",
            enabled = screensaverToggle.enabled,
            onEnabledChanged = screensaverToggle.onEnabledChanged,
        )
    }
}

@Composable
private fun RetroAchievementsConnectionStatus(
    credentials: RetroAchievementsCredentials?,
    onSignOutClicked: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        val rowModifier = Modifier.fillMaxWidth().padding(16.dp)
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsLabel(
                icon = Icons.Filled.EmojiEvents,
                text = if (credentials != null) "Signed in as ${credentials.username}" else "Not signed in",
            )
            if (credentials != null) {
                TextButton(onClick = onSignOutClicked) { Text("Sign Out") }
            }
        }
    }
}

@Composable
private fun RetroAchievementsCredentialsForm(
    input: RetroAchievementsCredentialsInput,
    connectStatus: RetroAchievementsConnectStatus,
) {
    var webApiKeyVisible by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.EmojiEvents, text = "RetroAchievements Account")
            OutlinedTextField(
                value = input.username,
                onValueChange = input.onUsernameChanged,
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            val webApiKeyTransformation =
                if (webApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation()
            OutlinedTextField(
                value = input.webApiKey,
                onValueChange = input.onWebApiKeyChanged,
                label = { Text("Web API Key") },
                singleLine = true,
                visualTransformation = webApiKeyTransformation,
                trailingIcon = {
                    WebApiKeyVisibilityToggle(
                        visible = webApiKeyVisible,
                        onToggle = { webApiKeyVisible = !webApiKeyVisible },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (connectStatus.connectError != null) {
                Text(
                    text = connectStatus.connectError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = connectStatus.onConnectClicked,
                enabled = !connectStatus.isConnecting && input.username.isNotBlank() && input.webApiKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (connectStatus.isConnecting) {
                    val spinnerColor = MaterialTheme.colorScheme.onPrimary
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = spinnerColor)
                } else {
                    Text("Connect")
                }
            }
            GetWebApiKeyRow()
        }
    }
}

@Composable
private fun GetWebApiKeyRow() {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Get your Web API Key from RetroAchievements → Settings → Applications → Web API Key",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { uriHandler.openUri("https://retroachievements.org/settings?tab=applications") }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Launch,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Get API Key")
        }
    }
}

@Composable
private fun WebApiKeyVisibilityToggle(
    visible: Boolean,
    onToggle: () -> Unit,
) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (visible) "Hide Web API Key" else "Show Web API Key",
        )
    }
}
