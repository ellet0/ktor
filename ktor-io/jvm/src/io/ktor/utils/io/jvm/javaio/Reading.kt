package io.ktor.utils.io.jvm.javaio

import io.ktor.utils.io.*
import io.ktor.utils.io.pool.*
import kotlinx.coroutines.*
import kotlinx.io.*
import kotlinx.io.Buffer
import kotlinx.io.IOException
import java.io.*
import java.nio.*
import kotlin.coroutines.*

/**
 * Open a channel and launch a coroutine to copy bytes from the input stream to the channel.
 * Please note that it may block your async code when started on [Dispatchers.Unconfined]
 * since [InputStream] is blocking on it's nature
 */
@OptIn(DelicateCoroutinesApi::class)
@Suppress("BlockingMethodInNonBlockingContext")
public fun InputStream.toByteReadChannel(
    context: CoroutineContext = Dispatchers.IO,
    pool: ObjectPool<ByteBuffer>
): ByteReadChannel = RawSourceChannel(asSource(), context)

/**
 * Open a channel and launch a coroutine to copy bytes from the input stream to the channel.
 * Please note that it may block your async code when started on [Dispatchers.Unconfined]
 * since [InputStream] is blocking on it's nature
 */
@OptIn(DelicateCoroutinesApi::class)
@Suppress("BlockingMethodInNonBlockingContext")
@JvmName("toByteReadChannelWithArrayPool")
public fun InputStream.toByteReadChannel(
    context: CoroutineContext = Dispatchers.IO,
    pool: ObjectPool<ByteArray> = ByteArrayPool
): ByteReadChannel = RawSourceChannel(asSource(), context)

internal class RawSourceChannel(
    private val source: RawSource,
    private val context: CoroutineContext
) : ByteReadChannel {
    private var closedToken: ClosedToken? = null
    private val buffer = Buffer()

    override val closedCause: Throwable?
        get() = closedToken?.cause

    override val isClosedForRead: Boolean
        get() = closedToken != null && buffer.exhausted()

    @InternalAPI
    override val readBuffer: Source
        get() = buffer

    override suspend fun awaitContent(): Boolean {
        closedCause?.let { throw it }

        withContext(context) {
            val result = source.readAtMostTo(buffer, Long.MAX_VALUE)
            if (result == -1L) {
                closedToken = ClosedToken(null)
            }
        }

        return closedToken != null
    }

    override fun cancel(cause: Throwable?) {
        if (closedToken != null) return
        source.close()
        closedToken = if (cause == null) {
            ClosedToken(null)
        } else {
            ClosedToken(IOException("Channel has been cancelled", cause))
        }
    }
}
