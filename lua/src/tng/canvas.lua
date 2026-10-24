local M = {}

--- @since (API level 5)
M.TYPE = "CANVAS"

--- Canvas draw instruction types.
--- @since (API level 5)
--- @enum INSTRUCTION_TYPE
M.INSTRUCTION_TYPE = {
	ARC = "ARC",
	CIRCLE = "CIRCLE",
	GROUP = "GROUP",
	LINE = "LINE",
	OVAL = "OVAL",
	PATH = "PATH",
	POINTS = "POINTS",
	RECT = "RECT",
	ROUND_RECT = "ROUND_RECT",
	TEXT = "TEXT",
}

--- Canvas drawing styles. These map to Compose DrawStyle variants.
--- @since (API level 5)
--- @enum DRAW_STYLE
M.DRAW_STYLE = {
	FILL = "fill",
	STROKE = "stroke",
}

--- Stroke cap styles.
--- @since (API level 5)
--- @enum STROKE_CAP
M.STROKE_CAP = {
	BUTT = "butt",
	ROUND = "round",
	SQUARE = "square",
}

--- Stroke join styles.
--- @since (API level 5)
--- @enum STROKE_JOIN
M.STROKE_JOIN = {
	MITER = "miter",
	ROUND = "round",
	BEVEL = "bevel",
}

--- How a list of points is interpreted.
--- @since (API level 5)
--- @enum POINT_MODE
M.POINT_MODE = {
	POINTS = "points",
	LINES = "lines",
	POLYGON = "polygon",
}

--- Clipping operations.
--- @since (API level 5)
--- @enum CLIP_OP
M.CLIP_OP = {
	INTERSECT = "intersect",
	DIFFERENCE = "difference",
}

--- Canvas text alignment within the optional text bounds.
--- @since (API level 5)
--- @enum TEXT_ALIGN
M.TEXT_ALIGN = {
	START = "start",
	CENTER = "center",
	END = "end",
}

--- Canvas text alignment on the vertical axis of the optional text bounds.
--- This is a Track & Graph abstraction; Compose has no equivalent vertical TextAlign.
--- @since (API level 5)
--- @enum TEXT_VERTICAL_ALIGN
M.TEXT_VERTICAL_ALIGN = {
	TOP = "top",
	CENTER = "center",
	BOTTOM = "bottom",
}

--- Canvas text overflow behavior.
--- @since (API level 5)
--- @enum TEXT_OVERFLOW
M.TEXT_OVERFLOW = {
	CLIP = "clip",
	ELLIPSIS = "ellipsis",
	VISIBLE = "visible",
}

--- Canvas font weights.
--- @since (API level 5)
--- @enum FONT_WEIGHT
M.FONT_WEIGHT = {
	NORMAL = "normal",
	MEDIUM = "medium",
	BOLD = "bold",
}

--- Blend modes supported by Compose DrawScope.
--- @since (API level 5)
--- @enum BLEND_MODE
M.BLEND_MODE = {
	CLEAR = "clear", SRC = "src", DST = "dst", SRC_OVER = "src_over", DST_OVER = "dst_over",
	SRC_IN = "src_in", DST_IN = "dst_in", SRC_OUT = "src_out", DST_OUT = "dst_out",
	SRC_ATOP = "src_atop", DST_ATOP = "dst_atop", XOR = "xor", PLUS = "plus",
	MODULATE = "modulate", SCREEN = "screen", OVERLAY = "overlay", DARKEN = "darken",
	LIGHTEN = "lighten", COLOR_DODGE = "color_dodge", COLOR_BURN = "color_burn",
	HARD_LIGHT = "hard_light", SOFT_LIGHT = "soft_light", DIFFERENCE = "difference",
	EXCLUSION = "exclusion", MULTIPLY = "multiply", HUE = "hue",
	SATURATION = "saturation", COLOR = "color", LUMINOSITY = "luminosity",
}

