/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.versionprovider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
import javax.inject.Inject

interface VersionProvider {
    fun getCurrentVersion(): Version?
}

class VersionProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : VersionProvider {
    override fun getCurrentVersion(): Version? {
        val packageInfo = context.packageManager
            .getPackageInfo(context.packageName, 0)
        return packageInfo.versionName?.toVersion()
    }
}
