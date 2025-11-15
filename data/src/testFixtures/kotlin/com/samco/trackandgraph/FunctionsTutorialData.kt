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

package com.samco.trackandgraph

import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionType
import com.samco.trackandgraph.data.interactor.DataInteractor
import kotlinx.serialization.json.Json

// Function graph JSON strings for Functions Tutorial group

// Function 254: Exercice - combines Running and Cycling features
private val FUNCTION_254_GRAPH = """{"nodes":[{"type":"FeatureNode","x":-1502.8608,"y":-311.32462,"id":2,"featureId":252},{"type":"FeatureNode","x":-1491.5502,"y":310.2478,"id":3,"featureId":253}],"outputNode":{"x":0.0,"y":0.0,"id":1,"dependencies":[{"connectorIndex":0,"nodeId":3},{"connectorIndex":0,"nodeId":2}]},"isDuration":false}"""

// Function 255: Exercice This Week - complex function with periodic data points generator and filter
private val FUNCTION_255_GRAPH = """{"nodes":[{"type":"FeatureNode","x":-2716.8413,"y":-839.1216,"id":2,"featureId":252},{"type":"FeatureNode","x":-2716.825,"y":-408.99643,"id":3,"featureId":253},{"type":"LuaScriptNode","x":-2708.9812,"y":339.08325,"id":4,"script":"-- Lua Function to generate periodic data points at regular intervals\n-- This function creates data points with value=1 at deterministic timestamps\nlocal core = require(\"tng.core\")\nlocal enum = require(\"tng.config\").enum\nlocal uint = require(\"tng.config\").uint\nlocal instant = require(\"tng.config\").instant\nlocal now_time = core.time()\nlocal now = now_time and now_time.timestamp or 0\nreturn {\n    -- Configuration metadata\n    id = \"periodic-data-points\",\n    version = \"1.1.1\",\n    inputCount = 0, -- This is a generator, not a transformer\n    categories = { \"_generators\" },\n    title = {\n        [\"en\"] = \"Periodic Data Points\",\n        [\"de\"] = \"Periodische Datenpunkte\",\n        [\"es\"] = \"Puntos de Datos Periódicos\",\n        [\"fr\"] = \"Points de Données Périodiques\",\n    },\n    description = {\n        [\"en\"] = [[\nGenerates data points with value=1 at regular intervals going back in time.\nConfiguration:\n- **Period**: Time period unit (Day, Week, Month, Year)\n- **Period Multiplier**: Generate data point every N periods (e.g., every 2 days)\n- **Cutoff**: Stop generating data points at this date/time\nGenerated data points will have:\n- value = 1.0\n- label = \"\" (empty)\n- note = \"\" (empty)]],\n        [\"de\"] = [[\nGeneriert Datenpunkte mit Wert=1 in regelmäßigen Abständen zurück in der Zeit.\nKonfiguration:\n- **Periode**: Zeitperiodeneinheit (Tag, Woche, Monat, Jahr)\n- **Periodenmultiplikator**: Datenpunkt alle N Perioden generieren (z.B. alle 2 Tage)\n- **Grenzwert**: Generierung bei diesem Datum/Zeit stoppen\nGenerierte Datenpunkte haben:\n- Wert = 1.0\n- Label = \"\" (leer)\n- Notiz = \"\" (leer)]],\n        [\"es\"] = [[\nGenera puntos de datos con valor=1 a intervalos regulares retrocediendo en el tiempo.\nConfiguración:\n- **Período**: Unidad de período de tiempo (Día, Semana, Mes, Año)\n- **Multiplicador de Período**: Generar punto de datos cada N períodos (ej. cada 2 días)\n- **Límite**: Detener generación de puntos de datos en esta fecha/hora\nLos puntos de datos generados tendrán:\n- valor = 1.0\n- etiqueta = \"\" (vacío)\n- nota = \"\" (vacío)]],\n        [\"fr\"] = [[\nGénère des points de données avec valeur=1 à intervalles réguliers en remontant dans le temps.\nConfiguration:\n- **Période**: Unité de période de temps (Jour, Semaine, Mois, Année)\n- **Multiplicateur de Période**: Générer un point de données tous les N périodes (ex. tous les 2 jours)\n- **Limite**: Arrêter la génération de points de données à cette date/heure\nLes points de données générés auront:\n- valeur = 1.0\n- étiquette = \"\" (vide)\n- note = \"\" (vide)]],\n    },\n    config = {\n        enum {\n            id = \"period\",\n            name = \"_period\",\n            options = { \"_day\", \"_week\", \"_month\", \"_year\" },\n            default = \"_day\",\n        },\n        uint {\n            id = \"period_multiplier\",\n            name = \"_period_multiplier\",\n            default = 1,\n        },\n        instant {\n            id = \"cutoff\",\n            name = \"_cutoff\",\n            default = now - (365 * core.DURATION.DAY),\n        },\n    },\n    -- Generator function\n    generator = function(_, config)\n        -- Parse configuration with defaults\n        local period_str = config and config.period or error(\"Period configuration is required\")\n        local period_multiplier = (config and config.period_multiplier) or 1\n        -- Don't allow 0 multiplier, fallback to 1\n        if period_multiplier == 0 then\n            period_multiplier = 1\n        end\n        local cutoff_timestamp = config and config.cutoff or error(\"Cutoff configuration is required\")\n        -- Map enum string to core.PERIOD constant\n        local period_map = {\n            [\"_day\"] = core.PERIOD.DAY,\n            [\"_week\"] = core.PERIOD.WEEK,\n            [\"_month\"] = core.PERIOD.MONTH,\n            [\"_year\"] = core.PERIOD.YEAR,\n        }\n        local period = period_map[period_str]\n        -- Get current time for comparison\n        local now = core.time().timestamp\n        -- If cutoff is in the future, no data points to generate\n        if cutoff_timestamp > now then\n            return function()\n                return nil\n            end\n        end\n        -- Estimate number of periods elapsed since anchor\n        local elapsed_ms = now - cutoff_timestamp\n        local estimated_periods\n        local period_duration_ms\n        if period == core.PERIOD.DAY then\n            period_duration_ms = period_multiplier * core.DURATION.DAY\n        elseif period == core.PERIOD.WEEK then\n            period_duration_ms = period_multiplier * core.DURATION.WEEK\n        elseif period == core.PERIOD.MONTH then\n            -- Average month length: 30.44 days\n            period_duration_ms = period_multiplier * 30.44 * core.DURATION.DAY\n        elseif period == core.PERIOD.YEAR then\n            -- Average year length: 365.25 days\n            period_duration_ms = period_multiplier * 365.25 * core.DURATION.DAY\n        else\n            error(\"Invalid period: \" .. tostring(period_str))\n        end\n        estimated_periods = math.floor(elapsed_ms / period_duration_ms)\n        local cutoff_date = core.date(cutoff_timestamp)\n        -- Jump close to now with one large shift\n        local candidate = core.shift(cutoff_date, period, estimated_periods * period_multiplier)\n        -- Fine-tune: shift forward until we pass \"now\"\n        while candidate.timestamp <= now do\n            candidate = core.shift(candidate, period, period_multiplier)\n        end\n        -- Back up one step to get the most recent data point <= now\n        local current = core.shift(candidate, period, -period_multiplier)\n        -- Return iterator function\n        return function()\n            -- Check if we've gone past the cutoff (with 1 second tolerance for millisecond precision loss)\n            if current.timestamp < cutoff_timestamp - 1000 then\n                return nil\n            end\n            -- Create data point at current timestamp\n            local data_point = {\n                timestamp = current.timestamp,\n                offset = current.offset,\n                value = 1.0,\n                label = \"\",\n                note = \"\",\n            }\n            -- Shift backwards by period * period_multiplier for next iteration\n            current = core.shift(current, period, -period_multiplier)\n            return data_point\n        end\n    end,\n}\n","inputConnectorCount":0,"configuration":[{"configType":"Enum","id":"period","value":"_week"},{"configType":"UInt","id":"period_multiplier","value":1},{"configType":"Instant","id":"cutoff","epochMilli":1731283213735}],"translations":{"_generators":{"type":"Translations","translations":{"en":"Generators","es":"Generadores","de":"Generatoren","fr":"Générateurs"}},"_period":{"type":"Translations","translations":{"en":"Period","es":"Período","de":"Periode","fr":"Période"}},"_day":{"type":"Translations","translations":{"en":"Day","es":"Día","de":"Tag","fr":"Jour"}},"_week":{"type":"Translations","translations":{"en":"Week","es":"Semana","de":"Woche","fr":"Semaine"}},"_month":{"type":"Translations","translations":{"en":"Month","es":"Mes","de":"Monat","fr":"Mois"}},"_year":{"type":"Translations","translations":{"en":"Year","es":"Año","de":"Jahr","fr":"Année"}},"_period_multiplier":{"type":"Translations","translations":{"en":"Period Multiplier","es":"Multiplicador de Período","de":"Periodenmultiplikator","fr":"Multiplicateur de Période"}},"_cutoff":{"type":"Translations","translations":{"en":"Cutoff","es":"Límite","de":"Grenzwert","fr":"Limite"}}},"catalogFunctionId":"periodic-data-points","catalogVersion":"1.1.1","dependencies":[]},{"type":"LuaScriptNode","x":-1309.9325,"y":-6.859787,"id":5,"script":"-- Lua Function to filter data points after a reference point\n-- Outputs all data points from the first source that come after the last point in the second source\nreturn {\n\t-- Configuration metadata\n\tid = \"filter-after-last\",\n\tversion = \"1.0.0\",\n\tinputCount = 2,\n\tcategories = {\"_filter\"},\n\ttitle = {\n\t\t[\"en\"] = \"Filter After Last\",\n\t\t[\"de\"] = \"Filtern nach Letztem\",\n\t\t[\"es\"] = \"Filtrar después del último\",\n\t\t[\"fr\"] = \"Filtrer après le dernier\",\n\t},\n\tdescription = {\n\t\t[\"en\"] = [[\nFilters data points from the first input source to only include those that occur after the last data point in the second input source.\nThis is useful for filtering data based on a reference event or timestamp from another tracker.\n]],\n\t\t[\"de\"] = [[\nFiltert Datenpunkte aus der ersten Eingabequelle, um nur diejenigen einzuschließen, die nach dem letzten Datenpunkt in der zweiten Eingabequelle auftreten.\nDies ist nützlich zum Filtern von Daten basierend auf einem Referenzereignis oder Zeitstempel von einem anderen Tracker.\n]],\n\t\t[\"es\"] = [[\nFiltra puntos de datos de la primera fuente de entrada para incluir solo aquellos que ocurren después del último punto de datos en la segunda fuente de entrada.\nEsto es útil para filtrar datos basados en un evento de referencia o marca de tiempo de otro rastreador.\n]],\n\t\t[\"fr\"] = [[\nFiltre les points de données de la première source d'entrée pour n'inclure que ceux qui se produisent après le dernier point de données de la deuxième source d'entrée.\nCeci est utile pour filtrer les données basées sur un événement de référence ou un horodatage d'un autre tracker.\n]],\n\t},\n\tconfig = {},\n\t-- Generator function\n\tgenerator = function(sources, config)\n\t\tlocal source1 = sources[1]\n\t\tlocal source2 = sources[2]\n\t\tlocal cutoff_timestamp = nil\n\t\treturn function()\n\t\t\t-- Initialize cutoff on first call\n\t\t\tif cutoff_timestamp == nil then\n\t\t\t\tlocal reference_point = source2.dp()\n\t\t\t\tcutoff_timestamp = reference_point and reference_point.timestamp\n\t\t\tend\n\t\t\t-- Get next point from source1 and check if it's after cutoff\n\t\t\tlocal data_point = source1.dp()\n\t\t\tif not data_point then\n\t\t\t\treturn nil\n\t\t\tend\n\t\t\t-- Data points are in reverse chronological order, so \"after\" means greater timestamp\n\t\t\tif not cutoff_timestamp or data_point.timestamp > cutoff_timestamp then\n\t\t\t\treturn data_point\n\t\t\tend\n\t\t\t-- If the data point is not after the cutoff we're done\n\t\t\treturn nil\n\t\tend\n\tend,\n}\n","inputConnectorCount":2,"translations":{"_filter":{"type":"Translations","translations":{"en":"Filter","es":"Filtro","de":"Filter","fr":"Filtre"}}},"catalogFunctionId":"filter-after-last","catalogVersion":"1.0.0","dependencies":[{"connectorIndex":0,"nodeId":3},{"connectorIndex":0,"nodeId":2},{"connectorIndex":1,"nodeId":4}]}],"outputNode":{"x":0.0,"y":0.0,"id":1,"dependencies":[{"connectorIndex":0,"nodeId":5}]},"isDuration":false}"""

