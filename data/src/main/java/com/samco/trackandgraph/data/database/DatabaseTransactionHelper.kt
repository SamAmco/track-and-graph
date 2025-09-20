package com.samco.trackandgraph.data.database

import androidx.room.withTransaction
import javax.inject.Inject

internal interface DatabaseTransactionHelper {
    suspend fun <R> withTransaction(block: suspend () -> R): R
}

internal class DatabaseTransactionHelperImpl @Inject constructor(
    private val database: TrackAndGraphDatabase
) : DatabaseTransactionHelper {
    override suspend fun <R> withTransaction(block: suspend () -> R): R =
        database.withTransaction(block)
}