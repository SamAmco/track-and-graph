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

package com.samco.trackandgraph.data.dependencyanalyser

import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.dependencyanalyser.queryresponse.GraphDependency
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DependencyAnalyserTest {

    private lateinit var mockDao: TrackAndGraphDatabaseDao
    private lateinit var dependencyAnalyser: DependencyAnalyser

    @Before
    fun setup() {
        mockDao = mock()
    }

    @Test
    fun `test simple graph dependency - single feature affects single graph`() = runTest {
        // Given: Feature 1 has a bar chart (graph 10)
        whenever(mockDao.getAllSingleDependencyGraphs()).thenReturn(listOf(
            GraphDependency(graphStatId = 10, featureId = 1)
        ))
        whenever(mockDao.getLineGraphGraphStatIds()).thenReturn(emptyList())
        whenever(mockDao.getLuaGraphGraphStatIds()).thenReturn(emptyList())
        whenever(mockDao.getFunctionFeatureIds()).thenReturn(emptyList())

        // When
        dependencyAnalyser = DependencyAnalyser.create(mockDao)
        val result = dependencyAnalyser.getDependentGraphs(1)

        // Then
        assertEquals(setOf(10L), result.graphStatIds)
    }

    @Test
    fun `test transitive dependency through function - feature affects function affects graph`() = runTest {
        // Given: 
        // - Feature 1 (tracker)
        // - Feature 2 (function that depends on feature 1)
        // - Graph 20 (depends on feature 2)
        whenever(mockDao.getAllSingleDependencyGraphs()).thenReturn(listOf(
            GraphDependency(graphStatId = 20, featureId = 2)
        ))
        whenever(mockDao.getLineGraphGraphStatIds()).thenReturn(emptyList())
        whenever(mockDao.getLuaGraphGraphStatIds()).thenReturn(emptyList())
        whenever(mockDao.getFunctionFeatureIds()).thenReturn(listOf(2L))
        whenever(mockDao.getFunctionInputFeatureIds(2L)).thenReturn(listOf(1L))

        // When
        dependencyAnalyser = DependencyAnalyser.create(mockDao)
        val result = dependencyAnalyser.getDependentGraphs(1)

        // Then: Updating feature 1 should affect graph 20 transitively through function feature 2
        assertEquals(setOf(20L), result.graphStatIds)
    }

    @Test
    fun `test multi-dependency graph - line graph depends on multiple features`() = runTest {
        // Given: Line graph 30 depends on features 1, 2, 3
        whenever(mockDao.getAllSingleDependencyGraphs()).thenReturn(emptyList())
        whenever(mockDao.getLineGraphGraphStatIds()).thenReturn(listOf(30L))
        whenever(mockDao.getLineGraphFeatureIds(30L)).thenReturn(listOf(1L, 2L, 3L))
        whenever(mockDao.getLuaGraphGraphStatIds()).thenReturn(emptyList())
        whenever(mockDao.getFunctionFeatureIds()).thenReturn(emptyList())

        // When
        dependencyAnalyser = DependencyAnalyser.create(mockDao)
        
        // Then: Each feature should affect the line graph
        assertEquals(setOf(30L), dependencyAnalyser.getDependentGraphs(1).graphStatIds)
        assertEquals(setOf(30L), dependencyAnalyser.getDependentGraphs(2).graphStatIds)
        assertEquals(setOf(30L), dependencyAnalyser.getDependentGraphs(3).graphStatIds)
    }

    @Test
    fun `test complex dependency chain - feature affects multiple graphs through different paths`() = runTest {
        // Given: Complex dependency scenario
        // - Feature 1 -> Bar chart 10 (direct)
        // - Feature 1 -> Function 2 -> Pie chart 20 (indirect through function)
        // - Feature 1 -> Line graph 30 (direct, multi-dependency)
        whenever(mockDao.getAllSingleDependencyGraphs()).thenReturn(listOf(
            GraphDependency(graphStatId = 10, featureId = 1), // Direct dependency
            GraphDependency(graphStatId = 20, featureId = 2)  // Function output
        ))
        whenever(mockDao.getLineGraphGraphStatIds()).thenReturn(listOf(30L))
        whenever(mockDao.getLineGraphFeatureIds(30L)).thenReturn(listOf(1L, 4L)) // Multi-dependency
        whenever(mockDao.getLuaGraphGraphStatIds()).thenReturn(emptyList())
        whenever(mockDao.getFunctionFeatureIds()).thenReturn(listOf(2L))
        whenever(mockDao.getFunctionInputFeatureIds(2L)).thenReturn(listOf(1L))

        // When
        dependencyAnalyser = DependencyAnalyser.create(mockDao)
        val result = dependencyAnalyser.getDependentGraphs(1)

        // Then: Feature 1 should affect all three graphs
        assertEquals(setOf(10L, 20L, 30L), result.graphStatIds)
    }

    @Test
    fun `test getFeaturesDependingOn - simple function dependency`() = runTest {
        // Given: Function feature 2 depends on feature 1
        whenever(mockDao.getAllSingleDependencyGraphs()).thenReturn(emptyList())
        whenever(mockDao.getLineGraphGraphStatIds()).thenReturn(emptyList())
        whenever(mockDao.getLuaGraphGraphStatIds()).thenReturn(emptyList())
        whenever(mockDao.getFunctionFeatureIds()).thenReturn(listOf(2L))
        whenever(mockDao.getFunctionInputFeatureIds(2L)).thenReturn(listOf(1L))

        // When
        dependencyAnalyser = DependencyAnalyser.create(mockDao)
        val result = dependencyAnalyser.getFeaturesDependingOn(1)

        // Then
        assertEquals(setOf(1L, 2L), result.featureIds)
    }

    @Test
    fun `test getFeaturesDependingOn - transitive function dependencies`() = runTest {
        // Given: Chain of function dependencies: 1 -> 2 -> 3
        whenever(mockDao.getAllSingleDependencyGraphs()).thenReturn(emptyList())
        whenever(mockDao.getLineGraphGraphStatIds()).thenReturn(emptyList())
        whenever(mockDao.getLuaGraphGraphStatIds()).thenReturn(emptyList())
        whenever(mockDao.getFunctionFeatureIds()).thenReturn(listOf(2L, 3L))
        whenever(mockDao.getFunctionInputFeatureIds(2L)).thenReturn(listOf(1L))
        whenever(mockDao.getFunctionInputFeatureIds(3L)).thenReturn(listOf(2L))

        // When
        dependencyAnalyser = DependencyAnalyser.create(mockDao)
        val result = dependencyAnalyser.getFeaturesDependingOn(1)

        // Then: Both feature 2 and 3 transitively depend on feature 1
        assertEquals(setOf(1L, 2L, 3L), result.featureIds)
    }

    @Test
    fun `test getOrphanedGraphs - graphs with no dependencies should be orphaned`() = runTest {
        // Given: Graph 40 has no dependencies (empty feature set)
        whenever(mockDao.getAllSingleDependencyGraphs()).thenReturn(listOf(
            GraphDependency(graphStatId = 10, featureId = 1), // Has dependency
            GraphDependency(graphStatId = 20, featureId = 2)  // Has dependency
        ))
        whenever(mockDao.getLineGraphGraphStatIds()).thenReturn(listOf(40L))
        whenever(mockDao.getLineGraphFeatureIds(40L)).thenReturn(emptyList()) // No dependencies
        whenever(mockDao.getLuaGraphGraphStatIds()).thenReturn(emptyList())
        whenever(mockDao.getFunctionFeatureIds()).thenReturn(emptyList())

        // When
        dependencyAnalyser = DependencyAnalyser.create(mockDao)
        val result = dependencyAnalyser.getOrphanedGraphs()

        // Then: Graph 40 should be orphaned
        assertTrue(result.graphStatIds.contains(40L))
    }

    @Test
    fun `test edge case - non-existent feature returns empty results`() = runTest {
        // Given: Only feature 1 exists
        whenever(mockDao.getAllSingleDependencyGraphs()).thenReturn(listOf(
            GraphDependency(graphStatId = 10, featureId = 1)
        ))
        whenever(mockDao.getLineGraphGraphStatIds()).thenReturn(emptyList())
        whenever(mockDao.getLuaGraphGraphStatIds()).thenReturn(emptyList())
        whenever(mockDao.getFunctionFeatureIds()).thenReturn(emptyList())

        // When
        dependencyAnalyser = DependencyAnalyser.create(mockDao)
        val graphResult = dependencyAnalyser.getDependentGraphs(999) // Non-existent feature
        val featureResult = dependencyAnalyser.getFeaturesDependingOn(999) // Non-existent feature

        // Then
        assertTrue(graphResult.graphStatIds.isEmpty())
        assertEquals(setOf(999L), featureResult.featureIds)
    }

    @Test
    fun `test cycle detection scenario - function dependencies form a cycle`() = runTest {
        // Given: Function dependencies that form a cycle
        // (this should be impossible, but let's handle it gracefully anyway)
        // Feature 1 -> Function 2 -> Function 3 -> Feature 1
        whenever(mockDao.getAllSingleDependencyGraphs()).thenReturn(emptyList())
        whenever(mockDao.getLineGraphGraphStatIds()).thenReturn(emptyList())
        whenever(mockDao.getLuaGraphGraphStatIds()).thenReturn(emptyList())
        whenever(mockDao.getFunctionFeatureIds()).thenReturn(listOf(2L, 3L, 1L))
        whenever(mockDao.getFunctionInputFeatureIds(2L)).thenReturn(listOf(1L))
        whenever(mockDao.getFunctionInputFeatureIds(3L)).thenReturn(listOf(2L))

        // When: Check what depends on feature 1 (for cycle detection)
        dependencyAnalyser = DependencyAnalyser.create(mockDao)
        val dependents = dependencyAnalyser.getFeaturesDependingOn(1)

        // Then: Features 2 and 3 depend on feature 1
        // So we not allow feature 1 to depend on features 1, 2, or 3
        assertEquals(setOf(1L, 2L, 3L), dependents.featureIds)
    }
}
