package com.lamadb.android.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.lamadb.android.R

enum class AppDestination(
    val labelRes: Int,
    val icon: ImageVector
) {
    Dashboard(R.string.nav_dashboard, Icons.Filled.Dashboard),
    Wiki(R.string.nav_wiki, Icons.AutoMirrored.Filled.MenuBook),
    Tasks(R.string.nav_tasks, Icons.Filled.Schedule),
    Health(R.string.nav_health, Icons.Filled.Favorite),
    Settings(R.string.nav_settings, Icons.Filled.Settings)
}
