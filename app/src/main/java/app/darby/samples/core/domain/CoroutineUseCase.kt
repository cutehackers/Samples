package app.darby.samples.core.domain

import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.jvm.Throws

/**
 * Coroutine UseCase with suspend function.
 *
 * in ViewModel class
 *
 * val getTasksUseCase = GetTasksUseCase(taskDataSource, Dispatchers.IO)
 *
 * viewModelScope.launch {
 *     val result: Result<List<Task>> = getTasksUseCase(GetTasksParams(user = 1234))
 *
 *     when (result) {
 *       result.isSuccess -> {
 *         val tasks: List<Task> = result.asSuccess.data
 *         tasksState.value = tasks
 *       }
 *       result.isError -> {
 *         val error = result.asError.exception
 *         // handle error
 *       }
 *       Result.Loading -> { }
 *     }
 * }
 *
 * ...
 *
 * class GetTasksUseCase(val taskDataSource: TaskDataSource, dispatcher: CoroutineDispatcher)
 *   : UseCase<GetTasksParams, List<Task>>(dispatcher) {
 *
 *   override suspend fun execute(parameters: GetTasksParams): List<Task> {
 *     // getTasks -> suspend fun TaskDataSource.getTasks(): List<Task>
 *     return taskDataSource.getTasks()
 *   }
 * }
 */
abstract class UseCase<in P, R>(private val dispatcher: CoroutineDispatcher) {

    suspend operator fun invoke(parameters: P): Result<R> {
        return try {
            withContext(dispatcher) {
                execute(parameters).let {
                    Result.Success(it)
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameters: P): R
}

/**
 * Coroutine UseCase with Flow
 */
abstract class FlowUseCase<in P, R>(private val dispatcher: CoroutineDispatcher) {
    operator fun invoke(parameters: P): Flow<Result<R>> = execute(parameters)
        .catch { e -> emit(Result.Error(Exception(e))) }
        .flowOn(dispatcher)

    protected abstract fun execute(parameters: P): Flow<Result<R>>
}

@Suppress("MemberVisibilityCanBePrivate")
abstract class MediatorUseCase<in P, R> {
    protected val result = MediatorLiveData<Result<R>>()

    // Make this as open so that mock instances can mock this method
    open fun observe(): MediatorLiveData<Result<R>> {
        return result
    }

    abstract fun execute(parameters: P)
}

// kotlin Result class 와 이름이 동일함.
sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception?) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

val Result<*>.isSuccess
    get() = this is Result.Success

val Result<*>.asSuccess
    get() = this as Result.Success

val Result<*>.isError
    get() = this is Result.Error

val Result<*>.asError
    get() = this as Result.Error

val Result<*>.succeeded
    get() = this is Result.Success && data != null

fun <T> Result<T>.successOr(fallback: T): T {
    return (this as? Result.Success<T>)?.data ?: fallback
}

val <T> Result<T>.data: T?
    get() = (this as? Result.Success)?.data
