-- Lua Function to calculate the running best streak of values above a reset threshold

local number = require("tng.config").number

return {
    id = "best-streak",
    version = "1.0.0",
    inputCount = 1,
    categories = { "_aggregation" },
    title = {
        ["en"] = "Best Streak",
        ["de"] = "Beste Serie",
        ["es"] = "Mejor Racha",
        ["fr"] = "Meilleure Série",
    },
    description = {
        ["en"] = [[
Calculates the best streak seen so far. A streak increases by one for each consecutive data point with a value greater than the reset threshold. Values less than or equal to the reset threshold reset the current streak to zero.

This function emits one output data point for each input data point, preserving the original timestamp, label, note, and offset. The output value is the best streak seen up to that point in time.

Configuration:
- **Reset Threshold**: Values less than or equal to this threshold reset the current streak. Defaults to 0.
]],
        ["de"] = [[
Berechnet die bisher beste Serie. Eine Serie erhöht sich um eins für jeden aufeinanderfolgenden Datenpunkt mit einem Wert über dem Zurücksetzungs-Schwellenwert. Werte kleiner oder gleich dem Schwellenwert setzen die aktuelle Serie auf null zurück.

Diese Funktion gibt für jeden Eingabedatenpunkt einen Ausgabedatenpunkt aus und behält den ursprünglichen Zeitstempel, das Label, die Notiz und den Offset bei. Der Ausgabewert ist die bis zu diesem Zeitpunkt beste Serie.

Konfiguration:
- **Zurücksetzungs-Schwellenwert**: Werte kleiner oder gleich diesem Schwellenwert setzen die aktuelle Serie zurück. Standardwert ist 0.
]],
        ["es"] = [[
Calcula la mejor racha vista hasta el momento. Una racha aumenta en uno por cada punto de datos consecutivo con un valor mayor que el umbral de reinicio. Los valores menores o iguales al umbral reinician la racha actual a cero.

Esta función emite un punto de datos de salida por cada punto de datos de entrada, conservando la marca de tiempo, la etiqueta, la nota y el desplazamiento originales. El valor de salida es la mejor racha vista hasta ese momento.

Configuración:
- **Umbral de reinicio**: Los valores menores o iguales a este umbral reinician la racha actual. El valor predeterminado es 0.
]],
        ["fr"] = [[
Calcule la meilleure série observée jusqu'à présent. Une série augmente de un pour chaque point de données consécutif dont la valeur est supérieure au seuil de réinitialisation. Les valeurs inférieures ou égales au seuil réinitialisent la série actuelle à zéro.

Cette fonction émet un point de données de sortie pour chaque point de données d'entrée, en conservant l'horodatage, l'étiquette, la note et le décalage d'origine. La valeur de sortie est la meilleure série observée jusqu'à ce moment.

Configuration:
- **Seuil de réinitialisation**: Les valeurs inférieures ou égales à ce seuil réinitialisent la série actuelle. La valeur par défaut est 0.
]],
    },
    config = {
        number {
            id = "reset_threshold",
            name = {
                ["en"] = "Reset Threshold",
                ["de"] = "Zurücksetzungs-Schwellenwert",
                ["es"] = "Umbral de reinicio",
                ["fr"] = "Seuil de réinitialisation",
            },
            default = 0.0,
        },
    },

    generator = function(source, config)
        local reset_threshold = config and config.reset_threshold or 0.0
        local all_points = source.dpall()

        local current_streak = 0
        local best_streak = 0

        for i = #all_points, 1, -1 do
            local data_point = all_points[i]

            if data_point.value <= reset_threshold then
                current_streak = 0
            else
                current_streak = current_streak + 1
                if current_streak > best_streak then
                    best_streak = current_streak
                end
            end

            data_point.value = best_streak
        end

        local index = 0
        return function()
            index = index + 1
            return all_points[index]
        end
    end,
}
