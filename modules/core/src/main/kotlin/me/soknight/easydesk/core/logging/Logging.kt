@file:Suppress("NOTHING_TO_INLINE", "unused")

package me.soknight.easydesk.core.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import java.lang.invoke.MethodHandles

@JvmSynthetic inline fun getLogger(): Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
@JvmSynthetic inline fun getLogger(clazz: Class<*>): Logger = LoggerFactory.getLogger(clazz)
@JvmSynthetic inline fun getLogger(name: String): Logger = LoggerFactory.getLogger(name)

// -------------- MESSAGE ----------------------------------------------------------------------------------------------

@JvmSynthetic inline fun Logger.error(lazyMessage: () -> Any?) {
    if (isErrorEnabled) error(lazyMessage().toString())
}

@JvmSynthetic inline fun Logger.warn(lazyMessage: () -> Any?) {
    if (isWarnEnabled) warn(lazyMessage().toString())
}

@JvmSynthetic inline fun Logger.info(lazyMessage: () -> Any?) {
    if (isInfoEnabled) info(lazyMessage().toString())
}

@JvmSynthetic inline fun Logger.debug(lazyMessage: () -> Any?) {
    if (isDebugEnabled) debug(lazyMessage().toString())
}

@JvmSynthetic inline fun Logger.trace(lazyMessage: () -> Any?) {
    if (isTraceEnabled) trace(lazyMessage().toString())
}

// -------------- THROWABLE + MESSAGE ----------------------------------------------------------------------------------

@JvmSynthetic inline fun Logger.error(throwable: Throwable?, lazyMessage: () -> Any?) {
    if (isErrorEnabled) error(lazyMessage().toString(), throwable)
}

@JvmSynthetic inline fun Logger.warn(throwable: Throwable?, lazyMessage: () -> Any?) {
    if (isWarnEnabled) warn(lazyMessage().toString(), throwable)
}

@JvmSynthetic inline fun Logger.info(throwable: Throwable?, lazyMessage: () -> Any?) {
    if (isInfoEnabled) info(lazyMessage().toString(), throwable)
}

@JvmSynthetic inline fun Logger.debug(throwable: Throwable?, lazyMessage: () -> Any?) {
    if (isDebugEnabled) debug(lazyMessage().toString(), throwable)
}

@JvmSynthetic inline fun Logger.trace(throwable: Throwable?, lazyMessage: () -> Any?) {
    if (isTraceEnabled) trace(lazyMessage().toString(), throwable)
}