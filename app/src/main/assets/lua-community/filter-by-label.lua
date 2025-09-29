-- Example Lua Function with Input Count and Configuration
-- This function filters data points by label

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
    generator = function(data_sources, config)
        local source = data_sources[1]
        local filter_label = config and config.filter_label

        local data_point = source.dp()
        while data_point do
            -- If no filter is set, return all data points
            -- Otherwise, only yield data points that match the filter label
            if filter_label == nil or filter_label == "" or data_point.label == filter_label then
                coroutine.yield(data_point)
            end
            data_point = source.dp()
        end
    end
}
