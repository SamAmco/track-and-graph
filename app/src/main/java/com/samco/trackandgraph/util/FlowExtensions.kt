package com.samco.trackandgraph.util

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

fun <T, R> Flow<T>.flatMapLatestScan(
    initial: R,
    transform: suspend (R, T) -> Flow<R>
): Flow<R> = channelFlow {
    var innerFlow: Flow<R>?
    var lastElement: R = initial
    var job: Job? = null

    collect { value ->
        innerFlow = transform(lastElement, value)

        job?.cancel()

        coroutineScope {
            job = launch(start = CoroutineStart.UNDISPATCHED) {
                innerFlow?.collect { transformedValue ->
                    lastElement = transformedValue
                    send(transformedValue)
                }
            }
        }
    }
}

fun <T> Flow<T>.bufferWithTimeout(timeoutMillis: Long): Flow<List<T>> = channelFlow {
    val currentList = mutableListOf<T>()
    var delayedEmitJob: Job? = null

    collect { item ->
        if (delayedEmitJob?.isActive == true) delayedEmitJob?.cancel()

        currentList.add(item)

        delayedEmitJob = launch {
            delay(timeoutMillis)
            if (currentList.isNotEmpty()) send(currentList.toList())
            currentList.clear()
        }
    }
}
