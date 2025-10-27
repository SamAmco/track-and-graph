-- Lua Function to filter data points after a cutoff timestamp
-- This function only passes through data points that occur at or after the specified cutoff time

local instant = require("tng.config").instant
local core = require("tng.core")

local now_time = core.time()
local now = now_time and now_time.timestamp or 0

return {
    -- Configuration metadata
    id = "filter-after-cutoff",
    version = "1.0.0",
    inputCount = 1,
    categories = {"_filter", "_time"},
    title = {
        ["en"] = "Filter After Cutoff",
        ["de"] = "Filtern nach Grenzwert",
        ["es"] = "Filtrar Después del Límite",
        ["fr"] = "Filtrer Après la Limite",
    },
    description = {
        ["en"] = "Filters data points to only include those at or after the specified cutoff time.",
        ["de"] = "Filtert Datenpunkte, um nur diejenigen ab dem angegebenen Grenzwert einzuschließen.",
        ["es"] = "Filtra puntos de datos para incluir solo aquellos en o después del límite especificado.",
        ["fr"] = "Filtre les points de données pour n'inclure que ceux à partir de la limite spécifiée.",
    },
    config = {
        instant {
            id = "cutoff",
            name = "_cutoff",
            default = now - (30 * core.DURATION.DAY),  -- 30 days ago
        },
    },

    -- Generator function
    generator = function(source, config)
        local cutoff = config and config.cutoff or error("Cutoff configuration is required")

        return function()
            while true do
                local data_point = source.dp()
                if not data_point then
                    return nil
                end

                -- Only return data points at or after the cutoff
                if data_point.timestamp >= cutoff then
                    return data_point
                end
                -- Otherwise, skip this data point and continue to the next
            end
        end
    end,
}
