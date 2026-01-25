-- Lua Function to set value to day of month
-- Sets each data point's value to its day of the month (1-31)

local core = require("tng.core")

return {
	-- Configuration metadata
	id = "value-to-day-of-month",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Value to Day of Month",
		["de"] = "Wert zu Tag des Monats",
		["es"] = "Valor a día del mes",
		["fr"] = "Valeur au jour du mois",
	},
	description = {
		["en"] = [[
Sets each data point's value to its day of the month (1-31).
]],
		["de"] = [[
Setzt den Wert jedes Datenpunkts auf seinen Tag des Monats (1-31).
]],
		["es"] = [[
Establece el valor de cada punto de datos en su día del mes (1-31).
]],
		["fr"] = [[
Définit la valeur de chaque point de données sur son jour du mois (1-31).
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

			local date = core.date(data_point)
			data_point.value = date.day

			return data_point
		end
	end,
}
