-- Lua Function to calculate time between data points
-- Outputs the duration in seconds between each data point and the previous one

local core = require("tng.core")
local checkbox = require("tng.config").checkbox

return {
	-- Configuration metadata
	id = "time-between",
	version = "1.0.1",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Time Between",
		["de"] = "Zeit dazwischen",
		["es"] = "Tiempo entre",
		["fr"] = "Temps entre",
	},
	description = {
		["en"] = [[
Calculates the duration in seconds between each data point and the previous one. The output value is the time difference in seconds and can be treated as a duration.

Configuration:
- **Include Time to Last**: Include the time between now and the last data point (default: false)
]],
		["de"] = [[
Berechnet die Dauer in Sekunden zwischen jedem Datenpunkt und dem vorherigen. Der Ausgabewert ist die Zeitdifferenz in Sekunden und kann als Dauer behandelt werden.

Konfiguration:
- **Zeit zum Letzten einschließen**: Die Zeit zwischen jetzt und dem letzten Datenpunkt einschließen (Standard: false)
]],
		["es"] = [[
Calcula la duración en segundos entre cada punto de datos y el anterior. El valor de salida es la diferencia de tiempo en segundos y puede tratarse como una duración.

Configuración:
- **Incluir tiempo al último**: Incluir el tiempo entre ahora y el último punto de datos (predeterminado: false)
]],
		["fr"] = [[
Calcule la durée en secondes entre chaque point de données et le précédent. La valeur de sortie est la différence de temps en secondes et peut être traitée comme une durée.

Configuration:
- **Inclure le temps jusqu'au dernier**: Inclure le temps entre maintenant et le dernier point de données (par défaut: false)
]],
	},
	config = {
		checkbox {
			id = "include_last",
			default = false,
			name = {
				["en"] = "Include Time to Last",
				["de"] = "Zeit zum Letzten einschließen",
				["es"] = "Incluir tiempo al último",
				["fr"] = "Inclure le temps jusqu'au dernier",
			},
		},
	},

	-- Generator function
	generator = function(source, config)
		local include_last = config and config.include_last or false
		local previous_point = nil

		return function()
			-- Initialize on first call
			if previous_point == nil then
				local first_point = source.dp()
				if not first_point then
					return nil
				end

				previous_point = first_point

				if include_last then
					-- Return synthetic point with time from now to last (oldest)
					local now = core.time().timestamp
					local duration_seconds = (now - first_point.timestamp) / 1000.0

					return {
						timestamp = first_point.timestamp,
						offset = first_point.offset,
						value = duration_seconds,
						label = "",
						note = "",
					}
				end
			end

			-- Get next data point
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			-- Calculate duration from previous to current
			local duration_seconds = (previous_point.timestamp - data_point.timestamp) / 1000.0

			-- Create output point using previous point's identity
			local output_point = {
				timestamp = previous_point.timestamp,
				offset = previous_point.offset,
				value = duration_seconds,
				label = previous_point.label,
				note = previous_point.note,
			}

			-- Update state for next iteration
			previous_point = data_point

			return output_point
		end
	end,
}
