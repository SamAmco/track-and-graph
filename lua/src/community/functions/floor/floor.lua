-- Lua Function to floor values
-- Rounds each data point's value down to the nearest multiple of a specified number

return {
	-- Configuration metadata
	id = "floor",
	version = "1.0.0",
	inputCount = 1,
	categories = {"arithmetic"},
	title = {
		["en"] = "Floor",
		["de"] = "Abrunden",
		["es"] = "Piso",
		["fr"] = "Plancher",
	},
	description = {
		["en"] = [[Rounds each data point's value down to the nearest multiple of a specified number.

Configuration:
• Nearest: Round down to the nearest multiple of this number (default: 1.0)]],
		["de"] = [[Rundet den Wert jedes Datenpunkts auf das nächste Vielfache einer angegebenen Zahl ab.

Konfiguration:
• Nächste: Auf das nächste Vielfache dieser Zahl abrunden (Standard: 1.0)]],
		["es"] = [[Redondea hacia abajo el valor de cada punto de datos al múltiplo más cercano de un número especificado.

Configuración:
• Más cercano: Redondear hacia abajo al múltiplo más cercano de este número (predeterminado: 1.0)]],
		["fr"] = [[Arrondit vers le bas la valeur de chaque point de données au multiple le plus proche d'un nombre spécifié.

Configuration:
• Plus proche: Arrondir vers le bas au multiple le plus proche de ce nombre (par défaut: 1.0)]],
	},
	config = {
		{
			id = "nearest",
			type = "number",
			default = 1.0,
			name = {
				["en"] = "Nearest",
				["de"] = "Nächste",
				["es"] = "Más cercano",
				["fr"] = "Plus proche",
			},
		},
	},

	-- Generator function
	generator = function(source, config)
		local nearest = config and config.nearest or 1.0

		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			-- Floor to nearest multiple
			data_point.value = math.floor(data_point.value / nearest) * nearest

			return data_point
		end
	end,
}