--- Gradient tiling behavior.
--- @since (API level 5)
--- @enum TILE_MODE
M.TILE_MODE = { CLAMP = "clamp", REPEATED = "repeated", MIRROR = "mirror", DECAL = "decal" }

--- Direction used when adding closed geometry to a path.
--- @since (API level 5)
--- @enum PATH_DIRECTION
M.PATH_DIRECTION = { CLOCKWISE = "clockwise", COUNTER_CLOCKWISE = "counter_clockwise" }

--- How a stamped path effect transforms its shape along the path.
--- @since (API level 5)
--- @enum STAMPED_PATH_EFFECT_STYLE
M.STAMPED_PATH_EFFECT_STYLE = { TRANSLATE = "translate", ROTATE = "rotate", MORPH = "morph" }

--- @since (API level 5)
--- @class CanvasPoint
--- @field x number Horizontal coordinate, increasing from left to right.
--- @field y number Vertical coordinate, increasing from top to bottom.
M.CanvasPoint = {}

--- @since (API level 5)
--- @class CanvasSize
--- @field width number
--- @field height number
M.CanvasSize = {}

--- Parameter tables accepted by CanvasDrawScope drawing methods.
--- @since (API level 5)
M.DrawLineParams = {}
--- @since (API level 5)
M.DrawCircleParams = {}
--- @since (API level 5)
M.DrawRectParams = {}
--- @since (API level 5)
M.DrawRoundRectParams = {}
--- @since (API level 5)
M.DrawOvalParams = {}
--- @since (API level 5)
M.DrawArcParams = {}
--- @since (API level 5)
M.DrawPathParams = {}
--- @since (API level 5)
M.DrawPointsParams = {}
--- @since (API level 5)
M.DrawTextParams = {}

local function copy_point(point)
	return { x = point.x, y = point.y }
end

local function copy_size(size)
	return { width = size.width, height = size.height }
end

local function copy_points(points)
	local result = {}
	for index, point in ipairs(points) do
		result[index] = copy_point(point)
	end
	return result
end

local function deep_copy(value)
	if type(value) ~= "table" then return value end
	local result = {}
	for key, child in pairs(value) do result[key] = deep_copy(child) end
	return result
end

--- Standard Compose gradient brush descriptions.
--- @since (API level 5)
M.Brush = {}

M.Brush.linearGradient = function(params)
	local result = deep_copy(params)
	result.type = "LINEAR_GRADIENT"
	return result
end

M.Brush.horizontalGradient = function(params)
	local result = deep_copy(params)
	result.type = "HORIZONTAL_GRADIENT"
	return result
end

M.Brush.verticalGradient = function(params)
	local result = deep_copy(params)
	result.type = "VERTICAL_GRADIENT"
	return result
end

M.Brush.radialGradient = function(params)
	local result = deep_copy(params)
	result.type = "RADIAL_GRADIENT"
	return result
end

M.Brush.sweepGradient = function(params)
	local result = deep_copy(params)
	result.type = "SWEEP_GRADIENT"
	return result
end

--- Compose color filter descriptions.
--- @since (API level 5)
M.ColorFilter = {}

M.ColorFilter.tint = function(params)
	local result = deep_copy(params)
	result.type = "TINT"
	return result
end

M.ColorFilter.colorMatrix = function(values)
	return { type = "COLOR_MATRIX", values = deep_copy(values) }
end

M.ColorFilter.lighting = function(params)
	local result = deep_copy(params)
	result.type = "LIGHTING"
	return result
end

--- Compose path effect descriptions.
--- @since (API level 5)
M.PathEffect = {}

M.PathEffect.corner = function(radius)
	return { type = "CORNER", radius = radius }
end

M.PathEffect.dash = function(params)
	return { type = "DASH", intervals = deep_copy(params.intervals), phase = params.phase or 0 }
end

M.PathEffect.chain = function(params)
	return { type = "CHAIN", outer = deep_copy(params.outer), inner = deep_copy(params.inner) }
end

