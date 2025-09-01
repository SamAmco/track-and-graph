package com.samco.trackandgraph.data.model

sealed class DataUpdateType {
    /**Data point(s) were created/updated/deleted for a given feature**/
    data class DataPoint(val featureId: Long) : DataUpdateType()

    data object GroupCreated : DataUpdateType()
    data object GroupUpdated : DataUpdateType()
    data object GroupDeleted : DataUpdateType()

    interface FeatureUpdate

    data object TrackerCreated : DataUpdateType(), FeatureUpdate
    data object TrackerUpdated : DataUpdateType(), FeatureUpdate
    data object TrackerDeleted : DataUpdateType(), FeatureUpdate

    data class GraphOrStatCreated(val graphStatId: Long) : DataUpdateType()
    data class GraphOrStatUpdated(val graphStatId: Long) : DataUpdateType()
    data object GraphOrStatDeleted : DataUpdateType()

    /**A reminder was created/updated/deleted **/
    data object Reminder : DataUpdateType()

    /**A global note was created/updated/deleted **/
    data object GlobalNote : DataUpdateType()

    /**Display indices were updated **/
    data object DisplayIndex : DataUpdateType()

    data object FunctionCreated : DataUpdateType(), FeatureUpdate
    data object FunctionUpdated : DataUpdateType(), FeatureUpdate
    data object FunctionDeleted : DataUpdateType(), FeatureUpdate
}