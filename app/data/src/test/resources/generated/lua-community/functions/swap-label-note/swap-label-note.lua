-- Lua Function to swap label and note fields
-- Swaps the label and note of each data point

return {
	-- Configuration metadata
	id = "swap-label-note",
	version = "1.0.0",
	inputCount = 1,
	categories = {"_transform"},
	title = {
		["en"] = "Swap Label and Note",
		["de"] = "Label und Notiz tauschen",
		["es"] = "Intercambiar etiqueta y nota",
		["fr"] = "Échanger étiquette et note",
	},
	description = {
		["en"] = [[
Swaps the label and note fields of each data point.
]],
		["de"] = [[
Tauscht die Label- und Notizfelder jedes Datenpunkts aus.
]],
		["es"] = [[
Intercambia los campos de etiqueta y nota de cada punto de datos.
]],
		["fr"] = [[
Échange les champs étiquette et note de chaque point de données.
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

			-- Swap label and note
			local temp = data_point.label
			data_point.label = data_point.note
			data_point.note = temp

			return data_point
		end
	end,
}
