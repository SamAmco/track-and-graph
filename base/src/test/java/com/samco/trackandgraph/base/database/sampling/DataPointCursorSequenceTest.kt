package com.samco.trackandgraph.base.database.sampling

import android.database.Cursor
import com.nhaarman.mockitokotlin2.*
import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DataPointCursorSequenceTest {
    @Test
    fun `Test sequence iterates the whole database correctly`() {
        //PREPARE
        val dao = mock<TrackAndGraphDatabaseDao>()
        val featureId = 0L
        val backingData = (0..3).toList()
        val cursor = mock<Cursor>()
        var cursorPosition = 0
        var value = 0
        whenever(dao.getDataPointsCursor(any())).thenReturn(cursor)

        whenever(cursor.position).thenAnswer { cursorPosition }
        whenever(cursor.count).thenReturn(backingData.size)
        whenever(cursor.moveToNext()).thenAnswer {
            cursorPosition++
            true
        }
        whenever(cursor.getString(eq(0))).thenReturn("2022-09-14T21:30:41.432+01:00")
        whenever(cursor.getLong(eq(1))).thenReturn(featureId)
        whenever(cursor.getDouble(eq(2))).thenAnswer { (value++).toDouble() }
        whenever(cursor.getString(eq(3))).thenReturn("")
        whenever(cursor.getString(eq(4))).thenReturn("")

        val sequence = DataPointCursorSequence(cursor)

        //EXECUTE
        val iterator1 = sequence.iterator()
        val output1 = iterator1.asSequence().toList()
        val output2 = sequence.iterator().asSequence().toList()

        //VERIFY
        assertEquals(backingData, output1.map { it.value.toInt() })
        assertEquals(backingData, output2.map { it.value.toInt() })
        verify(cursor, times(backingData.size)).getDouble(eq(2))
        assertEquals(backingData.size, cursorPosition)
        assertFalse(iterator1.hasNext())
    }
}