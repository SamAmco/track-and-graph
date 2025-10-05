-- Example Lua Function with Input Count and Configuration
-- This function filters data points by label

local function match(data_point, filter_label)
    if
        filter_label == nil
        or filter_label == ""
        or data_point.label == filter_label
    then
        return true
    else
        return false
    end
end


return {
    -- Configuration metadata
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
        }
    },

    -- Generator function
    generator = function(source, config)
        local filter_label = config and config.filter_label

        return function()
            local data_point = source.dp()
            while data_point and not match(data_point, filter_label) do
                data_point = source.dp()
            end
            return data_point
        end
    end
}
