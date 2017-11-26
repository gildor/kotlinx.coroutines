/*
 * Copyright 2016-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.coroutines.experimental.retrofit

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.retrofit.util.MockedCall
import kotlinx.coroutines.experimental.retrofit.util.NullBodyCall
import kotlinx.coroutines.experimental.retrofit.util.errorResponse
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException

private const val DONE = "Done!"

class CallAwaitTest {
    @Test
    fun asyncSuccess() = testBlocking {
        assertEquals(DONE, MockedCall(DONE).await())
    }

    @Test(expected = HttpException::class)
    fun asyncHttpException() = testBlocking {
        MockedCall<String>(error = HttpException(errorResponse<String>())).await()
    }

    @Test(expected = NullPointerException::class)
    fun asyncNullBody() = testBlocking {
        NullBodyCall<String>().await()
    }

    @Test(expected = IllegalArgumentException::class)
    fun asyncException() = testBlocking {
        MockedCall<String>(exception = IllegalArgumentException("wrong get param")).await()
    }

    @Test
    fun asyncResponseOk() = testBlocking {
        val result = MockedCall(DONE).awaitResponse()
        assertEquals(DONE, result.body())
    }

    @Test
    fun asyncResponseError() = testBlocking {
        val result = MockedCall<String>(error = HttpException(errorResponse<String>(500))).awaitResponse()
        assertEquals(500, result.code())
    }

    @Test
    fun asyncResponseNullBody() = testBlocking {
        val result = NullBodyCall<String>().awaitResponse()
        assertNull(result.body())
    }

    @Test
    fun asyncResponseNullableBody() = testBlocking {
        //Check that we can call awaitResponse() on nullable body
        val result = NullBodyCall<String?>().awaitResponse()
        assertNull(result.body())
    }

    @Test
    fun asyncResponseFailure() = testBlocking {
        val exception = IllegalStateException()
        try {
            MockedCall<String>(exception = exception).awaitResult()
        } catch (e: Exception) {
            assertSame(e, exception)
        }
    }

    @Test
    fun asyncResultOk() = testBlocking {
        val result = MockedCall(DONE).awaitResult()
        when (result) {
            is Result.Ok -> {
                assertEquals(DONE, result.value)
                assertEquals(200, result.response.code())
            }
            is Result.Error, is Result.Exception -> fail()
            else -> fail()
        }
    }

    @Test
    fun asyncResultNullBody() = testBlocking {
        val result = NullBodyCall<String>().awaitResult()
        assertNull(result.getOrNull())
    }

    @Test(expected = NullPointerException::class)
    fun asyncResultNullPointerForNullBody() = testBlocking {
        val result = NullBodyCall<String>().awaitResult()
        assertNull(result.getOrThrow())
    }

    @Test
    fun asyncResultByType() = testBlocking {
        val result = MockedCall(DONE).awaitResult()
        when (result) {
            is ResponseResult -> {
                assertEquals(200, result.response.code())
            }
            is ErrorResult -> fail()
            else -> fail()
        }
    }

    @Test
    fun resultOkTypes() = testBlocking {
        val result = MockedCall(DONE).awaitResult()
        if (result is ResponseResult) {
            // Mocked raw response doesn't contain body, but we can check code
            assertTrue(result.response.isSuccessful)
        }

        assertFalse(result is ErrorResult)
    }

    @Test
    fun resultErrorTypes() = testBlocking {
        val errorResponse = errorResponse<String>(500)
        val httpException = HttpException(errorResponse)
        val errorResult = MockedCall<String>(error = httpException).awaitResult()

        if (errorResult is ResponseResult) {
            assertEquals(500, errorResult.response.code())
        } else {
            fail()
        }

        if (errorResult is ErrorResult) {
            assertEquals(httpException.toString(), errorResult.exception.toString())
        } else {
            fail()
        }
    }

    @Test
    fun resultExceptionTypes() = testBlocking {
        val exception = IllegalStateException()
        val errorResult = MockedCall<String>(exception = exception).awaitResult()

        if (errorResult is ErrorResult) {
            assertEquals(exception, errorResult.exception)
        }

        assertFalse(errorResult is ResponseResult)
    }


    @Test
    fun asyncResultError() = testBlocking {
        val error = HttpException(errorResponse<String>(500))
        val result = MockedCall<String>(error = error).awaitResult()
        when (result) {
            is Result.Error -> {
                assertEquals(error.code(), result.exception.code())
                assertEquals(error.message, result.exception.message)
                assertEquals("Error response 500", result.exception.response().errorBody()?.string())
                assertEquals(500, result.response.code())
            }
            is Result.Ok, is Result.Exception -> fail()
            else -> fail()
        }
    }

    @Test
    fun asyncResultException() = testBlocking {
        val exception = IllegalArgumentException("wrong argument")
        val result = MockedCall<String>(exception = exception).awaitResult()
        when (result) {
            is Result.Exception -> {
                assertEquals(exception::class.java, result.exception::class.java)
                assertEquals(exception.message, result.exception.message)
            }
            is Result.Ok, is Result.Error -> fail()
            else -> fail()
        }
    }

}

private fun testBlocking(block: suspend CoroutineScope.() -> Unit) {
    runBlocking(Unconfined, block)
}