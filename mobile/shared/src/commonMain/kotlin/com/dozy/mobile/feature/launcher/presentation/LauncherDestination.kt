package com.dozy.mobile.feature.launcher.presentation

import com.dozy.mobile.navigation.NavigationCommand

sealed interface LauncherDestination : NavigationCommand {
    data object Home : LauncherDestination

    data object Welcome : LauncherDestination

    data object Tutorial : LauncherDestination

    data object Email : LauncherDestination
}
