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
package com.samco.trackandgraph.remoteconfig

val testEndpoints = Endpoints(
    github = "https://github.com",
    tutorialRoot = "https://tutorial.com",
    tutorialTracking = "https://tutorial.com/tracking",
    tutorialLua = "https://tutorial.com/lua",
    tutorialGraphs = "https://tutorial.com/graphs",
    luaCommunityScriptsRoot = "https://scripts.com",
    playStorePage = "https://play.google.com",
    functionCatalogueLocation = "https://functions.com/catalog",
    functionCatalogueSignature = "https://functions.com/signature",
    functionsDocs = "https://functions.com/docs",
    changelogsRoot = "https://changelogs.com"
)

val testRemoteConfig = RemoteConfiguration(
    endpoints = testEndpoints,
    trustedLuaScriptSources = listOf("https://trusted.com")
)

class FakeRemoteConfigProvider : RemoteConfigProvider {
    var remoteConfig: RemoteConfiguration? = testRemoteConfig

    override suspend fun getRemoteConfiguration(): RemoteConfiguration? = remoteConfig
}