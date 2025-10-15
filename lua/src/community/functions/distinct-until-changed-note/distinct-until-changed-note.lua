-- Lua Function to filter out consecutive duplicate notes
-- Only passes through data points when the note changes from the previous one

return {
    -- Configuration metadata
    id = "distinct-until-changed-note",
    version = "1.0.0",
    inputCount = 1,
    categories = {"filter"},
    title = {
        ["en"] = "Distinct Until Changed (Note)",
        ["de"] = "Eindeutig bis geändert (Notiz)",
        ["es"] = "Distinto hasta cambio (Nota)",
        ["fr"] = "Distinct jusqu'au changement (Note)",
    },
    description = {
        ["en"] = "Filters out consecutive duplicate notes. Only data points where the note differs from the previous one will pass through.",
        ["de"] = "Filtert aufeinanderfolgende doppelte Notizen heraus. Nur Datenpunkte, bei denen sich die Notiz von der vorherigen unterscheidet, werden durchgelassen.",
        ["es"] = "Filtra notas duplicadas consecutivas. Solo los puntos de datos donde la nota difiere de la anterior pasarán.",
        ["fr"] = "Filtre les notes en double consécutives. Seuls les points de données où la note diffère de la précédente passeront.",
    },
    config = {},

    -- Generator function
    generator = function(source, config)
        local last_note = nil

        return function()
            while true do
                local data_point = source.dp()
                if not data_point then
                    return nil
                end

                local current_note = data_point.note
                if current_note ~= last_note then
                    last_note = current_note
                    return data_point
                end
            end
        end
    end,
}
