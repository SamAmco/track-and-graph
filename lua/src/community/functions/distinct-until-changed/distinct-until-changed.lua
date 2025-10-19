-- Lua Function to filter out consecutive duplicates based on selected fields
-- Only passes through data points when the selected fields change from the previous one

local enum = require("tng.config").enum

return {
	-- Configuration metadata
	id = "distinct-until-changed",
	version = "1.0.0",
	inputCount = 1,
	categories = { "_filter" },
	title = {
		["en"] = "Distinct Until Changed",
		["de"] = "Eindeutig bis geändert",
		["es"] = "Distinto hasta cambio",
		["fr"] = "Distinct jusqu'au changement",
	},
	description = {
		["en"] = "Filters out consecutive duplicates based on the selected fields. Only data points where the selected fields differ from the previous one will pass through.\n\n• All Fields - Compare value, label, and note\n• Value Only - Compare value only\n• Label Only - Compare label only\n• Note Only - Compare note only\n• Value and Label - Compare value and label\n• Value and Note - Compare value and note\n• Label and Note - Compare label and note",
		["de"] = "Filtert aufeinanderfolgende Duplikate basierend auf den ausgewählten Feldern heraus. Nur Datenpunkte, bei denen sich die ausgewählten Felder vom vorherigen unterscheiden, werden durchgelassen.\n\n• Alle Felder - Vergleicht Wert, Label und Notiz\n• Nur Wert - Vergleicht nur Wert\n• Nur Label - Vergleicht nur Label\n• Nur Notiz - Vergleicht nur Notiz\n• Wert und Label - Vergleicht Wert und Label\n• Wert und Notiz - Vergleicht Wert und Notiz\n• Label und Notiz - Vergleicht Label und Notiz",
		["es"] = "Filtra duplicados consecutivos basándose en los campos seleccionados. Solo los puntos de datos donde los campos seleccionados difieren del anterior pasarán.\n\n• Todos los campos - Compara valor, etiqueta y nota\n• Solo valor - Compara solo valor\n• Solo etiqueta - Compara solo etiqueta\n• Solo nota - Compara solo nota\n• Valor y etiqueta - Compara valor y etiqueta\n• Valor y nota - Compara valor y nota\n• Etiqueta y nota - Compara etiqueta y nota",
		["fr"] = "Filtre les doublons consécutifs en fonction des champs sélectionnés. Seuls les points de données où les champs sélectionnés diffèrent du précédent passeront.\n\n• Tous les champs - Compare valeur, étiquette et note\n• Valeur uniquement - Compare la valeur uniquement\n• Étiquette uniquement - Compare l'étiquette uniquement\n• Note uniquement - Compare la note uniquement\n• Valeur et étiquette - Compare valeur et étiquette\n• Valeur et note - Compare valeur et note\n• Étiquette et note - Compare étiquette et note",
	},
	config = {
		enum {
			id = "compare_by",
			name = "_compare_by",
			options = {
				"_all_fields",
				"_value_only",
				"_label_only",
				"_note_only",
				"_value_and_label",
				"_value_and_note",
				"_label_and_note",
			},
			default = "_all_fields",
		},
	},

	-- Generator function
	generator = function(source, config)
		local compare_by = config and config.compare_by or "_all_fields"

		local last_value = nil
		local last_label = nil
		local last_note = nil

		return function()
			while true do
				local data_point = source.dp()
				if not data_point then
					return nil
				end

				local current_value = data_point.value
				local current_label = data_point.label
				local current_note = data_point.note

				local is_different = false

				if compare_by == "_all_fields" then
					is_different = (current_value ~= last_value)
						or (current_label ~= last_label)
						or (current_note ~= last_note)
				elseif compare_by == "_value_only" then
					is_different = (current_value ~= last_value)
				elseif compare_by == "_label_only" then
					is_different = (current_label ~= last_label)
				elseif compare_by == "_note_only" then
					is_different = (current_note ~= last_note)
				elseif compare_by == "_value_and_label" then
					is_different = (current_value ~= last_value) or (current_label ~= last_label)
				elseif compare_by == "_value_and_note" then
					is_different = (current_value ~= last_value) or (current_note ~= last_note)
				elseif compare_by == "_label_and_note" then
					is_different = (current_label ~= last_label) or (current_note ~= last_note)
				end

				if is_different then
					last_value = current_value
					last_label = current_label
					last_note = current_note
					return data_point
				end
			end
		end
	end,
}