M.PathEffect.stamped = function(params)
	return {
		type = "STAMPED",
		commands = deep_copy(params.path.commands),
		advance = params.advance,
		phase = params.phase or 0,
		style = params.style or M.STAMPED_PATH_EFFECT_STYLE.TRANSLATE,
	}
end

--- Operations accepted by CanvasDrawScope.withTransform.
--- @since (API level 5)
M.Transform = {}

M.Transform.translate = function(params)
	return { type = "TRANSLATE", left = params.left or 0, top = params.top or 0 }
end

M.Transform.rotate = function(params)
	local result = deep_copy(params)
	result.type = "ROTATE"
	return result
end

M.Transform.rotateRad = function(params)
	local result = deep_copy(params)
	result.type = "ROTATE_RAD"
	return result
end

M.Transform.scale = function(params)
	local result = deep_copy(params)
	result.type = "SCALE"
	return result
end

M.Transform.inset = function(params)
	local inset = params.inset or 0
	return {
		type = "INSET",
		left = params.left or params.horizontal or inset,
		top = params.top or params.vertical or inset,
		right = params.right or params.horizontal or inset,
		bottom = params.bottom or params.vertical or inset,
	}
end

M.Transform.matrix = function(matrix)
	return { type = "MATRIX", matrix = deep_copy(matrix) }
end

M.Transform.clipRect = function(params)
	local result = deep_copy(params)
	result.type = "CLIP_RECT"
	return result
end

M.Transform.clipPath = function(params)
	return {
		type = "CLIP_PATH",
		commands = deep_copy(params.path.commands),
		clip_op = params.clip_op or M.CLIP_OP.INTERSECT,
	}
end

local function default_top_left(params)
	return copy_point(params.top_left or { x = 0, y = 0 })
end

local function default_size(scope, params, top_left)
	return copy_size(params.size or {
		width = scope.size.width - top_left.x,
		height = scope.size.height - top_left.y,
	})
end

local function add_style(target, params)
	target.alpha = params.alpha or 1
	target.style = params.style or M.DRAW_STYLE.FILL
	target.stroke_width = params.stroke_width or 0
	target.cap = params.cap or M.STROKE_CAP.BUTT
	target.join = params.join or M.STROKE_JOIN.MITER
	target.miter = params.miter or 4
	target.path_effect = deep_copy(params.path_effect)
	target.blend_mode = params.blend_mode or M.BLEND_MODE.SRC_OVER
	target.color_filter = deep_copy(params.color_filter)
	return target
end

local function add_paint(target, params)
	target.color = params.color
	target.brush = deep_copy(params.brush)
	return target
end

local function copy_path(path)
	assert(type(path) == "table" and type(path.commands) == "table", "expected a canvas path")
	local commands = {}
	for index, command in ipairs(path.commands) do
		local copy = {}
		for key, value in pairs(command) do copy[key] = value end
		commands[index] = copy
	end
	return commands
end

local CanvasPath = {}
CanvasPath.__index = CanvasPath

local function path_command(self, command)
	table.insert(self.commands, command)
	return self
end

--- Moves the current path position.
--- @return CanvasPath
CanvasPath.moveTo = function(self, x, y)
	return path_command(self, { type = "MOVE_TO", x = x, y = y })
end

--- Moves the current path position relative to its current position.
--- @return CanvasPath
CanvasPath.relativeMoveTo = function(self, dx, dy)
	return path_command(self, { type = "RELATIVE_MOVE_TO", x = dx, y = dy })
end

--- Adds a straight line.
--- @return CanvasPath
CanvasPath.lineTo = function(self, x, y)
	return path_command(self, { type = "LINE_TO", x = x, y = y })
end

--- Adds a relative straight line.
--- @return CanvasPath
CanvasPath.relativeLineTo = function(self, dx, dy)
	return path_command(self, { type = "RELATIVE_LINE_TO", x = dx, y = dy })
end

--- Adds a quadratic Bézier curve.
--- @return CanvasPath
CanvasPath.quadraticTo = function(self, x1, y1, x2, y2)
	return path_command(self, { type = "QUADRATIC_TO", x1 = x1, y1 = y1, x2 = x2, y2 = y2 })
