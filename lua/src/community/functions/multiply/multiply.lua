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
    description = {
        ["en"] = [[Multiplies all incoming data point values by a specified multiplier.

Configuration:
• Multiplier: The number to multiply all values by (default: 1.0)]],
        ["de"] = [[Multipliziert alle eingehenden Datenpunktwerte mit einem bestimmten Multiplikator.

Konfiguration:
• Multiplikator: Die Zahl, mit der alle Werte multipliziert werden (Standard: 1.0)]],
        ["es"] = [[Multiplica todos los valores de puntos de datos entrantes por un multiplicador especificado.

Configuración:
• Multiplicador: El número por el cual multiplicar todos los valores (predeterminado: 1.0)]],
        ["fr"] = [[Multiplie toutes les valeurs de points de données entrantes par un multiplicateur spécifié.

Configuration:
• Multiplicateur: Le nombre par lequel multiplier toutes les valeurs (par défaut: 1.0)]]
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
