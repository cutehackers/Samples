package app.darby.samples.coroutine

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

/**
 * [LoadingCoroutineScope] tied to its LifecycleCoroutineScope as a parent coroutine scope.
 */
public val LifecycleCoroutineScope.loadingScope: LoadingCoroutineScope
    get() {
        while (true) {
            val ref = scopeRef.get() as LoadingCoroutineScope?
            if (ref != null) {
                return ref
            }
            // 아래 코드가 올바른 형태인지 확인할 것. 맞다면 LoadingCoroutineScope의 파라메터로 주입한다.
            // superCoroutineContext + SupervisorJob() + Dispatchers.Main.immediate

            val superCoroutineContext = coroutineContext
            val newLoadingScope = LoadingCoroutineScope(superCoroutineContext + SupervisorJob() + Dispatchers.Main.immediate)
            if (scopeRef.compareAndSet(null, newLoadingScope)) {
                return newLoadingScope
            }
        }
    }

internal val LifecycleCoroutineScope.scopeRef: AtomicReference<Any> by lazy {
    AtomicReference<Any>()
}

// Job <unique tag as Key>
class LoadingCoroutineScope(override val coroutineContext: CoroutineContext) : CoroutineScope {
    init {
        val job = SupervisorJob()
    }
}