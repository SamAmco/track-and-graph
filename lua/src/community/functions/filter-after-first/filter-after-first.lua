-- Lua Function to filter data points after a reference point
-- Outputs all data points from the first source that come after the first point in the second source

return {
	-- Configuration metadata
	id = "filter-after-first",
	version = "1.0.0",
	inputCount = 2,
	categories = {"filter"},
	title = {
		["en"] = "Filter After First",
		["de"] = "Filtern nach Erstem",
		["es"] = "Filtrar después del primero",
		["fr"] = "Filtrer après le premier",
	},
	description = {
		["en"] = [[Filters data points from the first input source to only include those that occur after the first data point in the second input source.

This is useful for filtering data based on a reference event or timestamp from another tracker.]],
		["de"] = [[Filtert Datenpunkte aus der ersten Eingabequelle, um nur diejenigen einzuschließen, die nach dem ersten Datenpunkt in der zweiten Eingabequelle auftreten.

Dies ist nützlich zum Filtern von Daten basierend auf einem Referenzereignis oder Zeitstempel von einem anderen Tracker.]],
		["es"] = [[Filtra puntos de datos de la primera fuente de entrada para incluir solo aquellos que ocurren después del primer punto de datos en la segunda fuente de entrada.

Esto es útil para filtrar datos basados en un evento de referencia o marca de tiempo de otro rastreador.]],
		["fr"] = [[Filtre les points de données de la première source d'entrée pour n'inclure que ceux qui se produisent après le premier point de données de la deuxième source d'entrée.

Ceci est utile pour filtrer les données basées sur un événement de référence ou un horodatage d'un autre tracker.]],
	},
	config = {},

	-- Generator function
	generator = function(sources, config)
		local source1 = sources[1]
		local source2 = sources[2]
		local cutoff_timestamp = nil

		return function()
			-- Initialize cutoff on first call
			if cutoff_timestamp == nil then
				local reference_point = source2.dp()
				cutoff_timestamp = reference_point and reference_point.timestamp
			end

			-- Get next point from source1 and check if it's after cutoff
			local data_point = source1.dp()
			if not data_point then
				return nil
			end

			-- Data points are in reverse chronological order, so "after" means greater timestamp
			if not cutoff_timestamp or data_point.timestamp > cutoff_timestamp then
				return data_point
			end

			-- If the data point is not after the cutoff we're done
			return nil
		end
	end,
}
