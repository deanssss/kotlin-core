package xyz.dean.kotlin_core.async.coroutines.dispatcher

object Dispatchers {
    val Default by lazy {
        DispatcherContext(DefaultDispatcher)
    }

    val Single by lazy {
        DispatcherContext(SingleDispatcher)
    }
}