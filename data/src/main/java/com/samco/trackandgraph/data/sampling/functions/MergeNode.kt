/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.data.sampling.functions

import com.samco.trackandgraph.data.database.dto.DataPoint
import timber.log.Timber
import java.util.PriorityQueue

private data class NodeDataPoint(
    val nodeId: Int,
    val dataPoint: DataPoint,
)

internal class MergeNode(
    private val downStreamSources: Map<Int, NodeRawDataSample?>
) : NodeRawDataSample() {

    override fun dispose() {
        downStreamSources.forEach { it.value?.dispose() }
    }

    override fun iterator(): Iterator<DataPoint> {
        val priorityQueue = PriorityQueue<NodeDataPoint>(
            compareByDescending { it.dataPoint.timestamp }
        )

        val iterators = downStreamSources.mapNotNull {
            val iterator = it.value?.iterator() ?: return@mapNotNull null
            it.key to iterator
        }.toMap()

        iterators.forEach {
            if (it.value.hasNext()) priorityQueue.add(NodeDataPoint(it.key, it.value.next()))
        }

        return object : Iterator<DataPoint> {
            override fun hasNext(): Boolean {
                return try {
                    priorityQueue.isNotEmpty()
                } catch (e: Throwable) {
                    Timber.e(e)
                    throw e
                }
            }

            override fun next(): DataPoint {
                return try {
                    val nodeDataPoint =
                        priorityQueue.poll() ?: throw NoSuchElementException("No more data points")
                    val iterator = iterators[nodeDataPoint.nodeId]
                        ?: throw NoSuchElementException("No iterator for node ${nodeDataPoint.nodeId}")
                    if (iterator.hasNext()) {
                        priorityQueue.add(NodeDataPoint(nodeDataPoint.nodeId, iterator.next()))
                    }
                    nodeDataPoint.dataPoint
                } catch (e: Throwable) {
                    Timber.e(e)
                    throw e
                }
            }
        }
    }
}
