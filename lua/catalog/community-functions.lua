return {
	functions={
		["absolute-value"]={
			script=[=[
-- Lua Function to take absolute value
-- Converts all data point values to their absolute value

return {
	-- Configuration metadata
	id = "absolute-value",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_arithmetic"},
	title = {
		["en"] = "Absolute Value",
		["de"] = "Absolutwert",
		["es"] = "Valor absoluto",
		["fr"] = "Valeur absolue",
	},
	description = {
		["en"] = [[
Converts each data point's value to its absolute value (removes negative sign).
]],
		["de"] = [[
Konvertiert den Wert jedes Datenpunkts zu seinem Absolutwert (entfernt negatives Vorzeichen).
]],
		["es"] = [[
Convierte el valor de cada punto de datos a su valor absoluto (elimina el signo negativo).
]],
		["fr"] = [[
Convertit la valeur de chaque point de données en sa valeur absolue (supprime le signe négatif).
]],
	},
	config = {},

	-- Generator function
	generator = function(source)
		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			-- Convert to absolute value
			data_point.value = math.abs(data_point.value)

			return data_point
		end
	end,
}
]=],
			version="1.0.0",
		},
		ceil={
			script=[=[
-- Lua Function to ceiling values
-- Rounds each data point's value up to the nearest multiple of a specified number

local number = require("tng.config").number

return {
	-- Configuration metadata
	id = "ceil",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_arithmetic"},
	title = {
		["en"] = "Ceiling",
		["de"] = "Aufrunden",
		["es"] = "Techo",
		["fr"] = "Plafond",
	},
	description = {
		["en"] = [[
Rounds each data point's value up to the nearest multiple of a specified number.

Configuration:
- **Nearest**: Round up to the nearest multiple of this number (default: 1.0)
]],
		["de"] = [[
Rundet den Wert jedes Datenpunkts auf das nächste Vielfache einer angegebenen Zahl auf.

Konfiguration:
- **Nächste**: Auf das nächste Vielfache dieser Zahl aufrunden (Standard: 1.0)
]],
		["es"] = [[
Redondea hacia arriba el valor de cada punto de datos al múltiplo más cercano de un número especificado.

Configuración:
- **Más cercano**: Redondear hacia arriba al múltiplo más cercano de este número (predeterminado: 1.0)
]],
		["fr"] = [[
Arrondit vers le haut la valeur de chaque point de données au multiple le plus proche d'un nombre spécifié.

Configuration:
- **Plus proche**: Arrondir vers le haut au multiple le plus proche de ce nombre (par défaut: 1.0)
]],
	},
	config = {
		number {
			id = "nearest",
			default = 1.0,
			name = {
				["en"] = "Nearest",
				["de"] = "Nächste",
				["es"] = "Más cercano",
				["fr"] = "Plus proche",
			},
		},
	},

	-- Generator function
	generator = function(source, config)
		local nearest = config and config.nearest or 1.0

		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			-- Ceiling to nearest multiple
			data_point.value = math.ceil(data_point.value / nearest) * nearest

			return data_point
		end
	end,
}
]=],
			version="1.0.0",
		},
		["cumulative-value"]={
			script=[=[
-- Lua Function to calculate cumulative sum of data point values
-- This function computes a running total, optionally resetting on specific labels

local checkbox = require("tng.config").checkbox
local text = require("tng.config").text

return {
    -- Configuration metadata
    id = "cumulative-value",
    version = "1.0.0",
    inputCount = 1,
    categories = {"_arithmetic"},
    title = {
        ["en"] = "Cumulative Value",
        ["de"] = "Kumulativer Wert",
        ["es"] = "Valor Acumulativo",
        ["fr"] = "Valeur Cumulative",
    },
    description = {
        ["en"] = "Calculates the cumulative sum of data point values. Optionally resets accumulation when a specific label is encountered.",
        ["de"] = "Berechnet die kumulative Summe der Datenpunktwerte. Setzt optional die Akkumulation zurück, wenn ein bestimmtes Label gefunden wird.",
        ["es"] = "Calcula la suma acumulativa de los valores de puntos de datos. Opcionalmente reinicia la acumulación cuando se encuentra una etiqueta específica.",
        ["fr"] = "Calcule la somme cumulative des valeurs de points de données. Réinitialise optionnellement l'accumulation lorsqu'une étiquette spécifique est rencontrée.",
    },
    config = {
        checkbox {
            id = "enable_reset",
            name = "_reset_on_label_match",
            default = false,
        },
        text {
            id = "reset_label",
            name = "_reset_label",
            default = "",
        },
        checkbox {
            id = "exact_match",
            name = "_match_exactly",
            default = false,
        },
        checkbox {
            id = "case_sensitive",
            name = "_case_sensitive",
            default = true,
        },
    },

    -- Generator function
    generator = function(source, config)
        local enable_reset = config.enable_reset
        local reset_label = config.reset_label
        local exact_match = config.exact_match
        local case_sensitive = config.case_sensitive

        -- Helper function to check if a label matches
        local function label_matches(dp_label)
            if not enable_reset then
                return false
            end

            local label_to_check = dp_label or ""
            local pattern = reset_label

            -- Apply case insensitivity if needed
            if not case_sensitive then
                label_to_check = label_to_check:lower()
                pattern = pattern:lower()
            end

            -- Check match type
            if exact_match then
                return label_to_check == pattern
            else
                -- search for pattern from the start, pattern matching disabled
                return label_to_check:find(pattern, 1, true) ~= nil
            end
        end

        -- Drain all data points from source (returns in reverse chronological order)
        local all_points = source.dpall()

        -- Calculate cumulative values by iterating in reverse (chronologically)
        -- Mutate data points in place
        local cumulative_sum = 0
        for i = #all_points, 1, -1 do
            local data_point = all_points[i]

            -- Check if we should reset
            if label_matches(data_point.label) then
                cumulative_sum = 0
            end

            -- Add current value to cumulative sum
            cumulative_sum = cumulative_sum + data_point.value

            -- Update the data point's value in place
            data_point.value = cumulative_sum
        end

        -- all_points is still in reverse chronological order, return iterator
        local index = 0
        return function()
            index = index + 1
            return all_points[index]
        end
    end,
}
]=],
			version="1.0.0",
		},
		["distinct-until-changed"]={
			script=[=[
-- Lua Function to filter out consecutive duplicates based on selected fields
-- Only passes through data points when the selected fields change from the previous one

local enum = require("tng.config").enum

return {
	-- Configuration metadata
	id = "distinct-until-changed",
	version = "1.0.0",
	inputCount = 1,
	categories = { "_filter" },
	title = {
		["en"] = "Distinct Until Changed",
		["de"] = "Eindeutig bis geändert",
		["es"] = "Distinto hasta cambio",
		["fr"] = "Distinct jusqu'au changement",
	},
	description = {
		["en"] = [[
Filters out consecutive duplicates based on the selected fields. Only data points where the selected fields differ from the previous one will pass through.

- **All Fields** - Compare value, label, and note
- **Value Only** - Compare value only
- **Label Only** - Compare label only
- **Note Only** - Compare note only
- **Value and Label** - Compare value and label
- **Value and Note** - Compare value and note
- **Label and Note** - Compare label and note
]],
		["de"] = [[
Filtert aufeinanderfolgende Duplikate basierend auf den ausgewählten Feldern heraus. Nur Datenpunkte, bei denen sich die ausgewählten Felder vom vorherigen unterscheiden, werden durchgelassen.

- **Alle Felder** - Vergleicht Wert, Label und Notiz
- **Nur Wert** - Vergleicht nur Wert
- **Nur Label** - Vergleicht nur Label
- **Nur Notiz** - Vergleicht nur Notiz
- **Wert und Label** - Vergleicht Wert und Label
- **Wert und Notiz** - Vergleicht Wert und Notiz
- **Label und Notiz** - Vergleicht Label und Notiz
]],
		["es"] = [[
Filtra duplicados consecutivos basándose en los campos seleccionados. Solo los puntos de datos donde los campos seleccionados difieren del anterior pasarán.

- **Todos los campos** - Compara valor, etiqueta y nota
- **Solo valor** - Compara solo valor
- **Solo etiqueta** - Compara solo etiqueta
- **Solo nota** - Compara solo nota
- **Valor y etiqueta** - Compara valor y etiqueta
- **Valor y nota** - Compara valor y nota
- **Etiqueta y nota** - Compara etiqueta y nota
]],
		["fr"] = [[
Filtre les doublons consécutifs en fonction des champs sélectionnés. Seuls les points de données où les champs sélectionnés diffèrent du précédent passeront.

- **Tous les champs** - Compare valeur, étiquette et note
- **Valeur uniquement** - Compare la valeur uniquement
- **Étiquette uniquement** - Compare l'étiquette uniquement
- **Note uniquement** - Compare la note uniquement
- **Valeur et étiquette** - Compare valeur et étiquette
- **Valeur et note** - Compare valeur et note
- **Étiquette et note** - Compare étiquette et note
]],
	},
	config = {
		enum {
			id = "compare_by",
			name = "_compare_by",
			options = {
				"_all_fields",
				"_value_only",
				"_label_only",
				"_note_only",
				"_value_and_label",
				"_value_and_note",
				"_label_and_note",
			},
			default = "_all_fields",
		},
	},

	-- Generator function
	generator = function(source, config)
		local compare_by = config and config.compare_by or "_all_fields"

		local last_value = nil
		local last_label = nil
		local last_note = nil

		return function()
			while true do
				local data_point = source.dp()
				if not data_point then
					return nil
				end

				local current_value = data_point.value
				local current_label = data_point.label
				local current_note = data_point.note

				local is_different = false

				if compare_by == "_all_fields" then
					is_different = (current_value ~= last_value)
						or (current_label ~= last_label)
						or (current_note ~= last_note)
				elseif compare_by == "_value_only" then
					is_different = (current_value ~= last_value)
				elseif compare_by == "_label_only" then
					is_different = (current_label ~= last_label)
				elseif compare_by == "_note_only" then
					is_different = (current_note ~= last_note)
				elseif compare_by == "_value_and_label" then
					is_different = (current_value ~= last_value) or (current_label ~= last_label)
				elseif compare_by == "_value_and_note" then
					is_different = (current_value ~= last_value) or (current_note ~= last_note)
				elseif compare_by == "_label_and_note" then
					is_different = (current_label ~= last_label) or (current_note ~= last_note)
				end

				if is_different then
					last_value = current_value
					last_label = current_label
					last_note = current_note
					return data_point
				end
			end
		end
	end,
}
]=],
			version="1.0.0",
		},
		divide={
			script=[=[
-- Lua Function to divide data point values by a configurable number
-- This function divides all incoming data point values by a specified divisor

local number = require("tng.config").number

return {
    -- Configuration metadata
    id = "divide",
    version = "1.0.0",
    inputCount = 1,
    categories = {"_arithmetic"},
    title = {
        ["en"] = "Divide Values",
        ["de"] = "Werte dividieren",
        ["es"] = "Dividir Valores",
        ["fr"] = "Diviser les Valeurs"
    },
    description = {
        ["en"] = [[
Divides all incoming data point values by a specified divisor.

Configuration:
- **Divisor**: The number to divide all values by (default: 1.0)
]],
        ["de"] = [[
Dividiert alle eingehenden Datenpunktwerte durch einen bestimmten Divisor.

Konfiguration:
- **Divisor**: Die Zahl, durch die alle Werte dividiert werden (Standard: 1.0)
]],
        ["es"] = [[
Divide todos los valores de puntos de datos entrantes por un divisor especificado.

Configuración:
- **Divisor**: El número por el cual dividir todos los valores (predeterminado: 1.0)
]],
        ["fr"] = [[
Divise toutes les valeurs de points de données entrantes par un diviseur spécifié.

Configuration:
- **Diviseur**: Le nombre par lequel diviser toutes les valeurs (par défaut: 1.0)
]]
    },
    config = {
        number {
            id = "divisor",
            default = 1.0,
            name = {
                ["en"] = "Divisor",
                ["de"] = "Divisor",
                ["es"] = "Divisor",
                ["fr"] = "Diviseur"
            }
        }
    },

    -- Generator function
    generator = function(source, config)
        local divisor = config and config.divisor or 1.0

        return function()
            local data_point = source.dp()
            if not data_point then return nil end

            data_point.value = data_point.value / divisor

            return data_point
        end
    end
}
]=],
			version="1.0.0",
		},
		["filter-after-cutoff"]={
			script=[=[
-- Lua Function to filter data points after a cutoff timestamp
-- This function only passes through data points that occur at or after the specified cutoff time

local instant = require("tng.config").instant
local core = require("tng.core")

local now_time = core.time()
local now = now_time and now_time.timestamp or 0

return {
    -- Configuration metadata
    id = "filter-after-cutoff",
    version = "1.0.0",
    inputCount = 1,
    categories = {"_filter", "_time"},
    title = {
        ["en"] = "Filter After Cutoff",
        ["de"] = "Filtern nach Grenzwert",
        ["es"] = "Filtrar Después del Límite",
        ["fr"] = "Filtrer Après la Limite",
    },
    description = {
        ["en"] = "Filters data points to only include those at or after the specified cutoff time.",
        ["de"] = "Filtert Datenpunkte, um nur diejenigen ab dem angegebenen Grenzwert einzuschließen.",
        ["es"] = "Filtra puntos de datos para incluir solo aquellos en o después del límite especificado.",
        ["fr"] = "Filtre les points de données pour n'inclure que ceux à partir de la limite spécifiée.",
    },
    config = {
        instant {
            id = "cutoff",
            name = "_cutoff",
            default = now - (30 * core.DURATION.DAY),  -- 30 days ago
        },
    },

    -- Generator function
    generator = function(source, config)
        local cutoff = config and config.cutoff or error("Cutoff configuration is required")

        return function()
            while true do
                local data_point = source.dp()
                if not data_point then
                    return nil
                end

                -- Only return data points at or after the cutoff
                if data_point.timestamp >= cutoff then
                    return data_point
                end
                -- Otherwise, skip this data point and continue to the next
            end
        end
    end,
}
]=],
			version="1.0.0",
		},
		["filter-after-last"]={
			script=[=[
-- Lua Function to filter data points after a reference point
-- Outputs all data points from the first source that come after the last point in the second source

return {
	-- Configuration metadata
	id = "filter-after-last",
	version = "1.0.0",
	inputCount = 2,
	categories = {"_filter"},
	title = {
		["en"] = "Filter After Last",
		["de"] = "Filtern nach Letztem",
		["es"] = "Filtrar después del último",
		["fr"] = "Filtrer après le dernier",
	},
	description = {
		["en"] = [[
Filters data points from the first input source to only include those that occur after the last data point in the second input source.

This is useful for filtering data based on a reference event or timestamp from another tracker.
]],
		["de"] = [[
Filtert Datenpunkte aus der ersten Eingabequelle, um nur diejenigen einzuschließen, die nach dem letzten Datenpunkt in der zweiten Eingabequelle auftreten.

Dies ist nützlich zum Filtern von Daten basierend auf einem Referenzereignis oder Zeitstempel von einem anderen Tracker.
]],
		["es"] = [[
Filtra puntos de datos de la primera fuente de entrada para incluir solo aquellos que ocurren después del último punto de datos en la segunda fuente de entrada.

Esto es útil para filtrar datos basados en un evento de referencia o marca de tiempo de otro rastreador.
]],
		["fr"] = [[
Filtre les points de données de la première source d'entrée pour n'inclure que ceux qui se produisent après le dernier point de données de la deuxième source d'entrée.

Ceci est utile pour filtrer les données basées sur un événement de référence ou un horodatage d'un autre tracker.
]],
	},
	config = {},

	-- Generator function
	generator = function(sources, config)
		local source1 = sources[1]
		local source2 = sources[2]
		local cutoff_timestamp = nil

		return function()
			-- Initialize cutoff on first call
			if cutoff_timestamp == nil then
				local reference_point = source2.dp()
				cutoff_timestamp = reference_point and reference_point.timestamp
			end

			-- Get next point from source1 and check if it's after cutoff
			local data_point = source1.dp()
			if not data_point then
				return nil
			end

			-- Data points are in reverse chronological order, so "after" means greater timestamp
			if not cutoff_timestamp or data_point.timestamp > cutoff_timestamp then
				return data_point
			end

			-- If the data point is not after the cutoff we're done
			return nil
		end
	end,
}
]=],
			version="1.0.0",
		},
		["filter-before-cutoff"]={
			script=[=[
-- Lua Function to filter data points before a cutoff timestamp
-- This function only passes through data points that occur before the specified cutoff time
local instant = require("tng.config").instant
local core = require("tng.core")

local now_time = core.time()
local now = now_time and now_time.timestamp or 0

return {
    -- Configuration metadata
    id = "filter-before-cutoff",
    version = "1.0.0",
    inputCount = 1,
    categories = { "_filter", "_time" },

    title = {
        ["en"] = "Filter Before Cutoff",
        ["de"] = "Filtern vor Grenzwert",
        ["es"] = "Filtrar Antes del Límite",
        ["fr"] = "Filtrer Avant la Limite",
    },

    description = {
        ["en"] = "Filters data points to only include those before the specified cutoff time.",
        ["de"] = "Filtert Datenpunkte, um nur diejenigen vor dem angegebenen Grenzwert einzuschließen.",
        ["es"] = "Filtra puntos de datos para incluir solo aquellos antes del límite especificado.",
        ["fr"] = "Filtre les points de données pour n'inclure que ceux avant la limite spécifiée.",
    },

    config = {
        instant {
            id = "cutoff",
            name = "_cutoff",
            default = now, -- Current time as default
        },
    },

    -- Generator function
    generator = function(source, config)
        local cutoff = config and config.cutoff or error("Cutoff configuration is required")

        return function()
            while true do
                local data_point = source.dp()
                if not data_point then
                    return nil
                end

                -- Only return data points before the cutoff
                if data_point.timestamp < cutoff then
                    return data_point
                end
                -- Otherwise, skip this data point and continue to the next
            end
        end
    end,
}
]=],
			version="1.0.0",
		},
		["filter-before-last"]={
			script=[=[
-- Lua Function to filter data points before a reference point
-- Outputs all data points from the first source that come before the last point in the second source

return {
	-- Configuration metadata
	id = "filter-before-last",
	version = "1.0.0",
	inputCount = 2,
	categories = { "_filter" },
	title = {
		["en"] = "Filter Before Last",
		["de"] = "Filtern vor Letztem",
		["es"] = "Filtrar antes del último",
		["fr"] = "Filtrer avant le dernier",
	},
	description = {
		["en"] = [[
Filters data points from the first input source to only include those that occur before the last data point in the second input source.

This is useful for filtering data based on a reference event or timestamp from another tracker.
]],
		["de"] = [[
Filtert Datenpunkte aus der ersten Eingabequelle, um nur diejenigen einzuschließen, die vor dem letzten Datenpunkt in der zweiten Eingabequelle auftreten.

Dies ist nützlich zum Filtern von Daten basierend auf einem Referenzereignis oder Zeitstempel von einem anderen Tracker.
]],
		["es"] = [[
Filtra puntos de datos de la primera fuente de entrada para incluir solo aquellos que ocurren antes del último punto de datos en la segunda fuente de entrada.

Esto es útil para filtrar datos basados en un evento de referencia o marca de tiempo de otro rastreador.
]],
		["fr"] = [[
Filtre les points de données de la première source d'entrée pour n'inclure que ceux qui se produisent avant le dernier point de données de la deuxième source d'entrée.

Ceci est utile pour filtrer les données basées sur un événement de référence ou un horodatage d'un autre tracker.
]],
	},
	config = {},

	-- Generator function
	generator = function(sources)
		local source1 = sources[1]
		local source2 = sources[2]

		-- Initialize cutoff on first call
		local reference_point = source2.dp()
		local cutoff_timestamp = reference_point and reference_point.timestamp

		return function()
			while true do
				-- Get next point from source1 and check if it's after cutoff
				local data_point = source1.dp()
				if not data_point then
					return nil
				end

				if not cutoff_timestamp or data_point.timestamp < cutoff_timestamp then
					return data_point
				end
			end
		end
	end
}
]=],
			version="1.0.0",
		},
		["filter-by-label"]={
			script=[=[
-- Example Lua Function with Input Count and Configuration
-- This function filters data points by label

local tng_config = require("tng.config")
local text = tng_config.text
local checkbox = tng_config.checkbox

local function match(data_point, filter_label, case_sensitive, match_exactly)
    if filter_label == nil then
        return true
    end

    local data_label = data_point.label
    if not data_label then return false end

    -- Apply case sensitivity
    if not case_sensitive then
        data_label = string.lower(data_label)
        filter_label = string.lower(filter_label)
    end

    -- Apply matching mode
    if match_exactly then
        return data_label == filter_label
    else
        return string.find(data_label, filter_label, 1, true) ~= nil
    end
end


return {
    -- Configuration metadata
    id = "filter-by-label",
    version = "1.0.1",
    inputCount = 1,
    categories = {"_filter"},
    title = {
        ["en"] = "Filter by Label",
        ["de"] = "Filtern nach Etikett",
        ["es"] = "Filtrar por Etiqueta",
        ["fr"] = "Filtrer par Étiquette"
    },
    description = {
        ["en"] = [[
Filters data points by their label field. Only data points matching the filter criteria will pass through.

Configuration:
- **Filter Label**: The text to search for in labels
- **Case Sensitive**: Match case exactly (default: false)
- **Match Exactly**: Require exact match instead of substring (default: false)
- **Invert**: Keep data points that DON'T match instead (default: false)
]],
        ["de"] = [[
Filtert Datenpunkte nach ihrem Label-Feld. Nur Datenpunkte, die den Filterkriterien entsprechen, werden durchgelassen.

Konfiguration:
- **Filter-Label**: Der Text, nach dem in Labels gesucht werden soll
- **Groß-/Kleinschreibung beachten**: Groß-/Kleinschreibung exakt beachten (Standard: false)
- **Exakt übereinstimmen**: Exakte Übereinstimmung statt Teilstring erforderlich (Standard: false)
- **Invertieren**: Datenpunkte behalten, die NICHT übereinstimmen (Standard: false)
]],
        ["es"] = [[
Filtra puntos de datos por su campo de etiqueta. Solo los puntos de datos que coincidan con los criterios del filtro pasarán.

Configuración:
- **Filtrar Etiqueta**: El texto a buscar en las etiquetas
- **Sensible a Mayúsculas**: Coincidir exactamente con mayúsculas y minúsculas (predeterminado: false)
- **Coincidir Exactamente**: Requerir coincidencia exacta en lugar de subcadena (predeterminado: false)
- **Invertir**: Mantener puntos de datos que NO coincidan (predeterminado: false)
]],
        ["fr"] = [[
Filtre les points de données par leur champ d'étiquette. Seuls les points de données correspondant aux critères du filtre passeront.

Configuration:
- **Filtrer l'Étiquette**: Le texte à rechercher dans les étiquettes
- **Sensible à la Casse**: Correspondance exacte de la casse (par défaut: false)
- **Correspondance Exacte**: Nécessite une correspondance exacte au lieu d'une sous-chaîne (par défaut: false)
- **Inverser**: Conserver les points de données qui NE correspondent PAS (par défaut: false)
]]
    },
    config = {
        text {
            id = "filter_label",
            name = {
                ["en"] = "Filter Label",
                ["de"] = "Filter-Label",
                ["es"] = "Filtrar Etiqueta",
                ["fr"] = "Filtrer l'Étiquette"
            }
        },
        checkbox {
            id = "case_sensitive",
            name = "_case_sensitive",
        },
        checkbox {
            id = "match_exactly",
            name = "_match_exactly",
        },
        checkbox {
            id = "invert",
            name = {
                ["en"] = "Invert",
                ["de"] = "Invertieren",
                ["es"] = "Invertir",
                ["fr"] = "Inverser"
            }
        }
    },

    -- Generator function
    generator = function(source, config)
        local filter_label = config and config.filter_label
        local case_sensitive = config and config.case_sensitive or false
        local match_exactly = config and config.match_exactly or false
        local invert = config and config.invert or false

        return function()
            local data_point = source.dp()
            local should_match = not invert
            while data_point and (match(data_point, filter_label, case_sensitive, match_exactly) ~= should_match) do
                data_point = source.dp()
            end
            return data_point
        end
    end
}
]=],
			version="1.0.1",
		},
		["filter-by-note"]={
			script=[=[
-- Lua Function to filter data points by note
-- This function filters data points by note

local tng_config = require("tng.config")
local text = tng_config.text
local checkbox = tng_config.checkbox

local function match(data_point, filter_note, case_sensitive, match_exactly)
    if filter_note == nil then
        return true
    end

    local data_note = data_point.note
    if not data_note then return false end

    -- Apply case sensitivity
    if not case_sensitive then
        data_note = string.lower(data_note)
        filter_note = string.lower(filter_note)
    end

    -- Apply matching mode
    if match_exactly then
        return data_note == filter_note
    else
        return string.find(data_note, filter_note, 1, true) ~= nil
    end
end


return {
    -- Configuration metadata
    id = "filter-by-note",
    version = "1.0.1",
    inputCount = 1,
    categories = {"_filter"},
    title = {
        ["en"] = "Filter by Note",
        ["de"] = "Filtern nach Notiz",
        ["es"] = "Filtrar por Nota",
        ["fr"] = "Filtrer par Note"
    },
    description = {
        ["en"] = [[
Filters data points by their note field. Only data points matching the filter criteria will pass through.

Configuration:
- **Filter Note**: The text to search for in notes
- **Case Sensitive**: Match case exactly (default: false)
- **Match Exactly**: Require exact match instead of substring (default: false)
- **Invert**: Keep data points that DON'T match instead (default: false)
]],
        ["de"] = [[
Filtert Datenpunkte nach ihrem Notiz-Feld. Nur Datenpunkte, die den Filterkriterien entsprechen, werden durchgelassen.

Konfiguration:
- **Filter-Notiz**: Der Text, nach dem in Notizen gesucht werden soll
- **Groß-/Kleinschreibung beachten**: Groß-/Kleinschreibung exakt beachten (Standard: false)
- **Exakt übereinstimmen**: Exakte Übereinstimmung statt Teilstring erforderlich (Standard: false)
- **Invertieren**: Datenpunkte behalten, die NICHT übereinstimmen (Standard: false)
]],
        ["es"] = [[
Filtra puntos de datos por su campo de nota. Solo los puntos de datos que coincidan con los criterios del filtro pasarán.

Configuración:
- **Filtrar Nota**: El texto a buscar en las notas
- **Sensible a Mayúsculas**: Coincidir exactamente con mayúsculas y minúsculas (predeterminado: false)
- **Coincidir Exactamente**: Requerir coincidencia exacta en lugar de subcadena (predeterminado: false)
- **Invertir**: Mantener puntos de datos que NO coincidan (predeterminado: false)
]],
        ["fr"] = [[
Filtre les points de données par leur champ de note. Seuls les points de données correspondant aux critères du filtre passeront.

Configuration:
- **Filtrer la Note**: Le texte à rechercher dans les notes
- **Sensible à la Casse**: Correspondance exacte de la casse (par défaut: false)
- **Correspondance Exacte**: Nécessite une correspondance exacte au lieu d'une sous-chaîne (par défaut: false)
- **Inverser**: Conserver les points de données qui NE correspondent PAS (par défaut: false)
]]
    },
    config = {
        text {
            id = "filter_note",
            name = {
                ["en"] = "Filter Note",
                ["de"] = "Filter-Notiz",
                ["es"] = "Filtrar Nota",
                ["fr"] = "Filtrer la Note"
            }
        },
        checkbox {
            id = "case_sensitive",
            name = "_case_sensitive",
        },
        checkbox {
            id = "match_exactly",
            name = "_match_exactly",
        },
        checkbox {
            id = "invert",
            name = {
                ["en"] = "Invert",
                ["de"] = "Invertieren",
                ["es"] = "Invertir",
                ["fr"] = "Inverser"
            }
        }
    },

    -- Generator function
    generator = function(source, config)
        local filter_note = config and config.filter_note
        local case_sensitive = config and config.case_sensitive or false
        local match_exactly = config and config.match_exactly or false
        local invert = config and config.invert or false

        return function()
            local data_point = source.dp()
            local should_match = not invert
            while data_point and (match(data_point, filter_note, case_sensitive, match_exactly) ~= should_match) do
                data_point = source.dp()
            end
            return data_point
        end
    end
}
]=],
			version="1.0.1",
		},
		["filter-greater-than"]={
			script=[=[
-- Lua Function to filter data points by value (greater than threshold)
-- Only passes through data points with values greater than a threshold

local tng_config = require("tng.config")
local number = tng_config.number
local checkbox = tng_config.checkbox

return {
    -- Configuration metadata
    id = "filter-greater-than",
    version = "1.0.0",
    inputCount = 1,
    categories = {"_filter"},
    title = {
        ["en"] = "Filter Greater Than",
        ["de"] = "Filtern größer als",
        ["es"] = "Filtrar mayor que",
        ["fr"] = "Filtrer supérieur à",
    },
    description = {
        ["en"] = [[
Filters data points by value. Only data points with values greater than the threshold will pass through.

Configuration:
- **Threshold**: The minimum value (exclusive by default)
- **Include Equal**: Also include values equal to the threshold (default: false)
]],
        ["de"] = [[
Filtert Datenpunkte nach Wert. Nur Datenpunkte mit Werten größer als der Schwellenwert werden durchgelassen.

Konfiguration:
- **Schwellenwert**: Der Mindestwert (standardmäßig exklusiv)
- **Gleich einschließen**: Werte gleich dem Schwellenwert auch einschließen (Standard: false)
]],
        ["es"] = [[
Filtra puntos de datos por valor. Solo los puntos de datos con valores mayores que el umbral pasarán.

Configuración:
- **Umbral**: El valor mínimo (exclusivo por defecto)
- **Incluir igual**: También incluir valores iguales al umbral (predeterminado: false)
]],
        ["fr"] = [[
Filtre les points de données par valeur. Seuls les points de données avec des valeurs supérieures au seuil passeront.

Configuration:
- **Seuil**: La valeur minimale (exclusive par défaut)
- **Inclure égal**: Inclure également les valeurs égales au seuil (par défaut: false)
]],
    },
    config = {
        number {
            id = "threshold",
            name = {
                ["en"] = "Threshold",
                ["de"] = "Schwellenwert",
                ["es"] = "Umbral",
                ["fr"] = "Seuil",
            },
        },
        checkbox {
            id = "include_equal",
            name = {
                ["en"] = "Include Equal",
                ["de"] = "Gleich einschließen",
                ["es"] = "Incluir igual",
                ["fr"] = "Inclure égal",
            },
        },
    },

    -- Generator function
    generator = function(source, config)
        local threshold = config and config.threshold or 0.0
        local include_equal = config and config.include_equal or false

        return function()
            while true do
                local data_point = source.dp()
                if not data_point then
                    return nil
                end

                local passes
                if include_equal then
                    passes = data_point.value >= threshold
                else
                    passes = data_point.value > threshold
                end

                if passes then
                    return data_point
                end
            end
        end
    end,
}
]=],
			version="1.0.0",
		},
		["filter-less-than"]={
			script=[=[
-- Lua Function to filter data points by value (less than threshold)
-- Only passes through data points with values less than a threshold

local tng_config = require("tng.config")
local number = tng_config.number
local checkbox = tng_config.checkbox

return {
    -- Configuration metadata
    id = "filter-less-than",
    version = "1.0.0",
    inputCount = 1,
    categories = {"_filter"},
    title = {
        ["en"] = "Filter Less Than",
        ["de"] = "Filtern kleiner als",
        ["es"] = "Filtrar menor que",
        ["fr"] = "Filtrer inférieur à",
    },
    description = {
        ["en"] = [[
Filters data points by value. Only data points with values less than the threshold will pass through.

Configuration:
- **Threshold**: The maximum value (exclusive by default)
- **Include Equal**: Also include values equal to the threshold (default: false)
]],
        ["de"] = [[
Filtert Datenpunkte nach Wert. Nur Datenpunkte mit Werten kleiner als der Schwellenwert werden durchgelassen.

Konfiguration:
- **Schwellenwert**: Der Maximalwert (standardmäßig exklusiv)
- **Gleich einschließen**: Werte gleich dem Schwellenwert auch einschließen (Standard: false)
]],
        ["es"] = [[
Filtra puntos de datos por valor. Solo los puntos de datos con valores menores que el umbral pasarán.

Configuración:
- **Umbral**: El valor máximo (exclusivo por defecto)
- **Incluir igual**: También incluir valores iguales al umbral (predeterminado: false)
]],
        ["fr"] = [[
Filtre les points de données par valeur. Seuls les points de données avec des valeurs inférieures au seuil passeront.

Configuration:
- **Seuil**: La valeur maximale (exclusive par défaut)
- **Inclure égal**: Inclure également les valeurs égales au seuil (par défaut: false)
]],
    },
    config = {
        number {
            id = "threshold",
            name = {
                ["en"] = "Threshold",
                ["de"] = "Schwellenwert",
                ["es"] = "Umbral",
                ["fr"] = "Seuil",
            },
        },
        checkbox {
            id = "include_equal",
            name = {
                ["en"] = "Include Equal",
                ["de"] = "Gleich einschließen",
                ["es"] = "Incluir igual",
                ["fr"] = "Inclure égal",
            },
        },
    },

    -- Generator function
    generator = function(source, config)
        local threshold = config and config.threshold or 0.0
        local include_equal = config and config.include_equal or false

        return function()
            while true do
                local data_point = source.dp()
                if not data_point then
                    return nil
                end

                local passes
                if include_equal then
                    passes = data_point.value <= threshold
                else
                    passes = data_point.value < threshold
                end

                if passes then
                    return data_point
                end
            end
        end
    end,
}
]=],
			version="1.0.0",
		},
		["filter-within-period"]={
			script=[=[
-- Lua Function to filter data points within a specified period from now
-- This function calculates a cutoff timestamp by subtracting a specified period from "now" and only passes through data points at or after that cutoff
local enum = require("tng.config").enum
local uint = require("tng.config").uint
local core = require("tng.core")

return {
    -- Configuration metadata
    id = "filter-within-period",
    version = "1.0.0",
    inputCount = 1,
    categories = { "_filter", "_time" },

    title = {
        ["en"] = "Filter Within Period",
        ["de"] = "Filtern innerhalb Periode",
        ["es"] = "Filtrar Dentro de Período",
        ["fr"] = "Filtrer Dans la Période",
    },

    description = {
        ["en"] = [[
Filters data points to only include those within the specified time period from now.
The cutoff is calculated by subtracting the specified period from the current time.

Configuration:
- **Period**: Time period unit (Day, Week, Month, Year)
- **Period Multiplier**: Number of periods to include (e.g., 2 weeks = Week + Multiplier 2)

For example, with Period=Day and Multiplier=7, only data from within the last 7 days will pass through.
]],
        ["de"] = [[
Filtert Datenpunkte, um nur diejenigen innerhalb der angegebenen Zeitperiode von jetzt einzuschließen.
Der Grenzwert wird berechnet, indem die angegebene Periode von der aktuellen Zeit abgezogen wird.

Konfiguration:
- **Periode**: Zeitperiodeneinheit (Tag, Woche, Monat, Jahr)
- **Periodenmultiplikator**: Anzahl der Perioden einzuschließen (z.B. 2 Wochen = Woche + Multiplikator 2)

Zum Beispiel, mit Periode=Tag und Multiplikator=7, passieren nur Daten innerhalb der letzten 7 Tage durch.
]],
        ["es"] = [[
Filtra puntos de datos para incluir solo aquellos dentro del período de tiempo especificado desde ahora.
El límite se calcula restando el período especificado del tiempo actual.

Configuración:
- **Período**: Unidad de período de tiempo (Día, Semana, Mes, Año)
- **Multiplicador de Período**: Número de períodos a incluir (ej. 2 semanas = Semana + Multiplicador 2)

Por ejemplo, con Período=Día y Multiplicador=7, solo pasarán datos dentro de los últimos 7 días.
]],
        ["fr"] = [[
Filtre les points de données pour n'inclure que ceux dans la période de temps spécifiée depuis maintenant.
La limite est calculée en soustrayant la période spécifiée du temps actuel.

Configuration:
- **Période**: Unité de période de temps (Jour, Semaine, Mois, Année)
- **Multiplicateur de Période**: Nombre de périodes à inclure (ex. 2 semaines = Semaine + Multiplicateur 2)

Par exemple, avec Période=Jour et Multiplicateur=7, seules les données dans les 7 derniers jours passeront.
]],
    },

    config = {
        enum {
            id = "period",
            name = "_period",
            options = { "_day", "_week", "_month", "_year" },
            default = "_month",
        },
        uint {
            id = "period_multiplier",
            name = "_period_multiplier",
            default = 1,
        },
    },

    -- Generator function
    generator = function(source, config)
        local period_str = config and config.period or error("Period configuration is required")
        local period_multiplier = (config and config.period_multiplier) or 30

        -- Don't allow 0 multiplier, fallback to 1
        if period_multiplier == 0 then
            period_multiplier = 1
        end

        -- Map enum string to core.PERIOD constant
        local period_map = {
            ["_day"] = core.PERIOD.DAY,
            ["_week"] = core.PERIOD.WEEK,
            ["_month"] = core.PERIOD.MONTH,
            ["_year"] = core.PERIOD.YEAR,
        }
        local period = period_map[period_str]
        if not period then
            error("Invalid period: " .. tostring(period_str))
        end

        -- Calculate cutoff timestamp: now - (period * multiplier)
        local now = core.time()
        local cutoff = core.shift(now, period, -period_multiplier)
        local cutoff_timestamp = cutoff.timestamp

        return function()
            local data_point = source.dp()
            if not data_point then
                return nil
            end

            -- Only return data points at or after the cutoff
            if data_point.timestamp >= cutoff_timestamp then
                return data_point
            end
        end
    end,
}
]=],
			version="1.0.0",
		},
		floor={
			script=[=[
-- Lua Function to floor values
-- Rounds each data point's value down to the nearest multiple of a specified number

local number = require("tng.config").number

return {
	-- Configuration metadata
	id = "floor",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_arithmetic"},
	title = {
		["en"] = "Floor",
		["de"] = "Abrunden",
		["es"] = "Piso",
		["fr"] = "Plancher",
	},
	description = {
		["en"] = [[
Rounds each data point's value down to the nearest multiple of a specified number.

Configuration:
- **Nearest**: Round down to the nearest multiple of this number (default: 1.0)
]],
		["de"] = [[
Rundet den Wert jedes Datenpunkts auf das nächste Vielfache einer angegebenen Zahl ab.

Konfiguration:
- **Nächste**: Auf das nächste Vielfache dieser Zahl abrunden (Standard: 1.0)
]],
		["es"] = [[
Redondea hacia abajo el valor de cada punto de datos al múltiplo más cercano de un número especificado.

Configuración:
- **Más cercano**: Redondear hacia abajo al múltiplo más cercano de este número (predeterminado: 1.0)
]],
		["fr"] = [[
Arrondit vers le bas la valeur de chaque point de données au multiple le plus proche d'un nombre spécifié.

Configuration:
- **Plus proche**: Arrondir vers le bas au multiple le plus proche de ce nombre (par défaut: 1.0)
]],
	},
	config = {
		number {
			id = "nearest",
			default = 1.0,
			name = {
				["en"] = "Nearest",
				["de"] = "Nächste",
				["es"] = "Más cercano",
				["fr"] = "Plus proche",
			},
		},
	},

	-- Generator function
	generator = function(source, config)
		local nearest = config and config.nearest or 1.0

		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			-- Floor to nearest multiple
			data_point.value = math.floor(data_point.value / nearest) * nearest

			return data_point
		end
	end,
}
]=],
			version="1.0.0",
		},
		multiply={
			script=[=[
-- Lua Function to multiply data point values by a configurable number
-- This function multiplies all incoming data point values by a specified multiplier

local number = require("tng.config").number

return {
    -- Configuration metadata
    id = "multiply",
    version = "1.0.0",
    inputCount = 1,
    categories = {"_arithmetic"},
    title = {
        ["en"] = "Multiply Values",
        ["de"] = "Werte multiplizieren",
        ["es"] = "Multiplicar Valores",
        ["fr"] = "Multiplier les Valeurs"
    },
    description = {
        ["en"] = [[
Multiplies all incoming data point values by a specified multiplier.

Configuration:
- **Multiplier**: The number to multiply all values by (default: 1.0)
]],
        ["de"] = [[
Multipliziert alle eingehenden Datenpunktwerte mit einem bestimmten Multiplikator.

Konfiguration:
- **Multiplikator**: Die Zahl, mit der alle Werte multipliziert werden (Standard: 1.0)
]],
        ["es"] = [[
Multiplica todos los valores de puntos de datos entrantes por un multiplicador especificado.

Configuración:
- **Multiplicador**: El número por el cual multiplicar todos los valores (predeterminado: 1.0)
]],
        ["fr"] = [[
Multiplie toutes les valeurs de points de données entrantes par un multiplicateur spécifié.

Configuration:
- **Multiplicateur**: Le nombre par lequel multiplier toutes les valeurs (par défaut: 1.0)
]]
    },
    config = {
        number {
            id = "multiplier",
            name = {
                ["en"] = "Multiplier",
                ["de"] = "Multiplikator",
                ["es"] = "Multiplicador",
                ["fr"] = "Multiplicateur"
            }
        }
    },

    -- Generator function
    generator = function(source, config)
        local multiplier = config and config.multiplier or 1.0

        return function()
            local data_point = source.dp()
            if not data_point then return nil end

            data_point.value = data_point.value * multiplier

            return data_point
        end
    end
}
]=],
			version="1.0.0",
		},
		["offset-value"]={
			script=[=[
-- Lua Function to offset data point values by a configurable number
-- This function adds a constant offset to all incoming data point values

local number = require("tng.config").number

return {
    -- Configuration metadata
    id = "offset-value",
    version = "1.0.0",
    inputCount = 1,
    categories = {"_arithmetic"},
    title = {
        ["en"] = "Offset Value",
        ["de"] = "Wert verschieben",
        ["es"] = "Desplazar Valor",
        ["fr"] = "Décaler la Valeur"
    },
    description = {
        ["en"] = [[
Adds a constant offset to all incoming data point values.

Configuration:
- **Offset**: The number to add to all values (default: 0.0). Use negative values to subtract.
]],
        ["de"] = [[
Fügt allen eingehenden Datenpunktwerten einen konstanten Offset hinzu.

Konfiguration:
- **Offset**: Die Zahl, die zu allen Werten addiert wird (Standard: 0.0). Verwenden Sie negative Werte zum Subtrahieren.
]],
        ["es"] = [[
Añade un desplazamiento constante a todos los valores de puntos de datos entrantes.

Configuración:
- **Desplazamiento**: El número a añadir a todos los valores (predeterminado: 0.0). Use valores negativos para restar.
]],
        ["fr"] = [[
Ajoute un décalage constant à toutes les valeurs de points de données entrantes.

Configuration:
- **Décalage**: Le nombre à ajouter à toutes les valeurs (par défaut: 0.0). Utilisez des valeurs négatives pour soustraire.
]]
    },
    config = {
        number {
            id = "offset",
            default = 0.0,
            name = {
                ["en"] = "Offset",
                ["de"] = "Offset",
                ["es"] = "Desplazamiento",
                ["fr"] = "Décalage"
            }
        }
    },

    -- Generator function
    generator = function(source, config)
        local offset = config and config.offset or 0.0

        return function()
            local data_point = source.dp()
            if not data_point then return nil end

            data_point.value = data_point.value + offset

            return data_point
        end
    end
}
]=],
			version="1.0.0",
		},
		["override-label"]={
			script=[=[
-- Lua Function to override the label of all data points with a configurable string
-- This function sets all incoming data point labels to a specified value

local text = require("tng.config").text

return {
    -- Configuration metadata
    id = "override-label",
    version = "1.0.0",
    inputCount = 1,
    categories = {"_transform"},
    title = {
        ["en"] = "Override Label",
        ["de"] = "Label überschreiben",
        ["es"] = "Sobrescribir Etiqueta",
        ["fr"] = "Remplacer l'Étiquette",
    },
    description = {
        ["en"] = [[
Sets all incoming data point labels to a specified value
]],
        ["de"] = [[
Setzt alle eingehenden Datenpunkt-Labels auf einen bestimmten Wert
]],
        ["es"] = [[
Establece todas las etiquetas de puntos de datos entrantes en un valor especificado
]],
        ["fr"] = [[
Définit toutes les étiquettes de points de données entrantes sur une valeur spécifiée
]],
    },
    config = {
        text {
            id = "new_label",
            name = {
                ["en"] = "New Label",
                ["de"] = "Neues Label",
                ["es"] = "Nueva Etiqueta",
                ["fr"] = "Nouvelle Étiquette",
            },
        },
    },

    -- Generator function
    generator = function(source, config)
        local new_label = config and config.new_label

        return function()
            local data_point = source.dp()
            if not data_point then
                return nil
            end

            if not new_label then
                return data_point
            end
            data_point.label = new_label

            return data_point
        end
    end,
}
]=],
			version="1.0.0",
		},
		["override-note"]={
			script=[=[
-- Lua Function to override the note of all data points with a configurable string
-- This function sets all incoming data point notes to a specified value

local text = require("tng.config").text

return {
    -- Configuration metadata
    id = "override-note",
    version = "1.0.0",
    inputCount = 1,
    categories = {"_transform"},
    title = {
        ["en"] = "Override Note",
        ["de"] = "Notiz überschreiben",
        ["es"] = "Sobrescribir Nota",
        ["fr"] = "Remplacer la Note",
    },
    description = {
        ["en"] = [[
Sets all incoming data point notes to a specified value
]],
        ["de"] = [[
Setzt alle eingehenden Datenpunkt-Notizen auf einen bestimmten Wert
]],
        ["es"] = [[
Establece todas las notas de puntos de datos entrantes en un valor especificado
]],
        ["fr"] = [[
Définit toutes les notes de points de données entrantes sur une valeur spécifiée
]],
    },
    config = {
        text {
            id = "new_note",
            name = {
                ["en"] = "New Note",
                ["de"] = "Neue Notiz",
                ["es"] = "Nueva Nota",
                ["fr"] = "Nouvelle Note",
            },
        },
    },

    -- Generator function
    generator = function(source, config)
        local new_note = config and config.new_note

        return function()
            local data_point = source.dp()
            if not data_point then
                return nil
            end

            if not new_note then
                return data_point
            end
            data_point.note = new_note

            return data_point
        end
    end,
}
]=],
			version="1.0.0",
		},
		["override-value"]={
			script=[=[
-- Lua Function to override the value of all data points with a configurable number
-- This function sets all incoming data point values to a specified value

local number = require("tng.config").number

return {
    -- Configuration metadata
    id = "override-value",
    version = "1.0.0",
    inputCount = 1,
    categories = {"_transform"},
    title = {
        ["en"] = "Override Value",
        ["de"] = "Wert überschreiben",
        ["es"] = "Sobrescribir Valor",
        ["fr"] = "Remplacer la Valeur",
    },
    description = {
        ["en"] = [[
Sets all incoming data point values to a specified value
]],
        ["de"] = [[
Setzt alle eingehenden Datenpunktwerte auf einen bestimmten Wert
]],
        ["es"] = [[
Establece todos los valores de puntos de datos entrantes en un valor especificado
]],
        ["fr"] = [[
Définit toutes les valeurs de points de données entrantes sur une valeur spécifiée
]],
    },
    config = {
        number {
            id = "new_value",
            name = {
                ["en"] = "New Value",
                ["de"] = "Neuer Wert",
                ["es"] = "Nuevo Valor",
                ["fr"] = "Nouvelle Valeur",
            },
        },
    },

    -- Generator function
    generator = function(source, config)
        local new_value = config and config.new_value

        return function()
            local data_point = source.dp()
            if not data_point then
                return nil
            end

            if not new_value then
                return data_point
            end
            data_point.value = new_value

            return data_point
        end
    end,
}
]=],
			version="1.0.0",
		},
		["pair-and-operate"]={
			script=[=[
-- Lua Function to pair the data points of two input sources and perform
-- an operation on their values (addition, subtraction, multiplication, or division).
local duration = require("tng.config").duration
local enum = require("tng.config").enum
local core = require("tng.core")

return {
  -- Configuration metadata
  id = "pair-and-operate",
  version = "1.0.3",
  inputCount = 2,
  categories = { "_combine" },
  title = {
    ["en"] = "Pair and Operate",
    ["de"] = "Paaren und Operieren",
    ["es"] = "Emparejar y Operar",
    ["fr"] = "Apparier et Opérer",
  },
  description = {
    ["en"] = [[
Pairs each data point in the first data source with the corresponding data point in the second data source and performs a specified operation (addition, subtraction, multiplication, or division) on their values. The pair for each data point is the first data point that falls within the given time threshold. Configuration:

- **Time Threshold:** The maximum duration between data points in the two sources to be considered a pair.
- **Operation:** The mathematical operation to perform on the paired data point values (addition, subtraction, multiplication, or division).
- **On Missing:** Specifies the behavior when a data point in the first source does not have a corresponding data point in the second source within the given time threshold. Options include:
  - **Skip:** Do not output anything for that data point.
  - **Pass Through:** Output the original data point from the first source without modification.

> **Note:** Division by zero is invalid and considered as missing data. The On Missing configuration will determine how such cases are handled.
]],
    ["de"] = [[
Paart jeden Datenpunkt in der ersten Datenquelle mit dem entsprechenden Datenpunkt in der zweiten Datenquelle und führt eine bestimmte Operation (Addition, Subtraktion, Multiplikation oder Division) mit ihren Werten durch. Das Paar für jeden Datenpunkt ist der erste Datenpunkt, der innerhalb des gegebenen Zeitschwellwerts liegt. Konfiguration:

- **Zeitschwellwert:** Die maximale Dauer zwischen Datenpunkten in den beiden Quellen, um als Paar betrachtet zu werden.
- **Operation:** Die mathematische Operation, die mit den gepaarten Datenpunktwerten durchgeführt werden soll (Addition, Subtraktion, Multiplikation oder Division).
- **Bei Fehlen:** Gibt das Verhalten an, wenn ein Datenpunkt in der ersten Quelle keinen entsprechenden Datenpunkt in der zweiten Quelle innerhalb des gegebenen Zeitschwellwerts hat. Optionen umfassen:
  - **Überspringen:** Nichts für diesen Datenpunkt ausgeben.
  - **Durchleiten:** Den ursprünglichen Datenpunkt aus der ersten Quelle ohne Änderung ausgeben.

> **Hinweis:** Division durch Null ist ungültig und wird als fehlende Daten betrachtet. Die Konfiguration "Bei Fehlen" bestimmt, wie solche Fälle behandelt werden.
]],
    ["es"] = [[
Empareja cada punto de datos en la primera fuente de datos con el punto de datos correspondiente en la segunda fuente de datos y realiza una operación específica (suma, resta, multiplicación o división) en sus valores. La pareja para cada punto de datos es el primer punto de datos que cae dentro del umbral de tiempo dado. Configuración:

- **Umbral de Tiempo:** La duración máxima entre puntos de datos en las dos fuentes para ser considerados una pareja.
- **Operación:** La operación matemática a realizar en los valores de puntos de datos emparejados (suma, resta, multiplicación o división).
- **En Faltante:** Especifica el comportamiento cuando un punto de datos en la primera fuente no tiene un punto de datos correspondiente en la segunda fuente dentro del umbral de tiempo dado. Las opciones incluyen:
  - **Omitir:** No generar nada para ese punto de datos.
  - **Pasar Sin Cambios:** Generar el punto de datos original de la primera fuente sin modificación.

> **Nota:** La división por cero es inválida y se considera como datos faltantes. La configuración "En Faltante" determinará cómo se manejan tales casos.
]],
    ["fr"] = [[
Apparie chaque point de données dans la première source de données avec le point de données correspondant dans la deuxième source de données et effectue une opération spécifiée (addition, soustraction, multiplication ou division) sur leurs valeurs. La paire pour chaque point de données est le premier point de données qui tombe dans le seuil de temps donné. Configuration:

- **Seuil de Temps:** La durée maximale entre les points de données dans les deux sources pour être considérés comme une paire.
- **Opération:** L'opération mathématique à effectuer sur les valeurs des points de données appariés (addition, soustraction, multiplication ou division).
- **En Cas de Manque:** Spécifie le comportement lorsqu'un point de données dans la première source n'a pas de point de données correspondant dans la deuxième source dans le seuil de temps donné. Les options incluent:
  - **Ignorer:** Ne rien générer pour ce point de données.
  - **Laisser Passer:** Générer le point de données original de la première source sans modification.

> **Note:** La division par zéro est invalide et considérée comme des données manquantes. La configuration "En Cas de Manque" déterminera comment de tels cas sont traités.
]],
  },
  config = {
    duration {
      id = "threshold",
      name = "_time_threshold",
      default = core.DURATION.MINUTE,
    },
    enum {
      id = "operation",
      name = "_operation",
      options = { "_addition", "_subtraction", "_multiplication", "_division" },
      default = "_addition",
    },
    enum {
      id = "on_missing",
      name = "_on_missing",
      options = { "_skip", "_pass_through" },
      default = "_skip",
    },
  },

  -- Generator function
  generator = function(sources, config)
    local threshold = config.threshold or error("Missing 'threshold' in config")
    local operation = config.operation or error("Missing 'operation' in config")
    local on_missing = config.on_missing or error("Missing 'on_missing' in config")
    local source1 = sources[1] or error("Missing first data source")
    local source2 = sources[2] or error("Missing second data source")

    local source2_carry = nil

    return function()
      local result_dp = nil

      while true do
        local data_point = source1.dp()
        if not data_point then
          return nil
        end

        result_dp = data_point

        local time1 = data_point.timestamp
        local paired_dp = nil
        while true do
          local candidate_dp = source2_carry or source2.dp()
          source2_carry = nil
          if not candidate_dp then
            break
          end

          local time2 = candidate_dp.timestamp
          local time_diff = math.abs(time1 - time2)

          if time_diff <= threshold then
            paired_dp = candidate_dp
            break
          elseif time2 < time1 - threshold then
            source2_carry = candidate_dp
            break
          end
        end


        if paired_dp then
          if operation == "_addition" then
            result_dp.value = data_point.value + paired_dp.value
          elseif operation == "_subtraction" then
            result_dp.value = data_point.value - paired_dp.value
          elseif operation == "_multiplication" then
            result_dp.value = data_point.value * paired_dp.value
          elseif operation == "_division" then
            if paired_dp.value == 0 then
              if on_missing == "_skip" then
                result_dp = nil
              end
            else
              result_dp.value = data_point.value / paired_dp.value
            end
          else
            error("invalid operation: " .. operation)
          end
        elseif on_missing == "_pass_through" then
          break
        else
          result_dp = nil
        end

        if result_dp ~= nil then break end
      end

      return result_dp
    end
  end,
}
]=],
			version="1.0.3",
		},
		["periodic-data-points"]={
			script=[=[
-- Lua Function to generate periodic data points at regular intervals
-- This function creates data points with value=1 at deterministic timestamps

local core = require("tng.core")
local enum = require("tng.config").enum
local uint = require("tng.config").uint
local instant = require("tng.config").instant

local now_time = core.time()
local now = now_time and now_time.timestamp or 0

return {
    -- Configuration metadata
    id = "periodic-data-points",
    version = "1.1.1",
    inputCount = 0, -- This is a generator, not a transformer
    categories = { "_generators" },
    title = {
        ["en"] = "Periodic Data Points",
        ["de"] = "Periodische Datenpunkte",
        ["es"] = "Puntos de Datos Periódicos",
        ["fr"] = "Points de Données Périodiques",
    },
    description = {
        ["en"] = [[
Generates data points with value=1 at regular intervals going back in time.

Configuration:
- **Period**: Time period unit (Day, Week, Month, Year)
- **Period Multiplier**: Generate data point every N periods (e.g., every 2 days)
- **Cutoff**: Stop generating data points at this date/time

Generated data points will have:
- value = 1.0
- label = "" (empty)
- note = "" (empty)]],
        ["de"] = [[
Generiert Datenpunkte mit Wert=1 in regelmäßigen Abständen zurück in der Zeit.

Konfiguration:
- **Periode**: Zeitperiodeneinheit (Tag, Woche, Monat, Jahr)
- **Periodenmultiplikator**: Datenpunkt alle N Perioden generieren (z.B. alle 2 Tage)
- **Grenzwert**: Generierung bei diesem Datum/Zeit stoppen

Generierte Datenpunkte haben:
- Wert = 1.0
- Label = "" (leer)
- Notiz = "" (leer)]],
        ["es"] = [[
Genera puntos de datos con valor=1 a intervalos regulares retrocediendo en el tiempo.

Configuración:
- **Período**: Unidad de período de tiempo (Día, Semana, Mes, Año)
- **Multiplicador de Período**: Generar punto de datos cada N períodos (ej. cada 2 días)
- **Límite**: Detener generación de puntos de datos en esta fecha/hora

Los puntos de datos generados tendrán:
- valor = 1.0
- etiqueta = "" (vacío)
- nota = "" (vacío)]],
        ["fr"] = [[
Génère des points de données avec valeur=1 à intervalles réguliers en remontant dans le temps.

Configuration:
- **Période**: Unité de période de temps (Jour, Semaine, Mois, Année)
- **Multiplicateur de Période**: Générer un point de données tous les N périodes (ex. tous les 2 jours)
- **Limite**: Arrêter la génération de points de données à cette date/heure

Les points de données générés auront:
- valeur = 1.0
- étiquette = "" (vide)
- note = "" (vide)]],
    },
    config = {
        enum {
            id = "period",
            name = "_period",
            options = { "_day", "_week", "_month", "_year" },
            default = "_day",
        },
        uint {
            id = "period_multiplier",
            name = "_period_multiplier",
            default = 1,
        },
        instant {
            id = "cutoff",
            name = "_cutoff",
            default = now - (365 * core.DURATION.DAY),
        },
    },

    -- Generator function
    generator = function(_, config)
        -- Parse configuration with defaults
        local period_str = config and config.period or error("Period configuration is required")
        local period_multiplier = (config and config.period_multiplier) or 1
        -- Don't allow 0 multiplier, fallback to 1
        if period_multiplier == 0 then
            period_multiplier = 1
        end
        local cutoff_timestamp = config and config.cutoff or error("Cutoff configuration is required")

        -- Map enum string to core.PERIOD constant
        local period_map = {
            ["_day"] = core.PERIOD.DAY,
            ["_week"] = core.PERIOD.WEEK,
            ["_month"] = core.PERIOD.MONTH,
            ["_year"] = core.PERIOD.YEAR,
        }
        local period = period_map[period_str]

        -- Get current time for comparison
        local now = core.time().timestamp

        -- If cutoff is in the future, no data points to generate
        if cutoff_timestamp > now then
            return function()
                return nil
            end
        end

        -- Estimate number of periods elapsed since anchor
        local elapsed_ms = now - cutoff_timestamp
        local estimated_periods
        local period_duration_ms

        if period == core.PERIOD.DAY then
            period_duration_ms = period_multiplier * core.DURATION.DAY
        elseif period == core.PERIOD.WEEK then
            period_duration_ms = period_multiplier * core.DURATION.WEEK
        elseif period == core.PERIOD.MONTH then
            -- Average month length: 30.44 days
            period_duration_ms = period_multiplier * 30.44 * core.DURATION.DAY
        elseif period == core.PERIOD.YEAR then
            -- Average year length: 365.25 days
            period_duration_ms = period_multiplier * 365.25 * core.DURATION.DAY
        else
            error("Invalid period: " .. tostring(period_str))
        end

        estimated_periods = math.floor(elapsed_ms / period_duration_ms)

        local cutoff_date = core.date(cutoff_timestamp)

        -- Jump close to now with one large shift
        local candidate = core.shift(cutoff_date, period, estimated_periods * period_multiplier)

        -- Fine-tune: shift forward until we pass "now"
        while candidate.timestamp <= now do
            candidate = core.shift(candidate, period, period_multiplier)
        end

        -- Back up one step to get the most recent data point <= now
        local current = core.shift(candidate, period, -period_multiplier)

        -- Return iterator function
        return function()
            -- Check if we've gone past the cutoff (with 1 second tolerance for millisecond precision loss)
            if current.timestamp < cutoff_timestamp - 1000 then
                return nil
            end

            -- Create data point at current timestamp
            local data_point = {
                timestamp = current.timestamp,
                offset = current.offset,
                value = 1.0,
                label = "",
                note = "",
            }

            -- Shift backwards by period * period_multiplier for next iteration
            current = core.shift(current, period, -period_multiplier)

            return data_point
        end
    end,
}
]=],
			version="1.1.1",
		},
		["random-value"]={
			script=[=[
-- Lua Function to override data point values with random values
-- This function replaces all incoming data point values with random numbers between min and max

local number = require("tng.config").number
local uint = require("tng.config").uint
local core = require("tng.core")
local random = require("tng.random")

local now = core.time()
local default_seed = now and now.timestamp or 0

return {
    -- Configuration metadata
    id = "random-value",
    version = "2.0.0",
    inputCount = 1,
    categories = { "_randomisers" },
    title = {
        ["en"] = "Random Value",
        ["de"] = "Zufälliger Wert",
        ["es"] = "Valor Aleatorio",
        ["fr"] = "Valeur Aléatoire",
    },
    description = {
        ["en"] = [[
Replaces all incoming data point values with random numbers between min and max.

Configuration:
- **Min Value**: The minimum value for random generation
- **Max Value**: The maximum value for random generation
- **Seed**: Random seed for reproducible results (defaults to current UTC timestamp)

The function automatically swaps min and max if max is smaller than min.]],
        ["de"] = [[
Ersetzt alle eingehenden Datenpunktwerte durch Zufallszahlen zwischen Min und Max.

Konfiguration:
- **Minimalwert**: Der Minimalwert für die Zufallsgenerierung
- **Maximalwert**: Der Maximalwert für die Zufallsgenerierung
- **Seed**: Zufalls-Seed für reproduzierbare Ergebnisse (Standard: aktueller UTC-Zeitstempel)

Die Funktion tauscht automatisch Min und Max, wenn Max kleiner als Min ist.]],
        ["es"] = [[
Reemplaza todos los valores de puntos de datos entrantes con números aleatorios entre mín y máx.

Configuración:
- **Valor Mínimo**: El valor mínimo para la generación aleatoria
- **Valor Máximo**: El valor máximo para la generación aleatoria
- **Semilla**: Semilla aleatoria para resultados reproducibles (predeterminado: marca de tiempo UTC actual)

La función intercambia automáticamente mín y máx si máx es menor que mín.]],
        ["fr"] = [[
Remplace toutes les valeurs de points de données entrantes par des nombres aléatoires entre min et max.

Configuration:
- **Valeur Minimale**: La valeur minimale pour la génération aléatoire
- **Valeur Maximale**: La valeur maximale pour la génération aléatoire
- **Graine**: Graine aléatoire pour des résultats reproductibles (par défaut: horodatage UTC actuel)

La fonction échange automatiquement min et max si max est inférieur à min.]],
    },
    config = {
        number {
            id = "min_value",
            name = "_min_value",
            default = 0.0,
        },
        number {
            id = "max_value",
            name = "_max_value",
            default = 1.0,
        },
        uint {
            id = "seed",
            name = "_seed",
            default = default_seed,
        },
    },

    -- Generator function
    generator = function(source, config)
        local min_val = config and config.min_value or 0.0
        local max_val = config and config.max_value or 1.0
        local seed = config and config.seed or core.time().timestamp

        -- Ensure min is always the smaller value
        if min_val > max_val then
            min_val, max_val = max_val, min_val
        end

        return function()
            local data_point = source.dp()
            if not data_point then
                return nil
            end

            -- Generate random value between min and max
            local rng = random.new_seeded_random(seed, data_point.timestamp)
            data_point.value = rng:next(min_val, max_val)

            return data_point
        end
    end,
}
]=],
			version="2.0.0",
		},
		round={
			script=[=[
-- Lua Function to round values
-- Rounds each data point's value to the nearest multiple of a specified number

local number = require("tng.config").number

return {
	-- Configuration metadata
	id = "round",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_arithmetic"},
	title = {
		["en"] = "Round",
		["de"] = "Runden",
		["es"] = "Redondear",
		["fr"] = "Arrondir",
	},
	description = {
		["en"] = [[
Rounds each data point's value to the nearest multiple of a specified number.

Configuration:
- **Nearest**: Round to the nearest multiple of this number (default: 1.0)
]],
		["de"] = [[
Rundet den Wert jedes Datenpunkts auf das nächste Vielfache einer angegebenen Zahl.

Konfiguration:
- **Nächste**: Auf das nächste Vielfache dieser Zahl runden (Standard: 1.0)
]],
		["es"] = [[
Redondea el valor de cada punto de datos al múltiplo más cercano de un número especificado.

Configuración:
- **Más cercano**: Redondear al múltiplo más cercano de este número (predeterminado: 1.0)
]],
		["fr"] = [[
Arrondit la valeur de chaque point de données au multiple le plus proche d'un nombre spécifié.

Configuration:
- **Plus proche**: Arrondir au multiple le plus proche de ce nombre (par défaut: 1.0)
]],
	},
	config = {
		number {
			id = "nearest",
			default = 1.0,
			name = {
				["en"] = "Nearest",
				["de"] = "Nächste",
				["es"] = "Más cercano",
				["fr"] = "Plus proche",
			},
		},
	},

	-- Generator function
	generator = function(source, config)
		local nearest = config and config.nearest or 1.0

		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			-- Round to nearest multiple
			data_point.value = math.floor((data_point.value / nearest) + 0.5) * nearest

			return data_point
		end
	end,
}
]=],
			version="1.0.0",
		},
		["snap-time-to"]={
			script=[=[
-- Lua Function to snap data point timestamps to a specific time of day
-- This function adjusts timestamps to the specified time of day based on the direction (next, previous, or nearest)

local localtime = require("tng.config").localtime
local enum = require("tng.config").enum
local core = require("tng.core")

return {
  -- Configuration metadata
  id = "snap-time-to",
  version = "1.0.1",
  inputCount = 1,
  categories = { "_time" },

  title = {
    ["en"] = "Snap Time To",
    ["de"] = "Zeit Einrasten Auf",
    ["es"] = "Ajustar Tiempo A",
    ["fr"] = "Aligner l'Heure Sur",
  },

  description = {
    ["en"] = [[
Snaps data point timestamps to a specific time of day.

- Time of Day: The target time (e.g., 09:30:00)
- Direction: Next, Previous, or Nearest occurrence of that time
]],
    ["de"] = [[
Rastet Datenpunkt-Zeitstempel auf eine bestimmte Tageszeit ein.

- Tageszeit: Die Zielzeit (z.B. 09:30:00)
- Richtung: Nächste, Vorherige oder Nächstgelegene Occurrence dieser Zeit
]],
    ["es"] = [[
Ajusta las marcas de tiempo de los puntos de datos a una hora específica del día.

- Hora del Día: La hora objetivo (ej. 09:30:00)
- Dirección: Siguiente, Anterior, o Más Cercana ocurrencia de esa hora
]],
    ["fr"] = [[
Aligne les horodatages des points de données sur une heure spécifique de la journée.

- Heure du Jour: L'heure cible (ex. 09:30:00)
- Direction: Prochaine, Précédente, ou Plus Proche occurrence de cette heure
]],
  },

  config = {
    localtime {
      id = "target_time",
      name = {
        ["en"] = "Time of Day",
        ["de"] = "Tageszeit",
        ["es"] = "Hora del Día",
        ["fr"] = "Heure du Jour",
      },
      default = 9 * core.DURATION.HOUR, -- 09:00:00
    },
    enum {
      id = "direction",
      name = {
        ["en"] = "Direction",
        ["de"] = "Richtung",
        ["es"] = "Dirección",
        ["fr"] = "Direction",
      },
      options = { "_next", "_nearest", "_last" },
      default = "_nearest",
    },
  },

  -- Generator function
  generator = function(source, config)
    local target_time = config and config.target_time or error("Target time is required")
    local direction = config and config.direction or error("Direction is required")

    return function()
      local data_point = source.dp()
      if not data_point then
        return nil
      end

      -- Get the date components of the data point
      local date = core.date(data_point)

      -- Calculate the target time on the same date
      local same_day_target = core.time({
        year = date.year,
        month = date.month,
        day = date.day,
        hour = 0,
        min = 0,
        sec = 0,
        zone = date.zone
      })
      same_day_target = core.shift(same_day_target, target_time)

      local new_timestamp

      if direction == "_next" then
        -- Find next occurrence of target time
        if data_point.timestamp <= same_day_target.timestamp then
          new_timestamp = same_day_target
        else
          -- Next day
          new_timestamp = core.shift(same_day_target, core.PERIOD.DAY)
        end
      elseif direction == "_last" then
        -- Find previous occurrence of target time
        if data_point.timestamp >= same_day_target.timestamp then
          new_timestamp = same_day_target
        else
          -- Previous day
          new_timestamp = core.shift(same_day_target, core.PERIOD.DAY, -1)
        end
      else -- "_nearest"
        -- Find nearest occurrence of target time
        local other_target
        if data_point.timestamp <= same_day_target.timestamp then
          other_target = core.shift(same_day_target, core.PERIOD.DAY, -1)
        else
          other_target = core.shift(same_day_target, core.PERIOD.DAY)
        end

        local diff_same = math.abs(data_point.timestamp - same_day_target.timestamp)
        local diff_other = math.abs(data_point.timestamp - other_target.timestamp)

        if diff_same <= diff_other then
          new_timestamp = same_day_target
        else
          new_timestamp = other_target
        end
      end

      -- Return data point with adjusted timestamp
      return {
        timestamp = new_timestamp.timestamp,
        offset = new_timestamp.offset,
        value = data_point.value,
        label = data_point.label,
        note = data_point.note,
      }
    end
  end,
}
]=],
			version="1.0.1",
		},
		["snap-to-weekday"]={
			script=[=[
-- Lua Function to snap data point timestamps to the same local time on a specific weekday
-- This function adjusts timestamps to the same local time but on the specified weekday based on the direction (next, last, or nearest)

local enum = require("tng.config").enum
local core = require("tng.core")

return {
  -- Configuration metadata
  id = "snap-to-weekday",
  version = "1.0.0",
  inputCount = 1,
  categories = { "_time" },

  title = {
    ["en"] = "Snap To Weekday",
    ["de"] = "Auf Wochentag Einrasten",
    ["es"] = "Ajustar A Día De La Semana",
    ["fr"] = "Aligner Sur Jour De La Semaine",
  },

  description = {
    ["en"] = [[
Snaps data point timestamps to the same local time on a specific weekday.

- Weekday: The target day of the week (Monday through Sunday)
- Direction: Last, Nearest, or Next occurrence of that local time on that weekday

The data point keeps its original time of day but moves to the specified weekday.
]],
    ["de"] = [[
Rastet Datenpunkt-Zeitstempel auf die gleiche Ortszeit an einem bestimmten Wochentag ein.

- Wochentag: Der Ziel-Wochentag (Montag bis Sonntag)
- Richtung: Letzte, Nächstgelegene oder Nächste Occurrence dieser Ortszeit an diesem Wochentag

Der Datenpunkt behält seine ursprüngliche Tageszeit bei, wird aber auf den angegebenen Wochentag verschoben.
]],
    ["es"] = [[
Ajusta las marcas de tiempo de los puntos de datos a la misma hora local en un día específico de la semana.

- Día de la Semana: El día objetivo de la semana (Lunes a Domingo)
- Dirección: Última, Más Cercana, o Siguiente ocurrencia de esa hora local en ese día de la semana

El punto de datos mantiene su hora original del día pero se mueve al día de la semana especificado.
]],
    ["fr"] = [[
Aligne les horodatages des points de données sur la même heure locale d'un jour spécifique de la semaine.

- Jour de la Semaine: Le jour cible de la semaine (Lundi à Dimanche)
- Direction: Dernière, Plus Proche, ou Prochaine occurrence de cette heure locale ce jour de la semaine

Le point de données conserve son heure d'origine mais se déplace vers le jour de la semaine spécifié.
]],
  },

  config = {
    enum {
      id = "target_weekday",
      name = {
        ["en"] = "Weekday",
        ["de"] = "Wochentag",
        ["es"] = "Día de la Semana",
        ["fr"] = "Jour de la Semaine",
      },
      options = { "_monday", "_tuesday", "_wednesday", "_thursday", "_friday", "_saturday", "_sunday" },
      default = "_monday",
    },
    enum {
      id = "direction",
      name = {
        ["en"] = "Direction",
        ["de"] = "Richtung",
        ["es"] = "Dirección",
        ["fr"] = "Direction",
      },
      options = { "_next", "_nearest", "_last" },
      default = "_nearest",
    },
  },

  -- Generator function
  generator = function(source, config)
    local target_weekday = config and config.target_weekday or error("target_weekday is required")
    local direction = config and config.direction or error("direction is required")

    -- Map weekday strings to numbers (Monday = 1, Sunday = 7)
    local weekday_map = {
      ["_monday"] = 1,
      ["_tuesday"] = 2,
      ["_wednesday"] = 3,
      ["_thursday"] = 4,
      ["_friday"] = 5,
      ["_saturday"] = 6,
      ["_sunday"] = 7,
    }
    local target_wday = weekday_map[target_weekday]
    if not target_wday then
      error("Invalid weekday: " .. target_weekday)
    end

    return function()
      local data_point = source.dp()
      if not data_point then
        return nil
      end

      -- Get the date components of the data point
      local date = core.date(data_point)
      local current_wday = date.wday

      -- Calculate days difference to target weekday
      local days_to_target = (target_wday - current_wday) % 7

      -- Calculate the target time on the target weekday in the same week
      -- Use the original time components from the data point
      local next_target = core.shift(data_point, core.PERIOD.DAY, days_to_target)

      local new_timestamp

      if days_to_target == 0 then
        -- Already on target weekday, no change needed
        new_timestamp = data_point
      elseif direction == "_next" then
        new_timestamp = next_target
      elseif direction == "_last" then
        new_timestamp = core.shift(next_target, core.PERIOD.WEEK, -1)
      else -- "_nearest"
        -- Find nearest occurrence of same time on target weekday
        local last_target = core.shift(next_target, core.PERIOD.WEEK, -1)
        local next_diff = math.abs(next_target.timestamp - data_point.timestamp)
        local last_diff = math.abs(data_point.timestamp - last_target.timestamp)

        if next_diff < last_diff then
          new_timestamp = next_target
        else
          new_timestamp = last_target
        end
      end

      -- Return data point with adjusted timestamp
      return {
        timestamp = new_timestamp.timestamp,
        offset = new_timestamp.offset,
        value = data_point.value,
        label = data_point.label,
        note = data_point.note,
      }
    end
  end,
}
]=],
			version="1.0.0",
		},
		["swap-label-note"]={
			script=[=[
-- Lua Function to swap label and note fields
-- Swaps the label and note of each data point

return {
	-- Configuration metadata
	id = "swap-label-note",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_transform"},
	title = {
		["en"] = "Swap Label and Note",
		["de"] = "Label und Notiz tauschen",
		["es"] = "Intercambiar etiqueta y nota",
		["fr"] = "Échanger étiquette et note",
	},
	description = {
		["en"] = [[
Swaps the label and note fields of each data point.
]],
		["de"] = [[
Tauscht die Label- und Notizfelder jedes Datenpunkts aus.
]],
		["es"] = [[
Intercambia los campos de etiqueta y nota de cada punto de datos.
]],
		["fr"] = [[
Échange les champs étiquette et note de chaque point de données.
]],
	},
	config = {},

	-- Generator function
	generator = function(source)
		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			-- Swap label and note
			local temp = data_point.label
			data_point.label = data_point.note
			data_point.note = temp

			return data_point
		end
	end,
}
]=],
			version="1.0.0",
		},
		["time-between"]={
			script=[=[
-- Lua Function to calculate time between data points
-- Outputs the duration in seconds between each data point and the previous one

local core = require("tng.core")
local checkbox = require("tng.config").checkbox

return {
	-- Configuration metadata
	id = "time-between",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Time Between",
		["de"] = "Zeit dazwischen",
		["es"] = "Tiempo entre",
		["fr"] = "Temps entre",
	},
	description = {
		["en"] = [[
Calculates the duration in seconds between each data point and the previous one. The output value is the time difference in seconds and can be treated as a duration.

Configuration:
- **Include Time to First**: Include the time between now and the first data point (default: false)
]],
		["de"] = [[
Berechnet die Dauer in Sekunden zwischen jedem Datenpunkt und dem vorherigen. Der Ausgabewert ist die Zeitdifferenz in Sekunden und kann als Dauer behandelt werden.

Konfiguration:
- **Zeit zum Ersten einschließen**: Die Zeit zwischen jetzt und dem ersten Datenpunkt einschließen (Standard: false)
]],
		["es"] = [[
Calcula la duración en segundos entre cada punto de datos y el anterior. El valor de salida es la diferencia de tiempo en segundos y puede tratarse como una duración.

Configuración:
- **Incluir tiempo al primero**: Incluir el tiempo entre ahora y el primer punto de datos (predeterminado: false)
]],
		["fr"] = [[
Calcule la durée en secondes entre chaque point de données et le précédent. La valeur de sortie est la différence de temps en secondes et peut être traitée comme une durée.

Configuration:
- **Inclure le temps jusqu'au premier**: Inclure le temps entre maintenant et le premier point de données (par défaut: false)
]],
	},
	config = {
		checkbox {
			id = "include_first",
			default = false,
			name = {
				["en"] = "Include Time to First",
				["de"] = "Zeit zum Ersten einschließen",
				["es"] = "Incluir tiempo al primero",
				["fr"] = "Inclure le temps jusqu'au premier",
			},
		},
	},

	-- Generator function
	generator = function(source, config)
		local include_first = config and config.include_first or false
		local previous_point = nil

		return function()
			-- Initialize on first call
			if previous_point == nil then
				local first_point = source.dp()
				if not first_point then
					return nil
				end

				previous_point = first_point

				if include_first then
					-- Return synthetic point with time from now to first
					local now = core.time().timestamp
					local duration_seconds = (now - first_point.timestamp) / 1000.0

					return {
						timestamp = first_point.timestamp,
						offset = first_point.offset,
						value = duration_seconds,
						label = "",
						note = "",
					}
				end
			end

			-- Get next data point
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			-- Calculate duration from previous to current
			local duration_seconds = (previous_point.timestamp - data_point.timestamp) / 1000.0

			-- Create output point using previous point's identity
			local output_point = {
				timestamp = previous_point.timestamp,
				offset = previous_point.offset,
				value = duration_seconds,
				label = previous_point.label,
				note = previous_point.note,
			}

			-- Update state for next iteration
			previous_point = data_point

			return output_point
		end
	end,
}
]=],
			version="1.0.0",
		},
		["value-difference"]={
			script=[=[
-- Lua Function to calculate value differences
-- Outputs the difference between each data point's value and the next one

return {
	-- Configuration metadata
	id = "value-difference",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_arithmetic", "_transform"},
	title = {
		["en"] = "Value Difference",
		["de"] = "Wertdifferenz",
		["es"] = "Diferencia de valor",
		["fr"] = "Différence de valeur",
	},
	description = {
		["en"] = [[
Calculates the difference between each data point's value and the next one. Each output point has its original identity with the value set to the difference.
]],
		["de"] = [[
Berechnet die Differenz zwischen dem Wert jedes Datenpunkts und dem nächsten. Jeder Ausgabepunkt hat seine ursprüngliche Identität mit dem Wert auf die Differenz gesetzt.
]],
		["es"] = [[
Calcula la diferencia entre el valor de cada punto de datos y el siguiente. Cada punto de salida tiene su identidad original con el valor establecido en la diferencia.
]],
		["fr"] = [[
Calcule la différence entre la valeur de chaque point de données et la suivante. Chaque point de sortie a son identité d'origine avec la valeur définie sur la différence.
]],
	},
	config = {},

	-- Generator function
	generator = function(source)
		local next_point = nil

		return function()
			-- Pre-load the next point on first call
			if next_point == nil then
				next_point = source.dp()
				if not next_point then
					return nil
				end
			end

			-- Current point is what we'll output
			local current_point = next_point

			-- Pre-load the next point for the next iteration
			next_point = source.dp()
			if not next_point then
				-- No more points, can't calculate difference
				return nil
			end

			-- Calculate difference (current - next)
			local difference = current_point.value - next_point.value

			-- Return current point with difference as value
			return {
				timestamp = current_point.timestamp,
				offset = current_point.offset,
				value = difference,
				label = current_point.label,
				note = current_point.note,
			}
		end
	end,
}
]=],
			version="1.0.0",
		},
		["value-to-day-of-month"]={
			script=[=[
-- Lua Function to set value to day of month
-- Sets each data point's value to its day of the month (1-31)

local core = require("tng.core")

return {
	-- Configuration metadata
	id = "value-to-day-of-month",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Value to Day of Month",
		["de"] = "Wert zu Tag des Monats",
		["es"] = "Valor a día del mes",
		["fr"] = "Valeur au jour du mois",
	},
	description = {
		["en"] = [[
Sets each data point's value to its day of the month (1-31).
]],
		["de"] = [[
Setzt den Wert jedes Datenpunkts auf seinen Tag des Monats (1-31).
]],
		["es"] = [[
Establece el valor de cada punto de datos en su día del mes (1-31).
]],
		["fr"] = [[
Définit la valeur de chaque point de données sur son jour du mois (1-31).
]],
	},
	config = {},

	-- Generator function
	generator = function(source)
		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			local date = core.date(data_point)
			data_point.value = date.day

			return data_point
		end
	end,
}
]=],
			version="1.0.0",
		},
		["value-to-day-of-week"]={
			script=[=[
-- Lua Function to set value to day of week
-- Sets each data point's value to its day of the week (1-7, Monday is 1)

local core = require("tng.core")

return {
	-- Configuration metadata
	id = "value-to-day-of-week",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Value to Day of Week",
		["de"] = "Wert zu Wochentag",
		["es"] = "Valor a día de la semana",
		["fr"] = "Valeur au jour de la semaine",
	},
	description = {
		["en"] = [[
Sets each data point's value to its day of the week (1-7, where Monday is 1 and Sunday is 7).
]],
		["de"] = [[
Setzt den Wert jedes Datenpunkts auf seinen Wochentag (1-7, wobei Montag 1 und Sonntag 7 ist).
]],
		["es"] = [[
Establece el valor de cada punto de datos en su día de la semana (1-7, donde lunes es 1 y domingo es 7).
]],
		["fr"] = [[
Définit la valeur de chaque point de données sur son jour de la semaine (1-7, où lundi est 1 et dimanche est 7).
]],
	},
	config = {},

	-- Generator function
	generator = function(source)
		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			-- Get the date from the timestamp
			local date = core.date(data_point)

			-- Set value to day of week
			data_point.value = date.wday

			return data_point
		end
	end,
}
]=],
			version="1.0.0",
		},
		["value-to-hour-of-day"]={
			script=[=[
-- Lua Function to set value to hour of day
-- Sets each data point's value to its hour of the day (0-23)

local core = require("tng.core")

return {
	-- Configuration metadata
	id = "value-to-hour-of-day",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Value to Hour (0-23)",
		["de"] = "Wert zu Stunde (0-23)",
		["es"] = "Valor a hora (0-23)",
		["fr"] = "Valeur à l'heure (0-23)",
	},
	description = {
		["en"] = [[
Sets each data point's value to its hour of the day (0-23).
]],
		["de"] = [[
Setzt den Wert jedes Datenpunkts auf seine Stunde des Tages (0-23).
]],
		["es"] = [[
Establece el valor de cada punto de datos en su hora del día (0-23).
]],
		["fr"] = [[
Définit la valeur de chaque point de données sur son heure du jour (0-23).
]],
	},
	config = {},

	-- Generator function
	generator = function(source)
		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			local date = core.date(data_point)
			data_point.value = date.hour or 0

			return data_point
		end
	end,
}
]=],
			version="1.0.0",
		},
		["value-to-minute-of-hour"]={
			script=[=[
-- Lua Function to set value to minute of hour
-- Sets each data point's value to its minute of the hour (0-59)

local core = require("tng.core")

return {
	-- Configuration metadata
	id = "value-to-minute-of-hour",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Value to Minute of Hour",
		["de"] = "Wert zu Minute der Stunde",
		["es"] = "Valor a minuto de la hora",
		["fr"] = "Valeur à la minute de l'heure",
	},
	description = {
		["en"] = [[
Sets each data point's value to its minute of the hour (0-59).
]],
		["de"] = [[
Setzt den Wert jedes Datenpunkts auf seine Minute der Stunde (0-59).
]],
		["es"] = [[
Establece el valor de cada punto de datos en su minuto de la hora (0-59).
]],
		["fr"] = [[
Définit la valeur de chaque point de données sur sa minute de l'heure (0-59).
]],
	},
	config = {},

	-- Generator function
	generator = function(source)
		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			local date = core.date(data_point)
			data_point.value = date.min or 0

			return data_point
		end
	end,
}
]=],
			version="1.0.0",
		},
		["value-to-month-of-year"]={
			script=[=[
-- Lua Function to set value to month of year
-- Sets each data point's value to its month of the year (1-12)

local core = require("tng.core")

return {
	-- Configuration metadata
	id = "value-to-month-of-year",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Value to Month of Year",
		["de"] = "Wert zu Monat des Jahres",
		["es"] = "Valor a mes del año",
		["fr"] = "Valeur au mois de l'année",
	},
	description = {
		["en"] = [[
Sets each data point's value to its month of the year (1-12, where January is 1).
]],
		["de"] = [[
Setzt den Wert jedes Datenpunkts auf seinen Monat des Jahres (1-12, wobei Januar 1 ist).
]],
		["es"] = [[
Establece el valor de cada punto de datos en su mes del año (1-12, donde enero es 1).
]],
		["fr"] = [[
Définit la valeur de chaque point de données sur son mois de l'année (1-12, où janvier est 1).
]],
	},
	config = {},

	-- Generator function
	generator = function(source)
		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			local date = core.date(data_point)
			data_point.value = date.month

			return data_point
		end
	end,
}
]=],
			version="1.0.0",
		},
		["value-to-second-of-minute"]={
			script=[=[
-- Lua Function to set value to second of minute
-- Sets each data point's value to its second of the minute (0-59)

local core = require("tng.core")

return {
	-- Configuration metadata
	id = "value-to-second-of-minute",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Value to Second of Minute",
		["de"] = "Wert zu Sekunde der Minute",
		["es"] = "Valor a segundo del minuto",
		["fr"] = "Valeur à la seconde de la minute",
	},
	description = {
		["en"] = [[
Sets each data point's value to its second of the minute (0-59).
]],
		["de"] = [[
Setzt den Wert jedes Datenpunkts auf seine Sekunde der Minute (0-59).
]],
		["es"] = [[
Establece el valor de cada punto de datos en su segundo del minuto (0-59).
]],
		["fr"] = [[
Définit la valeur de chaque point de données sur sa seconde de la minute (0-59).
]],
	},
	config = {},

	-- Generator function
	generator = function(source)
		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			local date = core.date(data_point)
			data_point.value = date.sec or 0

			return data_point
		end
	end,
}
]=],
			version="1.0.0",
		},
		["value-to-time-of-day"]={
			script=[=[
-- Lua Function to set value to time of day
-- Sets each data point's value to the time of day in seconds since midnight

local core = require("tng.core")

return {
	-- Configuration metadata
	id = "value-to-time-of-day",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Value to Time of Day",
		["de"] = "Wert zu Tageszeit",
		["es"] = "Valor a hora del día",
		["fr"] = "Valeur à l'heure de la journée",
	},
	description = {
		["en"] = [[
Sets each data point's value to the time of day in seconds since midnight. The output is a duration value representing elapsed time since the start of the day.
]],
		["de"] = [[
Setzt den Wert jedes Datenpunkts auf die Tageszeit in Sekunden seit Mitternacht. Die Ausgabe ist ein Dauerwert, der die verstrichene Zeit seit Tagesbeginn darstellt.
]],
		["es"] = [[
Establece el valor de cada punto de datos en la hora del día en segundos desde la medianoche. La salida es un valor de duración que representa el tiempo transcurrido desde el comienzo del día.
]],
		["fr"] = [[
Définit la valeur de chaque point de données sur l'heure de la journée en secondes depuis minuit. La sortie est une valeur de durée représentant le temps écoulé depuis le début de la journée.
]],
	},
	config = {},

	-- Generator function
	generator = function(source)
		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			-- Get the date from the data point
			local date = core.date(data_point)

			-- Calculate seconds since midnight
			local seconds_since_midnight = (date.hour or 0) * 3600 + (date.min or 0) * 60 + (date.sec or 0)

			-- Set value to time of day in seconds
			data_point.value = seconds_since_midnight

			return data_point
		end
	end,
}
]=],
			version="1.0.0",
		},
		["value-to-year"]={
			script=[=[
-- Lua Function to set value to year
-- Sets each data point's value to its year

local core = require("tng.core")

return {
	-- Configuration metadata
	id = "value-to-year",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Value to Year",
		["de"] = "Wert zu Jahr",
		["es"] = "Valor a año",
		["fr"] = "Valeur à l'année",
	},
	description = {
		["en"] = [[
Sets each data point's value to its year (e.g., 2025).
]],
		["de"] = [[
Setzt den Wert jedes Datenpunkts auf sein Jahr (z.B. 2025).
]],
		["es"] = [[
Establece el valor de cada punto de datos en su año (p. ej., 2025).
]],
		["fr"] = [[
Définit la valeur de chaque point de données sur son année (par exemple, 2025).
]],
	},
	config = {},

	-- Generator function
	generator = function(source)
		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			local date = core.date(data_point)
			data_point.value = date.year

			return data_point
		end
	end,
}
]=],
			version="1.0.0",
		},
	},
	published_at="2025-11-07T21:09:35Z",
	translations={
		_addition={
			de="Addition",
			en="Addition",
			es="Suma",
			fr="Addition",
		},
		_all_fields={
			de="Alle Felder",
			en="All Fields",
			es="Todos los campos",
			fr="Tous les champs",
		},
		_arithmetic={
			de="Arithmetik",
			en="Arithmetic",
			es="Aritmética",
			fr="Arithmétique",
		},
		_case_sensitive={
			de="Groß-/Kleinschreibung beachten",
			en="Case Sensitive",
			es="Distinguir Mayúsculas",
			fr="Sensible à la Casse",
		},
		_combine={
			de="Kombinieren",
			en="Combine",
			es="Combinar",
			fr="Combiner",
		},
		_compare_by={
			de="Vergleichen nach",
			en="Compare By",
			es="Comparar por",
			fr="Comparer par",
		},
		_cutoff={
			de="Grenzwert",
			en="Cutoff",
			es="Límite",
			fr="Limite",
		},
		_day={
			de="Tag",
			en="Day",
			es="Día",
			fr="Jour",
		},
		_division={
			de="Division",
			en="Division",
			es="División",
			fr="Division",
		},
		_filter={
			de="Filter",
			en="Filter",
			es="Filtro",
			fr="Filtre",
		},
		_friday={
			de="Freitag",
			en="Friday",
			es="Viernes",
			fr="Vendredi",
		},
		_generators={
			de="Generatoren",
			en="Generators",
			es="Generadores",
			fr="Générateurs",
		},
		_label_and_note={
			de="Label und Notiz",
			en="Label and Note",
			es="Etiqueta y nota",
			fr="Étiquette et note",
		},
		_label_only={
			de="Nur Label",
			en="Label Only",
			es="Solo etiqueta",
			fr="Étiquette uniquement",
		},
		_last={
			de="Letzte",
			en="Last",
			es="Último",
			fr="Dernier",
		},
		_match_exactly={
			de="Exakt übereinstimmen",
			en="Match Exactly",
			es="Coincidir Exactamente",
			fr="Correspondance Exacte",
		},
		_max_value={
			de="Maximalwert",
			en="Max Value",
			es="Valor Máximo",
			fr="Valeur Maximale",
		},
		_min_value={
			de="Minimalwert",
			en="Min Value",
			es="Valor Mínimo",
			fr="Valeur Minimale",
		},
		_monday={
			de="Montag",
			en="Monday",
			es="Lunes",
			fr="Lundi",
		},
		_month={
			de="Monat",
			en="Month",
			es="Mes",
			fr="Mois",
		},
		_multiplication={
			de="Multiplikation",
			en="Multiplication",
			es="Multiplicación",
			fr="Multiplication",
		},
		_nearest={
			de="Nächstgelegene",
			en="Nearest",
			es="Más Cercano",
			fr="Le Plus Proche",
		},
		_next={
			de="Nächste",
			en="Next",
			es="Siguiente",
			fr="Suivant",
		},
		_note_only={
			de="Nur Notiz",
			en="Note Only",
			es="Solo nota",
			fr="Note uniquement",
		},
		_on_missing={
			de="Bei Fehlen",
			en="On Missing",
			es="En Faltante",
			fr="En Cas de Manque",
		},
		_operation={
			de="Operation",
			en="Operation",
			es="Operación",
			fr="Opération",
		},
		_pass_through={
			de="Durchleiten",
			en="Pass Through",
			es="Pasar Sin Cambios",
			fr="Laisser Passer",
		},
		_period={
			de="Periode",
			en="Period",
			es="Período",
			fr="Période",
		},
		_period_multiplier={
			de="Periodenmultiplikator",
			en="Period Multiplier",
			es="Multiplicador de Período",
			fr="Multiplicateur de Période",
		},
		_randomisers={
			de="Zufallsgeneratoren",
			en="Randomisers",
			es="Aleatorizadores",
			fr="Générateurs Aléatoires",
		},
		_reset_label={
			de="Zurücksetzen-Label",
			en="Reset Label",
			es="Etiqueta de Restablecimiento",
			fr="Étiquette de Réinitialisation",
		},
		_reset_on_label_match={
			de="Zurücksetzen bei Label-Übereinstimmung",
			en="Reset on Label Match",
			es="Restablecer al Coincidir Etiqueta",
			fr="Réinitialiser sur Correspondance d'Étiquette",
		},
		_saturday={
			de="Samstag",
			en="Saturday",
			es="Sábado",
			fr="Samedi",
		},
		_seed={
			de="Seed",
			en="Seed",
			es="Semilla",
			fr="Graine",
		},
		_skip={
			de="Überspringen",
			en="Skip",
			es="Omitir",
			fr="Ignorer",
		},
		_subtraction={
			de="Subtraktion",
			en="Subtraction",
			es="Resta",
			fr="Soustraction",
		},
		_sunday={
			de="Sonntag",
			en="Sunday",
			es="Domingo",
			fr="Dimanche",
		},
		_thursday={
			de="Donnerstag",
			en="Thursday",
			es="Jueves",
			fr="Jeudi",
		},
		_time={
			de="Zeit",
			en="Time",
			es="Tiempo",
			fr="Temps",
		},
		_time_threshold={
			de="Zeitschwellwert",
			en="Time Threshold",
			es="Umbral de Tiempo",
			fr="Seuil de Temps",
		},
		_transform={
			de="Transformieren",
			en="Transform",
			es="Transformar",
			fr="Transformer",
		},
		_tuesday={
			de="Dienstag",
			en="Tuesday",
			es="Martes",
			fr="Mardi",
		},
		_value_and_label={
			de="Wert und Label",
			en="Value and Label",
			es="Valor y etiqueta",
			fr="Valeur et étiquette",
		},
		_value_and_note={
			de="Wert und Notiz",
			en="Value and Note",
			es="Valor y nota",
			fr="Valeur et note",
		},
		_value_only={
			de="Nur Wert",
			en="Value Only",
			es="Solo valor",
			fr="Valeur uniquement",
		},
		_wednesday={
			de="Mittwoch",
			en="Wednesday",
			es="Miércoles",
			fr="Mercredi",
		},
		_week={
			de="Woche",
			en="Week",
			es="Semana",
			fr="Semaine",
		},
		_year={
			de="Jahr",
			en="Year",
			es="Año",
			fr="Année",
		},
	},
}