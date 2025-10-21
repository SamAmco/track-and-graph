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
    version = "1.0.0",
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
            name = {
                ["en"] = "Case Sensitive",
                ["de"] = "Groß-/Kleinschreibung beachten",
                ["es"] = "Sensible a Mayúsculas",
                ["fr"] = "Sensible à la Casse"
            }
        },
        checkbox {
            id = "match_exactly",
            name = {
                ["en"] = "Match Exactly",
                ["de"] = "Exakt übereinstimmen",
                ["es"] = "Coincidir Exactamente",
                ["fr"] = "Correspondance Exacte"
            }
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
