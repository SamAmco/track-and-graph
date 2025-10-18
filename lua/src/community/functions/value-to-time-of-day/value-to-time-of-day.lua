-- Lua Function to set value to time of day
-- Sets each data point's value to the time of day in seconds since midnight

local core = require("tng.core")

return {
	-- Configuration metadata
	id = "value-to-time-of-day",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Value to Time of Day",
		["de"] = "Wert zu Tageszeit",
		["es"] = "Valor a hora del día",
		["fr"] = "Valeur à l'heure de la journée",
	},
	description = {
		["en"] = "Sets each data point's value to the time of day in seconds since midnight. The output is a duration value representing elapsed time since the start of the day.",
		["de"] = "Setzt den Wert jedes Datenpunkts auf die Tageszeit in Sekunden seit Mitternacht. Die Ausgabe ist ein Dauerwert, der die verstrichene Zeit seit Tagesbeginn darstellt.",
		["es"] = "Establece el valor de cada punto de datos en la hora del día en segundos desde la medianoche. La salida es un valor de duración que representa el tiempo transcurrido desde el comienzo del día.",
		["fr"] = "Définit la valeur de chaque point de données sur l'heure de la journée en secondes depuis minuit. La sortie est une valeur de durée représentant le temps écoulé depuis le début de la journée.",
	},
	config = {},

	-- Generator function
	generator = function(source)
		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			-- Get the date from the data point
			local date = core.date(data_point)

			-- Calculate seconds since midnight
			local seconds_since_midnight = (date.hour or 0) * 3600 + (date.min or 0) * 60 + (date.sec or 0)

			-- Set value to time of day in seconds
			data_point.value = seconds_since_midnight

			return data_point
		end
	end,
}
