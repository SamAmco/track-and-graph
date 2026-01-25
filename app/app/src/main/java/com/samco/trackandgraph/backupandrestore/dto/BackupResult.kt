package com.samco.trackandgraph.backupandrestore.dto

enum class BackupResult {
    SUCCESS,
    FAIL_COULD_NOT_WRITE_TO_FILE,
    FAIL_COULD_NOT_FIND_DATABASE,
    FAIL_COULD_NOT_COPY
}