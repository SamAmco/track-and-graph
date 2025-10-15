-- Lua Function to override the note of all data points with a configurable string
-- This function sets all incoming data point notes to a specified value

return {
    -- Configuration metadata
    id = "override-note",
    version = "1.0.0",
    inputCount = 1,
    categories = {"transform"},
    title = {
        ["en"] = "Override Note",
        ["de"] = "Notiz überschreiben",
        ["es"] = "Sobrescribir Nota",
        ["fr"] = "Remplacer la Note",
    },
    description = {
        ["en"] = "Sets all incoming data point notes to a specified value",
        ["de"] = "Setzt alle eingehenden Datenpunkt-Notizen auf einen bestimmten Wert",
        ["es"] = "Establece todas las notas de puntos de datos entrantes en un valor especificado",
        ["fr"] = "Définit toutes les notes de points de données entrantes sur une valeur spécifiée",
    },
    config = {
        {
            id = "new_note",
            type = "text",
            name = {
                ["en"] = "New Note",
                ["de"] = "Neue Notiz",
                ["es"] = "Nueva Nota",
                ["fr"] = "Nouvelle Note",
            },
        },
    },

    -- Generator function
    generator = function(source, config)
        local new_note = config and config.new_note

        return function()
            local data_point = source.dp()
            if not data_point then
                return nil
            end

            if not new_note then
                return data_point
            end
            data_point.note = new_note

            return data_point
        end
    end,
}
