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

package com.samco.trackandgraph.data.serialization

import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.database.dto.NodeDependency
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FunctionGraphMultiOutputSerializationTest {

    private val serializer = FunctionGraphSerializer(Json { ignoreUnknownKeys = false })

    private val primary = FunctionGraphNode.OutputNode(
        x = 1f, y = 2f, id = 10, dependencies = listOf(NodeDependency(0, 1))
    )
    private val secondary = FunctionGraphNode.OutputNode(
        x = 3f, y = 4f, id = 11, dependencies = listOf(NodeDependency(0, 2))
    )

    @Test
    fun `graph saved before multiple outputs existed deserializes with no additional outputs`() {
        // Exactly what older versions of the app wrote into functions_table.function_graph
        val legacyJson = """{"nodes":[],"outputNode":{"x":1.0,"y":2.0,"id":10,"dependencies":[]},"isDuration":false}"""

        val graph = serializer.deserialize(legacyJson, throwOnFailure = true)!!

        assertEquals(emptyList<FunctionGraphNode.OutputNode>(), graph.additionalOutputNodes)
        assertEquals(10, graph.outputNode.id)
        assertEquals(listOf(graph.outputNode), graph.allOutputNodes)
    }

    @Test
    fun `graph without additional outputs does not write the new field`() {
        val graph = FunctionGraph(nodes = emptyList(), outputNode = primary, isDuration = false)

        val json = serializer.serialize(graph, throwOnFailure = true)!!

        assertTrue(!json.contains("additionalOutputNodes"))
    }

    @Test
    fun `additional output nodes survive a serialization round trip`() {
        val graph = FunctionGraph(
            nodes = emptyList(),
            outputNode = primary,
            isDuration = true,
            additionalOutputNodes = listOf(secondary),
        )

        val json = serializer.serialize(graph, throwOnFailure = true)!!
        val result = serializer.deserialize(json, throwOnFailure = true)

        assertEquals(graph, result)
    }

    @Test
    fun `an output without its own duration flag uses the graph one`() {
        val graph = FunctionGraph(
            nodes = emptyList(), outputNode = primary, isDuration = true,
            additionalOutputNodes = listOf(secondary),
        )

        assertEquals(true, graph.isDurationFor(null))
        assertEquals(true, graph.isDurationFor(11))
    }

    @Test
    fun `an additional output can override the graph duration flag`() {
        val graph = FunctionGraph(
            nodes = emptyList(), outputNode = primary, isDuration = true,
            additionalOutputNodes = listOf(secondary.copy(isDuration = false)),
        )

        assertEquals(true, graph.isDurationFor(null))
        assertEquals(false, graph.isDurationFor(11))
    }

    @Test
    fun `an output duration flag survives a serialization round trip`() {
        val graph = FunctionGraph(
            nodes = emptyList(), outputNode = primary, isDuration = false,
            additionalOutputNodes = listOf(secondary.copy(isDuration = true)),
        )

        val result = serializer.deserialize(serializer.serialize(graph, throwOnFailure = true)!!, throwOnFailure = true)

        assertEquals(true, result!!.additionalOutputNodes.single().isDuration)
        assertEquals(null, result.outputNode.isDuration)
    }

    @Test
    fun `allOutputNodes lists the primary output first`() {
        val graph = FunctionGraph(
            nodes = emptyList(), outputNode = primary, isDuration = false,
            additionalOutputNodes = listOf(secondary),
        )

        assertEquals(listOf(primary, secondary), graph.allOutputNodes)
    }

    @Test
    fun `outputNodeFor returns the primary output for null`() {
        val graph = FunctionGraph(
            nodes = emptyList(), outputNode = primary, isDuration = false,
            additionalOutputNodes = listOf(secondary),
        )

        assertSame(primary, graph.outputNodeFor(null))
    }

    @Test
    fun `outputNodeFor finds an additional output by id`() {
        val graph = FunctionGraph(
            nodes = emptyList(), outputNode = primary, isDuration = false,
            additionalOutputNodes = listOf(secondary),
        )

        assertSame(secondary, graph.outputNodeFor(11))
        assertSame(primary, graph.outputNodeFor(10))
    }

    @Test
    fun `outputNodeFor throws for an unknown output id`() {
        val graph = FunctionGraph(nodes = emptyList(), outputNode = primary, isDuration = false)

        try {
            graph.outputNodeFor(99)
            fail("Expected an IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("99"))
        }
    }
}
