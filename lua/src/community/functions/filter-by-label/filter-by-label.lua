-- Example Lua Function with Input Count and Configuration
-- This function filters data points by label

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
    version = "1.0.0",
    inputCount = 1,
    title = {
        ["en"] = "Filter by Label",
        ["de"] = "Filtern nach Etikett",
        ["es"] = "Filtrar por Etiqueta",
        ["fr"] = "Filtrer par Étiquette"
    },
    config = {
        {
            id = "filter_label",
            type = "text",
            name = {
                ["en"] = "Filter Label",
                ["de"] = "Filter-Label",
                ["es"] = "Filtrar Etiqueta",
                ["fr"] = "Filtrer l'Étiquette"
            }
        },
        {
            id = "case_sensitive",
            type = "checkbox",
            name = {
                ["en"] = "Case Sensitive",
                ["de"] = "Groß-/Kleinschreibung beachten",
                ["es"] = "Sensible a Mayúsculas",
                ["fr"] = "Sensible à la Casse"
            }
        },
        {
            id = "match_exactly",
            type = "checkbox",
            name = {
                ["en"] = "Match Exactly",
                ["de"] = "Exakt übereinstimmen",
                ["es"] = "Coincidir Exactamente",
                ["fr"] = "Correspondance Exacte"
            }
        }
    },

    -- Generator function
    generator = function(source, config)
        local filter_label = config and config.filter_label
        local case_sensitive = config and config.case_sensitive or false
        local match_exactly = config and config.match_exactly or false

        return function()
            local data_point = source.dp()
            while data_point and not match(data_point, filter_label, case_sensitive, match_exactly) do
                data_point = source.dp()
            end
            return data_point
        end
    end
}
