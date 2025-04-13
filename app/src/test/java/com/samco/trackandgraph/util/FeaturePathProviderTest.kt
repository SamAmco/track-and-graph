package com.samco.trackandgraph.util

import com.samco.trackandgraph.base.database.dto.Feature
import junit.framework.Assert
import junit.framework.TestCase.assertEquals
import org.junit.Test

class FeaturePathProviderTest {

    private data class FeatureDto(
        override val featureId: Long,
        override val name: String,
        override val groupId: Long,
    ) : Feature {
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
}