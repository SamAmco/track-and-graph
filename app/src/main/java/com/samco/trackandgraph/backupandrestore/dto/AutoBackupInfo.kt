package com.samco.trackandgraph.backupandrestore.dto

import android.net.Uri
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.ChronoUnit

data class AutoBackupInfo(
    val uri: Uri,
    val nextScheduled: OffsetDateTime,
    val interval: Int,
    val units: ChronoUnit,
    val lastSuccessful: OffsetDateTime?
)