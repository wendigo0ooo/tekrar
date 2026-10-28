package com.wendigo.tekrar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wendigo.tekrar.core.ShareText
import com.wendigo.tekrar.repo.AppSettings
import com.wendigo.tekrar.ui.LaunchRequest
import com.wendigo.tekrar.ui.TekrarNavHost
import com.wendigo.tekrar.ui.theme.TekrarTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val launchRequest = MutableStateFlow<LaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeIntent(intent)
        val container = (application as TekrarApp).container
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )
            val request by launchRequest.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                AppSettings.THEME_LIGHT -> false
                AppSettings.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }
            TekrarTheme(darkTheme = darkTheme) {
                TekrarNavHost(
                    container = container,
                    launchRequest = request,
                    onLaunchRequestConsumed = { launchRequest.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(StudyRequest.EXTRA_START_STUDY, false)) {
            val limit = intent.getIntExtra(
                StudyRequest.EXTRA_STUDY_LIMIT,
                StudyRequest.DEFAULT_LIMIT,
            )
            launchRequest.value = LaunchRequest(
                studyLimit = if (limit > 0) limit else StudyRequest.DEFAULT_LIMIT,
            )
            intent.removeExtra(StudyRequest.EXTRA_START_STUDY)
            intent.removeExtra(StudyRequest.EXTRA_STUDY_LIMIT)
            return
        }
        if (intent.action == Intent.ACTION_SEND) {
            val term = ShareText.extract(intent.getCharSequenceExtra(Intent.EXTRA_TEXT))
            if (term != null) {
                launchRequest.value = LaunchRequest(initialTerm = term)
            }
            intent.removeExtra(Intent.EXTRA_TEXT)
        }
    }
}