end

--- Adds a relative quadratic Bézier curve.
--- @return CanvasPath
CanvasPath.relativeQuadraticTo = function(self, x1, y1, x2, y2)
	return path_command(self, { type = "RELATIVE_QUADRATIC_TO", x1 = x1, y1 = y1, x2 = x2, y2 = y2 })
end

--- Adds a cubic Bézier curve.
--- @return CanvasPath
CanvasPath.cubicTo = function(self, x1, y1, x2, y2, x3, y3)
	return path_command(self, {
		type = "CUBIC_TO", x1 = x1, y1 = y1, x2 = x2, y2 = y2, x3 = x3, y3 = y3,
	})
end

--- Adds a relative cubic Bézier curve.
--- @return CanvasPath
CanvasPath.relativeCubicTo = function(self, x1, y1, x2, y2, x3, y3)
	return path_command(self, {
		type = "RELATIVE_CUBIC_TO", x1 = x1, y1 = y1, x2 = x2, y2 = y2, x3 = x3, y3 = y3,
	})
end

--- Adds an elliptical arc to the path.
--- @return CanvasPath
CanvasPath.arcTo = function(self, params)
	return path_command(self, {
		type = "ARC_TO",
		top_left = copy_point(params.top_left or { x = 0, y = 0 }),
		size = copy_size(params.size),
		start_angle = params.start_angle,
		sweep_angle = params.sweep_angle,
		force_move_to = params.force_move_to == true,
	})
end

--- Adds an elliptical arc whose angles are expressed in radians.
--- @return CanvasPath
CanvasPath.arcToRad = function(self, params)
	local command = deep_copy(params)
	command.type = "ARC_TO_RAD"
	command.top_left = copy_point(params.top_left or { x = 0, y = 0 })
	command.size = copy_size(params.size)
	command.force_move_to = params.force_move_to == true
	return path_command(self, command)
end

--- Adds a rectangle contour.
--- @return CanvasPath
CanvasPath.addRect = function(self, params)
	return path_command(self, {
		type = "ADD_RECT",
		top_left = copy_point(params.top_left or { x = 0, y = 0 }),
		size = copy_size(params.size),
		direction = params.direction or M.PATH_DIRECTION.COUNTER_CLOCKWISE,
	})
end

--- Adds an oval contour.
--- @return CanvasPath
CanvasPath.addOval = function(self, params)
	local command = deep_copy(params)
	command.type = "ADD_OVAL"
	command.top_left = copy_point(params.top_left or { x = 0, y = 0 })
	command.size = copy_size(params.size)
	command.direction = params.direction or M.PATH_DIRECTION.COUNTER_CLOCKWISE
	return path_command(self, command)
end

--- Adds a rounded rectangle contour.
--- @return CanvasPath
CanvasPath.addRoundRect = function(self, params)
	local radius = params.corner_radius or {}
	return path_command(self, {
		type = "ADD_ROUND_RECT",
		top_left = copy_point(params.top_left or { x = 0, y = 0 }),
		size = copy_size(params.size),
		corner_radius = { x = radius.x or 0, y = radius.y or radius.x or 0 },
		direction = params.direction or M.PATH_DIRECTION.COUNTER_CLOCKWISE,
	})
end

--- Adds an arc as a new path contour.
--- @return CanvasPath
CanvasPath.addArc = function(self, params)
	return path_command(self, {
		type = "ADD_ARC",
		top_left = copy_point(params.top_left or { x = 0, y = 0 }),
		size = copy_size(params.size),
		start_angle = params.start_angle,
		sweep_angle = params.sweep_angle,
	})
end

--- Adds an arc contour whose angles are expressed in radians.
--- @return CanvasPath
CanvasPath.addArcRad = function(self, params)
	local command = deep_copy(params)
	command.type = "ADD_ARC_RAD"
	command.top_left = copy_point(params.top_left or { x = 0, y = 0 })
	command.size = copy_size(params.size)
	return path_command(self, command)
