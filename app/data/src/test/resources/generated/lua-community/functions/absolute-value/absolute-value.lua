-- Lua Function to take absolute value
-- Converts all data point values to their absolute value

return {
	-- Configuration metadata
	id = "absolute-value",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_arithmetic"},
	title = {
		["en"] = "Absolute Value",
		["de"] = "Absolutwert",
		["es"] = "Valor absoluto",
		["fr"] = "Valeur absolue",
	},
	description = {
		["en"] = [[
Converts each data point's value to its absolute value (removes negative sign).
]],
		["de"] = [[
Konvertiert den Wert jedes Datenpunkts zu seinem Absolutwert (entfernt negatives Vorzeichen).
]],
		["es"] = [[
Convierte el valor de cada punto de datos a su valor absoluto (elimina el signo negativo).
]],
		["fr"] = [[
Convertit la valeur de chaque point de données en sa valeur absolue (supprime le signe négatif).
]],
	},
	config = {},

	-- Generator function
	generator = function(source)
		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			-- Convert to absolute value
			data_point.value = math.abs(data_point.value)

			return data_point
		end
	end,
}
