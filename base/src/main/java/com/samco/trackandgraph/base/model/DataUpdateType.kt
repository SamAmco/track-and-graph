package com.samco.trackandgraph.base.model

sealed class DataUpdateType {
    /**
     * A data point was added/updated/deleted
     */
    object DataPoint : DataUpdateType()

    /**
     * A group was added/updated
     */
    object Group : DataUpdateType()

    /**
     * A group was deleted
     */
    object GroupDeleted : DataUpdateType()

    /**
     * A tracker was added/updated
     */
    object Tracker : DataUpdateType()

    /**
     * A feature was deleted
     */
    object FeatureDeleted : DataUpdateType()

    /**
     * A graph was added/updated/deleted
     */
    object GraphOrStat : DataUpdateType()

    /**
     * A reminder was added/updated/deleted
     */
    object Reminder : DataUpdateType()

    /**
     * A global note was added/updated/deleted
     */
    object GlobalNote : DataUpdateType()

    /**
     * Display indexes were changed
     */
    object DisplayIndex : DataUpdateType()

    /**
     * A function was added/updated/deleted
     */
    object Function : DataUpdateType()
}