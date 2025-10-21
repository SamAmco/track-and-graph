-- Lua Function to set value to hour of day
-- Sets each data point's value to its hour of the day (0-23)

local core = require("tng.core")

return {
	-- Configuration metadata
	id = "value-to-hour-of-day",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Value to Hour (0-23)",
		["de"] = "Wert zu Stunde (0-23)",
		["es"] = "Valor a hora (0-23)",
		["fr"] = "Valeur à l'heure (0-23)",
	},
	description = {
		["en"] = [[
Sets each data point's value to its hour of the day (0-23).
]],
		["de"] = [[
Setzt den Wert jedes Datenpunkts auf seine Stunde des Tages (0-23).
]],
		["es"] = [[
Establece el valor de cada punto de datos en su hora del día (0-23).
]],
		["fr"] = [[
Définit la valeur de chaque point de données sur son heure du jour (0-23).
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
			data_point.value = date.hour or 0

			return data_point
		end
	end,
}
