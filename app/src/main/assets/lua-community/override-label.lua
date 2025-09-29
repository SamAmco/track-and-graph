-- Lua Function to override the label of all data points with a configurable string
-- This function sets all incoming data point labels to a specified value

return {
    -- Configuration metadata
    version = "1.0.0",
    inputCount = 1,
    title = {
        ["en"] = "Override Label",
        ["de"] = "Label überschreiben",
        ["es"] = "Sobrescribir Etiqueta",
        ["fr"] = "Remplacer l'Étiquette"
    },
    config = {
        {
            id = "new_label",
            type = "text",
            name = {
                ["en"] = "New Label",
                ["de"] = "Neues Label",
                ["es"] = "Nueva Etiqueta",
                ["fr"] = "Nouvelle Étiquette"
            }
        }
    },

    -- Generator function
    generator = function(data_sources, config)
        local source = data_sources[1]
        local new_label = config and config.new_label or ""
        
        -- Convert to string if needed
        if type(new_label) ~= "string" then
            new_label = tostring(new_label)
        end

        local data_point = source.dp()
        while data_point do
            -- Create a new data point with the overridden label
            local new_data_point = {
                timestamp = data_point.timestamp,
                value = data_point.value,
                label = new_label,  -- Override the label
                note = data_point.note
            }
            
            coroutine.yield(new_data_point)
            data_point = source.dp()
        end
    end
}
