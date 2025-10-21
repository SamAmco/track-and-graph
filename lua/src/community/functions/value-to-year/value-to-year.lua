-- Lua Function to set value to year
-- Sets each data point's value to its year

local core = require("tng.core")

return {
	-- Configuration metadata
	id = "value-to-year",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Value to Year",
		["de"] = "Wert zu Jahr",
		["es"] = "Valor a año",
		["fr"] = "Valeur à l'année",
	},
	description = {
		["en"] = [[
Sets each data point's value to its year (e.g., 2025).
]],
		["de"] = [[
Setzt den Wert jedes Datenpunkts auf sein Jahr (z.B. 2025).
]],
		["es"] = [[
Establece el valor de cada punto de datos en su año (p. ej., 2025).
]],
		["fr"] = [[
Définit la valeur de chaque point de données sur son année (par exemple, 2025).
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
			data_point.value = date.year

			return data_point
		end
	end,
}
