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

package kotlinx.coroutines.experimental.retrofit.util

import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response

class MockedCall<T>(
        val ok: T? = null,
        val error: HttpException? = null,
        val exception: Throwable? = null
) : Call<T> {
    private var executed: Boolean = false
    private var cancelled: Boolean = false

    override fun execute(): Response<T> {
        markAsExecuted()
        return when {
            ok != null -> Response.success(ok)
            error != null -> errorResponse(error.code())
            exception != null -> throw exception
            else -> throw IllegalStateException("Wrong MockedCall state")
        }
    }

    override fun enqueue(callback: Callback<T>) {
        markAsExecuted()
        when {
            ok != null -> callback.onResponse(this, Response.success(ok))
            error != null -> callback.onResponse(this, errorResponse(error.code()))
            exception != null -> callback.onFailure(this, exception)
        }
    }

    override fun isCanceled() = cancelled

    override fun isExecuted() = executed

    override fun clone(): Call<T> = throw IllegalStateException("Not mocked")

    override fun request(): Request = throw IllegalStateException("Not mocked")

    private fun markAsExecuted() {
        if (executed) throw IllegalStateException("Request already executed")
        executed = true
    }

    override fun cancel() {
        cancelled = true
    }

}

fun <T> errorResponse(code: Int = 400, message: String = "Error response $code"): Response<T> =
        Response.error(code, ResponseBody.create(MediaType.parse("text/plain"), message))

fun okHttpResponse(code: Int = 200): okhttp3.Response = okhttp3.Response.Builder()
        .code(code)
        .protocol(Protocol.HTTP_1_1)
        .message("mock response")
        .request(Request.Builder().url("http://localhost").build())
        .build()