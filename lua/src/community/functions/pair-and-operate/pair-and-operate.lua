-- Lua Function to pair the data points of two input sources and perform
-- an operation on their values (addition, subtraction, multiplication, or division).
local duration = require("tng.config").duration
local enum = require("tng.config").enum
local core = require("tng.core")

return {
  -- Configuration metadata
  id = "pair-and-operate",
  version = "1.0.1",
  inputCount = 2,
  categories = { "_transform" },
  title = {
    ["en"] = "Pair and Operate",
    ["de"] = "Paaren und Operieren",
    ["es"] = "Emparejar y Operar",
    ["fr"] = "Apparier et Opérer",
  },
  description = {
    ["en"] = [[
Pairs each data point in the first data source with the corresponding data point in the second data source and performs a specified operation (addition, subtraction, multiplication, or division) on their values. The pair for each data point is the first data point that falls within the given time threshold. Configuration:

- **Time Threshold:** The maximum duration between data points in the two sources to be considered a pair.
- **Operation:** The mathematical operation to perform on the paired data point values (addition, subtraction, multiplication, or division).
- **On Missing:** Specifies the behavior when a data point in the first source does not have a corresponding data point in the second source within the given time threshold. Options include:
  - **Skip:** Do not output anything for that data point.
  - **Pass Through:** Output the original data point from the first source without modification.


**Note:** Division by zero is invalid and considered as missing data. The On Missing configuration will determine how such cases are handled.
]],
    ["de"] = [[
Paart jeden Datenpunkt in der ersten Datenquelle mit dem entsprechenden Datenpunkt in der zweiten Datenquelle und führt eine bestimmte Operation (Addition, Subtraktion, Multiplikation oder Division) mit ihren Werten durch. Das Paar für jeden Datenpunkt ist der erste Datenpunkt, der innerhalb des gegebenen Zeitschwellwerts liegt. Konfiguration:

- **Zeitschwellwert:** Die maximale Dauer zwischen Datenpunkten in den beiden Quellen, um als Paar betrachtet zu werden.
- **Operation:** Die mathematische Operation, die mit den gepaarten Datenpunktwerten durchgeführt werden soll (Addition, Subtraktion, Multiplikation oder Division).
- **Bei Fehlen:** Gibt das Verhalten an, wenn ein Datenpunkt in der ersten Quelle keinen entsprechenden Datenpunkt in der zweiten Quelle innerhalb des gegebenen Zeitschwellwerts hat. Optionen umfassen:
  - **Überspringen:** Nichts für diesen Datenpunkt ausgeben.
  - **Durchleiten:** Den ursprünglichen Datenpunkt aus der ersten Quelle ohne Änderung ausgeben.


**Hinweis:** Division durch Null ist ungültig und wird als fehlende Daten betrachtet. Die Konfiguration "Bei Fehlen" bestimmt, wie solche Fälle behandelt werden.
]],
    ["es"] = [[
Empareja cada punto de datos en la primera fuente de datos con el punto de datos correspondiente en la segunda fuente de datos y realiza una operación específica (suma, resta, multiplicación o división) en sus valores. La pareja para cada punto de datos es el primer punto de datos que cae dentro del umbral de tiempo dado. Configuración:

- **Umbral de Tiempo:** La duración máxima entre puntos de datos en las dos fuentes para ser considerados una pareja.
- **Operación:** La operación matemática a realizar en los valores de puntos de datos emparejados (suma, resta, multiplicación o división).
- **En Faltante:** Especifica el comportamiento cuando un punto de datos en la primera fuente no tiene un punto de datos correspondiente en la segunda fuente dentro del umbral de tiempo dado. Las opciones incluyen:
  - **Omitir:** No generar nada para ese punto de datos.
  - **Pasar Sin Cambios:** Generar el punto de datos original de la primera fuente sin modificación.


**Nota:** La división por cero es inválida y se considera como datos faltantes. La configuración "En Faltante" determinará cómo se manejan tales casos.
]],
    ["fr"] = [[
Apparie chaque point de données dans la première source de données avec le point de données correspondant dans la deuxième source de données et effectue une opération spécifiée (addition, soustraction, multiplication ou division) sur leurs valeurs. La paire pour chaque point de données est le premier point de données qui tombe dans le seuil de temps donné. Configuration:

- **Seuil de Temps:** La durée maximale entre les points de données dans les deux sources pour être considérés comme une paire.
- **Opération:** L'opération mathématique à effectuer sur les valeurs des points de données appariés (addition, soustraction, multiplication ou division).
- **En Cas de Manque:** Spécifie le comportement lorsqu'un point de données dans la première source n'a pas de point de données correspondant dans la deuxième source dans le seuil de temps donné. Les options incluent:
  - **Ignorer:** Ne rien générer pour ce point de données.
  - **Laisser Passer:** Générer le point de données original de la première source sans modification.


**Note:** La division par zéro est invalide et considérée comme des données manquantes. La configuration "En Cas de Manque" déterminera comment de tels cas sont traités.
]],
  },
  config = {
    duration {
      id = "threshold",
      name = "_time_threshold",
      default = core.DURATION.MINUTE,
    },
    enum {
      id = "operation",
      name = "_operation",
      options = { "_addition", "_subtraction", "_multiplication", "_division" },
      default = "_addition",
    },
    enum {
      id = "on_missing",
      name = "_on_missing",
      options = { "_skip", "_pass_through" },
      default = "_skip",
    },
  },

  -- Generator function
  generator = function(sources, config)
    local threshold = config.threshold or error("Missing 'threshold' in config")
    local operation = config.operation or error("Missing 'operation' in config")
    local on_missing = config.on_missing or error("Missing 'on_missing' in config")
    local source1 = sources[1] or error("Missing first data source")
    local source2 = sources[2] or error("Missing second data source")

    local source2_carry = nil

    return function()
      local result_dp = nil

      while true do
        local data_point = source1.dp()
        if not data_point then
          return nil
        end

        result_dp = data_point

        local time1 = data_point.timestamp
        local paired_dp = nil
        while true do
          local candidate_dp = source2_carry or source2.dp()
          source2_carry = nil
          if not candidate_dp then
            break
          end

          local time2 = candidate_dp.timestamp
          local time_diff = math.abs(time1 - time2)

          if time_diff <= threshold then
            paired_dp = candidate_dp
            break
          elseif time2 < time1 - threshold then
            source2_carry = candidate_dp
            break
          end
        end


        if paired_dp then
          if operation == "_addition" then
            result_dp.value = data_point.value + paired_dp.value
          elseif operation == "_subtraction" then
            result_dp.value = data_point.value - paired_dp.value
          elseif operation == "_multiplication" then
            result_dp.value = data_point.value * paired_dp.value
          elseif operation == "_division" then
            if paired_dp.value == 0 then
              if on_missing == "_skip" then
                result_dp = nil
              end
            else
              result_dp.value = data_point.value / paired_dp.value
            end
          else
            error("invalid operation: " .. operation)
          end
        elseif on_missing == "_pass_through" then
          break
        else
          result_dp = nil
        end

        if result_dp ~= nil then break end
      end

      return result_dp
    end
  end,
}
