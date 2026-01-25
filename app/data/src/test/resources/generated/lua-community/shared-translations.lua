-- Shared Translations
-- Maps translation keys to translated strings
-- Convention: All keys should be prefixed with _ to signal they are translation keys

local translations = {
	{
		_id = "_all_fields",
		["en"] = "All Fields",
		["de"] = "Alle Felder",
		["es"] = "Todos los campos",
		["fr"] = "Tous les champs",
	},
	{
		_id = "_value_only",
		["en"] = "Value Only",
		["de"] = "Nur Wert",
		["es"] = "Solo valor",
		["fr"] = "Valeur uniquement",
	},
	{
		_id = "_label_only",
		["en"] = "Label Only",
		["de"] = "Nur Label",
		["es"] = "Solo etiqueta",
		["fr"] = "Étiquette uniquement",
	},
	{
		_id = "_note_only",
		["en"] = "Note Only",
		["de"] = "Nur Notiz",
		["es"] = "Solo nota",
		["fr"] = "Note uniquement",
	},
	{
		_id = "_value_and_label",
		["en"] = "Value and Label",
		["de"] = "Wert und Label",
		["es"] = "Valor y etiqueta",
		["fr"] = "Valeur et étiquette",
	},
	{
		_id = "_value_and_note",
		["en"] = "Value and Note",
		["de"] = "Wert und Notiz",
		["es"] = "Valor y nota",
		["fr"] = "Valeur et note",
	},
	{
		_id = "_label_and_note",
		["en"] = "Label and Note",
		["de"] = "Label und Notiz",
		["es"] = "Etiqueta y nota",
		["fr"] = "Étiquette et note",
	},
	{
		_id = "_compare_by",
		["en"] = "Compare By",
		["de"] = "Vergleichen nach",
		["es"] = "Comparar por",
		["fr"] = "Comparer par",
	},
	{
		_id = "_arithmetic",
		["en"] = "Arithmetic",
		["de"] = "Arithmetik",
		["es"] = "Aritmética",
		["fr"] = "Arithmétique",
	},
	{
		_id = "_filter",
		["en"] = "Filter",
		["de"] = "Filter",
		["es"] = "Filtro",
		["fr"] = "Filtre",
	},
	{
		_id = "_transform",
		["en"] = "Transform",
		["de"] = "Transformieren",
		["es"] = "Transformar",
		["fr"] = "Transformer",
	},
	{
		_id = "_time",
		["en"] = "Time",
		["de"] = "Zeit",
		["es"] = "Tiempo",
		["fr"] = "Temps",
	},
	{
		_id = "_randomisers",
		["en"] = "Randomisers",
		["de"] = "Zufallsgeneratoren",
		["es"] = "Aleatorizadores",
		["fr"] = "Générateurs Aléatoires",
	},
	{
		_id = "_generators",
		["en"] = "Generators",
		["de"] = "Generatoren",
		["es"] = "Generadores",
		["fr"] = "Générateurs",
	},
	{
		_id = "_min_value",
		["en"] = "Min Value",
		["de"] = "Minimalwert",
		["es"] = "Valor Mínimo",
		["fr"] = "Valeur Minimale",
	},
	{
		_id = "_max_value",
		["en"] = "Max Value",
		["de"] = "Maximalwert",
		["es"] = "Valor Máximo",
		["fr"] = "Valeur Maximale",
	},
	{
		_id = "_seed",
		["en"] = "Seed",
		["de"] = "Seed",
		["es"] = "Semilla",
		["fr"] = "Graine",
	},
	{
		_id = "_day",
		["en"] = "Day",
		["de"] = "Tag",
		["es"] = "Día",
		["fr"] = "Jour",
	},
	{
		_id = "_week",
		["en"] = "Week",
		["de"] = "Woche",
		["es"] = "Semana",
		["fr"] = "Semaine",
	},
	{
		_id = "_month",
		["en"] = "Month",
		["de"] = "Monat",
		["es"] = "Mes",
		["fr"] = "Mois",
	},
	{
		_id = "_year",
		["en"] = "Year",
		["de"] = "Jahr",
		["es"] = "Año",
		["fr"] = "Année",
	},
	{
		_id = "_period",
		["en"] = "Period",
		["de"] = "Periode",
		["es"] = "Período",
		["fr"] = "Période",
	},
	{
		_id = "_cutoff",
		["en"] = "Cutoff",
		["de"] = "Grenzwert",
		["es"] = "Límite",
		["fr"] = "Limite",
	},
	{
		_id = "_period_multiplier",
		["en"] = "Period Multiplier",
		["de"] = "Periodenmultiplikator",
		["es"] = "Multiplicador de Período",
		["fr"] = "Multiplicateur de Période",
	},
	{
		_id = "_reset_on_label_match",
		["en"] = "Reset on Label Match",
		["de"] = "Zurücksetzen bei Label-Übereinstimmung",
		["es"] = "Restablecer al Coincidir Etiqueta",
		["fr"] = "Réinitialiser sur Correspondance d'Étiquette",
	},
	{
		_id = "_reset_label",
		["en"] = "Reset Label",
		["de"] = "Zurücksetzen-Label",
		["es"] = "Etiqueta de Restablecimiento",
		["fr"] = "Étiquette de Réinitialisation",
	},
	{
		_id = "_match_exactly",
		["en"] = "Match Exactly",
		["de"] = "Exakt übereinstimmen",
		["es"] = "Coincidir Exactamente",
		["fr"] = "Correspondance Exacte",
	},
	{
		_id = "_case_sensitive",
		["en"] = "Case Sensitive",
		["de"] = "Groß-/Kleinschreibung beachten",
		["es"] = "Distinguir Mayúsculas",
		["fr"] = "Sensible à la Casse",
	},
	{
		_id = "_addition",
		["en"] = "Addition",
		["de"] = "Addition",
		["es"] = "Suma",
		["fr"] = "Addition",
	},
	{
		_id = "_subtraction",
		["en"] = "Subtraction",
		["de"] = "Subtraktion",
		["es"] = "Resta",
		["fr"] = "Soustraction",
	},
	{
		_id = "_multiplication",
		["en"] = "Multiplication",
		["de"] = "Multiplikation",
		["es"] = "Multiplicación",
		["fr"] = "Multiplication",
	},
	{
		_id = "_division",
		["en"] = "Division",
		["de"] = "Division",
		["es"] = "División",
		["fr"] = "Division",
	},
	{
		_id = "_skip",
		["en"] = "Skip",
		["de"] = "Überspringen",
		["es"] = "Omitir",
		["fr"] = "Ignorer",
	},
	{
		_id = "_pass_through",
		["en"] = "Pass Through",
		["de"] = "Durchleiten",
		["es"] = "Pasar Sin Cambios",
		["fr"] = "Laisser Passer",
	},
	{
		_id = "_time_threshold",
		["en"] = "Time Threshold",
		["de"] = "Zeitschwellwert",
		["es"] = "Umbral de Tiempo",
		["fr"] = "Seuil de Temps",
	},
	{
		_id = "_operation",
		["en"] = "Operation",
		["de"] = "Operation",
		["es"] = "Operación",
		["fr"] = "Opération",
	},
	{
		_id = "_on_missing",
		["en"] = "On Missing",
		["de"] = "Bei Fehlen",
		["es"] = "En Faltante",
		["fr"] = "En Cas de Manque",
	},
	{
		_id = "_combine",
		["en"] = "Combine",
		["de"] = "Kombinieren",
		["es"] = "Combinar",
		["fr"] = "Combiner",
	},
	{
		_id = "_next",
		["en"] = "Next",
		["de"] = "Nächste",
		["es"] = "Siguiente",
		["fr"] = "Suivant",
	},
	{
		_id = "_last",
		["en"] = "Last",
		["de"] = "Letzte",
		["es"] = "Último",
		["fr"] = "Dernier",
	},
	{
		_id = "_nearest",
		["en"] = "Nearest",
		["de"] = "Nächstgelegene",
		["es"] = "Más Cercano",
		["fr"] = "Le Plus Proche",
	},
	{
		_id = "_monday",
		["en"] = "Monday",
		["de"] = "Montag",
		["es"] = "Lunes",
		["fr"] = "Lundi",
	},
	{
		_id = "_tuesday",
		["en"] = "Tuesday",
		["de"] = "Dienstag",
		["es"] = "Martes",
		["fr"] = "Mardi",
	},
	{
		_id = "_wednesday",
		["en"] = "Wednesday",
		["de"] = "Mittwoch",
		["es"] = "Miércoles",
		["fr"] = "Mercredi",
	},
	{
		_id = "_thursday",
		["en"] = "Thursday",
		["de"] = "Donnerstag",
		["es"] = "Jueves",
		["fr"] = "Jeudi",
	},
	{
		_id = "_friday",
		["en"] = "Friday",
		["de"] = "Freitag",
		["es"] = "Viernes",
		["fr"] = "Vendredi",
	},
	{
		_id = "_saturday",
		["en"] = "Saturday",
		["de"] = "Samstag",
		["es"] = "Sábado",
		["fr"] = "Samedi",
	},
	{
		_id = "_sunday",
		["en"] = "Sunday",
		["de"] = "Sonntag",
		["es"] = "Domingo",
		["fr"] = "Dimanche",
	},
	{
		_id = "_aggregation",
		["en"] = "Aggregation",
		["de"] = "Aggregation",
		["es"] = "Agregación",
		["fr"] = "Agrégation",
	},
	{
		_id = "_multiplier",
		["en"] = "Multiplier",
		["de"] = "Multiplikator",
		["es"] = "Multiplicador",
		["fr"] = "Multiplicateur",
	},
	{
		_id = "_window_size",
		["en"] = "Window Size",
		["de"] = "Fenstergröße",
		["es"] = "Tamaño de Ventana",
		["fr"] = "Taille de Fenêtre",
	},
	{
		_id = "_min",
		["en"] = "Min",
		["de"] = "Min",
		["es"] = "Mínimo",
		["fr"] = "Min",
	},
	{
		_id = "_max",
		["en"] = "Max",
		["de"] = "Max",
		["es"] = "Máximo",
		["fr"] = "Max",
	},
	{
		_id = "_average",
		["en"] = "Average",
		["de"] = "Durchschnitt",
		["es"] = "Promedio",
		["fr"] = "Moyenne",
	},
	{
		_id = "_sum",
		["en"] = "Sum",
		["de"] = "Summe",
		["es"] = "Suma",
		["fr"] = "Somme",
	},
	{
		_id = "_variance",
		["en"] = "Variance",
		["de"] = "Varianz",
		["es"] = "Varianza",
		["fr"] = "Variance",
	},
	{
		_id = "_standard_deviation",
		["en"] = "Standard Deviation",
		["de"] = "Standardabweichung",
		["es"] = "Desviación Estándar",
		["fr"] = "Écart-Type",
	},
	{
		_id = "_count",
		["en"] = "Count",
		["de"] = "Anzahl",
		["es"] = "Recuento",
		["fr"] = "Comptage",
	},
	{
		_id = "_seconds",
		["en"] = "Seconds",
		["de"] = "Sekunden",
		["es"] = "Segundos",
		["fr"] = "Secondes",
	},
	{
		_id = "_minutes",
		["en"] = "Minutes",
		["de"] = "Minuten",
		["es"] = "Minutos",
		["fr"] = "Minutes",
	},
	{
		_id = "_hours",
		["en"] = "Hours",
		["de"] = "Stunden",
		["es"] = "Horas",
		["fr"] = "Heures",
	},
	{
		_id = "_days",
		["en"] = "Days",
		["de"] = "Tage",
		["es"] = "Días",
		["fr"] = "Jours",
	},
	{
		_id = "_weeks",
		["en"] = "Weeks",
		["de"] = "Wochen",
		["es"] = "Semanas",
		["fr"] = "Semaines",
	},
	{
		_id = "_months",
		["en"] = "Months",
		["de"] = "Monate",
		["es"] = "Meses",
		["fr"] = "Mois",
	},
	{
		_id = "_years",
		["en"] = "Years",
		["de"] = "Jahre",
		["es"] = "Años",
		["fr"] = "Années",
	},
	{
		_id = "_threshold_units",
		["en"] = "Threshold Units",
		["de"] = "Schwellenwert-Einheiten",
		["es"] = "Unidades de Umbral",
		["fr"] = "Unités de Seuil",
	},
}

-- Validation: ensure all entries have all 4 required languages
local required_languages = { "en", "de", "es", "fr" }

for _, translation in ipairs(translations) do
	-- Check that the key is prefixed with _
	if not translation._id:match("^_") then
		error("Translation key must be prefixed with _: " .. translation._id)
	end

	-- Check that all required languages are present
	for _, lang in ipairs(required_languages) do
		if not translation[lang] or translation[lang] == "" then
			error("Translation " .. translation._id .. " is missing language: " .. lang)
		end
	end
end

-- Build index by ID
local translations_by_id = {}
for _, translation in ipairs(translations) do
	if translations_by_id[translation._id] then
		error("Duplicate translation key: " .. translation._id)
	end
	translations_by_id[translation._id] = translation
	translations_by_id[translation._id]._id = nil
end

return translations_by_id
