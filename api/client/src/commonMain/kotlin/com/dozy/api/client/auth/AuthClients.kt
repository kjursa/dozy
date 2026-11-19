package com.dozy.api.client.auth

import com.dozy.api.client.auth.v1.AuthClient

interface AuthClients { val v1: AuthClient }

internal class DefaultAuthClients(override val v1: AuthClient) : AuthClients