end

--- Appends another path with an optional offset.
--- @return CanvasPath
CanvasPath.addPath = function(self, path, offset)
	return path_command(self, {
		type = "ADD_PATH",
		commands = copy_path(path),
		offset = copy_point(offset or { x = 0, y = 0 }),
	})
end

--- Translates the path's existing geometry.
--- @return CanvasPath
CanvasPath.translate = function(self, offset)
	return path_command(self, { type = "TRANSLATE", offset = copy_point(offset) })
end

--- Applies a 4x4 matrix to the path's existing geometry.
--- @return CanvasPath
CanvasPath.transform = function(self, matrix)
	return path_command(self, { type = "TRANSFORM", matrix = deep_copy(matrix) })
end

--- Closes the current contour.
--- @return CanvasPath
CanvasPath.close = function(self)
	return path_command(self, { type = "CLOSE" })
end

--- Removes all commands from the path.
--- @return CanvasPath
CanvasPath.reset = function(self)
	self.commands = {}
	return self
end

CanvasPath.rewind = CanvasPath.reset

--- Creates a mutable path description for drawPath and clipPath.
--- @since (API level 5)
--- @return CanvasPath
M.path = function()
	return setmetatable({ commands = {} }, CanvasPath)
end

local CanvasDrawScope = {}
CanvasDrawScope.__index = CanvasDrawScope

--- Converts density-independent pixels to physical canvas pixels.
CanvasDrawScope.dp = function(self, value)
	return value * self.density
end

--- Converts scale-independent pixels to physical canvas pixels.
--- The supplied conversion table preserves Compose's non-linear Android font scaling.
CanvasDrawScope.sp = function(self, value)
	local sign = value < 0 and -1 or 1
	local absolute = math.abs(value)
	local scale = self.sp_to_px
	if scale == nil or #scale == 0 then
		return value * self.density * self.font_scale
	end

	local previous_sp = 0
	local previous_px = 0
	for _, point in ipairs(scale) do
		if absolute <= point.sp then
			if absolute == point.sp then return sign * point.px end
			local fraction = (absolute - previous_sp) / (point.sp - previous_sp)
			return sign * (previous_px + fraction * (point.px - previous_px))
		end
		previous_sp = point.sp
		previous_px = point.px
	end

	return value * (previous_px / previous_sp)
end

--- Records the equivalent of Compose DrawScope.drawLine.
CanvasDrawScope.drawLine = function(self, params)
	table.insert(self.instructions, add_paint({
		type = M.INSTRUCTION_TYPE.LINE,
		start = copy_point(params.start),
		finish = copy_point(params.finish),
		stroke_width = params.stroke_width or 0,
		cap = params.cap or M.STROKE_CAP.BUTT,
		alpha = params.alpha or 1,
		path_effect = deep_copy(params.path_effect),
		blend_mode = params.blend_mode or M.BLEND_MODE.SRC_OVER,
		color_filter = deep_copy(params.color_filter),
	}, params))
end

--- Records the equivalent of Compose DrawScope.drawRect.
CanvasDrawScope.drawRect = function(self, params)
	local top_left = default_top_left(params)
	table.insert(self.instructions, add_style(add_paint({
		type = M.INSTRUCTION_TYPE.RECT,
		top_left = top_left,
		size = default_size(self, params, top_left),
	}, params), params))
end

--- Records the equivalent of Compose DrawScope.drawRoundRect.
CanvasDrawScope.drawRoundRect = function(self, params)
	local top_left = default_top_left(params)
	local radius = params.corner_radius or {}
	table.insert(self.instructions, add_style(add_paint({
		type = M.INSTRUCTION_TYPE.ROUND_RECT,
		top_left = top_left,
		size = default_size(self, params, top_left),
		corner_radius = { x = radius.x or 0, y = radius.y or radius.x or 0 },
	}, params), params))
end

