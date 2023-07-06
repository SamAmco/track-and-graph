/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.samco.trackandgraph.graphstatview.factories

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.base.database.sampling.DataSample
import com.samco.trackandgraph.base.model.DataInteractor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.threeten.bp.OffsetDateTime

class PieChartDataFactoryTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dataInteractor = mock<DataInteractor>()

    private lateinit var uut: PieChartDataFactory

    private val graphStatId = 1L
    private val featureId = 2L

    private val graphOrStat = GraphOrStat(
        id = 1L,
        groupId = 1L,
        name = "name",
        type = GraphStatType.PIE_CHART,
        displayIndex = 1
    )

    private val feature = object : Feature {
        override val featureId: Long = this@PieChartDataFactoryTest.featureId
        override val name: String = "feature"
        override val groupId: Long = 1L
        override val displayIndex: Int = 1
        override val description: String = "description"
    }

    private val pieChart = PieChart(
        id = 1L,
        graphStatId = graphStatId,
        featureId = featureId,
        duration = null,
        endDate = null,
        sumByCount = false
    )

    @Before
    fun setUp() = runTest {
        uut = PieChartDataFactory(dataInteractor, testDispatcher)

        whenever(dataInteractor.getFeatureById(eq(featureId))).thenReturn(feature)
    }

    @Test
    fun `test sum by count`() = runTest {
        //PREPARE
        val dataSample = DataSample.fromSequence(
            sequenceOf(
                dataPoint(1.0, "label1"),
                dataPoint(1.0, "label1"),
                dataPoint(2.0, "label1"),
                dataPoint(2.0, "label2"),
                dataPoint(2.0, "label2"),
                dataPoint(3.0, "label3"),
                dataPoint(4.0, "label4"),
                dataPoint(4.0, "label4"),
            )
        ) {}

        whenever(dataInteractor.getDataSampleForFeatureId(eq(featureId))).thenReturn(dataSample)
        whenever(dataInteractor.getPieChartByGraphStatId(eq(graphStatId))).thenReturn(
            pieChart.copy(sumByCount = true)
        )

        //EXECUTE
        val data = uut.getViewData(graphOrStat)

        //VERIFY
        assertEquals(4, data.segments!!.size)
        assertEquals(
            listOf(3, 2, 1, 2).map { it * 100 / 8 },
            data.segments!!.map { it.value.toInt() }
        )
        assertEquals(
            listOf("label1", "label2", "label3", "label4"),
            data.segments!!.map { it.title }
        )
    }

    @Test
    fun `test sum by value`() = runTest {
        //PREPARE
        val dataSample = DataSample.fromSequence(
            sequenceOf(
                dataPoint(1.0, "label1"),
                dataPoint(1.0, "label1"),
                dataPoint(2.0, "label1"),
                dataPoint(2.0, "label2"),
                dataPoint(2.0, "label2"),
                dataPoint(3.0, "label3"),
                dataPoint(4.0, "label4"),
                dataPoint(4.0, "label4"),
            )
        ) {}

        whenever(dataInteractor.getDataSampleForFeatureId(eq(featureId))).thenReturn(dataSample)
        whenever(dataInteractor.getPieChartByGraphStatId(eq(graphStatId))).thenReturn(
            pieChart.copy(sumByCount = false)
        )

        //EXECUTE
        val data = uut.getViewData(graphOrStat)

        //VERIFY
        assertEquals(4, data.segments!!.size)
        assertEquals(
            listOf(4.0, 4.0, 3.0, 8.0).map { (it * 100 / 19).toInt() },
            data.segments!!.map { it.value.toInt() }
        )
        assertEquals(
            listOf("label1", "label2", "label3", "label4"),
            data.segments!!.map { it.title }
        )
    }

    @Test
    fun `test does not filter empty labels`() = runTest {
        //PREPARE
        val dataSample = DataSample.fromSequence(
            sequenceOf(
                dataPoint(1.0, "label1"),
                dataPoint(1.0, ""),
            )
        ) {}

        whenever(dataInteractor.getDataSampleForFeatureId(eq(featureId))).thenReturn(dataSample)
        whenever(dataInteractor.getPieChartByGraphStatId(eq(graphStatId))).thenReturn(
            pieChart.copy(sumByCount = false)
        )

        //EXECUTE
        val data = uut.getViewData(graphOrStat)

        //VERIFY
        assertEquals(2, data.segments!!.size)
        assertEquals(
            listOf(1, 1).map { it * 100 / 2 },
            data.segments!!.map { it.value.toInt() }
        )
        assertEquals(
            listOf("", "label1"),
            data.segments!!.map { it.title }
        )
    }

    private fun dataPoint(value: Double, label: String) = object : IDataPoint() {
        override val timestamp: OffsetDateTime = OffsetDateTime.now()
        override val value: Double = value
        override val label: String = label
    }
}