local M = {}

--- Custom UI graph type.
--- @since (API level 2)
M.TYPE = "CUSTOM"

--- Custom UI component types.
--- @since (API level 2)
--- @enum COMPONENT_TYPE
M.COMPONENT_TYPE = {
	ROW = "ROW",
	COLUMN = "COLUMN",
	TEXT = "TEXT",
}

--- @since (API level 2)
--- @class LayoutParams
--- @field children table[]: Child components in display order.
--- @field spacing? number: Space between adjacent children. Defaults to 0.
M.LayoutParams = {}

--- @since (API level 2)
--- @class TextParams
--- @field text string: Text to display.
--- @field color? Color: Text color.
--- @field size? number: Text size.
M.TextParams = {}

--- @since (API level 2)
--- @class CanvasParams
--- @field height number: Canvas height.
--- @field draw fun(draw_scope): Callback that records canvas draw instructions.
M.CanvasParams = {}

local layout = function(component_type, params)
	params = params or {}
	return {
		type = component_type,
		children = params.children or {},
		spacing = params.spacing or 0,
	}
end

--- Creates a horizontal layout component.
--- @since (API level 2)
--- @param params LayoutParams
--- @return table
M.row = function(params)
	return layout(M.COMPONENT_TYPE.ROW, params)
end

--- Creates a vertical layout component.
--- @since (API level 2)
--- @param params LayoutParams
--- @return table
M.column = function(params)
	return layout(M.COMPONENT_TYPE.COLUMN, params)
end

--- Creates a text component.
--- @since (API level 2)
--- @param params TextParams
--- @return table
M.text = function(params)
	assert(type(params) == "table", "text params must be a table")
	assert(type(params.text) == "string", "text must be a string")
	return {
		type = M.COMPONENT_TYPE.TEXT,
		text = params.text,
		color = params.color,
		size = params.size,
	}
end

--- Creates a canvas component.
--- @since (API level 2)
--- @param params CanvasParams
--- @return table
M.canvas = function(params)
	assert(type(params) == "table", "canvas params must be a table")
	assert(type(params.height) == "number", "canvas height must be a number")
	local component = require("tng.canvas").create(params.draw)
	component.height = params.height
	return component
end

return M
