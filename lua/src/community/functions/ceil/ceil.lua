-- Lua Function to ceiling values
-- Rounds each data point's value up to the nearest multiple of a specified number

return {
	-- Configuration metadata
	id = "ceil",
	version = "1.0.0",
	inputCount = 1,
	categories = {"arithmetic"},
	title = {
		["en"] = "Ceiling",
		["de"] = "Aufrunden",
		["es"] = "Techo",
		["fr"] = "Plafond",
	},
	description = {
		["en"] = [[Rounds each data point's value up to the nearest multiple of a specified number.

Configuration:
• Nearest: Round up to the nearest multiple of this number (default: 1.0)]],
		["de"] = [[Rundet den Wert jedes Datenpunkts auf das nächste Vielfache einer angegebenen Zahl auf.

Konfiguration:
• Nächste: Auf das nächste Vielfache dieser Zahl aufrunden (Standard: 1.0)]],
		["es"] = [[Redondea hacia arriba el valor de cada punto de datos al múltiplo más cercano de un número especificado.

Configuración:
• Más cercano: Redondear hacia arriba al múltiplo más cercano de este número (predeterminado: 1.0)]],
		["fr"] = [[Arrondit vers le haut la valeur de chaque point de données au multiple le plus proche d'un nombre spécifié.

Configuration:
• Plus proche: Arrondir vers le haut au multiple le plus proche de ce nombre (par défaut: 1.0)]],
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

			-- Ceiling to nearest multiple
			data_point.value = math.ceil(data_point.value / nearest) * nearest

			return data_point
		end
	end,
}
