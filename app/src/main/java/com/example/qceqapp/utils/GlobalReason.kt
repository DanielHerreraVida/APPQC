package com.example.qceqapp.utils
object GlobalReason {
    var reason: String? = null

    fun set(value: String?) {
        reason = value?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun clear() {
        reason = null
    }

    fun get(): String {
        return reason ?: "Not Inspected"
    }
}
