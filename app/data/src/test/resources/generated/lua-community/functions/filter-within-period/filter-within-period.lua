-- Lua Function to filter data points within a specified period from now
-- This function calculates a cutoff timestamp by subtracting a specified period from "now" and only passes through data points at or after that cutoff
local enum = require("tng.config").enum
local uint = require("tng.config").uint
local core = require("tng.core")

return {
    -- Configuration metadata
    id = "filter-within-period",
    version = "1.0.0",
    inputCount = 1,
    categories = { "_filter", "_time" },

    title = {
        ["en"] = "Filter Within Period",
        ["de"] = "Filtern innerhalb Periode",
        ["es"] = "Filtrar Dentro de Período",
        ["fr"] = "Filtrer Dans la Période",
    },

    description = {
        ["en"] = [[
Filters data points to only include those within the specified time period from now.
The cutoff is calculated by subtracting the specified period from the current time.

Configuration:
- **Period**: Time period unit (Day, Week, Month, Year)
- **Period Multiplier**: Number of periods to include (e.g., 2 weeks = Week + Multiplier 2)

For example, with Period=Day and Multiplier=7, only data from within the last 7 days will pass through.
]],
        ["de"] = [[
Filtert Datenpunkte, um nur diejenigen innerhalb der angegebenen Zeitperiode von jetzt einzuschließen.
Der Grenzwert wird berechnet, indem die angegebene Periode von der aktuellen Zeit abgezogen wird.

Konfiguration:
- **Periode**: Zeitperiodeneinheit (Tag, Woche, Monat, Jahr)
- **Periodenmultiplikator**: Anzahl der Perioden einzuschließen (z.B. 2 Wochen = Woche + Multiplikator 2)

Zum Beispiel, mit Periode=Tag und Multiplikator=7, passieren nur Daten innerhalb der letzten 7 Tage durch.
]],
        ["es"] = [[
Filtra puntos de datos para incluir solo aquellos dentro del período de tiempo especificado desde ahora.
El límite se calcula restando el período especificado del tiempo actual.

Configuración:
- **Período**: Unidad de período de tiempo (Día, Semana, Mes, Año)
- **Multiplicador de Período**: Número de períodos a incluir (ej. 2 semanas = Semana + Multiplicador 2)

Por ejemplo, con Período=Día y Multiplicador=7, solo pasarán datos dentro de los últimos 7 días.
]],
        ["fr"] = [[
Filtre les points de données pour n'inclure que ceux dans la période de temps spécifiée depuis maintenant.
La limite est calculée en soustrayant la période spécifiée du temps actuel.

Configuration:
- **Période**: Unité de période de temps (Jour, Semaine, Mois, Année)
- **Multiplicateur de Période**: Nombre de périodes à inclure (ex. 2 semaines = Semaine + Multiplicateur 2)

Par exemple, avec Période=Jour et Multiplicateur=7, seules les données dans les 7 derniers jours passeront.
]],
    },

    config = {
        enum {
            id = "period",
            name = "_period",
            options = { "_day", "_week", "_month", "_year" },
            default = "_month",
        },
        uint {
            id = "period_multiplier",
            name = "_period_multiplier",
            default = 1,
        },
    },

    -- Generator function
    generator = function(source, config)
        local period_str = config and config.period or error("Period configuration is required")
        local period_multiplier = (config and config.period_multiplier) or 30

        -- Don't allow 0 multiplier, fallback to 1
        if period_multiplier == 0 then
            period_multiplier = 1
        end

        -- Map enum string to core.PERIOD constant
        local period_map = {
            ["_day"] = core.PERIOD.DAY,
            ["_week"] = core.PERIOD.WEEK,
            ["_month"] = core.PERIOD.MONTH,
            ["_year"] = core.PERIOD.YEAR,
        }
        local period = period_map[period_str]
        if not period then
            error("Invalid period: " .. tostring(period_str))
        end

        -- Calculate cutoff timestamp: now - (period * multiplier)
        local now = core.time()
        local cutoff = core.shift(now, period, -period_multiplier)
        local cutoff_timestamp = cutoff.timestamp

        return function()
            local data_point = source.dp()
            if not data_point then
                return nil
            end

            -- Only return data points at or after the cutoff
            if data_point.timestamp >= cutoff_timestamp then
                return data_point
            end
        end
    end,
}
