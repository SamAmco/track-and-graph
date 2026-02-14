local core = require("tng.core")
local enum = require("tng.config").enum
local uint = require("tng.config").uint

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
    aggregator = function() return aggregation.running_min_aggregator(placement) end
  elseif type == "_max" then
    aggregator = function() return aggregation.running_max_aggregator(placement) end
  elseif type == "_average" then
    aggregator = function() return aggregation.avg_aggregator(placement) end
  elseif type == "_sum" then
    aggregator = function() return aggregation.sum_aggregator(placement) end
  elseif type == "_variance" then
    aggregator = function() return aggregation.variance_aggregator(placement) end
  elseif type == "_standard_deviation" then
    aggregator = function() return aggregation.stdev_aggregator(placement) end
  elseif type == "_count" then
    aggregator = function() return aggregation.count_aggregator(placement) end
  else
    error("Unknown aggregation_type " .. tostring(type))
  end

  return aggregator
end

local get_threshold = function(config)
  if type(config.threshold) ~= "string" then
    error("config.threshold is not a string")
  end

  if config.threshold == "_seconds" then
    return core.DURATION.SECOND
  elseif config.threshold == "_minutes" then
    return core.DURATION.MINUTE
  elseif config.threshold == "_hours" then
    return core.DURATION.HOUR
  elseif config.threshold == "_days" then
    return core.PERIOD.DAY
  elseif config.threshold == "_weeks" then
    return core.PERIOD.WEEK
  elseif config.threshold == "_months" then
    return core.PERIOD.MONTH
  elseif config.threshold == "_years" then
    return core.PERIOD.YEAR
  else
    error("Unknown threshold: " .. tostring(config.threshold))
  end
end

