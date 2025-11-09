local core = require("tng.core")
local enum = require("tng.config").enum
local uint = require("tng.config").uint

local get_aggregator = function(config)
  local aggregation = require("tng.aggregation")
  local type = config.aggregation_type or error("aggregation_type required")
  local aggregator

  if type == "_min" then
    aggregator = aggregation.running_min_aggregator()
  elseif type == "_max" then
    aggregator = aggregation.running_max_aggregator()
  elseif type == "_average" then
    aggregator = aggregation.avg_aggregator()
  elseif type == "_sum" then
    aggregator = aggregation.sum_aggregator()
  elseif type == "_variance" then
    aggregator = aggregation.variance_aggregator()
  elseif type == "_standard_deviation" then
    aggregator = aggregation.stdev_aggregator()
  elseif type == "_count" then
    aggregator = aggregation.count_aggregator()
  else
    error("Unknown aggregation_type " .. tostring(type))
  end

  return aggregator
end

local get_window = function(config)
  if type(config.window) ~= "string" then
    error("config.window is not a string")
  end

  if config.window == "_seconds" then
    return core.DURATION.SECOND
  elseif config.window == "_minutes" then
    return core.DURATION.MINUTE
  elseif config.window == "_hours" then
    return core.DURATION.HOUR
  elseif config.window == "_days" then
    return core.PERIOD.DAY
  elseif config.window == "_weeks" then
    return core.PERIOD.WEEK
  elseif config.window == "_months" then
    return core.PERIOD.MONTH
  elseif config.window == "_years" then
    return core.PERIOD.YEAR
  else
    error("Unknown window: " .. tostring(config.window))
  end
end

