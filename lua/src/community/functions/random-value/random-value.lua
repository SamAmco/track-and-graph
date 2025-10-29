-- Lua Function to override data point values with random values
-- This function replaces all incoming data point values with random numbers between min and max

local number = require("tng.config").number
local uint = require("tng.config").uint
local core = require("tng.core")
local random = require("tng.random")

local now = core.time()
local default_seed = now and now.timestamp or 0

return {
    -- Configuration metadata
    id = "random-value",
    version = "2.0.0",
    inputCount = 1,
    categories = { "_randomisers" },
    title = {
        ["en"] = "Random Value",
        ["de"] = "Zufälliger Wert",
        ["es"] = "Valor Aleatorio",
        ["fr"] = "Valeur Aléatoire",
    },
    description = {
        ["en"] = [[
Replaces all incoming data point values with random numbers between min and max.

Configuration:
- **Min Value**: The minimum value for random generation
- **Max Value**: The maximum value for random generation
- **Seed**: Random seed for reproducible results (defaults to current UTC timestamp)

The function automatically swaps min and max if max is smaller than min.]],
        ["de"] = [[
Ersetzt alle eingehenden Datenpunktwerte durch Zufallszahlen zwischen Min und Max.

Konfiguration:
- **Minimalwert**: Der Minimalwert für die Zufallsgenerierung
- **Maximalwert**: Der Maximalwert für die Zufallsgenerierung
- **Seed**: Zufalls-Seed für reproduzierbare Ergebnisse (Standard: aktueller UTC-Zeitstempel)

Die Funktion tauscht automatisch Min und Max, wenn Max kleiner als Min ist.]],
        ["es"] = [[
Reemplaza todos los valores de puntos de datos entrantes con números aleatorios entre mín y máx.

Configuración:
- **Valor Mínimo**: El valor mínimo para la generación aleatoria
- **Valor Máximo**: El valor máximo para la generación aleatoria
- **Semilla**: Semilla aleatoria para resultados reproducibles (predeterminado: marca de tiempo UTC actual)

La función intercambia automáticamente mín y máx si máx es menor que mín.]],
        ["fr"] = [[
Remplace toutes les valeurs de points de données entrantes par des nombres aléatoires entre min et max.

Configuration:
- **Valeur Minimale**: La valeur minimale pour la génération aléatoire
- **Valeur Maximale**: La valeur maximale pour la génération aléatoire
- **Graine**: Graine aléatoire pour des résultats reproductibles (par défaut: horodatage UTC actuel)

La fonction échange automatiquement min et max si max est inférieur à min.]],
    },
    config = {
        number {
            id = "min_value",
            name = "_min_value",
            default = 0.0,
        },
        number {
            id = "max_value",
            name = "_max_value",
            default = 1.0,
        },
        uint {
            id = "seed",
            name = "_seed",
            default = default_seed,
        },
    },

    -- Generator function
    generator = function(source, config)
        local min_val = config and config.min_value or 0.0
        local max_val = config and config.max_value or 1.0
        local seed = config and config.seed or core.time().timestamp

        -- Ensure min is always the smaller value
        if min_val > max_val then
            min_val, max_val = max_val, min_val
        end

        return function()
            local data_point = source.dp()
            if not data_point then
                return nil
            end

            -- Generate random value between min and max
            local rng = random.new_seeded_random(seed, data_point.timestamp)
            data_point.value = rng:next(min_val, max_val)

            return data_point
        end
    end,
}
