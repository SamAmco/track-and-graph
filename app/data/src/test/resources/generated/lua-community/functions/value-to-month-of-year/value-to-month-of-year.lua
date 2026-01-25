-- Lua Function to set value to month of year
-- Sets each data point's value to its month of the year (1-12)

local core = require("tng.core")

return {
	-- Configuration metadata
	id = "value-to-month-of-year",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Value to Month of Year",
		["de"] = "Wert zu Monat des Jahres",
		["es"] = "Valor a mes del año",
		["fr"] = "Valeur au mois de l'année",
	},
	description = {
		["en"] = [[
Sets each data point's value to its month of the year (1-12, where January is 1).
]],
		["de"] = [[
Setzt den Wert jedes Datenpunkts auf seinen Monat des Jahres (1-12, wobei Januar 1 ist).
]],
		["es"] = [[
Establece el valor de cada punto de datos en su mes del año (1-12, donde enero es 1).
]],
		["fr"] = [[
Définit la valeur de chaque point de données sur son mois de l'année (1-12, où janvier est 1).
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
			data_point.value = date.month

			return data_point
		end
	end,
}
