package com.samco.trackandgraph.base.model

sealed class DataUpdateType {
    /**A data point was created/updated/deleted **/
    object DataPoint : DataUpdateType()

    object GroupCreated : DataUpdateType()
    object GroupUpdated : DataUpdateType()
    object GroupDeleted : DataUpdateType()

    object TrackerCreated : DataUpdateType()
    object TrackerUpdated : DataUpdateType()
    object TrackerDeleted : DataUpdateType()

    object GraphOrStatCreated : DataUpdateType()
    object GraphOrStatUpdated : DataUpdateType()
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