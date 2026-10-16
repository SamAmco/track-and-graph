local M = {}

--- Graph result type for a custom declarative UI.
--- @since (API level 2)
M.TYPE = "CUSTOM"

--- @since (API level 2)
--- @enum UNIT
M.UNIT = { PX = "PX", DP = "DP", SP = "SP" }

--- @since (API level 2)
--- @enum COMPONENT_TYPE
M.COMPONENT_TYPE = {
	ROW = "ROW",
	COLUMN = "COLUMN",
	BOX = "BOX",
	SPACER = "SPACER",
	TEXT = "TEXT",
	SURFACE = "SURFACE",
}

--- @since (API level 2)
--- @enum MODIFIER_TYPE
M.MODIFIER_TYPE = {
	WIDTH = "WIDTH",
	HEIGHT = "HEIGHT",
	FILL_WIDTH = "FILL_WIDTH",
	FILL_HEIGHT = "FILL_HEIGHT",
	WRAP_WIDTH = "WRAP_WIDTH",
	WRAP_HEIGHT = "WRAP_HEIGHT",
	WEIGHT = "WEIGHT",
	MIN_WIDTH = "MIN_WIDTH",
	MAX_WIDTH = "MAX_WIDTH",
	MIN_HEIGHT = "MIN_HEIGHT",
	MAX_HEIGHT = "MAX_HEIGHT",
	ASPECT_RATIO = "ASPECT_RATIO",
	PADDING = "PADDING",
	BACKGROUND = "BACKGROUND",
	BORDER = "BORDER",
	CLIP = "CLIP",
	ELEVATION = "ELEVATION",
	ALIGN = "ALIGN",
}

--- @since (API level 2)
--- @enum ALIGNMENT
M.ALIGNMENT = { START = "START", CENTER = "CENTER", END = "END", STRETCH = "STRETCH" }

--- @since (API level 2)
--- @enum BOX_ALIGNMENT
M.BOX_ALIGNMENT = {
	TOP_START = "TOP_START",
	TOP_CENTER = "TOP_CENTER",
	TOP_END = "TOP_END",
	CENTER_START = "CENTER_START",
	CENTER = "CENTER",
	CENTER_END = "CENTER_END",
	BOTTOM_START = "BOTTOM_START",
	BOTTOM_CENTER = "BOTTOM_CENTER",
	BOTTOM_END = "BOTTOM_END",
}

--- @since (API level 2)
--- @enum ARRANGEMENT
M.ARRANGEMENT = {
	START = "START",
	CENTER = "CENTER",
	END = "END",
	SPACE_BETWEEN = "SPACE_BETWEEN",
	SPACE_AROUND = "SPACE_AROUND",
	SPACE_EVENLY = "SPACE_EVENLY",
}

--- @since (API level 2)
--- @enum TEXT_ALIGN
M.TEXT_ALIGN = { START = "START", CENTER = "CENTER", END = "END" }

--- @since (API level 2)
--- @enum TEXT_OVERFLOW
M.TEXT_OVERFLOW = { CLIP = "CLIP", ELLIPSIS = "ELLIPSIS" }

--- @since (API level 2)
--- @enum FONT_WEIGHT
M.FONT_WEIGHT = { NORMAL = "NORMAL", MEDIUM = "MEDIUM", BOLD = "BOLD" }

--- @since (API level 2)
--- @class Dimension
--- @field unit UNIT
--- @field value number
M.Dimension = {}

--- @since (API level 2)
--- @class LayoutParams
--- @field key? string: Stable identity across renders.
--- @field modifier? table[]: Ordered modifiers.
--- @field children? table[]: Child components in display order.
--- @field spacing? Dimension|string: Explicit dimension or spacing token between adjacent children.
--- @field arrangement? ARRANGEMENT: Main-axis arrangement.
--- @field alignment? ALIGNMENT: Cross-axis alignment.
M.LayoutParams = {}

--- @since (API level 2)
--- @class BoxParams
--- @field key? string: Stable identity across renders.
--- @field modifier? table[]: Ordered modifiers.
--- @field children? table[]: Stacked child components.
--- @field alignment? BOX_ALIGNMENT: Default alignment for children.
M.BoxParams = {}

--- @since (API level 2)
--- @class TextParams
--- @field key? string: Stable identity across renders.
--- @field modifier? table[]: Ordered modifiers.
--- @field text string: Text to display.
--- @field color? Color|string: Raw or theme color token.
--- @field font_size? Dimension: Text size, normally expressed in sp.
--- @field font_weight? FONT_WEIGHT
--- @field text_align? TEXT_ALIGN
--- @field max_lines? integer
--- @field overflow? TEXT_OVERFLOW
M.TextParams = {}

--- @since (API level 2)
--- @class SurfaceParams
--- @field key? string: Stable identity across renders.
--- @field modifier? table[]: Ordered modifiers.
--- @field child? table: Surface content.
--- @field color? Color|string: Raw or theme background color token.
--- @field content_color? Color|string: Raw or theme content color token.
--- @field shape? string: Shape token.
--- @field tonal_elevation? Dimension
--- @field shadow_elevation? Dimension
M.SurfaceParams = {}

--- @since (API level 2)
--- @class CanvasParams
--- @field key? string: Stable identity across renders.
--- @field modifier? table[]: Ordered modifiers.
--- @field draw fun(draw_scope): Callback invoked after the canvas has been measured.
M.CanvasParams = {}

