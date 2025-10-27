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