/**
 * Creates the Functions Tutorial group with trackers and functions to demonstrate the functions feature.
 * This group contains:
 * - 2 Trackers: Running and Cycling (both DURATION type)
 * - 2 Functions: Exercice (combines Running and Cycling) and Exercice This Week (filters combined data to current week)
 */
suspend fun createFunctionsTutorialGroup(dataInteractor: DataInteractor) {
    val outerGroupId = dataInteractor.insertGroup(createGroup("Demo"))
    createTutorialGroup(dataInteractor, outerGroupId)
}

private suspend fun createTutorialGroup(dataInteractor: DataInteractor, parent: Long) {
    val groupId = dataInteractor.insertGroup(
        createGroup(
            name = "Tutorial",
            parentGroupId = parent,
            displayIndex = 0
        )
    )

    // Create tracker: Running
    val runningTrackerId = dataInteractor.insertTracker(
        createTracker(
            name = "Running ",
            groupId = groupId,
            displayIndex = 0,
            dataType = DataType.DURATION,
            hasDefaultValue = false,
            suggestionType = TrackerSuggestionType.LABEL_ONLY,
            suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
        )
    )
    val runningFeatureId = dataInteractor.getTrackerById(runningTrackerId)!!.featureId

    // Create tracker: Cycling
    val cyclingTrackerId = dataInteractor.insertTracker(
        createTracker(
            name = "Cycling ",
            groupId = groupId,
            displayIndex = 1,
            dataType = DataType.DURATION,
            hasDefaultValue = false,
            suggestionType = TrackerSuggestionType.LABEL_ONLY,
            suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
        )
    )
    val cyclingFeatureId = dataInteractor.getTrackerById(cyclingTrackerId)!!.featureId

    // Create functions
    val json = Json { ignoreUnknownKeys = true }

    // Function 254: Exercice - combines Running and Cycling
    val exerciceId = dataInteractor.insertFunction(
        Function(
            name = "Exercice ",
            groupId = groupId,
            displayIndex = 0,
            description = "",
            functionGraph = json.decodeFromString(FUNCTION_254_GRAPH),
            inputFeatureIds = listOf(runningFeatureId, cyclingFeatureId)
        )
    )
    val exerciceFeatureId = dataInteractor.getFunctionById(exerciceId!!)!!.featureId

    // Function 255: Exercice This Week - filters Exercice data to current week
    dataInteractor.insertFunction(
        Function(
            name = "Exercice This Week",
            groupId = groupId,
            displayIndex = 0,
            description = "",
            functionGraph = json.decodeFromString(FUNCTION_255_GRAPH),
            inputFeatureIds = listOf(runningFeatureId, cyclingFeatureId) // Complex dependencies in graph
        )
    )
}
