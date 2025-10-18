-- Example function demonstrating all available configuration types
-- This function showcases text, number, checkbox, enum, and uint inputs

return {
	id = "a1-test",
	version = "1.0.0",
	inputCount = 1,
	categories = { "_transform" },
	title = {
		["en"] = "Configuration Types Demo",
		["de"] = "Konfigurationstypen-Demo",
		["es"] = "Demostración de tipos de configuración",
		["fr"] = "Démonstration des types de configuration",
	},
	description = {
		["en"] = "Demonstrates all available configuration input types: text, number, checkbox, enum, and uint. This function passes through all data points unchanged.",
		["de"] = "Demonstriert alle verfügbaren Konfigurationseingabetypen: Text, Nummer, Kontrollkästchen, Aufzählung und uint. Diese Funktion gibt alle Datenpunkte unverändert weiter.",
		["es"] = "Demuestra todos los tipos de entrada de configuración disponibles: texto, número, casilla de verificación, enumeración y uint. Esta función pasa todos los puntos de datos sin cambios.",
		["fr"] = "Démontre tous les types d'entrée de configuration disponibles : texte, nombre, case à cocher, énumération et uint. Cette fonction transmet tous les points de données inchangés.",
	},
	config = {
		{
			id = "text_example",
			type = "text",
			name = {
				["en"] = "Text Input",
				["de"] = "Texteingabe",
				["es"] = "Entrada de texto",
				["fr"] = "Saisie de texte",
			},
			default = "Example text",
		},
		{
			id = "number_example",
			type = "number",
			name = {
				["en"] = "Number Input",
				["de"] = "Zahleneingabe",
				["es"] = "Entrada numérica",
				["fr"] = "Saisie numérique",
			},
			default = 3.14,
		},
		{
			id = "checkbox_example",
			type = "checkbox",
			name = {
				["en"] = "Checkbox Input",
				["de"] = "Kontrollkästchen",
				["es"] = "Casilla de verificación",
				["fr"] = "Case à cocher",
			},
			default = true,
		},
		{
			id = "enum_example",
			type = "enum",
			name = {
				["en"] = "Enum Input",
				["de"] = "Aufzählungseingabe",
				["es"] = "Entrada de enumeración",
				["fr"] = "Saisie d'énumération",
			},
			options = { "_hours", "_days", "_weeks" },
			default = "_days",
		},
		{
			id = "uint_example",
			type = "uint",
			name = {
				["en"] = "Unsigned Integer Input",
				["de"] = "Vorzeichenlose Ganzzahleingabe",
				["es"] = "Entrada de entero sin signo",
				["fr"] = "Saisie d'entier non signé",
			},
			default = 42,
		},
	},

	generator = function(source, config)
		-- Access configuration values (not used in this demo)
		local text_val = config and config.text_example
		local number_val = config and config.number_example
		local checkbox_val = config and config.checkbox_example
		local enum_val = config and config.enum_example
		local uint_val = config and config.uint_example

		-- Pass through all data points unchanged
		return function()
			return source[1].dp()
		end
	end,
}
