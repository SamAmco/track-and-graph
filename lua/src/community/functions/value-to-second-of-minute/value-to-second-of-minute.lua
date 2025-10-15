-- Lua Function to set value to second of minute
-- Sets each data point's value to its second of the minute (0-59)

local core = require("tng.core")

return {
	-- Configuration metadata
	id = "value-to-second-of-minute",
	version = "1.0.0",
	inputCount = 1,
	title = {
		["en"] = "Value to Second of Minute",
		["de"] = "Wert zu Sekunde der Minute",
		["es"] = "Valor a segundo del minuto",
		["fr"] = "Valeur à la seconde de la minute",
	},
	description = {
		["en"] = "Sets each data point's value to its second of the minute (0-59).",
		["de"] = "Setzt den Wert jedes Datenpunkts auf seine Sekunde der Minute (0-59).",
		["es"] = "Establece el valor de cada punto de datos en su segundo del minuto (0-59).",
		["fr"] = "Définit la valeur de chaque point de données sur sa seconde de la minute (0-59).",
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
			data_point.value = date.sec or 0

			return data_point
		end
	end,
}
