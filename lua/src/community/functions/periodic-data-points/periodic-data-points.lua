-- Lua Function to generate periodic data points at regular intervals
-- This function creates data points with value=1 at deterministic timestamps

local core = require("tng.core")
local enum = require("tng.config").enum
local uint = require("tng.config").uint
local localtime = require("tng.config").localtime
local instant = require("tng.config").instant

return {
    -- Configuration metadata
    id = "periodic-data-points",
    version = "1.0.0",
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
- **Time of Day**: The time of day for each data point
- **Cutoff**: Stop generating data points at this date/time

Generated data points will have:
- value = 1.0
- label = "" (empty)
- note = "" (empty)
- Deterministic timestamps at the specified time of day, spaced by the period

The function generates data points on-demand, working backwards from now to the cutoff.]],
        ["de"] = [[
Generiert Datenpunkte mit Wert=1 in regelmäßigen Abständen zurück in der Zeit.

Konfiguration:
- **Periode**: Zeitperiodeneinheit (Tag, Woche, Monat, Jahr)
- **Periodenmultiplikator**: Datenpunkt alle N Perioden generieren (z.B. alle 2 Tage)
- **Tageszeit**: Die Tageszeit für jeden Datenpunkt
- **Grenzwert**: Generierung bei diesem Datum/Zeit stoppen

Generierte Datenpunkte haben:
- Wert = 1.0
- Label = "" (leer)
- Notiz = "" (leer)
- Deterministische Zeitstempel zur angegebenen Tageszeit, im Abstand der Periode

Die Funktion generiert Datenpunkte bei Bedarf, rückwärts von jetzt bis zum Grenzwert.]],
        ["es"] = [[
Genera puntos de datos con valor=1 a intervalos regulares retrocediendo en el tiempo.

Configuración:
- **Período**: Unidad de período de tiempo (Día, Semana, Mes, Año)
- **Multiplicador de Período**: Generar punto de datos cada N períodos (ej. cada 2 días)
- **Hora del Día**: La hora del día para cada punto de datos
- **Límite**: Detener generación de puntos de datos en esta fecha/hora

Los puntos de datos generados tendrán:
- valor = 1.0
- etiqueta = "" (vacío)
- nota = "" (vacío)
- Marcas de tiempo determinísticas a la hora especificada, espaciadas por el período

La función genera puntos de datos bajo demanda, retrocediendo desde ahora hasta el límite.]],
        ["fr"] = [[
Génère des points de données avec valeur=1 à intervalles réguliers en remontant dans le temps.

Configuration:
- **Période**: Unité de période de temps (Jour, Semaine, Mois, Année)
- **Multiplicateur de Période**: Générer un point de données tous les N périodes (ex. tous les 2 jours)
- **Heure de la Journée**: L'heure de la journée pour chaque point de données
- **Limite**: Arrêter la génération de points de données à cette date/heure

Les points de données générés auront:
- valeur = 1.0
- étiquette = "" (vide)
- note = "" (vide)
- Horodatages déterministes à l'heure spécifiée, espacés par la période

La fonction génère des points de données à la demande, en remontant de maintenant jusqu'à la limite.]],
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
        localtime {
            id = "time_of_day",
            name = "_time_of_day",
            default = 12 * core.DURATION.HOUR,
        },
        instant {
            id = "cutoff",
            name = "_cutoff",
            default = core.time().timestamp - (365 * core.DURATION.DAY),
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
        local time_of_day_ms = config and config.time_of_day or error("Time of Day configuration is required")
        local cutoff_timestamp = config and config.cutoff or error("Cutoff configuration is required")

        -- Map enum string to core.PERIOD constant
        local period_map = {
            ["_day"] = core.PERIOD.DAY,
            ["_week"] = core.PERIOD.WEEK,
            ["_month"] = core.PERIOD.MONTH,
            ["_year"] = core.PERIOD.YEAR,
        }
        local period = period_map[period_str]

        -- Helper: apply time_of_day to a timestamp
        local function apply_time_of_day(timestamp)
            local hours = math.floor(time_of_day_ms / core.DURATION.HOUR)
            local remaining = time_of_day_ms % core.DURATION.HOUR
            local minutes = math.floor(remaining / core.DURATION.MINUTE)
            remaining = remaining % core.DURATION.MINUTE
            local seconds = math.floor(remaining / core.DURATION.SECOND)

            local date = core.date(timestamp)
            date.hour = hours
            date.min = minutes
            date.sec = seconds

            return core.time(date)
        end

        -- Create anchor: cutoff with time_of_day applied (fixed, deterministic)
        local anchor = apply_time_of_day(cutoff_timestamp)

        -- If applying time_of_day moved us backwards, shift forward by days
        -- (not the full period) to get past the cutoff without losing data
        while anchor.timestamp < cutoff_timestamp do
            anchor = core.shift(anchor, core.PERIOD.DAY, 1)
        end

        -- Get current time for comparison
        local now = core.time().timestamp

        -- If cutoff is in the future, no data points to generate
        if anchor.timestamp > now then
            return function()
                return nil
            end
        end

        -- Estimate number of periods elapsed since anchor
        local elapsed_ms = now - anchor.timestamp
        local estimated_periods

        if period == core.PERIOD.DAY then
            local period_duration_ms = period_multiplier * core.DURATION.DAY
            estimated_periods = math.floor(elapsed_ms / period_duration_ms)
        elseif period == core.PERIOD.WEEK then
            local period_duration_ms = period_multiplier * core.DURATION.WEEK
            estimated_periods = math.floor(elapsed_ms / period_duration_ms)
        elseif period == core.PERIOD.MONTH then
            -- Average month length: 30.44 days
            local avg_month_ms = 30.44 * core.DURATION.DAY
            local period_duration_ms = period_multiplier * avg_month_ms
            estimated_periods = math.floor(elapsed_ms / period_duration_ms)
        elseif period == core.PERIOD.YEAR then
            -- Average year length: 365.25 days
            local avg_year_ms = 365.25 * core.DURATION.DAY
            local period_duration_ms = period_multiplier * avg_year_ms
            estimated_periods = math.floor(elapsed_ms / period_duration_ms)
        else
            error("Invalid period: " .. tostring(period_str))
        end

        -- Jump close to now with one large shift
        local candidate = core.shift(anchor, period, estimated_periods * period_multiplier)

        -- Fine-tune: shift forward until we pass "now"
        while candidate.timestamp <= now do
            candidate = core.shift(candidate, period, period_multiplier)
        end

        -- Back up one step to get the most recent data point <= now
        local current = core.shift(candidate, period, -period_multiplier)

        -- Return iterator function
        return function()
            -- Check if we've gone past the anchor (cutoff with time applied)
            if current.timestamp < anchor.timestamp then
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
