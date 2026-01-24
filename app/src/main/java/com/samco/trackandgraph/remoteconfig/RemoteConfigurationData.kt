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
package com.samco.trackandgraph.remoteconfig

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteConfiguration(
    @SerialName("endpoints")
    val endpoints: Endpoints,
    @SerialName("trusted-lua-script-sources")
    val trustedLuaScriptSources: List<String>,
    @SerialName("changelogs-root")
    val changelogsRoot: String
)

@Serializable
data class Endpoints(
    val github: String,
    @SerialName("tutorial-root")
    val tutorialRoot: String,
    @SerialName("tutorial-tracking")
    val tutorialTracking: String,
    @SerialName("tutorial-lua")
    val tutorialLua: String,
    @SerialName("tutorial-graphs")
    val tutorialGraphs: String,
    @SerialName("tutorial-functions-reminders")
    val tutorialFunctionsReminders: String,
    @SerialName("lua-community-scripts-root")
    val luaCommunityScriptsRoot: String,
    @SerialName("play-store-page")
    val playStorePage: String,
    @SerialName("function-catalogue-location")
    val functionCatalogueLocation: String,
    @SerialName("function-catalogue-signature")
    val functionCatalogueSignature: String,
    @SerialName("functions-docs")
    val functionsDocs: String,
    @SerialName("donate-url")
    val donateUrl: String
)
