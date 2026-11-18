package com.dozy.mobile.feature.login.presentation

import com.dozy.mobile.navigation.NavigationCommand

sealed interface LoginDestination : NavigationCommand {
    data object OpenEmail : LoginDestination

    data object OpenPin : LoginDestination

    data object OpenMain : LoginDestination

    data object ReturnToEmail : LoginDestination
}
