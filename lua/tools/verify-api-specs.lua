#!/usr/bin/env lua
-- verify-api-specs.lua
-- Validates that all exported symbols from tng modules have API spec entries

local api_specs = require("tools.lib.api-specs")

local function main()
	print("Verifying API specs...")

	local ok, result = api_specs.verify()

	if not ok and result.error then
		print("✗ " .. result.error)
		os.exit(1)
	end

	-- Print results for each module
	for _, module_result in ipairs(result.modules) do
		if #module_result.errors > 0 then
			print(string.format("✗ %s.lua - %d errors:", module_result.name, #module_result.errors))
			for _, error_msg in ipairs(module_result.errors) do
				print(error_msg)
			end
		else
			print(string.format("✓ %s.lua - all %d exports have specs",
				module_result.name, module_result.export_count))
		end
	end

	print()
	if result.total_errors > 0 then
		print(string.format("ERROR: API spec validation failed with %d error(s)", result.total_errors))
		os.exit(1)
	else
		print(string.format("✓ All API specs valid (%d exports across %d modules)",
			result.total_exports, #result.modules))
		os.exit(0)
	end
end

-- Run with error handling
local success, err = pcall(main)
if not success then
	io.stderr:write("FATAL ERROR: " .. tostring(err) .. "\n")
	os.exit(1)
end
