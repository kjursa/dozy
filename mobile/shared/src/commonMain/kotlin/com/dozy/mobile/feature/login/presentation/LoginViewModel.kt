package com.dozy.mobile.feature.login.presentation

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.feature.login.domain.repository.LoginRepository
import com.dozy.mobile.navigation.AppNavigator
import com.dozy.mobile.navigation.NavigationCommand
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.compose.mvi.StatePersistence
import kotlinx.coroutines.launch

class LoginViewModel(
    val navigator: AppNavigator,
    persistence: StatePersistence<LoginUiState>,
    private val repository: LoginRepository,
) : MviViewModel<LoginUiState, LoginAction, Nothing>(LoginUiState(), persistence) {
    override fun onAction(action: LoginAction) {
        when (action) {
            LoginAction.TutorialContinued -> {
                repository.markTutorialSeen()
                updateState { copy(tutorialSeen = true) }
                navigateTo(LoginDestination.OpenEmail)
            }
            is LoginAction.EmailChanged -> updateState { copy(email = action.value, emailError = false, loginError = false) }
            LoginAction.EmailSubmitted -> submitEmail()
            LoginAction.ContinueAsGuest -> {
                updateState { copy(tutorialSeen = true) }
                viewModelScope.launch {
                    if (repository.continueAsGuest()) navigateTo(LoginDestination.OpenMain)
                }
            }
            LoginAction.BackToEmail -> navigateTo(LoginDestination.ReturnToEmail)
            LoginAction.ResendPin -> resendPin()
            is LoginAction.PinDigit -> appendPinDigit(action.value)
            LoginAction.DeletePinDigit -> updateState { copy(pin = pin.dropLast(1)) }
            LoginAction.ClearPin -> updateState { copy(pin = "") }
        }
    }

    private fun submitEmail() {
        val email = uiState.value.email.trim()
        if (!EMAIL_PATTERN.matches(email)) {
            updateState { copy(emailError = true) }
            return
        }
        if (uiState.value.sendingPin) return
        updateState { copy(email = email, emailError = false, sendingPin = true, pin = "") }
        viewModelScope.launch {
            val sent = repository.sendPin(email)
            updateState { copy(sendingPin = false, loginError = !sent) }
            if (sent) navigateTo(LoginDestination.OpenPin)
        }
    }

    private fun resendPin() {
        val email = uiState.value.email
        if (email.isBlank() || uiState.value.sendingPin) return
        updateState { copy(sendingPin = true) }
        viewModelScope.launch {
            val sent = repository.sendPin(email)
            updateState { copy(sendingPin = false, loginError = !sent) }
        }
    }

    private fun appendPinDigit(digit: Int) {
        val state = uiState.value
        if (digit !in 0..9 || state.pin.length == PIN_LENGTH || state.verifyingPin) return
        val pin = state.pin + digit
        updateState { copy(pin = pin) }
        if (pin.length == PIN_LENGTH) verifyPin(pin)
    }

    private fun verifyPin(pin: String) {
        val email = uiState.value.email
        updateState { copy(verifyingPin = true) }
        viewModelScope.launch {
            val verified = repository.verifyPin(email, pin)
            updateState { copy(verifyingPin = false, pin = if (verified) pin else "", loginError = !verified) }
            if (verified) navigateTo(LoginDestination.OpenMain)
        }
    }

    private companion object {
        const val PIN_LENGTH = 9
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }

    private fun navigateTo(command: NavigationCommand) {
        viewModelScope.launch { navigator.navigate(command) }
    }
}
