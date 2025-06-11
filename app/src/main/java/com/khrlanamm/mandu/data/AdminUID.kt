package com.khrlanamm.mandu.data

object AdminUID {
    private val adminIds = listOf(
        "azh3zjxgW6VmhzWjafGoZMXjhxV2",
        "yIY6GWhsnhcVAKNd5Dj7fIMayYz1"
    )

    fun isAdmin(userId: String?): Boolean {
        return userId != null && adminIds.contains(userId)
    }
}
