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
package com.samco.trackandgraph.fakes

import com.samco.trackandgraph.remoteconfig.RemoteConfiguration
import com.samco.trackandgraph.remoteconfig.Endpoints

val FakeRemoteConfiguration = RemoteConfiguration(
    endpoints = Endpoints(
        github = "https://github.com",
        tutorialRoot = "https://tutorial.com",
        tutorialTracking = "https://tutorial.com/tracking",
        tutorialLua = "https://tutorial.com/lua",
        tutorialGraphs = "https://tutorial.com/graphs",
        luaCommunityScriptsRoot = "https://community.com/scripts",
        playStorePage = "https://play.google.com/store",
        functionCatalogueLocation = "https://example.com/catalogue.lua",
        functionCatalogueSignature = "https://example.com/signature.json"
    ),
    trustedLuaScriptSources = emptyList()
)
