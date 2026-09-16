-- Converts duration data point values from seconds to a selected time unit.

local enum = require("tng.config").enum

local seconds_per_unit = {
	["_seconds"] = 1,
	["_minutes"] = 60,
	["_hours"] = 60 * 60,
	["_days"] = 24 * 60 * 60,
	["_weeks"] = 7 * 24 * 60 * 60,
	["_months"] = 30.44 * 24 * 60 * 60,
	["_years"] = 365.25 * 24 * 60 * 60,
}

return {
	id = "duration-in-units",
	version = "1.0.0",
	inputCount = 1,
	categories = { "_time" },
	title = {
		["en"] = "Duration in Units",
		["de"] = "Dauer in Einheiten",
		["es"] = "Duración en unidades",
		["fr"] = "Durée en unités",
	},
	description = {
		["en"] = [[
Converts each duration data point's value from seconds to the selected time unit. The output values are ordinary numbers rather than durations.

Months use an average length of 30.44 days and years use an average length of 365.25 days.
]],
		["de"] = [[
Konvertiert den Wert jedes Dauer-Datenpunkts von Sekunden in die ausgewählte Zeiteinheit. Die Ausgabewerte sind gewöhnliche Zahlen und keine Dauern.

Monate verwenden eine durchschnittliche Länge von 30,44 Tagen und Jahre eine durchschnittliche Länge von 365,25 Tagen.
]],
		["es"] = [[
Convierte el valor de cada punto de datos de duración de segundos a la unidad de tiempo seleccionada. Los valores de salida son números normales, no duraciones.

Los meses usan una duración media de 30,44 días y los años una duración media de 365,25 días.
]],
		["fr"] = [[
Convertit la valeur de chaque point de données de durée des secondes vers l'unité de temps sélectionnée. Les valeurs de sortie sont des nombres ordinaires, et non des durées.

Les mois utilisent une durée moyenne de 30,44 jours et les années une durée moyenne de 365,25 jours.
]],
	},
	config = {
		enum {
			id = "unit",
			name = {
				["en"] = "Time Unit",
				["de"] = "Zeiteinheit",
				["es"] = "Unidad de tiempo",
				["fr"] = "Unité de temps",
			},
			options = {
				"_seconds",
				"_minutes",
				"_hours",
				"_days",
				"_weeks",
				"_months",
				"_years",
			},
			default = "_hours",
		},
	},

	generator = function(source, config)
		local unit = config and config.unit or "_hours"
		local divisor = seconds_per_unit[unit]

		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			data_point.value = data_point.value / divisor
			return data_point
		end
	end,
}
