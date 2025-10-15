-- Lua Function to override the label of all data points with a configurable string
-- This function sets all incoming data point labels to a specified value

return {
    -- Configuration metadata
    id = "override-label",
    version = "1.0.0",
    inputCount = 1,
    categories = {"transform"},
    title = {
        ["en"] = "Override Label",
        ["de"] = "Label überschreiben",
        ["es"] = "Sobrescribir Etiqueta",
        ["fr"] = "Remplacer l'Étiquette",
    },
    description = {
        ["en"] = "Sets all incoming data point labels to a specified value",
        ["de"] = "Setzt alle eingehenden Datenpunkt-Labels auf einen bestimmten Wert",
        ["es"] = "Establece todas las etiquetas de puntos de datos entrantes en un valor especificado",
        ["fr"] = "Définit toutes les étiquettes de points de données entrantes sur une valeur spécifiée",
    },
    config = {
        {
            id = "new_label",
            type = "text",
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