--- Records the equivalent of Compose DrawScope.drawCircle.
CanvasDrawScope.drawCircle = function(self, params)
	table.insert(self.instructions, add_style(add_paint({
		type = M.INSTRUCTION_TYPE.CIRCLE,
		radius = params.radius or math.min(self.size.width, self.size.height) / 2,
		center = copy_point(params.center or self.center),
	}, params), params))
end

--- Records the equivalent of Compose DrawScope.drawOval.
CanvasDrawScope.drawOval = function(self, params)
	local top_left = default_top_left(params)
	table.insert(self.instructions, add_style(add_paint({
		type = M.INSTRUCTION_TYPE.OVAL,
		top_left = top_left,
		size = default_size(self, params, top_left),
	}, params), params))
end

--- Records the equivalent of Compose DrawScope.drawArc.
CanvasDrawScope.drawArc = function(self, params)
	local top_left = default_top_left(params)
	table.insert(self.instructions, add_style(add_paint({
		type = M.INSTRUCTION_TYPE.ARC,
		start_angle = params.start_angle,
		sweep_angle = params.sweep_angle,
		use_center = params.use_center == true,
		top_left = top_left,
		size = default_size(self, params, top_left),
	}, params), params))
end

--- Records the equivalent of Compose DrawScope.drawPath.
CanvasDrawScope.drawPath = function(self, params)
	table.insert(self.instructions, add_style(add_paint({
		type = M.INSTRUCTION_TYPE.PATH,
		commands = copy_path(params.path),
	}, params), params))
end

--- Records the equivalent of Compose DrawScope.drawPoints.
CanvasDrawScope.drawPoints = function(self, params)
	table.insert(self.instructions, add_paint({
		type = M.INSTRUCTION_TYPE.POINTS,
		points = copy_points(params.points),
		mode = params.mode or M.POINT_MODE.POINTS,
		stroke_width = params.stroke_width or 0,
		cap = params.cap or M.STROKE_CAP.BUTT,
		alpha = params.alpha or 1,
		path_effect = deep_copy(params.path_effect),
		blend_mode = params.blend_mode or M.BLEND_MODE.SRC_OVER,
		color_filter = deep_copy(params.color_filter),
	}, params))
end

--- Records Compose's DrawScope.drawText extension.
--- `font_size` is a physical-pixel value like other canvas dimensions; use `scope:sp(...)`
--- to obtain a scaled font size.
CanvasDrawScope.drawText = function(self, params)
	table.insert(self.instructions, add_paint({
		type = M.INSTRUCTION_TYPE.TEXT,
		text = tostring(params.text or ""),
		top_left = default_top_left(params),
		size = params.size and copy_size(params.size) or nil,
		font_size = params.font_size,
		font_weight = params.font_weight or M.FONT_WEIGHT.NORMAL,
		text_align = params.text_align or M.TEXT_ALIGN.START,
		vertical_align = params.vertical_align or M.TEXT_VERTICAL_ALIGN.TOP,
		overflow = params.overflow or M.TEXT_OVERFLOW.CLIP,
		max_lines = params.max_lines or 2147483647,
		soft_wrap = params.soft_wrap ~= false,
		alpha = params.alpha or 1,
		blend_mode = params.blend_mode or M.BLEND_MODE.SRC_OVER,
	}, params))
end

local function child_scope(self, instructions, size)
	local child_size = size or copy_size(self.size)
	return setmetatable({
		instructions = instructions,
		size = child_size,
		center = { x = child_size.width / 2, y = child_size.height / 2 },
		density = self.density,
		font_scale = self.font_scale,
		sp_to_px = self.sp_to_px,
	}, CanvasDrawScope)
end

local function record_group(self, operation, size, draw)
	assert(type(draw) == "function", "canvas transform requires a draw callback")
	local group = { type = M.INSTRUCTION_TYPE.GROUP, operation = operation, instructions = {} }
	table.insert(self.instructions, group)
	draw(child_scope(self, group.instructions, size))
end

--- Draws within a translated coordinate space.
CanvasDrawScope.translate = function(self, params, draw)
	record_group(self, { type = "TRANSLATE", left = params.left or 0, top = params.top or 0 }, nil, draw)
