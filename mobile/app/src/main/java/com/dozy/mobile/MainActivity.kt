package com.dozy.mobile

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.dozy.mobile.feature.story.data.background.GenerationWorkScheduler
import com.dozy.mobile.feature.story.presentation.generation.notificationStoryId
import org.koin.android.ext.android.get

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
        window.statusBarColor =
            androidx.compose.ui.graphics
                .Color(0xFFFAF5EB)
                .toArgb()
        window.navigationBarColor =
            androidx.compose.ui.graphics
                .Color(0xFFFAF5EB)
                .toArgb()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            val permission =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) get<GenerationWorkScheduler>().checkNow()
                }
            DozyApp(
                notificationStory = notificationStory,
                backgroundCompletionSupported = true,
                requestNotifications =
                    if (Build.VERSION.SDK_INT >= 33) {
                        { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    } else {
                        null
                    },
                onNotificationHandled = {
                    notificationStory = null
                    intent.data = null
                },
            )
        }
    }
}
