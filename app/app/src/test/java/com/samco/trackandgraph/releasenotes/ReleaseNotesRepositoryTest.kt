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

import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.localisation.TranslatedString
import com.samco.trackandgraph.downloader.DownloadResult
import com.samco.trackandgraph.downloader.FileDownloader
import com.samco.trackandgraph.remoteconfig.FakeRemoteConfigProvider
import com.samco.trackandgraph.remoteconfig.testRemoteConfig
import com.samco.trackandgraph.storage.FakeFileCache
import com.samco.trackandgraph.storage.FakePrefsPersistenceProvider
import com.samco.trackandgraph.versionprovider.FakeVersionProvider
import io.github.z4kn4fein.semver.toVersion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.URI

@OptIn(ExperimentalCoroutinesApi::class)
class ReleaseNotesRepositoryTest {

    private val persistenceProvider = FakePrefsPersistenceProvider()
    private val versionProvider = FakeVersionProvider()
    private val fileCache = FakeFileCache()

    private val dataInteractor: DataInteractor = mock()
    private val downloader: FileDownloader = mock()
    private val remoteConfigProvider: FakeRemoteConfigProvider = FakeRemoteConfigProvider()

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private val json = Json { ignoreUnknownKeys = false }

    private lateinit var repository: ReleaseNotesRepositoryImpl

    val testIndexJson = """
    {
      "changelogs": {
        "8.2.0": {
          "en": "changelogs/8.2.0/en.md",
          "de": "changelogs/8.2.0/de.md"
        },
        "8.1.0": {
          "en": "changelogs/8.1.0/en.md",
          "de": "changelogs/8.1.0/de.md"
        },
        "8.0.0": {
          "en": "changelogs/8.0.0/en.md",
          "de": "changelogs/8.0.0/de.md"
        }
      }
    }
""".trimIndent()

    val testReleaseNote820En = "# Release 8.2.0\n\nNew features in 8.2.0"
    val testReleaseNote820De = "# Version 8.2.0\n\nNeue Funktionen in 8.2.0"
    val testReleaseNote810En = "# Release 8.1.0\n\nNew features in 8.1.0"
    val testReleaseNote810De = "# Version 8.1.0\n\nNeue Funktionen in 8.1.0"
    val testReleaseNote800En = "# Release 8.0.0\n\nInitial release"
    val testReleaseNote800De = "# Version 8.0.0\n\nErste Version"

    @Before
    fun setup() {
        repository = ReleaseNotesRepositoryImpl(
            io = testDispatcher,
            persistenceProvider = persistenceProvider,
            versionProvider = versionProvider,
            dataInteractor = dataInteractor,
            downloader = downloader,
            json = json,
            fileCache = fileCache,
            remoteConfigProvider = remoteConfigProvider
        )
    }

    private suspend fun setupSuccessfulDownloads() {
        // Setup index download
        whenever(downloader.downloadFileWithETag(URI("https://changelogs.com/index.json"), null))
            .thenReturn(DownloadResult.Downloaded(testIndexJson.toByteArray(), "index-etag"))

        // Setup release note downloads
        whenever(
            downloader.downloadFileWithETag(
                URI("https://changelogs.com/changelogs/8.2.0/en.md"),
                null
            )
        )
            .thenReturn(
                DownloadResult.Downloaded(
                    testReleaseNote820En.toByteArray(),
                    "820-en-etag"
                )
            )
        whenever(
            downloader.downloadFileWithETag(
                URI("https://changelogs.com/changelogs/8.2.0/de.md"),
                null
            )
        )
            .thenReturn(
                DownloadResult.Downloaded(
                    testReleaseNote820De.toByteArray(),
                    "820-de-etag"
                )
            )
        whenever(
            downloader.downloadFileWithETag(
                URI("https://changelogs.com/changelogs/8.1.0/en.md"),
                null
            )
        )
            .thenReturn(
                DownloadResult.Downloaded(
                    testReleaseNote810En.toByteArray(),
                    "810-en-etag"
                )
            )
        whenever(
            downloader.downloadFileWithETag(
                URI("https://changelogs.com/changelogs/8.1.0/de.md"),
                null
            )
        )
            .thenReturn(
                DownloadResult.Downloaded(
                    testReleaseNote810De.toByteArray(),
                    "810-de-etag"
                )
            )
        whenever(
            downloader.downloadFileWithETag(
                URI("https://changelogs.com/changelogs/8.0.0/en.md"),
                null
            )
        )
            .thenReturn(
                DownloadResult.Downloaded(
                    testReleaseNote800En.toByteArray(),
                    "800-en-etag"
                )
            )
        whenever(
            downloader.downloadFileWithETag(
                URI("https://changelogs.com/changelogs/8.0.0/de.md"),
                null
            )
        )
            .thenReturn(
                DownloadResult.Downloaded(
                    testReleaseNote800De.toByteArray(),
                    "800-de-etag"
                )
            )
    }

