package com.wendigo.tekrar.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wendigo.tekrar.BuildConfig
import com.wendigo.tekrar.R
import com.wendigo.tekrar.ui.components.Panel
import com.wendigo.tekrar.ui.components.ScreenHeader

/**
 * Profil sekmesi: istatistik, ayarlar ve AI koç ayarlarına giriş.
 * Alt menüde 4 sekme kalsın diye İstatistik/Ayarlar buradan açılır.
 */
@Composable
fun ProfileScreen(
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onAiSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.profile_title),
            subtitle = stringResource(R.string.profile_subtitle),
        )

        ProfileLink(
            title = stringResource(R.string.profile_stats),
            subtitle = stringResource(R.string.profile_stats_desc),
            onClick = onStats,
        )
        ProfileLink(
            title = stringResource(R.string.profile_settings),
            subtitle = stringResource(R.string.profile_settings_desc),
            onClick = onSettings,
        )
        ProfileLink(
            title = stringResource(R.string.profile_ai),
            subtitle = stringResource(R.string.profile_ai_desc),
            onClick = onAiSettings,
        )

        Text(
            text = stringResource(R.string.profile_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun ProfileLink(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Panel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = ">",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
