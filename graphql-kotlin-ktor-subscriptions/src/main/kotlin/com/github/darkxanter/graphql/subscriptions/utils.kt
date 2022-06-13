package com.github.darkxanter.graphql.subscriptions

/**
 * This is the best cast saftey we can get with the generics
 */
@Suppress("UNCHECKED_CAST")
internal fun castToMapOfStringString(payload: Any?): Map<String, String> {
    if (payload != null && payload is Map<*, *> && payload.isNotEmpty()) {
        if (payload.keys.first() is String && payload.values.first() is String) {
            return payload as Map<String, String>
        }
    }

    return emptyMap()
}
