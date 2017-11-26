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

import kotlinx.coroutines.experimental.retrofit.util.errorResponse
import kotlinx.coroutines.experimental.retrofit.util.okHttpResponse
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException

class ResultTest {
    private val result = "result"
    private val default = "default"
    private val ok = Result.Ok(result, okHttpResponse())
    private val error = Result.Error(HttpException(errorResponse<Nothing>()), okHttpResponse(401))
    private val exception = Result.Exception(IllegalArgumentException())
    @Test
    fun getOrNull() {
        assertEquals(result, ok.getOrNull())
        assertNull(error.getOrNull())
        assertNull(exception.getOrNull())
    }

    @Test
    fun getOrDefault() {
        assertEquals(result, ok.getOrDefault(default))
        assertEquals(default, error.getOrDefault(default))
        assertEquals(default, exception.getOrDefault(default))
    }

    @Test
    fun getOrThrowOk() {
        assertEquals(result, ok.getOrThrow())
    }

    @Test(expected = HttpException::class)
    fun getOrThrowError() {
        error.getOrThrow()
    }

    @Test(expected = IllegalArgumentException::class)
    fun getOrThrowException() {
        exception.getOrThrow()
    }

    @Test(expected = IllegalStateException::class)
    fun getOrThrowCustomException() {
        exception.getOrThrow(IllegalStateException("Custom!"))
    }
}