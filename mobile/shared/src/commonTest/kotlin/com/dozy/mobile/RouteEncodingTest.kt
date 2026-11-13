package com.dozy.mobile

import com.dozy.mobile.navigation.encodeRouteArgument
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteEncodingTest {
    @Test fun encodesDelimitersPercentUnicodeAndSpaces() {
        assertEquals("builtin%3Awoods%2Fa%3Fb%3Dc%26d%23e%25%20%C5%BC", encodeRouteArgument("builtin:woods/a?b=c&d#e% ż"))
        assertEquals("abc-._~012", encodeRouteArgument("abc-._~012"))
        assertEquals("%5B%22hero%2F1%22%5D", encodeRouteArgument("[\"hero/1\"]"))
    }
}
