package com.samco.trackandgraph.time

import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

interface TimeProvider {
    fun now(): ZonedDateTime
    fun defaultZone(): ZoneId
}

class TimeProviderImpl @Inject constructor() : TimeProvider {
    override fun now(): ZonedDateTime = ZonedDateTime.now()
    
    override fun defaultZone(): ZoneId = ZonedDateTime.now().zone
}