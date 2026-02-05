package com.bbm.multitask

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform