local canvas = require("tng.canvas")
local theme = require("tng.theme")
local ui = require("tng.ui")

local padding = 70
local canvas_height = 360

local function value_range(values)
	local minimum = math.min(0, values[1])
	local maximum = math.max(0, values[1])
	for i = 2, #values do
		minimum = math.min(minimum, values[i])
		maximum = math.max(maximum, values[i])
	end
	if minimum == maximum then
		maximum = minimum + 1
	end
	return minimum, maximum
end

local function summary(values)
	if #values == 0 then
		return { ui.text({ text = "No data" }) }
	end
	local minimum, maximum = value_range(values)
	return {
		ui.text({ text = "Minimum: " .. minimum }),
		ui.text({ text = "Maximum: " .. maximum }),
	}
end

return function(context, state, data)
	local values = data.values
	return {
		ui = ui.surface({
		key = "graph_surface",
		color = theme.color.surface,
		content_color = theme.color.on_surface,
		shape = theme.shape.medium,
		shadow_elevation = ui.dp(4),
		modifier = ui.modifier(ui.fill_width(), ui.padding(theme.spacing.x_small)),
		child = ui.column({
			key = "graph_content",
			spacing = theme.spacing.large,
			children = {
				ui.text({
					key = "graph_title",
					text = "Basic graph",
					color = theme.color.primary,
					font_size = ui.sp(24),
					font_weight = ui.FONT_WEIGHT.BOLD,
				}),
				ui.row({
					key = "graph_summary",
					spacing = theme.spacing.medium,
					arrangement = ui.ARRANGEMENT.SPACE_BETWEEN,
					alignment = ui.ALIGNMENT.CENTER,
					children = summary(values),
				}),
				ui.canvas({
					key = "graph_canvas",
					modifier = ui.modifier(
						ui.fill_width(),
						ui.height(ui.dp(canvas_height)),
						ui.clip(theme.shape.medium),
						ui.background(theme.color.surface_variant)
					),
					draw = function(draw_scope)
						local width = draw_scope.size.width
						local height = draw_scope.size.height
						local left, right = padding, width - padding
						local top, bottom = padding, height - padding

						draw_scope:drawLine({
							color = theme.color.outline,
							start = { x = left, y = top },
							finish = { x = left, y = bottom },
							stroke_width = 3,
						})
						draw_scope:drawLine({
							color = theme.color.outline,
							start = { x = left, y = bottom },
							finish = { x = right, y = bottom },
							stroke_width = 3,
						})
						if #values == 0 then
							return
						end

						local minimum, maximum = value_range(values)
						local points = {}
						for i, value in ipairs(values) do
							local x_fraction = #values == 1 and 0.5 or (i - 1) / (#values - 1)
							local y_fraction = (value - minimum) / (maximum - minimum)
							points[i] = {
								x = left + x_fraction * (right - left),
								y = bottom - y_fraction * (bottom - top),
							}
						end
						for i = 2, #points do
							draw_scope:drawLine({
								color = theme.color.primary,
								start = points[i - 1],
								finish = points[i],
								stroke_width = 6,
								cap = canvas.STROKE_CAP.ROUND,
							})
						end
						for _, point in ipairs(points) do
							draw_scope:drawCircle({
								color = theme.color.primary,
								center = point,
								radius = 11,
							})
						end
					end,
				}),
			},
		}),
		}),
		state = state,
	}
end
