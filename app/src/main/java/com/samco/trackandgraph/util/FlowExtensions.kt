package com.samco.trackandgraph.util

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
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