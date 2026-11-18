package com.kjursa.foundation.presentation.navigation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/** Single-host command queue. Commands wait for a host; they are not persisted across process death. */
class Navigator<Command : Any> {
    private val channel = Channel<Command>(Channel.BUFFERED)
    val commands = channel.receiveAsFlow()

    suspend fun navigate(command: Command) {
        channel.send(command)
    }
}
