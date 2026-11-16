package com.dozy.api.client

sealed interface TechnicalError {
    data class Server(val status: Int) : TechnicalError
    data class Communication(val exception: Exception) : TechnicalError
}
