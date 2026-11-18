package com.dozy.mobile.feature.login.presentation

import androidx.lifecycle.viewModelScope
import com.dozy.mobile.feature.login.domain.repository.LoginRepository
import com.kjursa.foundation.compose.mvi.MviViewModel
import com.kjursa.foundation.compose.mvi.StatePersistence
import kotlinx.coroutines.launch

class LoginViewModel(
    persistence: StatePersistence<LoginUiState>,
    private val repository: LoginRepository,
) : MviViewModel<LoginUiState, LoginAction, LoginEffect>(LoginUiState(), persistence) {
    override fun onAction(action: LoginAction) {
        when (action) {
            LoginAction.TutorialContinued -> {
                repository.markTutorialSeen()
                updateState { copy(tutorialSeen = true) }
                sendEffect(LoginEffect.OpenEmail)
            }
            is LoginAction.EmailChanged -> updateState { copy(email = action.value, emailError = false) }
            LoginAction.EmailSubmitted -> submitEmail()
            LoginAction.ContinueAsGuest -> {
                updateState { copy(tutorialSeen = true) }
                viewModelScope.launch {
                    repository.continueAsGuest()
                    sendEffect(LoginEffect.OpenMain)
                }
            }
            LoginAction.BackToEmail -> sendEffect(LoginEffect.ReturnToEmail)
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
            repository.sendPin(email)
            updateState { copy(sendingPin = false) }
            sendEffect(LoginEffect.OpenPin)
        }
    }

    private fun resendPin() {
        val email = uiState.value.email
        if (email.isBlank() || uiState.value.sendingPin) return
        updateState { copy(sendingPin = true) }
        viewModelScope.launch {
            repository.sendPin(email)
            updateState { copy(sendingPin = false) }
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
            updateState { copy(verifyingPin = false, pin = if (verified) pin else "") }
            if (verified) sendEffect(LoginEffect.OpenMain)
        }
    }

    private companion object {
        const val PIN_LENGTH = 9
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
