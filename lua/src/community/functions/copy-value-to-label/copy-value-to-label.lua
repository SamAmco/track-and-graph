-- Copies the value of each data point to its label

local enum = require("tng.config").enum

return {
	id = "copy-value-to-label",
	version = "1.0.0",
	inputCount = 1,
	categories = { "_transform" },
	title = {
		["en"] = "Copy Value to Label",
		["de"] = "Wert in Label kopieren",
		["es"] = "Copiar valor a etiqueta",
		["fr"] = "Copier la valeur vers l'étiquette",
	},
	description = {
		["en"] = "Copies each data point's value to its label as a string.",
		["de"] = "Kopiert den Wert jedes Datenpunkts als Zeichenkette in sein Label.",
		["es"] = "Copia el valor de cada punto de datos a su etiqueta como cadena.",
		["fr"] = "Copie la valeur de chaque point de données vers son étiquette sous forme de chaîne.",
	},
	config = {
		enum {
			id = "mode",
			name = "_mode",
			options = {
				"_overwrite",
				"_prepend",
				"_append",
			},
			default = "_overwrite",
		},
	},

	generator = function(source, config)
		local mode = config and config.mode or "_overwrite"

		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			local value_str = tostring(data_point.value)
			local existing = data_point.label or ""

			if mode == "_overwrite" then
				data_point.label = value_str
			elseif mode == "_prepend" then
				data_point.label = value_str .. existing
			elseif mode == "_append" then
				data_point.label = existing .. value_str
			end

			return data_point
		end
	end,
}