    @Test
    fun `new user with no data - sets last seen to current and returns empty list`() = runTest {
        // Given: New user (no last seen version) with no data
        versionProvider.setCurrentVersion("8.1.0".toVersion())
        whenever(dataInteractor.hasAnyFeatures()).thenReturn(false)

        // When
        val result = repository.getNewReleaseNotes()

        // Then
        assertTrue(result.isEmpty())

        // Verify last seen was set to current version
        val metadataStore = persistenceProvider.getDataStoreForPath("release-notes/metadata")
        assertEquals("8.1.0", metadataStore.getPreference("last_seen"))
    }

    @Test
    fun `existing user on 8_1_x with no last seen - shows all release notes`() = runTest {
        // Given: Existing user on 8.1.x with no last seen version
        versionProvider.setCurrentVersion("8.1.0".toVersion())
        whenever(dataInteractor.hasAnyFeatures()).thenReturn(true)
        setupSuccessfulDownloads()

        // When
        val result = repository.getNewReleaseNotes()

        // Then
        assertEquals(3, result.size)
        assertEquals("8.2.0".toVersion(), result[0].version)
        assertEquals("8.1.0".toVersion(), result[1].version)
        assertEquals("8.0.0".toVersion(), result[2].version)

        // Verify translations are present
        val release820 = result[0].text as TranslatedString.Translations
        assertEquals(testReleaseNote820En, release820.values["en"])
        assertEquals(testReleaseNote820De, release820.values["de"])

        val release810 = result[1].text as TranslatedString.Translations
        assertEquals(testReleaseNote810En, release810.values["en"])
        assertEquals(testReleaseNote810De, release810.values["de"])

        val release800 = result[2].text as TranslatedString.Translations
        assertEquals(testReleaseNote800En, release800.values["en"])
        assertEquals(testReleaseNote800De, release800.values["de"])
    }

    @Test
    fun `existing user not on 8_1_x with no last seen - sets last seen and returns empty`() =
        runTest {
            // Given: Existing user not on 8.1.x with no last seen version
            versionProvider.setCurrentVersion("8.2.0".toVersion())
            whenever(dataInteractor.hasAnyFeatures()).thenReturn(true)

            // When
            val result = repository.getNewReleaseNotes()

            // Then
            assertTrue(result.isEmpty())

            // Verify last seen was set to current version
            val metadataStore = persistenceProvider.getDataStoreForPath("release-notes/metadata")
            assertEquals("8.2.0", metadataStore.getPreference("last_seen"))
        }

