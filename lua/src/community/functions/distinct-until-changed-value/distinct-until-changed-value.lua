-- Lua Function to filter out consecutive duplicate values
-- Only passes through data points when the value changes from the previous one

return {
    -- Configuration metadata
    id = "distinct-until-changed-value",
    version = "1.0.0",
    inputCount = 1,
    categories = {"filter"},
    title = {
        ["en"] = "Distinct Until Changed (Value)",
        ["de"] = "Eindeutig bis geändert (Wert)",
        ["es"] = "Distinto hasta cambio (Valor)",
        ["fr"] = "Distinct jusqu'au changement (Valeur)",
    },
    description = {
        ["en"] = "Filters out consecutive duplicate values. Only data points where the value differs from the previous one will pass through.",
        ["de"] = "Filtert aufeinanderfolgende doppelte Werte heraus. Nur Datenpunkte, bei denen sich der Wert vom vorherigen unterscheidet, werden durchgelassen.",
        ["es"] = "Filtra valores duplicados consecutivos. Solo los puntos de datos donde el valor difiere del anterior pasarán.",
        ["fr"] = "Filtre les valeurs en double consécutives. Seuls les points de données où la valeur diffère de la précédente passeront.",
    },
    config = {},

    -- Generator function
    generator = function(source, config)
        local last_value = nil

        return function()
            while true do
                local data_point = source.dp()
                if not data_point then
                    return nil
                end

                local current_value = data_point.value
                if current_value ~= last_value then
                    last_value = current_value
                    return data_point
                end
            end
        end
    end,
}
