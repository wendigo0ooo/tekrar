package com.wendigo.tekrar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wendigo.tekrar.repo.AppSettings
import com.wendigo.tekrar.ui.TekrarNavHost
import com.wendigo.tekrar.ui.theme.TekrarTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TekrarApp).container
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )
            val darkTheme = when (settings.themeMode) {
                AppSettings.THEME_LIGHT -> false
                AppSettings.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }
            TekrarTheme(darkTheme = darkTheme) {
                TekrarNavHost(container)
            }
        }
    }
}