return {
  id = "rolling-window-aggregation",
  version = "3.0.0",
  inputCount = 1,
  title = {
    ["en"] = "Rolling Window",
    ["de"] = "Rollierendes Fenster",
    ["es"] = "Ventana Móvil",
    ["fr"] = "Fenêtre Glissante",
  },
  categories = { "_aggregation" },
  description = {
    ["en"] = [[
Calculates aggregate statistics over a moving time window for each data point. The function looks backward in time from each point and aggregates all values within the specified window period.

For example, with a 7-day window and average aggregation, each output point represents the average of all values in the 7 days leading up to that point.

**Configuration Options:**

- **Aggregation**: The operation to perform on values in each window:
  - Min: Minimum value
  - Max: Maximum value
  - Average: Mean of all values
  - Sum: Total of all values
  - Count: Number of data points
  - Variance: Statistical variance
  - Standard Deviation: Statistical standard deviation

- **Window Size**: The time unit for the lookback period (seconds, minutes, hours, days, weeks, months, or years)

- **Multiplier**: How many window size units to look back (e.g., multiplier of 3 with window size "days" = 3-day window)
    ]],
    ["de"] = [[
Berechnet Aggregationsstatistiken über ein bewegliches Zeitfenster für jeden Datenpunkt. Die Funktion schaut von jedem Punkt aus rückwärts in der Zeit und aggregiert alle Werte innerhalb des angegebenen Fensterzeitraums.

Zum Beispiel repräsentiert bei einem 7-Tage-Fenster und Durchschnittsaggregation jeder Ausgabepunkt den Durchschnitt aller Werte in den 7 Tagen bis zu diesem Punkt.

**Konfigurationsoptionen:**

- **Aggregation**: Die Operation, die auf Werte in jedem Fenster angewendet wird:
  - Min: Minimalwert
  - Max: Maximalwert
  - Durchschnitt: Mittelwert aller Werte
  - Summe: Gesamtsumme aller Werte
  - Anzahl: Anzahl der Datenpunkte
  - Varianz: Statistische Varianz
  - Standardabweichung: Statistische Standardabweichung

- **Fenstergröße**: Die Zeiteinheit für den Rückblickzeitraum (Sekunden, Minuten, Stunden, Tage, Wochen, Monate oder Jahre)

- **Multiplikator**: Wie viele Fenstergrößeneinheiten zurückgeschaut werden soll (z.B. Multiplikator von 3 mit Fenstergröße "Tage" = 3-Tage-Fenster)
    ]],
    ["es"] = [[
Calcula estadísticas agregadas sobre una ventana de tiempo móvil para cada punto de datos. La función mira hacia atrás en el tiempo desde cada punto y agrega todos los valores dentro del período de ventana especificado.

Por ejemplo, con una ventana de 7 días y agregación promedio, cada punto de salida representa el promedio de todos los valores en los 7 días previos a ese punto.

**Opciones de Configuración:**

- **Agregación**: La operación a realizar sobre los valores en cada ventana:
  - Mínimo: Valor mínimo
  - Máximo: Valor máximo
  - Promedio: Media de todos los valores
  - Suma: Total de todos los valores
  - Recuento: Número de puntos de datos
  - Varianza: Varianza estadística
  - Desviación Estándar: Desviación estándar estadística

- **Tamaño de Ventana**: La unidad de tiempo para el período de retrospección (segundos, minutos, horas, días, semanas, meses o años)

- **Multiplicador**: Cuántas unidades de tamaño de ventana mirar hacia atrás (p.ej., multiplicador de 3 con tamaño de ventana "días" = ventana de 3 días)
    ]],
    ["fr"] = [[
Calcule des statistiques agrégées sur une fenêtre de temps mobile pour chaque point de données. La fonction regarde en arrière dans le temps à partir de chaque point et agrège toutes les valeurs dans la période de fenêtre spécifiée.

Par exemple, avec une fenêtre de 7 jours et une agrégation moyenne, chaque point de sortie représente la moyenne de toutes les valeurs des 7 jours précédant ce point.

**Options de Configuration:**

- **Agrégation**: L'opération à effectuer sur les valeurs dans chaque fenêtre:
  - Min: Valeur minimale
  - Max: Valeur maximale
  - Moyenne: Moyenne de toutes les valeurs
  - Somme: Total de toutes les valeurs
  - Comptage: Nombre de points de données
  - Variance: Variance statistique
  - Écart-Type: Écart-type statistique

- **Taille de Fenêtre**: L'unité de temps pour la période de rétrospection (secondes, minutes, heures, jours, semaines, mois ou années)

- **Multiplicateur**: Combien d'unités de taille de fenêtre regarder en arrière (par ex., multiplicateur de 3 avec taille de fenêtre "jours" = fenêtre de 3 jours)
    ]]
  },
  config = {
    enum {
      id = "aggregation_type",
      name = "_aggregation",
      options = { "_min", "_max", "_average", "_sum", "_count", "_variance", "_standard_deviation" },
      default = "_average"
    },
    uint {
      id = "multiplier",
      name = "_multiplier",
      default = 1
    },
    enum {
      id = "window",
      name = "_window_size",
      options = { "_seconds", "_minutes", "_hours", "_days", "_weeks", "_months", "_years", },
      default = "_weeks"
    },
  },
  generator = function(source, config)
    local aggregator = get_aggregator(config)
    local window = get_window(config)
    local multiplier = config.multiplier
    local carry = nil

    return function()
      if #aggregator.window > 0 then
        aggregator:pop()
      end

      if #aggregator.window == 0 then
        if carry ~= nil then
          aggregator:push(carry)
          carry = nil
        else
          local next_dp = source.dp()
          if next_dp == nil then
            return nil
          end
          aggregator:push(next_dp)
        end
      end

      local last_dp = aggregator.window[1]
      if last_dp == nil then
        return nil
      end

      local new_window_start = core.shift(last_dp, window, -multiplier)

      while true do
        if carry ~= nil then
          if carry.timestamp >= new_window_start.timestamp then
            aggregator:push(carry)
            carry = nil
          else
            break
          end
        end

        local next_dp = source.dp()

        if next_dp == nil then
          break
        elseif next_dp.timestamp >= new_window_start.timestamp then
          aggregator:push(next_dp)
        else
          carry = next_dp
          break
        end
      end

      return aggregator:run()
    end
  end
}
