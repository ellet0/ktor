/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.tests.utils.*
import io.ktor.util.*
import kotlin.test.*

class AttributesTest : ClientLoader(Int.MAX_VALUE) {
    @Test
    fun testKeepAttributes() = clientTests {
        val attrName = "my-key"

        config {
            install("attr-test") {
                receivePipeline.intercept(HttpReceivePipeline.After) {
                    println("Intercepting")
                    val attr = it.call.request.attributes[AttributeKey<String>(attrName)]

                    assertEquals("test-data", attr)
                }
            }
        }

        test { client ->
            println("Client: $client")
            val response = client.get("$TEST_SERVER/content/hello") {
                setAttributes {
                    put(
                        AttributeKey<String>(attrName),
                        "test-data"
                    )
                }
            }

            println("Response: $response")

            val body = response.body<String>()

            println("Done")

            assertEquals("hello", body)
        }
    }
}
