package com.samco.trackandgraph.backupandrestore.dto

enum class RestoreResult {
    SUCCESS,
    FAIL_INVALID_DATABASE,
    FAIL_COULD_NOT_FIND_OR_READ_DATABASE_FILE,
    FAIL_COULD_NOT_COPY
}