end

--- Draws within a clockwise-rotated coordinate space.
CanvasDrawScope.rotate = function(self, params, draw)
	record_group(self, {
		type = "ROTATE",
		degrees = params.degrees,
		pivot = copy_point(params.pivot or self.center),
	}, nil, draw)
end

--- Draws within a clockwise-rotated coordinate space using radians.
CanvasDrawScope.rotateRad = function(self, params, draw)
	record_group(self, {
		type = "ROTATE_RAD",
		radians = params.radians,
		pivot = copy_point(params.pivot or self.center),
	}, nil, draw)
end

--- Draws within a scaled coordinate space.
CanvasDrawScope.scale = function(self, params, draw)
	record_group(self, {
		type = "SCALE",
		x = params.x or params.scale or 1,
		y = params.y or params.scale or 1,
		pivot = copy_point(params.pivot or self.center),
	}, nil, draw)
end

--- Draws after applying an arbitrary Compose 4x4 matrix.
CanvasDrawScope.transform = function(self, matrix, draw)
	record_group(self, { type = "MATRIX", matrix = deep_copy(matrix) }, nil, draw)
end

--- Applies several transforms with one save/restore around the nested drawing.
CanvasDrawScope.withTransform = function(self, operations, draw)
	local size = copy_size(self.size)
	for _, operation in ipairs(operations) do
		if operation.type == "INSET" then
			size.width = size.width - (operation.left or 0) - (operation.right or 0)
			size.height = size.height - (operation.top or 0) - (operation.bottom or 0)
		end
	end
	record_group(self, { type = "WITH_TRANSFORM", operations = deep_copy(operations) }, size, draw)
end

--- Insets both the coordinate origin and the available size for nested drawing.
CanvasDrawScope.inset = function(self, params, draw)
	local left = params.left or params.horizontal or params.inset or 0
	local top = params.top or params.vertical or params.inset or 0
	local right = params.right or params.horizontal or params.inset or 0
	local bottom = params.bottom or params.vertical or params.inset or 0
	record_group(self, {
		type = "INSET", left = left, top = top, right = right, bottom = bottom,
	}, {
		width = self.size.width - left - right,
		height = self.size.height - top - bottom,
	}, draw)
end

--- Clips nested drawing to a rectangle.
CanvasDrawScope.clipRect = function(self, params, draw)
	record_group(self, {
		type = "CLIP_RECT",
		left = params.left or 0,
		top = params.top or 0,
		right = params.right or self.size.width,
		bottom = params.bottom or self.size.height,
		clip_op = params.clip_op or M.CLIP_OP.INTERSECT,
	}, nil, draw)
end

--- Clips nested drawing to a path.
CanvasDrawScope.clipPath = function(self, params, draw)
	record_group(self, {
		type = "CLIP_PATH",
		commands = copy_path(params.path),
		clip_op = params.clip_op or M.CLIP_OP.INTERSECT,
	}, nil, draw)
end

--- Creates a deferred canvas renderer. The returned function records draw calls once bounds are known.
--- @since (API level 5)
--- @param draw fun(draw_scope): A callback that issues draw calls on the supplied scope.
--- @return fun(context: table): table A function returning recorded draw instructions for a canvas context.
M.create = function(draw)
	assert(type(draw) == "function", "canvas draw callback must be a function")

	return function(context)
		assert(type(context) == "table", "canvas context must be a table")
		assert(type(context.bounds) == "table", "canvas context must contain bounds")

		local result = { type = M.TYPE, instructions = {} }
		local size = { width = context.bounds.width, height = context.bounds.height }
		local draw_scope = setmetatable({
			instructions = result.instructions,
			size = size,
			center = { x = size.width / 2, y = size.height / 2 },
			density = context.density or 1,
			font_scale = context.font_scale or 1,
			sp_to_px = context.sp_to_px,
		}, CanvasDrawScope)
		draw(draw_scope)
		return result
	end
end

return M
