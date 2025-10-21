-- Lua Function to set value to day of week
-- Sets each data point's value to its day of the week (1-7, Monday is 1)

local core = require("tng.core")

return {
	-- Configuration metadata
	id = "value-to-day-of-week",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Value to Day of Week",
		["de"] = "Wert zu Wochentag",
		["es"] = "Valor a día de la semana",
		["fr"] = "Valeur au jour de la semaine",
	},
	description = {
		["en"] = [[
Sets each data point's value to its day of the week (1-7, where Monday is 1 and Sunday is 7).
]],
		["de"] = [[
Setzt den Wert jedes Datenpunkts auf seinen Wochentag (1-7, wobei Montag 1 und Sonntag 7 ist).
]],
		["es"] = [[
Establece el valor de cada punto de datos en su día de la semana (1-7, donde lunes es 1 y domingo es 7).
]],
		["fr"] = [[
Définit la valeur de chaque point de données sur son jour de la semaine (1-7, où lundi est 1 et dimanche est 7).
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

			-- Get the date from the timestamp
			local date = core.date(data_point)

			-- Set value to day of week
			data_point.value = date.wday

			return data_point
		end
	end,
}
