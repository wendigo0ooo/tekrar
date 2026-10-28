package com.wendigo.tekrar.ui

data class LaunchRequest(
    val studyLimit: Int? = null,
    val initialTerm: String? = null,
    val nonce: Long = System.nanoTime(),
)
