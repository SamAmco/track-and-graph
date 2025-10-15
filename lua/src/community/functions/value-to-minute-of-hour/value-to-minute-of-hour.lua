-- Lua Function to set value to minute of hour
-- Sets each data point's value to its minute of the hour (0-59)

local core = require("tng.core")

return {
	-- Configuration metadata
	id = "value-to-minute-of-hour",
	version = "1.0.0",
	inputCount = 1,
	categories = {"time"},
	title = {
		["en"] = "Value to Minute of Hour",
		["de"] = "Wert zu Minute der Stunde",
		["es"] = "Valor a minuto de la hora",
		["fr"] = "Valeur à la minute de l'heure",
	},
	description = {
		["en"] = "Sets each data point's value to its minute of the hour (0-59).",
		["de"] = "Setzt den Wert jedes Datenpunkts auf seine Minute der Stunde (0-59).",
		["es"] = "Establece el valor de cada punto de datos en su minuto de la hora (0-59).",
		["fr"] = "Définit la valeur de chaque point de données sur sa minute de l'heure (0-59).",
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
			data_point.value = date.min or 0

			return data_point
		end
	end,
}
