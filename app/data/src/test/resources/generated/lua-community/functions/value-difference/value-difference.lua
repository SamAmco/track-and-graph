-- Lua Function to calculate value differences
-- Outputs the difference between each data point's value and the next one

return {
	-- Configuration metadata
	id = "value-difference",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_arithmetic", "_transform"},
	title = {
		["en"] = "Value Difference",
		["de"] = "Wertdifferenz",
		["es"] = "Diferencia de valor",
		["fr"] = "Différence de valeur",
	},
	description = {
		["en"] = [[
Calculates the difference between each data point's value and the next one. Each output point has its original identity with the value set to the difference.
]],
		["de"] = [[
Berechnet die Differenz zwischen dem Wert jedes Datenpunkts und dem nächsten. Jeder Ausgabepunkt hat seine ursprüngliche Identität mit dem Wert auf die Differenz gesetzt.
]],
		["es"] = [[
Calcula la diferencia entre el valor de cada punto de datos y el siguiente. Cada punto de salida tiene su identidad original con el valor establecido en la diferencia.
]],
		["fr"] = [[
Calcule la différence entre la valeur de chaque point de données et la suivante. Chaque point de sortie a son identité d'origine avec la valeur définie sur la différence.
]],
	},
	config = {},

	-- Generator function
	generator = function(source)
		local next_point = nil

		return function()
			-- Pre-load the next point on first call
			if next_point == nil then
				next_point = source.dp()
				if not next_point then
					return nil
				end
			end

			-- Current point is what we'll output
			local current_point = next_point

			-- Pre-load the next point for the next iteration
			next_point = source.dp()
			if not next_point then
				-- No more points, can't calculate difference
				return nil
			end

			-- Calculate difference (current - next)
			local difference = current_point.value - next_point.value

			-- Return current point with difference as value
			return {
				timestamp = current_point.timestamp,
				offset = current_point.offset,
				value = difference,
				label = current_point.label,
				note = current_point.note,
			}
		end
	end,
}
