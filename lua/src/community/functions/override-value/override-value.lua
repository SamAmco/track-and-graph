-- Lua Function to override the value of all data points with a configurable number
-- This function sets all incoming data point values to a specified value

return {
    -- Configuration metadata
    id = "override-value",
    version = "1.0.0",
    inputCount = 1,
    title = {
        ["en"] = "Override Value",
        ["de"] = "Wert überschreiben",
        ["es"] = "Sobrescribir Valor",
        ["fr"] = "Remplacer la Valeur",
    },
    description = {
        ["en"] = "Sets all incoming data point values to a specified value",
        ["de"] = "Setzt alle eingehenden Datenpunktwerte auf einen bestimmten Wert",
        ["es"] = "Establece todos los valores de puntos de datos entrantes en un valor especificado",
        ["fr"] = "Définit toutes les valeurs de points de données entrantes sur une valeur spécifiée",
    },
    config = {
        {
            id = "new_value",
            type = "number",
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
