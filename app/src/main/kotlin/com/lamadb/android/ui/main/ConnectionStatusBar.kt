package com.lamadb.android.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lamadb.android.R
import com.lamadb.android.network.ConnectionState

/**
 * A thin status indicator meant to sit in the system status-bar area.
 *
 * It reports the current dashboard/back-end connectivity state without
 * floating over the web dashboard's header actions (which caused the
 * theme toggle to be overlapped).
 */
@Composable
fun ConnectionStatusBar(
    state: ConnectionState,
    modifier: Modifier = Modifier
) {
    val color = when (state) {
        ConnectionState.Available -> MaterialTheme.colorScheme.primary
        ConnectionState.Unavailable -> MaterialTheme.colorScheme.error
    }
    val label = when (state) {
        ConnectionState.Available -> stringResource(R.string.dashboard_status_online)
        ConnectionState.Unavailable -> stringResource(R.string.dashboard_status_offline)
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(statusBarHeight)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.0f)),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
