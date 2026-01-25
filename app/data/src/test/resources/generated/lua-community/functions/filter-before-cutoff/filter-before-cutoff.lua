-- Lua Function to filter data points before a cutoff timestamp
-- This function only passes through data points that occur before the specified cutoff time
local instant = require("tng.config").instant
local core = require("tng.core")

local now_time = core.time()
local now = now_time and now_time.timestamp or 0

return {
    -- Configuration metadata
    id = "filter-before-cutoff",
    version = "1.0.0",
    inputCount = 1,
    categories = { "_filter", "_time" },

    title = {
        ["en"] = "Filter Before Cutoff",
        ["de"] = "Filtern vor Grenzwert",
        ["es"] = "Filtrar Antes del Límite",
        ["fr"] = "Filtrer Avant la Limite",
    },

    description = {
        ["en"] = "Filters data points to only include those before the specified cutoff time.",
        ["de"] = "Filtert Datenpunkte, um nur diejenigen vor dem angegebenen Grenzwert einzuschließen.",
        ["es"] = "Filtra puntos de datos para incluir solo aquellos antes del límite especificado.",
        ["fr"] = "Filtre les points de données pour n'inclure que ceux avant la limite spécifiée.",
    },

    config = {
        instant {
            id = "cutoff",
            name = "_cutoff",
            default = now, -- Current time as default
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

                -- Only return data points before the cutoff
                if data_point.timestamp < cutoff then
                    return data_point
                end
                -- Otherwise, skip this data point and continue to the next
            end
        end
    end,
}
