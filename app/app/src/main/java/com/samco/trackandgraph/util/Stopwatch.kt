package com.samco.trackandgraph.util

class Stopwatch {
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var isRunning = false

    fun start() {
        startTime = System.nanoTime()
        isRunning = true
    }

    fun stop() {
        endTime = System.nanoTime()
        isRunning = false
    }

    val elapsedMillis: Long
        get() = getElapsedTimeMillis()

    private fun getElapsedNanos(): Long {
        return if (isRunning) System.nanoTime() - startTime
        else endTime -startTime
    }

    private fun getElapsedTimeMillis(): Long {
        return getElapsedNanos() / 1000000
    }
}