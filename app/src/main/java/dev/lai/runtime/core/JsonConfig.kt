package dev.lai.runtime.core

import kotlinx.serialization.json.Json

val LaiJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    isLenient = false
    prettyPrint = false
}
