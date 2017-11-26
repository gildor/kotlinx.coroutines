# Module kotlinx-coroutines-retrofit

Integration with [Retrofit](http://square.github.io/retrofit/) callback [Call](https://square.github.io/retrofit/2.x/retrofit/retrofit2/Call.html).

This integration can be used to 
If you looking for call adapter that allow to use Defferred please check this [pull request in Retrofit](https://github.com/square/retrofit/pull/2550) or [this library](https://github.com/tinsukE/retrofit-coroutines)

## How to use
There are three suspending extensions:

### `.await()`

Common await API that returns result or throw exception
```kotlin
fun Call<T>.await(): T
```

In case of HTTP error or invocation exception `await()` throws exception

```kotlin
// You can use retrofit suspended extension inside any coroutine block
fun main(args: Array<String>) = runBlocking {
    try {
        // Wait (suspend) for result
        val getUser: User = api.getUser("username").await()
        // Now we can work with result object
        println("User ${getUser.name} loaded")
    } catch (e: HttpException) {
        // Catch http errors
        println("exception${e.code()}", e)
    } catch (e: Throwable) {
        // All other exceptions (non-http)
        println("Something broken", e)
    }
}
```

### `.awaitResponse()`

Await API that returns [Response](https://square.github.io/retrofit/2.x/retrofit/retrofit2/Response.html) or throws exception
```kotlin
fun Call<T>.awaitResponse(): Response<T>
```

In case of exception `awaitResponse()` throws exception

```kotlin
// You can use retrofit suspended extension inside any coroutine block
fun main(args: Array<String>) = runBlocking {
    try {
        // Wait (suspend) for response
        val response: Response<User> = api.getUser("username").awaitResponse()
        if (response.isSuccessful()) {
          // Now we can work with response object
          println("User ${response.body().name} loaded")
        }
    } catch (e: Throwable) {
        // All other exceptions (non-http)
        println("Something broken", e)
    }
}
```

### `.awaitResult()`

API based on sealed class `Result`:

```kotlin
fun Call<T>.awaitResult(): Result<T>
```

```kotlin
fun main(args: Array<String>) = runBlocking {
    // Wait (suspend) for Result
    val result: Result<User> = api.getUser("username").awaitResult()
    // Check result type
    when (result) {
        //Successful HTTP result
        is Result.Ok -> saveToDb(result.value)
        // Any HTTP error
        is Result.Error -> log("HTTP error with code ${result.error.code()}", result.error)
        // Exception while request invocation
        is Result.Exception -> log("Something broken", e)
    }
}
```

Also, `Result` has a few handy extension functions that allow to avoid `when` block matching:

```kotlin
fun main(args: Array<String>) = runBlocking {
    val result: User = api.getUser("username").awaitResult()
    
    //Return value for success or null for any http error or exception
    result.getOrNull()
    
    //Return result or default value
    result.getOrDefault(User("empty-getUser"))
    
    //Return value or throw exception (HttpException or original exception)
    result.getOrThrow()
    //Also supports custom exceptions to override original ones
    result.getOrThrow(IlleagalStateException("User request failed"))
}
```

All `Result` classes also implemented one or both interfaces: `ResponseResult` and `ErrorResult`
You can use them for access to shared properties of different classes from `Result`
 
```kotlin
fun main(args: Array<String>) = runBlocking {
  val result: User = api.getUser("username").awaitResult()
  
  //Result.Ok and Result.Error both implement ResponseResult
  if (result is ResponseResult) {
      //And after smart cast you now have an access to okhttp3 Response property of result
      println("Result ${result.response.code()}: ${result.response.message()}")
  }
  
  //Result.Error and Result.Exception implement ErrorResult
  if (result is ErrorResult) {
      // Here yoy have an access to `exception` property of result
      throw result.exception
  }
}
```


## Nullable body

To prevent unexpected behavior with nullable body of response `Call<Body?>`
extensions `.await()` and `.awaitResult()` available only for 
non nullable `Call<Body>` or platform `Call<Body!>` body types:

```kotlin
fun main(args: Array<String>) = runBlocking {
  val user: Call<User> = api.getUser("username")
  val userOrNull: Call<User?> = api.getUserOrNull("username")
  
  // Doesn't work, because User is nullable
  // userOrNull.await()
    
  // Works for non-nullable type
  try {
      val result: User = user.await()  
  } catch (e: NullPointerException) {
      // If body will be null you will get NullPointerException
  }
  
  // You can use .awaitResult() to catch possible problems with nullable body
  val nullableResult = api.getUser("username").awaitResult().getOrNull()
  // But type of body should be non-nullable
  // api.getUserOrNull("username").awaitResult()
  
  // If you still want to use nullable body to clarify your api
  // use awaitResponse() instead:
  val responseBody: User? = userOrNull.awaitResponse().body()
}
``` 