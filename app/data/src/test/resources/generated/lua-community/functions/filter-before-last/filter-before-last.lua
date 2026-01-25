-- Lua Function to filter data points before a reference point
-- Outputs all data points from the first source that come before the last point in the second source

return {
	-- Configuration metadata
	id = "filter-before-last",
	version = "1.0.0",
	inputCount = 2,
	categories = { "_filter" },
	title = {
		["en"] = "Filter Before Last",
		["de"] = "Filtern vor Letztem",
		["es"] = "Filtrar antes del último",
		["fr"] = "Filtrer avant le dernier",
	},
	description = {
		["en"] = [[
Filters data points from the first input source to only include those that occur before the last data point in the second input source.

This is useful for filtering data based on a reference event or timestamp from another tracker.
]],
		["de"] = [[
Filtert Datenpunkte aus der ersten Eingabequelle, um nur diejenigen einzuschließen, die vor dem letzten Datenpunkt in der zweiten Eingabequelle auftreten.

Dies ist nützlich zum Filtern von Daten basierend auf einem Referenzereignis oder Zeitstempel von einem anderen Tracker.
]],
		["es"] = [[
Filtra puntos de datos de la primera fuente de entrada para incluir solo aquellos que ocurren antes del último punto de datos en la segunda fuente de entrada.

Esto es útil para filtrar datos basados en un evento de referencia o marca de tiempo de otro rastreador.
]],
		["fr"] = [[
Filtre les points de données de la première source d'entrée pour n'inclure que ceux qui se produisent avant le dernier point de données de la deuxième source d'entrée.

Ceci est utile pour filtrer les données basées sur un événement de référence ou un horodatage d'un autre tracker.
]],
	},
	config = {},

	-- Generator function
	generator = function(sources)
		local source1 = sources[1]
		local source2 = sources[2]

		-- Initialize cutoff on first call
		local reference_point = source2.dp()
		local cutoff_timestamp = reference_point and reference_point.timestamp

		return function()
			while true do
				-- Get next point from source1 and check if it's after cutoff
				local data_point = source1.dp()
				if not data_point then
					return nil
				end

				if not cutoff_timestamp or data_point.timestamp < cutoff_timestamp then
					return data_point
				end
			end
		end
	end
}
