-- Strict runtime view of the editable shared translation records.

local records = require("community.shared-translations-data")
local required_languages = { "en", "de", "es", "fr" }
local translations_by_id = {}

for _, record in ipairs(records) do
	local id = record._id
	if type(id) ~= "string" or not id:match("^_") then
		error("Translation key must be a string prefixed with _: " .. tostring(id))
	end
	if translations_by_id[id] then
		error("Duplicate translation key: " .. id)
	end
	for _, language in ipairs(required_languages) do
		if type(record[language]) ~= "string" or record[language] == "" then
			error("Translation " .. id .. " is missing language: " .. language)
		end
	end

	local values = {}
	for language, value in pairs(record) do
		if language ~= "_id" then
			values[language] = value
		end
	end
	translations_by_id[id] = values
end

return translations_by_id
