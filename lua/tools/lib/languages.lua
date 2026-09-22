local M = {}

local source = debug.getinfo(1, "S").source
local module_path = source:sub(1, 1) == "@" and source:sub(2) or source
local module_dir = module_path:match("^(.*[/\\])") or ""
local languages_path = module_dir .. "../../../configuration/translation-languages.tsv"

function M.load()
	local file, open_error = io.open(languages_path, "r")
	if not file then
		error("Could not open translation language manifest: " .. tostring(open_error))
	end

	local languages = {}
	local seen = {}
	local line_number = 0
	for raw_line in file:lines() do
		line_number = line_number + 1
		local line = raw_line:match("^%s*(.-)%s*$")
		if line ~= "" and not line:match("^#") then
			local locale, language = line:match("^([^\t]+)\t(.+)$")
			if not locale or not language then
				file:close()
				error(string.format("%s:%d: expected tab-separated locale and name", languages_path, line_number))
			end
			if seen[locale] then
				file:close()
				error(string.format("%s:%d: duplicate locale %s", languages_path, line_number, locale))
			end
			seen[locale] = true
			table.insert(languages, { locale = locale, language = language })
		end
	end
	file:close()

	if not languages[1] or languages[1].locale ~= "en" or languages[1].language ~= "English" then
		error(languages_path .. ": first language must be en<TAB>English")
	end
	return languages
end

function M.codes()
	local codes = {}
	for _, language in ipairs(M.load()) do
		table.insert(codes, language.locale)
	end
	return codes
end

return M
