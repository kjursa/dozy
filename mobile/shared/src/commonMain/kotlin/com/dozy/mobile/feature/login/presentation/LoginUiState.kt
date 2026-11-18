package com.dozy.mobile.feature.login.presentation

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class LoginUiState(
    val tutorialSeen: Boolean = false,
    val email: String = "",
    val pin: String = "",
    @Transient val emailError: Boolean = false,
    @Transient val sendingPin: Boolean = false,
    @Transient val verifyingPin: Boolean = false,
    @Transient val loginError: Boolean = false,
)
