-- Lua Function to calculate cumulative sum of data point values
-- This function computes a running total, optionally resetting on specific labels

local checkbox = require("tng.config").checkbox
local text = require("tng.config").text

return {
    -- Configuration metadata
    id = "cumulative-value",
    version = "1.0.0",
    inputCount = 1,
    categories = {"_arithmetic"},
    title = {
        ["en"] = "Cumulative Value",
        ["de"] = "Kumulativer Wert",
        ["es"] = "Valor Acumulativo",
        ["fr"] = "Valeur Cumulative",
    },
    description = {
        ["en"] = "Calculates the cumulative sum of data point values. Optionally resets accumulation when a specific label is encountered.",
        ["de"] = "Berechnet die kumulative Summe der Datenpunktwerte. Setzt optional die Akkumulation zurück, wenn ein bestimmtes Label gefunden wird.",
        ["es"] = "Calcula la suma acumulativa de los valores de puntos de datos. Opcionalmente reinicia la acumulación cuando se encuentra una etiqueta específica.",
        ["fr"] = "Calcule la somme cumulative des valeurs de points de données. Réinitialise optionnellement l'accumulation lorsqu'une étiquette spécifique est rencontrée.",
    },
    config = {
        checkbox {
            id = "enable_reset",
            name = "_reset_on_label_match",
            default = false,
        },
        text {
            id = "reset_label",
            name = "_reset_label",
            default = "",
        },
        checkbox {
            id = "exact_match",
            name = "_match_exactly",
            default = false,
        },
        checkbox {
            id = "case_sensitive",
            name = "_case_sensitive",
            default = true,
        },
    },

    -- Generator function
    generator = function(source, config)
        local enable_reset = config.enable_reset
        local reset_label = config.reset_label
        local exact_match = config.exact_match
        local case_sensitive = config.case_sensitive

        -- Helper function to check if a label matches
        local function label_matches(dp_label)
            if not enable_reset then
                return false
            end

            local label_to_check = dp_label or ""
            local pattern = reset_label

            -- Apply case insensitivity if needed
            if not case_sensitive then
                label_to_check = label_to_check:lower()
                pattern = pattern:lower()
            end

            -- Check match type
            if exact_match then
                return label_to_check == pattern
            else
                -- search for pattern from the start, pattern matching disabled
                return label_to_check:find(pattern, 1, true) ~= nil
            end
        end

        -- Drain all data points from source (returns in reverse chronological order)
        local all_points = source.dpall()

        -- Calculate cumulative values by iterating in reverse (chronologically)
        -- Mutate data points in place
        local cumulative_sum = 0
        for i = #all_points, 1, -1 do
            local data_point = all_points[i]

            -- Check if we should reset
            if label_matches(data_point.label) then
                cumulative_sum = 0
            end

            -- Add current value to cumulative sum
            cumulative_sum = cumulative_sum + data_point.value

            -- Update the data point's value in place
            data_point.value = cumulative_sum
        end

        -- all_points is still in reverse chronological order, return iterator
        local index = 0
        return function()
            index = index + 1
            return all_points[index]
        end
    end,
}
