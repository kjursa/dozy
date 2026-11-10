package com.dozy.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.dozy.mobile.ui.theme.DozyColorScheme
import com.dozy.mobile.ui.theme.DozyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = DozyColorScheme.background.toArgb()
        window.navigationBarColor = DozyColorScheme.background.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            DozyTheme {
                DozyApp()
            }
        }
    }
}
