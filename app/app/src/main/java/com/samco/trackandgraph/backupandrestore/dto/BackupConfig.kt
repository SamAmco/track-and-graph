package com.samco.trackandgraph.backupandrestore.dto

import android.net.Uri
import com.samco.trackandgraph.helpers.PrefHelper
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.ChronoUnit

data class BackupConfig(
    val uri: Uri,
    val firstDate: OffsetDateTime,
    val interval: Int,
    val units: ChronoUnit
) {
    companion object {
        private fun chronoUnitToPrefUnit(chronoUnit: ChronoUnit): PrefHelper.BackupConfigUnit =
            when (chronoUnit) {
                ChronoUnit.HOURS -> PrefHelper.BackupConfigUnit.HOURS
                ChronoUnit.DAYS -> PrefHelper.BackupConfigUnit.DAYS
                ChronoUnit.WEEKS -> PrefHelper.BackupConfigUnit.WEEKS
                else -> throw IllegalArgumentException("Invalid unit")
            }

        private fun prefUnitToChronoUnit(prefUnit: PrefHelper.BackupConfigUnit): ChronoUnit =
            when (prefUnit) {
                PrefHelper.BackupConfigUnit.HOURS -> ChronoUnit.HOURS
                PrefHelper.BackupConfigUnit.DAYS -> ChronoUnit.DAYS
                PrefHelper.BackupConfigUnit.WEEKS -> ChronoUnit.WEEKS
            }
    }

    constructor(prefHelperData: PrefHelper.BackupConfigData) : this(
        uri = prefHelperData.uri,
        firstDate = prefHelperData.firstDate,
        interval = prefHelperData.interval,
        units = prefUnitToChronoUnit(prefHelperData.units)
    )

    fun asPrefHelperData() = PrefHelper.BackupConfigData(
        uri = uri,
        firstDate = firstDate,
        interval = interval,
        units = chronoUnitToPrefUnit(units)
    )

}