package com.samco.trackandgraph.data.model

import androidx.room.withTransaction
import com.samco.trackandgraph.data.database.TrackAndGraphDatabase
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