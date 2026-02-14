local core = require("tng.core")
local enum = require("tng.config").enum

local PLACEMENT_MAP = {
  _window_start = "start",
  _window_midpoint = "mid",
  _window_end = "end",
}

local get_aggregator_factory = function(config)
  local aggregation = require("tng.aggregation")
  local type = config.aggregation_type or error("aggregation_type required")
  local placement = PLACEMENT_MAP[config.placement] or "end"
  local aggregator

  if type == "_min" then
    aggregator = function() return aggregation.simple_min_aggregator2(placement) end
  elseif type == "_max" then
    aggregator = function() return aggregation.simple_max_aggregator2(placement) end
  elseif type == "_average" then
    aggregator = function() return aggregation.avg_aggregator2(placement) end
  elseif type == "_sum" then
    aggregator = function() return aggregation.sum_aggregator2(placement) end
  elseif type == "_variance" then
    aggregator = function() return aggregation.variance_aggregator2(placement) end
  elseif type == "_standard_deviation" then
    aggregator = function() return aggregation.stdev_aggregator2(placement) end
  elseif type == "_count" then
    aggregator = function() return aggregation.count_aggregator2(placement) end
  else
    error("Unknown aggregation_type " .. tostring(type))
  end

  return aggregator
end

local get_period = function(config)
  if type(config.period) ~= "string" then
    error("config.period is not a string")
  end

  if config.period == "_days" then
    return core.PERIOD.DAY
  elseif config.period == "_weeks" then
    return core.PERIOD.WEEK
  elseif config.period == "_months" then
    return core.PERIOD.MONTH
  elseif config.period == "_years" then
    return core.PERIOD.YEAR
  else
    error("Unknown period: " .. tostring(config.period))
  end
end

