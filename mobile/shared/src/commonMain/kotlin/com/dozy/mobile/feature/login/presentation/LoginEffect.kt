package com.dozy.mobile.feature.login.presentation

sealed interface LoginEffect {
    data object OpenEmail : LoginEffect

    data object OpenPin : LoginEffect

    data object OpenMain : LoginEffect

    data object ReturnToEmail : LoginEffect
}
