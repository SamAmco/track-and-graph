-- Lua Function to multiply data point values by a configurable number
-- This function multiplies all incoming data point values by a specified multiplier

return {
    -- Configuration metadata
    version = "1.0.0",
    inputCount = 1,
    title = {
        ["en"] = "Multiply Values",
        ["de"] = "Werte multiplizieren",
        ["es"] = "Multiplicar Valores",
        ["fr"] = "Multiplier les Valeurs"
    },
    config = {
        {
            id = "multiplier",
            type = "number",
            name = {
                ["en"] = "Multiplier",
                ["de"] = "Multiplikator",
                ["es"] = "Multiplicador",
                ["fr"] = "Multiplicateur"
            }
        }
    },

    -- Generator function
    generator = function(data_sources, config)
        local source = data_sources[1]
        local multiplier = config and config.multiplier or 1.0

        local data_point = source.dp()
        while data_point do
            -- Create a new data point with the multiplied value
            local new_data_point = {
                timestamp = data_point.timestamp,
                value = data_point.value * multiplier,
                label = data_point.label,
                note = data_point.note
            }

            coroutine.yield(new_data_point)
            data_point = source.dp()
        end
    end
}
