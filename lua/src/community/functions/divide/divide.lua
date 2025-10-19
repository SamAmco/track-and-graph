-- Lua Function to divide data point values by a configurable number
-- This function divides all incoming data point values by a specified divisor

local number = require("tng.config").number

return {
    -- Configuration metadata
    id = "divide",
    version = "1.0.0",
    inputCount = 1,
    categories = {"_arithmetic"},
    title = {
        ["en"] = "Divide Values",
        ["de"] = "Werte dividieren",
        ["es"] = "Dividir Valores",
        ["fr"] = "Diviser les Valeurs"
    },
    description = {
        ["en"] = [[Divides all incoming data point values by a specified divisor.

Configuration:
• Divisor: The number to divide all values by (default: 1.0)]],
        ["de"] = [[Dividiert alle eingehenden Datenpunktwerte durch einen bestimmten Divisor.

Konfiguration:
• Divisor: Die Zahl, durch die alle Werte dividiert werden (Standard: 1.0)]],
        ["es"] = [[Divide todos los valores de puntos de datos entrantes por un divisor especificado.

Configuración:
• Divisor: El número por el cual dividir todos los valores (predeterminado: 1.0)]],
        ["fr"] = [[Divise toutes les valeurs de points de données entrantes par un diviseur spécifié.

Configuration:
• Diviseur: Le nombre par lequel diviser toutes les valeurs (par défaut: 1.0)]]
    },
    config = {
        number {
            id = "divisor",
            default = 1.0,
            name = {
                ["en"] = "Divisor",
                ["de"] = "Divisor",
                ["es"] = "Divisor",
                ["fr"] = "Diviseur"
            }
        }
    },

    -- Generator function
    generator = function(source, config)
        local divisor = config and config.divisor or 1.0

        return function()
            local data_point = source.dp()
            if not data_point then return nil end

            data_point.value = data_point.value / divisor

            return data_point
        end
    end
}
