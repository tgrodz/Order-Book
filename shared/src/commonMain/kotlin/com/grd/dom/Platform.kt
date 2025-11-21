package com.grd.dom

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform