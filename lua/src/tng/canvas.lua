local M = {}

--- @since (API level 2)
M.TYPE = "CANVAS"

--- Canvas draw instruction types.
--- @since (API level 2)
--- @enum INSTRUCTION_TYPE
M.INSTRUCTION_TYPE = {
	CIRCLE = "CIRCLE",
	LINE = "LINE",
}

--- Canvas drawing styles. These map to Compose DrawStyle variants.
--- @since (API level 2)
--- @enum DRAW_STYLE
M.DRAW_STYLE = {
	FILL = "fill",
	STROKE = "stroke",
}

--- Stroke cap styles. These map to Compose StrokeCap variants.
--- @since (API level 2)
--- @enum STROKE_CAP
M.STROKE_CAP = {
	BUTT = "butt",
	ROUND = "round",
	SQUARE = "square",
}

--- @since (API level 2)
--- @class CanvasPoint
--- @field x number: Horizontal coordinate, increasing from left to right.
--- @field y number: Vertical coordinate, increasing from top to bottom.
M.CanvasPoint = {}

--- @since (API level 2)
--- @class DrawLineParams
--- @field color Color: The line color.
--- @field start CanvasPoint: The first point of the line.
--- @field finish CanvasPoint: The second point of the line. Equivalent to Compose DrawScope.drawLine's `end` parameter.
--- @field stroke_width? number: Stroke width in canvas units. Defaults to 0 (a hairline).
--- @field cap? STROKE_CAP: Stroke cap style. Defaults to butt.
--- @field alpha? number: Opacity from 0 to 1. Defaults to 1.
M.DrawLineParams = {}

--- @since (API level 2)
--- @class DrawCircleParams
--- @field color Color: The circle color.
--- @field radius number: Circle radius.
--- @field center CanvasPoint: Centre of the circle.
--- @field alpha? number: Opacity from 0 to 1. Defaults to 1.
--- @field style? DRAW_STYLE: Whether the circle is filled or stroked. Defaults to fill.
--- @field stroke_width? number: Stroke width when style is stroke. Defaults to 0 (a hairline).
M.DrawCircleParams = {}

local CanvasDrawScope = {}
CanvasDrawScope.__index = CanvasDrawScope

local copy_point = function(point)
	return { x = point.x, y = point.y }
end

--- Records the equivalent of a Compose DrawScope.drawLine call.
--- @param params DrawLineParams
CanvasDrawScope.drawLine = function(self, params)
	table.insert(self.instructions, {
		type = M.INSTRUCTION_TYPE.LINE,
		color = params.color,
		start = copy_point(params.start),
		finish = copy_point(params.finish),
		stroke_width = params.stroke_width or 0,
		cap = params.cap or M.STROKE_CAP.BUTT,
		alpha = params.alpha or 1,
	})
end

--- Records the equivalent of a Compose DrawScope.drawCircle call.
--- @param params DrawCircleParams
CanvasDrawScope.drawCircle = function(self, params)
	table.insert(self.instructions, {
		type = M.INSTRUCTION_TYPE.CIRCLE,
		color = params.color,
		radius = params.radius,
		center = copy_point(params.center),
		alpha = params.alpha or 1,
		style = params.style or M.DRAW_STYLE.FILL,
		stroke_width = params.stroke_width or 0,
	})
end

--- Creates a canvas component by recording draw calls in order.
--- @since (API level 2)
--- @param draw fun(draw_scope): A callback that issues draw calls on the supplied scope.
--- @return table: A serializable canvas component containing the recorded draw instructions.
M.create = function(draw)
	assert(type(draw) == "function", "canvas draw callback must be a function")

	local result = {
		type = M.TYPE,
		instructions = {},
	}
	local draw_scope = setmetatable({
		instructions = result.instructions,
	}, CanvasDrawScope)
	draw(draw_scope)
	return result
end

return M
