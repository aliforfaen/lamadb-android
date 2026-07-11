package com.lamadb.android.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lamadb.android.data.api.DashboardHeaderResponse
import com.lamadb.android.data.api.LamaDBApiClient
import com.lamadb.android.data.api.TickerItemResponse
import kotlinx.coroutines.delay

private const val TICKER_REFRESH_INTERVAL_MS = 60_000L

/**
 * A thin scrolling ticker that displays ticker-flagged events from the
 * LamaDB dashboard header endpoint.
 *
 * Items scroll horizontally in a marquee. Breaking items are highlighted with
 * a small badge. If no ticker items exist, a neutral "all clear" message is shown.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardTicker(
    serverUrl: String,
    apiKey: String,
    modifier: Modifier = Modifier
) {
    val apiClient = remember(serverUrl, apiKey) {
        LamaDBApiClient(serverUrl = serverUrl, apiKey = apiKey)
    }

    var header by remember { mutableStateOf<DashboardHeaderResponse?>(null) }
    var loadError by remember { mutableStateOf(false) }

    LaunchedEffect(apiClient) {
        while (true) {
            apiClient.getDashboardHeader()
                .onSuccess { header = it; loadError = false }
                .onFailure { loadError = true }
            delay(TICKER_REFRESH_INTERVAL_MS)
        }
    }

    val items = header?.ticker
    val displayText = remember(items) {
        buildTickerText(items)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp)
            .basicMarquee(
                iterations = Int.MAX_VALUE,
                velocity = 30.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall,
            color = if (loadError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1
        )
    }
}

private fun buildTickerText(items: List<TickerItemResponse>?): String {
    if (items == null) return ""
    if (items.isEmpty()) {
        return "✓ All systems operational — no ticker items yet"
    }

    return items.joinToString("    •    ") { item ->
        val breaking = "breaking" in item.tags
        val prefix = when {
            breaking -> "[BREAKING] ${item.icon} "
            item.severity == "critical" || item.severity == "error" -> "${item.icon} "
            item.severity == "warn" -> "${item.icon} "
            else -> "${item.icon} "
        }
        "$prefix${item.title}"
    }
}
