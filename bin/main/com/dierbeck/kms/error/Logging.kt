package com.dierbeck.kms.error

import org.slf4j.LoggerFactory

// Extension function for logging
fun Any.logger() = LoggerFactory.getLogger(this::class.java)