local dimension = function(unit, value)
	assert(type(value) == "number", "dimension value must be a number")
	return { unit = unit, value = value }
end

M.px = function(value)
	return dimension(M.UNIT.PX, value)
end

M.dp = function(value)
	return dimension(M.UNIT.DP, value)
end

M.sp = function(value)
	return dimension(M.UNIT.SP, value)
end

--- Creates an ordered modifier list.
M.modifier = function(...)
	return { ... }
end

local value_modifier = function(modifier_type, value)
	return { type = modifier_type, value = value }
end

M.width = function(value)
	return value_modifier(M.MODIFIER_TYPE.WIDTH, value)
end

M.height = function(value)
	return value_modifier(M.MODIFIER_TYPE.HEIGHT, value)
end

M.fill_width = function(fraction)
	return { type = M.MODIFIER_TYPE.FILL_WIDTH, fraction = fraction or 1 }
end

M.fill_height = function(fraction)
	return { type = M.MODIFIER_TYPE.FILL_HEIGHT, fraction = fraction or 1 }
end

M.wrap_width = function()
	return { type = M.MODIFIER_TYPE.WRAP_WIDTH }
end

M.wrap_height = function()
	return { type = M.MODIFIER_TYPE.WRAP_HEIGHT }
end

M.weight = function(value, fill)
	assert(type(value) == "number" and value > 0, "weight must be positive")
	return { type = M.MODIFIER_TYPE.WEIGHT, value = value, fill = fill ~= false }
end

M.min_width = function(value)
	return value_modifier(M.MODIFIER_TYPE.MIN_WIDTH, value)
end

M.max_width = function(value)
	return value_modifier(M.MODIFIER_TYPE.MAX_WIDTH, value)
end

M.min_height = function(value)
	return value_modifier(M.MODIFIER_TYPE.MIN_HEIGHT, value)
end

M.max_height = function(value)
	return value_modifier(M.MODIFIER_TYPE.MAX_HEIGHT, value)
end

M.aspect_ratio = function(value)
	assert(type(value) == "number" and value > 0, "aspect ratio must be positive")
	return value_modifier(M.MODIFIER_TYPE.ASPECT_RATIO, value)
end

--- Pass one dimension/token for all sides, or an edge table containing all, horizontal, vertical, start, top, end, and/or bottom.
M.padding = function(value)
	assert(type(value) == "table" or type(value) == "string", "padding must be a dimension, token, or edge table")
	if type(value) == "string" or value.unit ~= nil then
		return { type = M.MODIFIER_TYPE.PADDING, all = value }
	end
	return {
		type = M.MODIFIER_TYPE.PADDING,
		all = value.all,
		horizontal = value.horizontal,
		vertical = value.vertical,
		start = value.start,
		top = value.top,
		["end"] = value["end"],
		bottom = value.bottom,
	}
end

M.background = function(color, background_shape)
	return { type = M.MODIFIER_TYPE.BACKGROUND, color = color, shape = background_shape }
end

M.border = function(params)
	assert(type(params) == "table", "border params must be a table")
	return { type = M.MODIFIER_TYPE.BORDER, width = params.width, color = params.color, shape = params.shape }
end

M.clip = function(clip_shape)
	return { type = M.MODIFIER_TYPE.CLIP, shape = clip_shape }
end

M.elevation = function(value)
	return value_modifier(M.MODIFIER_TYPE.ELEVATION, value)
end

M.align = function(alignment)
	return value_modifier(M.MODIFIER_TYPE.ALIGN, alignment)
end

local component = function(component_type, params)
	params = params or {}
	return { type = component_type, key = params.key, modifier = params.modifier or {} }
end

local layout = function(component_type, params)
	params = params or {}
	local result = component(component_type, params)
	result.children = params.children or {}
	result.spacing = params.spacing or M.dp(0)
	result.arrangement = params.arrangement or M.ARRANGEMENT.START
	result.alignment = params.alignment or M.ALIGNMENT.START
	return result
end

M.row = function(params)
	return layout(M.COMPONENT_TYPE.ROW, params)
end

M.column = function(params)
	return layout(M.COMPONENT_TYPE.COLUMN, params)
end

M.box = function(params)
	params = params or {}
	local result = component(M.COMPONENT_TYPE.BOX, params)
	result.children = params.children or {}
	result.alignment = params.alignment or M.BOX_ALIGNMENT.TOP_START
	return result
end

M.spacer = function(params)
	return component(M.COMPONENT_TYPE.SPACER, params)
end

M.text = function(params)
	assert(type(params) == "table", "text params must be a table")
	assert(type(params.text) == "string", "text must be a string")
	local result = component(M.COMPONENT_TYPE.TEXT, params)
	result.text = params.text
	result.color = params.color
	result.font_size = params.font_size
	result.font_weight = params.font_weight
	result.text_align = params.text_align
	result.max_lines = params.max_lines
	result.overflow = params.overflow
	return result
end

M.surface = function(params)
	params = params or {}
	local result = component(M.COMPONENT_TYPE.SURFACE, params)
	result.child = params.child
	result.color = params.color
	result.content_color = params.content_color
	result.shape = params.shape
	result.tonal_elevation = params.tonal_elevation
	result.shadow_elevation = params.shadow_elevation
	return result
end

M.canvas = function(params)
	assert(type(params) == "table", "canvas params must be a table")
	local canvas = require("tng.canvas")
	local result = component(canvas.TYPE, params)
	result.draw = canvas.create(params.draw)
	return result
end

return M
