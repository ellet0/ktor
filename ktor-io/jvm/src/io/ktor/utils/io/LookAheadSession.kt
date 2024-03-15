/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io

import io.ktor.utils.io.core.*
import java.nio.*

public typealias LookAheadSession = LookAheadSuspendSession

public class LookAheadSuspendSession(private val channel: ByteReadChannel) {
    private val buffer = ByteBuffer.allocate(4096)
    @OptIn(InternalAPI::class)
    public fun request(min: Int, max: Int): ByteBuffer? {
        if (channel.readBuffer.remaining < min) return null
        buffer.clear()
        channel.readBuffer.preview {
            it.readAvailable(buffer)
        }
        buffer.flip()
        return buffer
    }

    @OptIn(InternalAPI::class)
    public suspend fun awaitAtLeast(min: Int): Boolean {
        var result = true
        while (channel.readBuffer.remaining < min && result) {
            result = channel.awaitContent()
        }

        return result
    }

    @OptIn(InternalAPI::class)
    public fun consumed(count: Int) {
        channel.readBuffer.discard(count.toLong())
    }
}

public suspend fun ByteReadChannel.lookAhead(block: suspend LookAheadSuspendSession.() -> Unit) {
    block(LookAheadSuspendSession(this))
}

public suspend fun ByteReadChannel.lookAheadSuspend(block: suspend LookAheadSuspendSession.() -> Unit) {
    block(LookAheadSuspendSession(this))
}
