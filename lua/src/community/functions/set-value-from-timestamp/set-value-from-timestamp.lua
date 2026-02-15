-- Sets each data point's value to a chosen component of its timestamp

local core = require("tng.core")
local enum = require("tng.config").enum

local extractors = {
	["_year_value"] = function(date)
		return date.year
	end,
	["_month_of_year"] = function(date)
		return date.month
	end,
	["_day_of_month"] = function(date)
		return date.day
	end,
	["_day_of_week"] = function(date)
		return date.wday
	end,
	["_hour_of_day"] = function(date)
		return date.hour or 0
	end,
	["_minute_of_hour"] = function(date)
		return date.min or 0
	end,
	["_second_of_minute"] = function(date)
		return date.sec or 0
	end,
	["_duration_since_midnight"] = function(date)
		return (date.hour or 0) * 3600 + (date.min or 0) * 60 + (date.sec or 0)
	end,
}

return {
	id = "set-value-from-timestamp",
	version = "1.0.1",
	inputCount = 1,
	categories = { "_time" },
	title = {
		["en"] = "Set Value from Timestamp",
		["de"] = "Wert aus Zeitstempel setzen",
		["es"] = "Establecer valor desde marca de tiempo",
		["fr"] = "Définir la valeur depuis l'horodatage",
	},
	description = {
		["en"] = "Sets each data point's value to a chosen component of its timestamp.",
		["de"] = "Setzt den Wert jedes Datenpunkts auf eine gewählte Komponente seines Zeitstempels.",
		["es"] = "Establece el valor de cada punto de datos en un componente elegido de su marca de tiempo.",
		["fr"] = "Définit la valeur de chaque point de données sur un composant choisi de son horodatage.",
	},
	config = {
		enum {
			id = "component",
			name = "_component",
			options = {
				"_year_value",
				"_month_of_year",
				"_day_of_month",
				"_day_of_week",
				"_hour_of_day",
				"_minute_of_hour",
				"_second_of_minute",
				"_duration_since_midnight",
			},
			default = "_day_of_month",
		},
	},

	generator = function(source, config)
		local component = config and config.component or "_day_of_month"
		local extract = extractors[component]

		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			local date = core.date(data_point)
			data_point.value = extract(date)

			return data_point
		end
	end,
}
