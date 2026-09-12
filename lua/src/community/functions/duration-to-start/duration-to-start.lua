-- Move duration data points from the end of their duration to the start.
-- Duration values in Track & Graph are expressed in seconds, while timestamps are milliseconds.

local core = require("tng.core")

-- Input data points arrive newest-first. Negative duration values can move an arbitrarily old
-- input point to an arbitrarily new output timestamp, so the complete input must be transformed
-- before its chronological output order can be known.
local function comes_before(left, right)
	if left.data_point.timestamp ~= right.data_point.timestamp then
		return left.data_point.timestamp > right.data_point.timestamp
	end
	return left.input_order < right.input_order
end

return {
	id = "duration-to-start",
	version = "1.0.3",
	inputCount = 1,
	categories = { "_time" },
	title = {
		["en"] = "Move Duration to Start",
		["de"] = "Dauer an den Anfang verschieben",
		["es"] = "Mover duración al inicio",
		["fr"] = "Déplacer la durée au début",
	},
	description = {
		["en"] = [[
Moves each duration data point's timestamp from the end of its duration to the start.

Expects duration data points as input. Values, labels, and notes are preserved. The output maintains chronological order, including when durations overlap.

**Performance warning:** This function must load and sort all input data points before producing output, so it may be slow and use more memory with large datasets.
]],
		["de"] = [[
Verschiebt den Zeitstempel jedes Dauer-Datenpunkts vom Ende seiner Dauer an den Anfang.

Erwartet Dauer-Datenpunkte als Eingabe. Werte, Beschriftungen und Notizen bleiben erhalten. Die Ausgabe behält die chronologische Reihenfolge bei, auch wenn sich Dauern überschneiden.

**Leistungshinweis:** Diese Funktion muss vor der Ausgabe alle Eingabedatenpunkte laden und sortieren. Bei großen Datensätzen kann sie daher langsam sein und mehr Speicher benötigen.
]],
		["es"] = [[
Mueve la marca de tiempo de cada punto de datos de duración desde el final de su duración hasta el inicio.

Espera puntos de datos de duración como entrada. Se conservan los valores, las etiquetas y las notas. La salida mantiene el orden cronológico, incluso cuando las duraciones se superponen.

**Advertencia de rendimiento:** Esta función debe cargar y ordenar todos los puntos de datos de entrada antes de producir resultados, por lo que puede ser lenta y usar más memoria con conjuntos de datos grandes.
]],
		["fr"] = [[
Déplace l'horodatage de chaque point de données de durée de la fin de sa durée vers le début.

Attend des points de données de durée en entrée. Les valeurs, les libellés et les notes sont conservés. La sortie maintient l'ordre chronologique, même lorsque les durées se chevauchent.

**Avertissement concernant les performances :** Cette fonction doit charger et trier tous les points de données d'entrée avant de produire une sortie. Elle peut donc être lente et utiliser davantage de mémoire avec de grands jeux de données.
]],
	},
	config = {},

	generator = function(source)
		local sorted_points = nil
		local output_index = 1

		return function()
			if not sorted_points then
				sorted_points = {}
				local input_order = 0
				while true do
					local data_point = source.dp()
					if not data_point then
						break
					end
					input_order = input_order + 1
					sorted_points[input_order] = {
						data_point = core.shift(data_point, -(data_point.value * 1000)),
						input_order = input_order,
					}
				end
				table.sort(sorted_points, comes_before)
			end

			local output = sorted_points[output_index]
			if output then
				output_index = output_index + 1
				return output.data_point
			end
			return nil
		end
	end,
}