return {
  id = "adaptve-clustering-aggregation-v4",
  version = "4.0.0",
  inputCount = 1,
  title = {
    ["en"] = "Adaptive Clustering",
    ["de"] = "Adaptives Clustering",
    ["es"] = "Agrupación Adaptativa",
    ["fr"] = "Regroupement Adaptatif",
  },
  categories = { "_aggregation" },
  description = {
    ["en"] = [[
Groups nearby data points into clusters and aggregates each cluster into a single value. The function groups any sequence of data points with less than the given time threshold between each data point.

For example, with a 1-hour threshold, if you have several data points at 9:00am, 9:30am, and 10:15am, they will be grouped into a single cluster. If the next data point was at 11:16am it would start a new cluster.

**Configuration Options:**

- **Aggregation**: The operation to perform on values in each cluster:
  - Min: Minimum value
  - Max: Maximum value
  - Average: Mean of all values
  - Sum: Total of all values
  - Count: Number of data points
  - Variance: Statistical variance
  - Standard Deviation: Statistical standard deviation

- **Threshold Units**: The time unit for clustering proximity (seconds, minutes, hours, days, weeks, months, or years)

- **Multiplier**: How many threshold size units define the clustering threshold (e.g., multiplier of 2 with threshold units "hours" = 2-hour clustering threshold)

- **Placement**: Where in the cluster to place each output data point:
  - Window End: At the most recent data point in the cluster (default)
  - Window Midpoint: At the temporal center of the cluster
  - Window Start: At the oldest data point in the cluster
    ]],
    ["de"] = [[
Gruppiert nahe beieinander liegende Datenpunkte in Cluster und aggregiert jeden Cluster zu einem einzelnen Wert. Die Funktion gruppiert jede Sequenz von Datenpunkten mit weniger als dem angegebenen Zeitschwellenwert zwischen jedem Datenpunkt.

Zum Beispiel, mit einem 1-Stunden-Schwellenwert, wenn Sie mehrere Datenpunkte um 9:00 Uhr, 9:30 Uhr und 10:15 Uhr haben, werden sie zu einem einzigen Cluster gruppiert. Wenn der nächste Datenpunkt um 11:16 Uhr wäre, würde ein neuer Cluster beginnen.

**Konfigurationsoptionen:**

- **Aggregation**: Die Operation, die auf Werte in jedem Cluster angewendet wird:
  - Min: Minimalwert
  - Max: Maximalwert
  - Durchschnitt: Mittelwert aller Werte
  - Summe: Gesamtsumme aller Werte
  - Anzahl: Anzahl der Datenpunkte
  - Varianz: Statistische Varianz
  - Standardabweichung: Statistische Standardabweichung

- **Schwellenwert-Einheiten**: Die Zeiteinheit für Clustering-Nähe (Sekunden, Minuten, Stunden, Tage, Wochen, Monate oder Jahre)

- **Multiplikator**: Wie viele Schwellenwert-Einheiten den Clustering-Schwellenwert definieren (z.B. Multiplikator von 2 mit Schwellenwert-Einheiten "Stunden" = 2-Stunden-Clustering-Schwellenwert)

- **Platzierung**: Wo im Cluster jeder Ausgabedatenpunkt platziert wird:
  - Clusterende: Am neuesten Datenpunkt im Cluster (Standard)
  - Clustermitte: In der zeitlichen Mitte des Clusters
  - Clusteranfang: Am ältesten Datenpunkt im Cluster
    ]],
    ["es"] = [[
Agrupa puntos de datos cercanos en clústeres y agrega cada clúster en un único valor. La función agrupa cualquier secuencia de puntos de datos con menos del umbral de tiempo dado entre cada punto de datos.

Por ejemplo, con un umbral de 1 hora, si tiene varios puntos de datos a las 9:00am, 9:30am y 10:15am, se agruparán en un solo clúster. Si el siguiente punto de datos fuera a las 11:16am, comenzaría un nuevo clúster.

**Opciones de Configuración:**

- **Agregación**: La operación a realizar sobre los valores en cada clúster:
  - Mínimo: Valor mínimo
  - Máximo: Valor máximo
  - Promedio: Media de todos los valores
  - Suma: Total de todos los valores
  - Recuento: Número de puntos de datos
  - Varianza: Varianza estadística
  - Desviación Estándar: Desviación estándar estadística

- **Unidades de Umbral**: La unidad de tiempo para la proximidad de agrupación (segundos, minutos, horas, días, semanas, meses o años)

- **Multiplicador**: Cuántas unidades de tamaño de umbral definen el umbral de agrupación (p.ej., multiplicador de 2 con unidades de umbral "horas" = umbral de agrupación de 2 horas)

- **Colocación**: Dónde en el clúster colocar cada punto de datos de salida:
  - Fin del Clúster: En el punto de datos más reciente en el clúster (predeterminado)
  - Punto Medio del Clúster: En el centro temporal del clúster
  - Inicio del Clúster: En el punto de datos más antiguo en el clúster
    ]],
    ["fr"] = [[
Regroupe les points de données proches en clusters et agrège chaque cluster en une valeur unique. La fonction regroupe toute séquence de points de données avec moins que le seuil de temps donné entre chaque point de données.

Par exemple, avec un seuil de 1 heure, si vous avez plusieurs points de données à 9h00, 9h30 et 10h15, ils seront regroupés en un seul cluster. Si le prochain point de données était à 11h16, il commencerait un nouveau cluster.

**Options de Configuration:**

- **Agrégation**: L'opération à effectuer sur les valeurs dans chaque cluster:
  - Min: Valeur minimale
  - Max: Valeur maximale
  - Moyenne: Moyenne de toutes les valeurs
  - Somme: Total de toutes les valeurs
  - Comptage: Nombre de points de données
  - Variance: Variance statistique
  - Écart-Type: Écart-type statistique

- **Unités de Seuil**: L'unité de temps pour la proximité de regroupement (secondes, minutes, heures, jours, semaines, mois ou années)

- **Multiplicateur**: Combien d'unités de taille de seuil définissent le seuil de regroupement (par ex., multiplicateur de 2 avec unités de seuil "heures" = seuil de regroupement de 2 heures)

- **Placement**: Où dans le cluster placer chaque point de données de sortie:
  - Fin du Cluster: Au point de données le plus récent dans le cluster (par défaut)
  - Point Médian du Cluster: Au centre temporel du cluster
  - Début du Cluster: Au point de données le plus ancien dans le cluster
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
      id = "threshold",
      name = "_threshold_units",
      options = { "_seconds", "_minutes", "_hours", "_days", "_weeks", "_months", "_years", },
      default = "_hours"
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
    local threshold = get_threshold(config)
    local multiplier = config.multiplier
    local carry = nil

    return function()
      local aggregator = agg_factory()

      local cutoff

      if carry ~= nil then
        aggregator:push(carry)
        cutoff = core.shift(carry, threshold, -multiplier)
        carry = nil
      else
        local ref = source.dp()

        if ref == nil then
          return nil
        end

        aggregator:push(ref)
        cutoff = core.shift(ref, threshold, -multiplier)
      end

      while true do
        local next_dp = source.dp()

        if next_dp == nil then break end

        if next_dp.timestamp >= cutoff.timestamp then
          aggregator:push(next_dp)
          cutoff = core.shift(next_dp, threshold, -multiplier)
        else
          carry = next_dp
          break
        end
      end

      return aggregator:run()
    end
  end
}
