-- Example function demonstrating all available configuration types
-- This function showcases text, number, checkbox, enum, uint, duration, and localtime inputs

local tng_config = require("tng.config")
local text = tng_config.text
local number = tng_config.number
local checkbox = tng_config.checkbox
local enum = tng_config.enum
local uint = tng_config.uint
local duration = tng_config.duration
local localtime = tng_config.localtime

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
		["en"] =
		"Demonstrates all available configuration input types: text, number, checkbox, enum, uint, duration, and localtime. This function passes through all data points unchanged.",
		["de"] =
		"Demonstriert alle verfügbaren Konfigurationseingabetypen: Text, Nummer, Kontrollkästchen, Aufzählung, uint, Dauer und Ortszeit. Diese Funktion gibt alle Datenpunkte unverändert weiter.",
		["es"] =
		"Demuestra todos los tipos de entrada de configuración disponibles: texto, número, casilla de verificación, enumeración, uint, duración y hora local. Esta función pasa todos los puntos de datos sin cambios.",
		["fr"] =
		"Démontre tous les types d'entrée de configuration disponibles : texte, nombre, case à cocher, énumération, uint, durée et heure locale. Cette fonction transmet tous les points de données inchangés.",
	},
	config = {
		text {
			id = "text_example",
			name = {
				["en"] = "Text Input",
				["de"] = "Texteingabe",
				["es"] = "Entrada de texto",
				["fr"] = "Saisie de texte",
			},
			default = "Example text",
		},
		number {
			id = "number_example",
			name = {
				["en"] = "Number Input",
				["de"] = "Zahleneingabe",
				["es"] = "Entrada numérica",
				["fr"] = "Saisie numérique",
			},
			default = 3.14,
		},
		checkbox {
			id = "checkbox_example",
			name = {
				["en"] = "Checkbox Input",
				["de"] = "Kontrollkästchen",
				["es"] = "Casilla de verificación",
				["fr"] = "Case à cocher",
			},
			default = true,
		},
		enum {
			id = "enum_example",
			name = {
				["en"] = "Enum Input",
				["de"] = "Aufzählungseingabe",
				["es"] = "Entrada de enumeración",
				["fr"] = "Saisie d'énumération",
			},
			options = { "_hours", "_days", "_weeks" },
			default = "_days",
		},
		uint {
			id = "uint_example",
			name = {
				["en"] = "Unsigned Integer Input",
				["de"] = "Vorzeichenlose Ganzzahleingabe",
				["es"] = "Entrada de entero sin signo",
				["fr"] = "Saisie d'entier non signé",
			},
			default = 42,
		},
		duration {
			id = "duration_example",
			name = {
				["en"] = "Duration Input",
				["de"] = "Dauereingabe",
				["es"] = "Entrada de duración",
				["fr"] = "Saisie de durée",
			},
			default = 3600000, -- 1 hour (DURATION.HOUR)
		},
		localtime {
			id = "localtime_example",
			name = {
				["en"] = "Time of Day Input",
				["de"] = "Tageszeiteingabe",
				["es"] = "Entrada de hora del día",
				["fr"] = "Saisie de l'heure de la journée",
			},
			default = 52200000, -- 14:30 (2:30 PM) = 14.5 hours * DURATION.HOUR
		},
	},

	generator = function(source, config)
		-- Access configuration values (not used in this demo)
		local text_val = config and config.text_example
		local number_val = config and config.number_example
		local checkbox_val = config and config.checkbox_example
		local enum_val = config and config.enum_example
		local uint_val = config and config.uint_example
		local duration_val = config and config.duration_example
		local localtime_val = config and config.localtime_example

		-- Pass through all data points unchanged
		return function()
			return source.dp()
		end
	end,
}
