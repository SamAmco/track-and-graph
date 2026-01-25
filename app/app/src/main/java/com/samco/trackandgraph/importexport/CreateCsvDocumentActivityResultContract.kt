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
package com.samco.trackandgraph.importexport

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.samco.trackandgraph.R
import org.threeten.bp.OffsetDateTime

/**
 * Custom ActivityResultContract for creating CSV export files.
 * Generates a default filename with timestamp and group name.
 */
class CreateCsvDocumentActivityResultContract(
    private val groupName: String?
) : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            val now = OffsetDateTime.now()

            val generatedName = if (groupName.isNullOrEmpty()) {
                context.getString(
                    R.string.export_file_name_null_group_suffix,
                    "TrackAndGraph", now.year, now.monthValue,
                    now.dayOfMonth, now.hour, now.minute, now.second
                )
            } else
                context.getString(
                    R.string.export_file_name_suffix,
                    "TrackAndGraph", groupName, now.year, now.monthValue,
                    now.dayOfMonth, now.hour, now.minute, now.second
                )

            putExtra(Intent.EXTRA_TITLE, generatedName)
            type = "text/csv"
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent?.data
    }
}