return {
  id = "periodic-aggregation-v4",
  version = "4.0.1",
  inputCount = 1,
  title = {
    ["en"] = "Periodic Aggregation",
    ["de"] = "Periodische Aggregation",
    ["es"] = "Agregación Periódica",
    ["fr"] = "Agrégation Périodique",
  },
  categories = { "_aggregation" },
  description = {
    ["en"] = [[
Aggregates data points into fixed calendar periods (days, weeks, months, or years). Each period produces one aggregated value containing all data points that fall within that period's boundaries.

For example, with a daily period and average aggregation, all measurements from each calendar day are combined into a single value representing the average for that day.

**Configuration Options:**

- **Period**: The calendar period to aggregate by (days, weeks, months, or years)

- **Aggregation**: The operation to perform on values in each period:
  - Min: Minimum value
  - Max: Maximum value
  - Average: Mean of all values
  - Sum: Total of all values
  - Count: Number of data points
  - Variance: Statistical variance
  - Standard Deviation: Statistical standard deviation

- **Placement**: Where in the time window to place each output data point:
  - Window End: At the most recent data point in the window (default)
  - Window Midpoint: At the temporal center of the window
  - Window Start: At the oldest data point in the window
    ]],
    ["de"] = [[
Aggregiert Datenpunkte in feste Kalenderperioden (Tage, Wochen, Monate oder Jahre). Jede Periode erzeugt einen aggregierten Wert, der alle Datenpunkte enthält, die innerhalb der Grenzen dieser Periode liegen.

Zum Beispiel, mit einer täglichen Periode und Durchschnittsaggregation werden alle Messungen von jedem Kalendertag zu einem einzelnen Wert kombiniert, der den Durchschnitt für diesen Tag repräsentiert.

**Konfigurationsoptionen:**

- **Periode**: Die Kalenderperiode zur Aggregation (Tage, Wochen, Monate oder Jahre)

- **Aggregation**: Die Operation, die auf Werte in jeder Periode angewendet wird:
  - Min: Minimalwert
  - Max: Maximalwert
  - Durchschnitt: Mittelwert aller Werte
  - Summe: Gesamtsumme aller Werte
  - Anzahl: Anzahl der Datenpunkte
  - Varianz: Statistische Varianz
  - Standardabweichung: Statistische Standardabweichung

- **Platzierung**: Wo im Zeitfenster jeder Ausgabedatenpunkt platziert wird:
  - Fensterende: Am neuesten Datenpunkt im Fenster (Standard)
  - Fenstermitte: In der zeitlichen Mitte des Fensters
  - Fensteranfang: Am ältesten Datenpunkt im Fenster
    ]],
    ["es"] = [[
Agrega puntos de datos en períodos de calendario fijos (días, semanas, meses o años). Cada período produce un valor agregado que contiene todos los puntos de datos que caen dentro de los límites de ese período.

Por ejemplo, con un período diario y agregación promedio, todas las mediciones de cada día del calendario se combinan en un único valor que representa el promedio de ese día.

**Opciones de Configuración:**

- **Período**: El período de calendario para agregar (días, semanas, meses o años)

- **Agregación**: La operación a realizar sobre los valores en cada período:
  - Mínimo: Valor mínimo
  - Máximo: Valor máximo
  - Promedio: Media de todos los valores
  - Suma: Total de todos los valores
  - Recuento: Número de puntos de datos
  - Varianza: Varianza estadística
  - Desviación Estándar: Desviación estándar estadística

- **Colocación**: Dónde en la ventana de tiempo colocar cada punto de datos de salida:
  - Fin de Ventana: En el punto de datos más reciente en la ventana (predeterminado)
  - Punto Medio de Ventana: En el centro temporal de la ventana
  - Inicio de Ventana: En el punto de datos más antiguo en la ventana

    ]],
    ["fr"] = [[
Agrège les points de données en périodes de calendrier fixes (jours, semaines, mois ou années). Chaque période produit une valeur agrégée contenant tous les points de données qui se situent dans les limites de cette période.

Par exemple, avec une période quotidienne et une agrégation moyenne, toutes les mesures de chaque jour du calendrier sont combinées en une seule valeur représentant la moyenne de ce jour.

**Options de Configuration:**

- **Période**: La période de calendrier pour l'agrégation (jours, semaines, mois ou années)

- **Agrégation**: L'opération à effectuer sur les valeurs dans chaque période:
  - Min: Valeur minimale
  - Max: Valeur maximale
  - Moyenne: Moyenne de toutes les valeurs
  - Somme: Total de toutes les valeurs
  - Comptage: Nombre de points de données
  - Variance: Variance statistique
  - Écart-Type: Écart-type statistique

- **Placement**: Où dans la fenêtre de temps placer chaque point de données de sortie:
  - Fin de Fenêtre: Au point de données le plus récent dans la fenêtre (par défaut)
  - Point Médian de Fenêtre: Au centre temporel de la fenêtre
  - Début de Fenêtre: Au point de données le plus ancien dans la fenêtre
    ]]
  },
  config = {
    enum {
      id = "period",
      name = "_period",
      options = { "_days", "_weeks", "_months", "_years" },
      default = "_days"
    },
    enum {
      id = "aggregation_type",
      name = "_aggregation",
      options = { "_min", "_max", "_average", "_sum", "_count", "_variance", "_standard_deviation" },
      default = "_average"
    },
    enum {
      id = "placement",
      name = "_placement",
      options = { "_window_end", "_window_midpoint", "_window_start" },
      default = "_window_end"
    },
  },
  generator = function(source, config)
    local agg_factory = get_aggregator_factory(config)
    local period = get_period(config)
    local carry = source.dp()

    local current_window_start
    local current_window_end

    if carry ~= nil then
      current_window_end = core.get_end_of_period(period, carry)
      current_window_start = core.shift(current_window_end, period, -1)
    end

    return function()
      if carry == nil then
        return nil
      end

      if carry.timestamp < current_window_start.timestamp then
        -- No data points in this window, return empty aggregation
        current_window_end = current_window_start
        current_window_start = core.shift(current_window_start, period, -1)
        return {
          timestamp = current_window_end.timestamp - 1,
          offset = current_window_end.offset,
          value = 0,
        }
      end

      local aggregator = agg_factory()
      aggregator:push(carry)

      local window_data_points = source.dpafter(current_window_start)

      for _, dp in ipairs(window_data_points) do
        aggregator:push(dp)
      end

      current_window_end = current_window_start
      current_window_start = core.shift(current_window_start, period, -1)

      carry = source.dp()

      local aggregate = aggregator:run()

      return {
        -- Subtract 1 millisecond to ensure the timestamp falls within the period
        timestamp = current_window_end.timestamp - 1,
        offset = current_window_end.offset,
        value = aggregate.value,
        label = aggregate.label,
        note = aggregate.note
      }
    end
  end
}
