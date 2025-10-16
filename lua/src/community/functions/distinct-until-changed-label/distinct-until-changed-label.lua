-- Lua Function to filter out consecutive duplicate labels
-- Only passes through data points when the label changes from the previous one

return {
	-- Configuration metadata
	id = "distinct-until-changed-label",
	version = "1.0.0",
	inputCount = 1,
	categories = { "filter" },
	title = {
		["en"] = "Distinct Until Changed (Label)",
		["de"] = "Eindeutig bis geändert (Label)",
		["es"] = "Distinto hasta cambio (Etiqueta)",
		["fr"] = "Distinct jusqu'au changement (Étiquette)",
	},
	description = {
		["en"] = "Filters out consecutive duplicate labels. Only data points where the label differs from the previous one will pass through.",
		["de"] = "Filtert aufeinanderfolgende doppelte Labels heraus. Nur Datenpunkte, bei denen sich das Label vom vorherigen unterscheidet, werden durchgelassen.",
		["es"] = "Filtra etiquetas duplicadas consecutivas. Solo los puntos de datos donde la etiqueta difiere de la anterior pasarán.",
		["fr"] = "Filtre les étiquettes en double consécutives. Seuls les points de données où l'étiquette diffère de la précédente passeront.",
	},
	config = {
		{
			id = "enumConfig",
			type = "enum",
			name = { en = "Time Unit", de = "Zeiteinheit", es = "Unidad de tiempo", fr = "Unité de temps" },
			options = { "seconds", "minutes", "hours", "days" },
			default = "hours",
		},
	},

	-- Generator function
	generator = function(source, config)
		local last_label = nil

		return function()
			while true do
				local data_point = source.dp()
				if not data_point then
					return nil
				end

				local current_label = data_point.label
				if current_label ~= last_label then
					last_label = current_label
					return data_point
				end
			end
		end
	end,
}
