/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.backupandrestore

import android.net.Uri
import com.samco.trackandgraph.backupandrestore.dto.AutoBackupInfo
import com.samco.trackandgraph.backupandrestore.dto.BackupConfig
import com.samco.trackandgraph.backupandrestore.dto.BackupResult
import com.samco.trackandgraph.backupandrestore.dto.RestoreResult
import kotlinx.coroutines.flow.Flow

interface BackupRestoreInteractor {

    val autoBackupConfig: Flow<AutoBackupInfo?>

    suspend fun performManualBackup(uri: Uri): BackupResult

    suspend fun performManualRestore(uri: Uri): RestoreResult

    suspend fun performAutoBackup(): BackupResult

    suspend fun backupNowAndSetAutoBackupConfig(backupConfig: BackupConfig): BackupResult

    fun validAutoBackupConfiguration(backupConfig: BackupConfig): Boolean

    suspend fun getAutoBackupInfo(): AutoBackupInfo?

    fun disableAutoBackup()
}