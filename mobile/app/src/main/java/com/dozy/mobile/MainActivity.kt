package com.dozy.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.dozy.mobile.feature.story.presentation.generation.notificationStoryId
import com.dozy.mobile.ui.theme.DozyColorScheme
import com.dozy.mobile.ui.theme.DozyTheme

class MainActivity : ComponentActivity() {
    private var notificationStory by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationStory = notificationStoryId(intent.dataString)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationStory = notificationStoryId(intent.dataString)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = DozyColorScheme.background.toArgb()
        window.navigationBarColor = DozyColorScheme.background.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            DozyTheme {
                DozyApp(notificationStory) {
                    notificationStory = null
                    intent.data = null
                }
            }
        }
    }
}
