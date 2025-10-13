-- Lua Function to offset data point values by a configurable number
-- This function adds a constant offset to all incoming data point values

return {
    -- Configuration metadata
    id = "offset-value",
    version = "1.0.0",
    inputCount = 1,
    title = {
        ["en"] = "Offset Value",
        ["de"] = "Wert verschieben",
        ["es"] = "Desplazar Valor",
        ["fr"] = "Décaler la Valeur"
    },
    description = {
        ["en"] = [[Adds a constant offset to all incoming data point values.

Configuration:
• Offset: The number to add to all values (default: 0.0). Use negative values to subtract.]],
        ["de"] = [[Fügt allen eingehenden Datenpunktwerten einen konstanten Offset hinzu.

Konfiguration:
• Offset: Die Zahl, die zu allen Werten addiert wird (Standard: 0.0). Verwenden Sie negative Werte zum Subtrahieren.]],
        ["es"] = [[Añade un desplazamiento constante a todos los valores de puntos de datos entrantes.

Configuración:
• Desplazamiento: El número a añadir a todos los valores (predeterminado: 0.0). Use valores negativos para restar.]],
        ["fr"] = [[Ajoute un décalage constant à toutes les valeurs de points de données entrantes.

Configuration:
• Décalage: Le nombre à ajouter à toutes les valeurs (par défaut: 0.0). Utilisez des valeurs négatives pour soustraire.]]
    },
    config = {
        {
            id = "offset",
            type = "number",
            default = 0.0,
            name = {
                ["en"] = "Offset",
                ["de"] = "Offset",
                ["es"] = "Desplazamiento",
                ["fr"] = "Décalage"
            }
        }
    },

    -- Generator function
    generator = function(source, config)
        local offset = config and config.offset or 0.0

        return function()
            local data_point = source.dp()
            if not data_point then return nil end

            data_point.value = data_point.value + offset

            return data_point
        end
    end
}
