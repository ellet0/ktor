/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io.jvm.javaio

import io.ktor.utils.io.*
import kotlinx.coroutines.*
import java.io.*
import kotlin.math.*

/**
 * Create blocking [java.io.InputStream] for this channel that does block every time the channel suspends at read
 * Similar to do reading in [runBlocking] however you can pass it to regular blocking API
 */
@OptIn(InternalAPI::class)
public fun ByteReadChannel.toInputStream(parent: Job? = null): InputStream = object : InputStream() {

    var count = 0
    override fun read(): Int {
        if (isClosedForRead) return -1
        if (readBuffer.exhausted()) blockingWait()

        if (isClosedForRead) return -1
        return readBuffer.readByte().toInt() and 0xff
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (isClosedForRead) return -1
        if (readBuffer.exhausted()) blockingWait()

        val count = min(availableForRead, len)
        val result = readBuffer.readAtMostTo(b, off, off + count)
        return result
    }

    private fun blockingWait() {
        runBlocking {
            awaitContent()
        }
    }

    override fun close() {
        cancel()
    }
}

/**
 * Create blocking [java.io.OutputStream] for this channel that does block every time the channel suspends at write
 * Similar to do reading in [runBlocking] however you can pass it to regular blocking API
 */
public fun ByteWriteChannel.toOutputStream(): OutputStream = TODO()
