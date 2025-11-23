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
package com.samco.trackandgraph.releasenotes

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.localisation.TranslatedString
import com.samco.trackandgraph.downloader.DownloadResult
import com.samco.trackandgraph.downloader.FileDownloader
import com.samco.trackandgraph.remoteconfig.RemoteConfigProvider
import com.samco.trackandgraph.storage.FileCache
import com.samco.trackandgraph.storage.PrefsPersistenceProvider
import com.samco.trackandgraph.versionprovider.VersionProvider
import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.constraints.Constraint
import io.github.z4kn4fein.semver.satisfies
import io.github.z4kn4fein.semver.toVersion
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import timber.log.Timber
import javax.inject.Inject

data class ReleaseNote(
    val version: Version,
    val text: TranslatedString,
)

interface ReleaseNotesRepository {
    suspend fun getNewReleaseNotes(): List<ReleaseNote>
    suspend fun registerSeenReleaseNotes()
}

class ReleaseNotesRepositoryImpl @Inject constructor(
    @IODispatcher private val io: CoroutineDispatcher,
    private val persistenceProvider: PrefsPersistenceProvider,
    private val versionProvider: VersionProvider,
    private val dataInteractor: DataInteractor,
    private val downloader: FileDownloader,
    private val json: Json,
    private val fileCache: FileCache,
    private val remoteConfigProvider: RemoteConfigProvider,
) : ReleaseNotesRepository {
    private val metadataStore =
        persistenceProvider.getDataStore("$RELEASE_NOTES_DATA_ROOT/metadata")

    override suspend fun getNewReleaseNotes(): List<ReleaseNote> = withContext(io) {
        try {
            // Get the current version and the last seen version
            val registeredVersions = getRegisteredVersions()
            if (registeredVersions == null) return@withContext emptyList()

            if (registeredVersions.lastSeen == null) {
                if (dataInteractor.hasAnyFeatures()) {
                    // Special case: Show all release notes if you're on v8.1.x to make sure the
                    // functions feature is pointed out to beta users who already have the feature.
                    // You can remove this logic on any version >=v8.2.
                    if (registeredVersions.current.satisfies(Constraint.parse("8.1.x"))) {
                        return@withContext getAllReleaseNotes()
                    } else {
                        setLastSeenToCurrentVersion()
                        return@withContext emptyList()
                    }
                } else {
                    setLastSeenToCurrentVersion()
                    return@withContext emptyList()
                }
            } else {
                if (registeredVersions.lastSeen == registeredVersions.current) {
                    return@withContext emptyList()
                } else if (registeredVersions.lastSeen < registeredVersions.current) {
                    return@withContext getReleaseNotesAfterVersion(registeredVersions.lastSeen)
                }
            }

            return@withContext emptyList()
        } catch (t: Throwable) {
            Timber.e(t)
            return@withContext emptyList()
        }
    }

    private suspend fun getAllReleaseNotes(): List<ReleaseNote> {
        val index = downloadReleaseNotesIndex() ?: return emptyList()

        val releaseNotes = mutableListOf<ReleaseNote>()
        for ((versionString, localeMap) in index.changelogs) {
            val version = versionString.toVersion()
            val translations = downloadReleaseNotesForVersion(versionString, localeMap)
            if (translations.isNotEmpty()) {
                releaseNotes.add(
                    ReleaseNote(
                        version = version,
                        text = TranslatedString.Translations(translations)
                    )
                )
            }
        }

        // Sort by version descending (newest first)
        return releaseNotes.sortedByDescending { it.version }
    }

    private suspend fun getReleaseNotesAfterVersion(lastSeenVersion: Version): List<ReleaseNote> {
        val index = downloadReleaseNotesIndex() ?: return emptyList()
        val currentVersion = versionProvider.getCurrentVersion() ?: return emptyList()

        val releaseNotes = mutableListOf<ReleaseNote>()
        for ((versionString, localeMap) in index.changelogs) {
            val version = versionString.toVersion()
            // Only include versions newer than the last seen version
            if (version > lastSeenVersion && version <= currentVersion) {
                val translations = downloadReleaseNotesForVersion(versionString, localeMap)
                if (translations.isNotEmpty()) {
                    releaseNotes.add(
                        ReleaseNote(
                            version = version,
                            text = TranslatedString.Translations(translations)
                        )
                    )
                }
            }
        }

        // Sort by version descending (newest first)
        return releaseNotes.sortedByDescending { it.version }
    }

    override suspend fun registerSeenReleaseNotes() {
        try {
            setLastSeenToCurrentVersion()
        } catch (t: Throwable) {
            Timber.e(t)
        }
    }

    private suspend fun setLastSeenToCurrentVersion() {
        versionProvider.getCurrentVersion()?.let { current ->
            metadataStore.edit { it[LAST_SEEN_KEY] = current.toString() }
        }
    }

    private suspend fun getRegisteredVersions(): RegisteredVersions? {
        val props = metadataStore.data.firstOrNull()
        val currentVersion = versionProvider.getCurrentVersion() ?: return null

        return RegisteredVersions(
            lastSeen = props?.get(LAST_SEEN_KEY)?.toVersion(),
            current = currentVersion
        )
    }

    private data class RegisteredVersions(
        val lastSeen: Version?,
        val current: Version,
    )

    private suspend fun downloadReleaseNotesIndex(): ReleaseNotesIndex? {
        val remoteConfig = remoteConfigProvider.getRemoteConfiguration() ?: return null
        val indexUrl = "${remoteConfig.endpoints.changelogsRoot}/index.json"

        // Try to download with ETag support
        val cachedETag = fileCache.getETag(INDEX_CACHE_KEY)
        return when (val result = downloader.downloadFileWithETag(URI(indexUrl), cachedETag)) {
            is DownloadResult.Downloaded -> {
                // Store the new data and ETag
                val releaseNotesIndex =
                    json.decodeFromString<ReleaseNotesIndex>(String(result.data))
                fileCache.storeFile(INDEX_CACHE_KEY, result.data, result.etag)
                releaseNotesIndex
            }
            // The remote file hasn't changed, use the cached version
            is DownloadResult.UseCache -> {
                // Use cached version
                val cachedFile = fileCache.getFile(INDEX_CACHE_KEY)
                cachedFile?.let { json.decodeFromString<ReleaseNotesIndex>(String(it.data)) }
            }
            // Download failed, we can't check release notes right now
            null -> null
        }
    }

    private suspend fun downloadReleaseNotesForVersion(
        versionString: String,
        localeMap: Map<String, String>
    ): Map<String, String> {
        val translations = mutableMapOf<String, String>()

        for ((locale, relativePath) in localeMap) {
            val remoteConfig = remoteConfigProvider.getRemoteConfiguration() ?: continue
            val fileUrl = "${remoteConfig.endpoints.changelogsRoot}/$relativePath"
            val cacheKey = "$RELEASE_NOTES_DATA_ROOT/$versionString/$locale"

            // Try to download with ETag support
            val cachedETag = fileCache.getETag(cacheKey)
            when (val result = downloader.downloadFileWithETag(URI(fileUrl), cachedETag)) {
                is DownloadResult.Downloaded -> {
                    // Store the new data and ETag
                    fileCache.storeFile(cacheKey, result.data, result.etag)
                    translations[locale] = String(result.data)
                }

                is DownloadResult.UseCache -> {
                    // Use cached version
                    val cachedFile = fileCache.getFile(cacheKey)
                    cachedFile?.let { translations[locale] = String(it.data) }
                }

                null -> throw IllegalStateException("Failed to download release note: $fileUrl")
            }
        }

        return translations
    }

    @Serializable
    private data class ReleaseNotesIndex(
        val changelogs: Map<String, Map<String, String>>
    )

    companion object {
        private const val RELEASE_NOTES_DATA_ROOT = "release-notes"
        private const val INDEX_CACHE_KEY = "$RELEASE_NOTES_DATA_ROOT/index"
        private val LAST_SEEN_KEY = stringPreferencesKey("last_seen")
    }
}
