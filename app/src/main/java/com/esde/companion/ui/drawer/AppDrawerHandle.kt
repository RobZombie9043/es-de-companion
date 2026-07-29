package com.esde.companion.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Purely the visual pill shown at the top of the App Drawer, indicating it can be
 * dragged closed. No gesture handling of its own - see MainScreen, which owns all
 * drawer open/close drag logic (both the whole-screen gesture and this handle's
 * dedicated one) so that logic stays in one place rather than split between here and
 * MainScreen depending on which zone a drag started in.
 */
@Composable
fun AppDrawerHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(6.dp)
                .background(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(3.dp)),
        )
    }
}