package com.dozy.mobile.navigation

/** Percent-encode UTF-8 route values, including delimiters and literal percent signs. */
fun encodeRouteArgument(value: String): String =
    buildString {
        val hex = "0123456789ABCDEF"
        value.encodeToByteArray().forEach { byte ->
            val code = byte.toInt() and 255
            if (code in 65..90 || code in 97..122 || code in 48..57 || code.toChar() in "-._~") {
                append(code.toChar())
            } else {
                append('%')
                append(hex[code shr 4])
                append(hex[code and 15])
            }
        }
    }
