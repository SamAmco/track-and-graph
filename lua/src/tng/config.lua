-- Configuration API for Track & Graph Lua functions
-- Provides helper functions for defining configuration items

local M = {}

--- Create a text configuration item
--- @param spec table Configuration specification with id, name, and optional default
--- @return table Configuration item ready for use in config array
function M.text(spec)
	return {
		id = spec.id,
		type = "text",
		name = spec.name,
		default = spec.default,
	}
end

--- Create a number configuration item
--- @param spec table Configuration specification with id, name, and optional default
--- @return table Configuration item ready for use in config array
function M.number(spec)
	return {
		id = spec.id,
		type = "number",
		name = spec.name,
		default = spec.default,
	}
end

--- Create a checkbox configuration item
--- @param spec table Configuration specification with id, name, and optional default
--- @return table Configuration item ready for use in config array
function M.checkbox(spec)
	return {
		id = spec.id,
		type = "checkbox",
		name = spec.name,
		default = spec.default,
	}
end

--- Create an enum configuration item
--- @param spec table Configuration specification with id, name, options array, and optional default
--- @return table Configuration item ready for use in config array
function M.enum(spec)
	return {
		id = spec.id,
		type = "enum",
		name = spec.name,
		options = spec.options,
		default = spec.default,
	}
end

--- Create an unsigned integer configuration item
--- @param spec table Configuration specification with id, name, and optional default
--- @return table Configuration item ready for use in config array
function M.uint(spec)
	return {
		id = spec.id,
		type = "uint",
		name = spec.name,
		default = spec.default,
	}
end

--- Create a duration configuration item
--- @param spec table Configuration specification with id, name, and optional default (in seconds)
--- @return table Configuration item ready for use in config array
function M.duration(spec)
	return {
		id = spec.id,
		type = "duration",
		name = spec.name,
		default = spec.default,
	}
end

return M
