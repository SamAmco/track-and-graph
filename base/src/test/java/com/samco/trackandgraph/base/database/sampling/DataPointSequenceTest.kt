package com.samco.trackandgraph.base.database.sampling

import com.nhaarman.mockitokotlin2.*
import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.entity.DataPoint
import org.junit.Assert.assertEquals
import org.junit.Test
import org.threeten.bp.OffsetDateTime

class DataPointSequenceTest {
    @Test
    fun `Test sequence iterates the whole database correctly`() {
        //PREPARE
        val dao = mock<TrackAndGraphDatabaseDao>()
        val featureId = 0L
        val backingData = IntRange(0, 999).toList()
        whenever(dao.getNumberOfDataPointsForFeature(eq(featureId))).thenReturn(1000)
        whenever(dao.getDataPointsForFeatureSync(any(), any(), any())).thenAnswer { invocation ->
            val id = invocation.arguments[0] as Long
            assertEquals(0L, id)
            return@thenAnswer backingData
                .drop(invocation.arguments[1] as Int)
                .take(invocation.arguments[2] as Int)
                .map {
                    DataPoint(
                        OffsetDateTime.MAX,
                        id,
                        it.toDouble(),
                        "",
                        ""
                    )
                }
        }
        val sequence = DataPointSequence(dao, featureId)

        //EXECUTE
        val output = sequence.toList()

        //VERIFY
        verify(dao, times(13)).getDataPointsForFeatureSync(any(), any(), any())
        verify(dao, times(1)).getNumberOfDataPointsForFeature(eq(0L))
        assertEquals(
            backingData,
            output.map { it.value.toInt() }
        )

    }
}