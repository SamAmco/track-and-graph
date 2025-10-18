-- Lua Function to filter data points by value (less than threshold)
-- Only passes through data points with values less than a threshold

return {
    -- Configuration metadata
    id = "filter-less-than",
    version = "1.0.0",
    inputCount = 1,
    categories = {"_filter"},
    title = {
        ["en"] = "Filter Less Than",
        ["de"] = "Filtern kleiner als",
        ["es"] = "Filtrar menor que",
        ["fr"] = "Filtrer inférieur à",
    },
    description = {
        ["en"] = [[Filters data points by value. Only data points with values less than the threshold will pass through.

Configuration:
• Threshold: The maximum value (exclusive by default)
• Include Equal: Also include values equal to the threshold (default: false)]],
        ["de"] = [[Filtert Datenpunkte nach Wert. Nur Datenpunkte mit Werten kleiner als der Schwellenwert werden durchgelassen.

Konfiguration:
• Schwellenwert: Der Maximalwert (standardmäßig exklusiv)
• Gleich einschließen: Werte gleich dem Schwellenwert auch einschließen (Standard: false)]],
        ["es"] = [[Filtra puntos de datos por valor. Solo los puntos de datos con valores menores que el umbral pasarán.

Configuración:
• Umbral: El valor máximo (exclusivo por defecto)
• Incluir igual: También incluir valores iguales al umbral (predeterminado: false)]],
        ["fr"] = [[Filtre les points de données par valeur. Seuls les points de données avec des valeurs inférieures au seuil passeront.

Configuration:
• Seuil: La valeur maximale (exclusive par défaut)
• Inclure égal: Inclure également les valeurs égales au seuil (par défaut: false)]],
    },
    config = {
        {
            id = "threshold",
            type = "number",
            name = {
                ["en"] = "Threshold",
                ["de"] = "Schwellenwert",
                ["es"] = "Umbral",
                ["fr"] = "Seuil",
            },
        },
        {
            id = "include_equal",
            type = "checkbox",
            name = {
                ["en"] = "Include Equal",
                ["de"] = "Gleich einschließen",
                ["es"] = "Incluir igual",
                ["fr"] = "Inclure égal",
            },
        },
    },

    -- Generator function
    generator = function(source, config)
        local threshold = config and config.threshold or 0.0
        local include_equal = config and config.include_equal or false

        return function()
            while true do
                local data_point = source.dp()
                if not data_point then
                    return nil
                end

                local passes
                if include_equal then
                    passes = data_point.value <= threshold
                else
                    passes = data_point.value < threshold
                end

                if passes then
                    return data_point
                end
            end
        end
    end,
}