    @Test
    fun `user on same version as last seen - returns empty list`() = runTest {
        // Given: User on same version as last seen
        versionProvider.setCurrentVersion("8.1.0".toVersion())
        val metadataStore = persistenceProvider.getDataStoreForPath("release-notes/metadata")
        metadataStore.setPreference("last_seen", "8.1.0")

        // When
        val result = repository.getNewReleaseNotes()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `user on newer version than last seen - returns release notes for versions in between`() =
        runTest {
            // Given: User upgraded from 8.0.0 to 8.1.5
            versionProvider.setCurrentVersion("8.1.5".toVersion())
            val metadataStore = persistenceProvider.getDataStoreForPath("release-notes/metadata")
            metadataStore.setPreference("last_seen", "8.0.0")
            setupSuccessfulDownloads()

            // When
            val result = repository.getNewReleaseNotes()

            // Then
            assertEquals(1, result.size) // Only 8.1.0 should be included (8.2.0 > current version)
            assertEquals("8.1.0".toVersion(), result[0].version)

            val release810 = result[0].text as TranslatedString.Translations
            assertEquals(testReleaseNote810En, release810.values["en"])
            assertEquals(testReleaseNote810De, release810.values["de"])
        }

    @Test
    fun `registerSeenReleaseNotes prevents seeing same release notes again`() = runTest {
        // Given: User upgraded from 8.0.0 to 8.1.5 and sees release notes
        versionProvider.setCurrentVersion("8.1.5".toVersion())
        val metadataStore = persistenceProvider.getDataStoreForPath("release-notes/metadata")
        metadataStore.setPreference("last_seen", "8.0.0")
        setupSuccessfulDownloads()

        // When: Get release notes first time
        val firstResult = repository.getNewReleaseNotes()

        // Then: Should see 8.1.0 release notes
        assertEquals(1, firstResult.size)
        assertEquals("8.1.0".toVersion(), firstResult[0].version)

        // When: Register as seen and check again
        repository.registerSeenReleaseNotes()
        val secondResult = repository.getNewReleaseNotes()

        // Then: Should not see any release notes
        assertTrue(secondResult.isEmpty())

        // Verify last seen was updated to current version
        assertEquals("8.1.5", metadataStore.getPreference("last_seen"))
    }

    @Test
    fun `returns empty list when remote config unavailable`() = runTest {
        // Given: Remote config is unavailable
        versionProvider.setCurrentVersion("8.1.5".toVersion())
        val metadataStore = persistenceProvider.getDataStoreForPath("release-notes/metadata")
        metadataStore.setPreference("last_seen", "8.0.0")
        remoteConfigProvider.remoteConfig = null

        // When
        val result = repository.getNewReleaseNotes()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns empty list when index download fails`() = runTest {
        // Given: Index download fails
        versionProvider.setCurrentVersion("8.1.5".toVersion())
        val metadataStore = persistenceProvider.getDataStoreForPath("release-notes/metadata")
        metadataStore.setPreference("last_seen", "8.0.0")
        remoteConfigProvider.remoteConfig = testRemoteConfig
        whenever(downloader.downloadFileWithETag(URI("https://changelogs.com/index.json"), null))
            .thenReturn(null)

        // When
        val result = repository.getNewReleaseNotes()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns empty list when release note download fails`() = runTest {
        // Given: Index downloads but release note download fails
        versionProvider.setCurrentVersion("8.1.5".toVersion())
        val metadataStore = persistenceProvider.getDataStoreForPath("release-notes/metadata")
        metadataStore.setPreference("last_seen", "8.0.0")
        
        // Setup successful index download
        whenever(downloader.downloadFileWithETag(URI("https://changelogs.com/index.json"), null))
            .thenReturn(DownloadResult.Downloaded(testIndexJson.toByteArray(), "index-etag"))
        
        // Setup failed release note download
        whenever(
            downloader.downloadFileWithETag(
                URI("https://changelogs.com/changelogs/8.1.0/en.md"),
                null
            )
        ).thenReturn(null) // Download fails

        // When & Then: Should throw exception which results in empty list from outer try-catch
        val result = repository.getNewReleaseNotes()
        assertTrue(result.isEmpty())

        // Verify last seen was not updated to current version (we will retry next time)
        assertEquals("8.0.0", metadataStore.getPreference("last_seen"))
    }

    @Test
    fun `uses cached release notes after partial download failure`() = runTest {
        // Given: User upgraded from 8.0.0 to 8.1.5, first attempt partially succeeds
        versionProvider.setCurrentVersion("8.1.5".toVersion())
        val metadataStore = persistenceProvider.getDataStoreForPath("release-notes/metadata")
        metadataStore.setPreference("last_seen", "8.0.0")
        
        // Setup successful index download (first attempt)
        whenever(downloader.downloadFileWithETag(URI("https://changelogs.com/index.json"), null))
            .thenReturn(DownloadResult.Downloaded(testIndexJson.toByteArray(), "index-etag"))
        
        // First attempt: English downloads successfully, German fails
        whenever(
            downloader.downloadFileWithETag(
                URI("https://changelogs.com/changelogs/8.1.0/en.md"),
                null
            )
        ).thenReturn(DownloadResult.Downloaded(testReleaseNote810En.toByteArray(), "810-en-etag"))
        
        whenever(
            downloader.downloadFileWithETag(
                URI("https://changelogs.com/changelogs/8.1.0/de.md"),
                null
            )
        ).thenReturn(null) // First attempt fails
        
        // First attempt should fail and return empty
        val firstResult = repository.getNewReleaseNotes()
        assertTrue(firstResult.isEmpty())
        
        // Second attempt: English uses cache (ETag match), German downloads successfully
        whenever(
            downloader.downloadFileWithETag(
                URI("https://changelogs.com/changelogs/8.1.0/en.md"),
                "810-en-etag"
            )
        ).thenReturn(DownloadResult.UseCache)
        
        // Setup the cached file that will be retrieved
        fileCache.preloadFile(
            "release-notes/8.1.0/en",
            testReleaseNote810En.toByteArray(),
            "810-en-etag"
        )
        // Setup successful index download (first attempt)
        whenever(downloader.downloadFileWithETag(URI("https://changelogs.com/index.json"), "index-etag"))
            .thenReturn(DownloadResult.UseCache)

        whenever(
            downloader.downloadFileWithETag(
                URI("https://changelogs.com/changelogs/8.1.0/de.md"),
                null
            )
        ).thenReturn(DownloadResult.Downloaded(testReleaseNote810De.toByteArray(), "810-de-etag"))
        
        // When: Second attempt should succeed using cached English + new German
        val secondResult = repository.getNewReleaseNotes()
        
        // Then: Should get complete release notes
        assertEquals(1, secondResult.size)
        assertEquals("8.1.0".toVersion(), secondResult[0].version)
        
        val release810 = secondResult[0].text as TranslatedString.Translations
        assertEquals(testReleaseNote810En, release810.values["en"]) // From cache
        assertEquals(testReleaseNote810De, release810.values["de"]) // Fresh download
    }

    @Test
    fun `multiple calls to getNewReleaseNotes without registerSeen returns consistent results`() = runTest {
        // Given: User upgraded from 8.0.0 to 8.1.5
        versionProvider.setCurrentVersion("8.1.5".toVersion())
        val metadataStore = persistenceProvider.getDataStoreForPath("release-notes/metadata")
        metadataStore.setPreference("last_seen", "8.0.0")
        
        // Setup successful downloads for first call
        whenever(downloader.downloadFileWithETag(URI("https://changelogs.com/index.json"), null))
            .thenReturn(DownloadResult.Downloaded(testIndexJson.toByteArray(), "index-etag"))
        whenever(
            downloader.downloadFileWithETag(
                URI("https://changelogs.com/changelogs/8.1.0/en.md"),
                null
            )
        ).thenReturn(DownloadResult.Downloaded(testReleaseNote810En.toByteArray(), "810-en-etag"))
        whenever(
            downloader.downloadFileWithETag(
                URI("https://changelogs.com/changelogs/8.1.0/de.md"),
                null
            )
        ).thenReturn(DownloadResult.Downloaded(testReleaseNote810De.toByteArray(), "810-de-etag"))
        
        // When: First call
        val firstResult = repository.getNewReleaseNotes()
        
        // Then: Should get release notes
        assertEquals(1, firstResult.size)
        assertEquals("8.1.0".toVersion(), firstResult[0].version)
        
        // Setup cache responses for second call (ETags will be used)
        whenever(downloader.downloadFileWithETag(URI("https://changelogs.com/index.json"), "index-etag"))
            .thenReturn(DownloadResult.UseCache)
        fileCache.preloadFile("release-notes/index", testIndexJson.toByteArray(), "index-etag")
        
        whenever(
            downloader.downloadFileWithETag(
                URI("https://changelogs.com/changelogs/8.1.0/en.md"),
                "810-en-etag"
            )
        ).thenReturn(DownloadResult.UseCache)
        
        whenever(
            downloader.downloadFileWithETag(
                URI("https://changelogs.com/changelogs/8.1.0/de.md"),
                "810-de-etag"
            )
        ).thenReturn(DownloadResult.UseCache)
        
        // When: Second call without registerSeen
        val secondResult = repository.getNewReleaseNotes()
        
        // Then: Should get identical results
        assertEquals(1, secondResult.size)
        assertEquals("8.1.0".toVersion(), secondResult[0].version)

        assertEquals(firstResult, secondResult)

        // Verify last seen was not updated (still waiting for registerSeen)
        assertEquals("8.0.0", metadataStore.getPreference("last_seen"))
    }

    @Test
    fun `corrupted index JSON does not get cached`() = runTest {
        // Given: User upgraded from 8.0.0 to 8.1.5
        versionProvider.setCurrentVersion("8.1.5".toVersion())
        val metadataStore = persistenceProvider.getDataStoreForPath("release-notes/metadata")
        metadataStore.setPreference("last_seen", "8.0.0")
        
        // Setup download of corrupted JSON
        val corruptedJson = """{"invalid": "json structure"}"""
        whenever(downloader.downloadFileWithETag(URI("https://changelogs.com/index.json"), null))
            .thenReturn(DownloadResult.Downloaded(corruptedJson.toByteArray(), "corrupt-etag"))
        
        // When: Try to get release notes
        val result = repository.getNewReleaseNotes()
        
        // Then: Should return empty list (JSON decode fails)
        assertTrue(result.isEmpty())
        
        // Verify corrupted data was NOT cached (cache should be empty)
        val cachedFile = fileCache.getFile("release-notes/index")
        assertEquals(null, cachedFile)
    }

    @Test
    fun `valid index JSON gets cached after successful decode`() = runTest {
        // Given: User upgraded from 8.0.0 to 8.1.5
        versionProvider.setCurrentVersion("8.1.5".toVersion())
        val metadataStore = persistenceProvider.getDataStoreForPath("release-notes/metadata")
        metadataStore.setPreference("last_seen", "8.0.0")
        
        // Setup download of valid JSON
        whenever(downloader.downloadFileWithETag(URI("https://changelogs.com/index.json"), null))
            .thenReturn(DownloadResult.Downloaded(testIndexJson.toByteArray(), "valid-etag"))

        // When: Get release notes
        val result = repository.getNewReleaseNotes()
        
        // Then: Should not return release notes (none successful downloads setup)
        assertEquals(0, result.size)

        // Verify valid data WAS cached
        val cachedFile = fileCache.getFile("release-notes/index")
        assertEquals("valid-etag", cachedFile!!.etag)
        assertEquals(testIndexJson, String(cachedFile.data))
    }
}
