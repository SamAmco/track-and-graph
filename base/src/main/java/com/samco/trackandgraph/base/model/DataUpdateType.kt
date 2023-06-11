package com.samco.trackandgraph.base.model

sealed class DataUpdateType {
    /**Data point(s) were created/updated/deleted for a given feature**/
    data class DataPoint(val featureId: Long) : DataUpdateType()

    object GroupCreated : DataUpdateType()
    object GroupUpdated : DataUpdateType()
    object GroupDeleted : DataUpdateType()

    object TrackerCreated : DataUpdateType()
    object TrackerUpdated : DataUpdateType()
    object TrackerDeleted : DataUpdateType()

    data class GraphOrStatCreated(val graphStatId: Long) : DataUpdateType()
    data class GraphOrStatUpdated(val graphStatId: Long) : DataUpdateType()
    object GraphOrStatDeleted : DataUpdateType()

    /**A reminder was created/updated/deleted **/
    object Reminder : DataUpdateType()

    /**A global note was created/updated/deleted **/
    object GlobalNote : DataUpdateType()

    /**Display indices were updated **/
    object DisplayIndex : DataUpdateType()

    /**A global note was created/updated/deleted **/
    //TODO split out function into separate CRUD events
    object Function : DataUpdateType()
}