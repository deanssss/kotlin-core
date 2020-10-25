package xyz.dean.kotlin_core.async.coroutines.imitate

sealed class CoroutineState {
    private var disposableList: DisposableList = DisposableList.Nil

    fun from(oldState: CoroutineState) = apply {
        disposableList = oldState.disposableList
    }

    fun with(disposable: Disposable) = apply {
        disposableList = DisposableList.Cons(disposable, disposableList)
    }

    fun withOut(disposable: Disposable) = apply {
        disposableList = disposableList.remove(disposable)
    }

    fun <T> notifyCompletion(result: Result<T>) {
        this.disposableList.loopOn<CompletionHandlerDisposable<T>> {
            it.onComplete(result)
        }
    }

    fun clear() {
        disposableList = DisposableList.Nil
    }
}

class InComplete : CoroutineState()

class Complete<T>(
    val value: T? = null,
    val exception: Throwable? = null
) : CoroutineState()
