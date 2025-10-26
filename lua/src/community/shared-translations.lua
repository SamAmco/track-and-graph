-- Shared Translations
-- Maps translation keys to translated strings
-- Convention: All keys should be prefixed with _ to signal they are translation keys

local translations = {
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
		_id = "_frequency",
		["en"] = "Frequency",
		["de"] = "Häufigkeit",
		["es"] = "Frecuencia",
		["fr"] = "Fréquence",
	},
	{
		_id = "_time_of_day",
		["en"] = "Time of Day",
		["de"] = "Tageszeit",
		["es"] = "Hora del Día",
		["fr"] = "Heure de la Journée",
	},
	{
		_id = "_time_jitter",
		["en"] = "Time Jitter",
		["de"] = "Zeitliche Streuung",
		["es"] = "Variación de Tiempo",
		["fr"] = "Fluctuation Temporelle",
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
