package com.dozy.mobile.feature.login.presentation

sealed interface LoginAction {
    data object TutorialContinued : LoginAction

    data class EmailChanged(
        val value: String,
    ) : LoginAction

    data object EmailSubmitted : LoginAction

    data object ContinueAsGuest : LoginAction

    data object BackToEmail : LoginAction

    data object ResendPin : LoginAction

    data class PinDigit(
        val value: Int,
    ) : LoginAction

    data object DeletePinDigit : LoginAction

    data object ClearPin : LoginAction
}
