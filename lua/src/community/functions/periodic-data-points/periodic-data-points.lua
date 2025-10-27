-- Lua Function to generate periodic data points at regular intervals
-- This function creates data points with value=1 at deterministic timestamps

local core = require("tng.core")
local enum = require("tng.config").enum
local uint = require("tng.config").uint
local instant = require("tng.config").instant

local now_time = core.time()
local now = now_time and now_time.timestamp or 0

return {
    -- Configuration metadata
    id = "periodic-data-points",
    version = "1.1.0",
    inputCount = 0, -- This is a generator, not a transformer
    categories = { "_generators" },
    title = {
        ["en"] = "Periodic Data Points",
        ["de"] = "Periodische Datenpunkte",
        ["es"] = "Puntos de Datos Periódicos",
        ["fr"] = "Points de Données Périodiques",
    },
    description = {
        ["en"] = [[
Generates data points with value=1 at regular intervals going back in time.

Configuration:
- **Period**: Time period unit (Day, Week, Month, Year)
- **Period Multiplier**: Generate data point every N periods (e.g., every 2 days)
- **Cutoff**: Stop generating data points at this date/time

Generated data points will have:
- value = 1.0
- label = "" (empty)
- note = "" (empty)]],
        ["de"] = [[
Generiert Datenpunkte mit Wert=1 in regelmäßigen Abständen zurück in der Zeit.

Konfiguration:
- **Periode**: Zeitperiodeneinheit (Tag, Woche, Monat, Jahr)
- **Periodenmultiplikator**: Datenpunkt alle N Perioden generieren (z.B. alle 2 Tage)
- **Grenzwert**: Generierung bei diesem Datum/Zeit stoppen

Generierte Datenpunkte haben:
- Wert = 1.0
- Label = "" (leer)
- Notiz = "" (leer)]],
        ["es"] = [[
Genera puntos de datos con valor=1 a intervalos regulares retrocediendo en el tiempo.

Configuración:
- **Período**: Unidad de período de tiempo (Día, Semana, Mes, Año)
- **Multiplicador de Período**: Generar punto de datos cada N períodos (ej. cada 2 días)
- **Límite**: Detener generación de puntos de datos en esta fecha/hora

Los puntos de datos generados tendrán:
- valor = 1.0
- etiqueta = "" (vacío)
- nota = "" (vacío)]],
        ["fr"] = [[
Génère des points de données avec valeur=1 à intervalles réguliers en remontant dans le temps.

Configuration:
- **Période**: Unité de période de temps (Jour, Semaine, Mois, Année)
- **Multiplicateur de Période**: Générer un point de données tous les N périodes (ex. tous les 2 jours)
- **Limite**: Arrêter la génération de points de données à cette date/heure

Les points de données générés auront:
- valeur = 1.0
- étiquette = "" (vide)
- note = "" (vide)]],
    },
    config = {
        enum {
            id = "period",
            name = "_period",
            options = { "_day", "_week", "_month", "_year" },
            default = "_day",
        },
        uint {
            id = "period_multiplier",
            name = "_period_multiplier",
            default = 1,
        },
        instant {
            id = "cutoff",
            name = "_cutoff",
            default = now - (365 * core.DURATION.DAY),
        },
    },

    -- Generator function
    generator = function(_, config)
        -- Parse configuration with defaults
        local period_str = config and config.period or error("Period configuration is required")
        local period_multiplier = (config and config.period_multiplier) or 1
        -- Don't allow 0 multiplier, fallback to 1
        if period_multiplier == 0 then
            period_multiplier = 1
        end
        local cutoff_timestamp = config and config.cutoff or error("Cutoff configuration is required")

        -- Map enum string to core.PERIOD constant
        local period_map = {
            ["_day"] = core.PERIOD.DAY,
            ["_week"] = core.PERIOD.WEEK,
            ["_month"] = core.PERIOD.MONTH,
            ["_year"] = core.PERIOD.YEAR,
        }
        local period = period_map[period_str]

        -- Anchor is simply the cutoff timestamp
        local anchor_time = core.date(cutoff_timestamp)

        -- Get current time for comparison
        local now = core.time().timestamp

        -- If cutoff is in the future, no data points to generate
        if cutoff_timestamp > now then
            return function()
                return nil
            end
        end

        -- Estimate number of periods elapsed since anchor
        local elapsed_ms = now - cutoff_timestamp
        local estimated_periods
        local period_duration_ms

        if period == core.PERIOD.DAY then
            period_duration_ms = period_multiplier * core.DURATION.DAY
        elseif period == core.PERIOD.WEEK then
            period_duration_ms = period_multiplier * core.DURATION.WEEK
        elseif period == core.PERIOD.MONTH then
            -- Average month length: 30.44 days
            period_duration_ms = period_multiplier * 30.44 * core.DURATION.DAY
        elseif period == core.PERIOD.YEAR then
            -- Average year length: 365.25 days
            period_duration_ms = period_multiplier * 365.25 * core.DURATION.DAY
        else
            error("Invalid period: " .. tostring(period_str))
        end

        estimated_periods = math.floor(elapsed_ms / period_duration_ms)

        -- Jump close to now with one large shift
        local candidate = core.shift(anchor_time, period, estimated_periods * period_multiplier)

        -- Fine-tune: shift forward until we pass "now"
        while candidate.timestamp <= now do
            candidate = core.shift(candidate, period, period_multiplier)
        end

        -- Back up one step to get the most recent data point <= now
        local current = core.shift(candidate, period, -period_multiplier)

        -- Return iterator function
        return function()
            -- Check if we've gone past the cutoff
            if current.timestamp < cutoff_timestamp then
                return nil
            end

            -- Create data point at current timestamp
            local data_point = {
                timestamp = current.timestamp,
                offset = current.offset,
                value = 1.0,
                label = "",
                note = "",
            }

            -- Shift backwards by period * period_multiplier for next iteration
            current = core.shift(current, period, -period_multiplier)

            return data_point
        end
    end,
}
