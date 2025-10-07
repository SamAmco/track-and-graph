-- Lua Function to multiply data point values by a configurable number
-- This function multiplies all incoming data point values by a specified multiplier

return {
    -- Configuration metadata
    id = "multiply",
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
