@file:Suppress("unused")

package app.darby.samples.core.domain

import io.reactivex.*

abstract class ObservableUseCase<P, R> {

    operator fun invoke(parameters: P): Observable<R> = execute(parameters)

    protected abstract fun execute(parameters: P): Observable<R>
}

abstract class FlowableUseCase<P, R> {

    operator fun invoke(parameters: P): Flowable<R> = execute(parameters)

    protected abstract fun execute(parameters: P): Flowable<R>
}

abstract class SingleUseCase<P, R> {

    operator fun invoke(parameters: P): Single<R> = execute(parameters)

    protected abstract fun execute(parameters: P): Single<R>
}

abstract class MaybeUseCase<P, R> {

    operator fun invoke(parameters: P): Maybe<R> = execute(parameters)

    protected abstract fun execute(parameters: P): Maybe<R>
}

abstract class CompletableUseCase<P> {

    operator fun invoke(parameters: P): Completable = execute(parameters)

    protected abstract fun execute(parameters: P): Completable
}
