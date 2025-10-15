-- Lua Function to round values
-- Rounds each data point's value to the nearest multiple of a specified number

return {
	-- Configuration metadata
	id = "round",
	version = "1.0.0",
	inputCount = 1,
	categories = {"arithmetic"},
	title = {
		["en"] = "Round",
		["de"] = "Runden",
		["es"] = "Redondear",
		["fr"] = "Arrondir",
	},
	description = {
		["en"] = [[Rounds each data point's value to the nearest multiple of a specified number.

Configuration:
• Nearest: Round to the nearest multiple of this number (default: 1.0)]],
		["de"] = [[Rundet den Wert jedes Datenpunkts auf das nächste Vielfache einer angegebenen Zahl.

Konfiguration:
• Nächste: Auf das nächste Vielfache dieser Zahl runden (Standard: 1.0)]],
		["es"] = [[Redondea el valor de cada punto de datos al múltiplo más cercano de un número especificado.

Configuración:
• Más cercano: Redondear al múltiplo más cercano de este número (predeterminado: 1.0)]],
		["fr"] = [[Arrondit la valeur de chaque point de données au multiple le plus proche d'un nombre spécifié.

Configuration:
• Plus proche: Arrondir au multiple le plus proche de ce nombre (par défaut: 1.0)]],
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

			-- Round to nearest multiple
			data_point.value = math.floor((data_point.value / nearest) + 0.5) * nearest

			return data_point
		end
	end,
}
