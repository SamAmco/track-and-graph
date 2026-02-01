package com.samco.trackandgraph.util

import com.samco.trackandgraph.data.database.dto.Feature
import junit.framework.Assert
import junit.framework.TestCase.assertEquals
import org.junit.Test

class FeaturePathProviderTest {

    private data class FeatureDto(
        override val featureId: Long,
        override val name: String,
        override val groupIds: Set<Long>,
    ) : Feature {
        constructor(featureId: Long, name: String, groupId: Long) : this(featureId, name, setOf(groupId))
        override val displayIndex: Int = 0
        override val description: String = ""
    }

    @Test
    fun test_feature_path_provider() {
        //PREPARE
        val groups = listOf(
            group(parentId = null),
            group("group1", 1),
            group("group1child1", 2, 1),
            group("group1child2", 3, 1),
        )

        val features = listOf(
            FeatureDto(0L, "Test", 0),
            FeatureDto(1L, "Test2", 1),
            FeatureDto(2L, "Test3", 2)
        )

        val provider = FeaturePathProvider(features, groups)

        //EXECUTE
        val ans0 = provider.getPathForFeature(0)
        val ans1 = provider.getPathForFeature(1)
        val ans2 = provider.getPathForFeature(2)

        //VERIFY
        Assert.assertEquals("/Test", ans0)
        Assert.assertEquals("/group1/Test2", ans1)
        Assert.assertEquals("/group1/group1child1/Test3", ans2)
    }

    @Test
    fun `feature path provider sorted feature map should be sorted alphabetically`() {
        val groups = listOf(
            group(parentId = null),
            group("Apple", 2, 0),
            group("Banana", 1, 0),
            group("Zebra", 3, 0),
        )

        val features = listOf(
            FeatureDto(0L, "Zebra", 0),
            FeatureDto(1L, "Apple", 1),
            FeatureDto(2L, "Banana", 2),
            FeatureDto(3L, "Carrot", 3),
            FeatureDto(4L, "Banana", 0),
        )

        val sorted = FeaturePathProvider(features, groups).sortedFeatureMap()

        assertEquals(
            listOf(
                2L to "/Apple/Banana",
                4L to "/Banana",
                1L to "/Banana/Apple",
                0L to "/Zebra",
                3L to "/Zebra/Carrot",
            ),
            sorted.map { it.key to it.value }
        )
    }

    @Test
    fun `a feature in a nested group with no other features in it gives a full path`() {
        val groups = listOf(
            group(parentId = null),
            group("Apple", 1, 0),
            group("Banana", 2, 1),
        )

        val features = listOf( FeatureDto(1L, "Carrot", 2))

        val sorted = FeaturePathProvider(features, groups).sortedFeatureMap()

        assertEquals(
            listOf(1L to "/Apple/Banana/Carrot"),
            sorted.map { it.key to it.value }
        )
    }

    @Test
    fun `feature in two groups with common prefix shows collapsed path`() {
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("b", 2, 1),
            group("c", 3, 1),
        )

        val features = listOf(
            FeatureDto(1L, "feat", setOf(2L, 3L))
        )

        val provider = FeaturePathProvider(features, groups)

        assertEquals("/a/.../feat", provider.getPathForFeature(1L))
    }

    @Test
    fun `feature in two groups with no common prefix shows ellipsis after root`() {
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("b", 2, 1),
            group("c", 3, 0),
            group("d", 4, 3),
        )

        val features = listOf(
            FeatureDto(1L, "feat", setOf(2L, 4L))
        )

        val provider = FeaturePathProvider(features, groups)

        assertEquals("/.../feat", provider.getPathForFeature(1L))
    }

    @Test
    fun `feature in multiple groups with longer common prefix`() {
        val groups = listOf(
            group(parentId = null),
            group("path", 1, 0),
            group("to", 2, 1),
            group("ga", 3, 2),
            group("gb", 4, 2),
        )

        val features = listOf(
            FeatureDto(1L, "feat", setOf(3L, 4L))
        )

        val provider = FeaturePathProvider(features, groups)

        assertEquals("/path/to/.../feat", provider.getPathForFeature(1L))
    }

    @Test
    fun `feature in three groups`() {
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("x", 2, 1),
            group("y", 3, 1),
            group("z", 4, 1),
        )

        val features = listOf(
            FeatureDto(1L, "feat", setOf(2L, 3L, 4L))
        )

        val provider = FeaturePathProvider(features, groups)

        assertEquals("/a/.../feat", provider.getPathForFeature(1L))
    }

    @Test
    fun `feature in groups with identical paths returns single path without ellipsis`() {
        val groups = listOf(
            group(parentId = null),
            group("same", 1, 0),
        )

        val features = listOf(
            FeatureDto(1L, "feat", setOf(1L))
        )

        val provider = FeaturePathProvider(features, groups)

        assertEquals("/same/feat", provider.getPathForFeature(1L))
    }

    @Test
    fun `feature with duplicate group id in set shows full path`() {
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("b", 2, 1),
        )

        // Set deduplicates, so this is effectively setOf(2L)
        val features = listOf(
            FeatureDto(1L, "feat", setOf(2L, 2L))
        )

        val provider = FeaturePathProvider(features, groups)

        assertEquals("/a/b/feat", provider.getPathForFeature(1L))
    }

    @Test
    fun `feature with duplicate group id plus another group shows collapsed path`() {
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("b", 2, 1),
            group("c", 3, 1),
        )

        // Set deduplicates, so this is effectively setOf(2L, 3L)
        val features = listOf(
            FeatureDto(1L, "feat", setOf(2L, 2L, 3L))
        )

        val provider = FeaturePathProvider(features, groups)

        assertEquals("/a/.../feat", provider.getPathForFeature(1L))
    }
}