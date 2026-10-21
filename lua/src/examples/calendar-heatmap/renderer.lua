local canvas = require("tng.canvas")
local graphui = require("tng.graphui")
local theme = require("tng.theme")
local ui = require("tng.ui")

local days_per_week = 7
local wide_layout_min_width_dp = 600
local narrow_row_height_dp = 28
local narrow_marker_radius_dp = 9

local function feature_positions(source_count)
	if source_count <= 1 then return 1, 1 end
	local columns = math.ceil(math.sqrt(source_count))
	return columns, math.ceil(source_count / columns)
end

local function draw_label(scope, text, color, top_left, size, text_align, font_size_sp)
	scope:drawText({
		text = text,
		color = color,
		top_left = top_left,
		size = size,
		font_size = scope:sp(font_size_sp or 12),
		font_weight = canvas.FONT_WEIGHT.MEDIUM,
		text_align = text_align or canvas.TEXT_ALIGN.CENTER,
		vertical_align = canvas.TEXT_VERTICAL_ALIGN.CENTER,
		max_lines = 1,
		soft_wrap = false,
	})
end

local function draw_day(scope, data, day, index, center, radius)
	scope:drawCircle({
		color = theme.color.surface_variant,
		center = center,
		radius = radius,
		alpha = day.future and 0.2 or 0.65,
	})

	if not day.future then
		local source_count = #data.sources
		local feature_columns, feature_rows = feature_positions(source_count)
		local feature_span = math.max(feature_columns, feature_rows)
		for source_index, source in ipairs(data.sources) do
			local intensity = source.values[index]
			if intensity ~= nil and intensity > 0 then
				local feature_center = center
				local feature_radius = radius * (0.45 + 0.55 * intensity)
				if source_count > 1 then
					local feature_column = (source_index - 1) % feature_columns
					local feature_row = math.floor((source_index - 1) / feature_columns)
					local step = radius * 1.5 / feature_span
					feature_center = {
						x = center.x + (feature_column - (feature_columns - 1) / 2) * step,
						y = center.y + (feature_row - (feature_rows - 1) / 2) * step,
					}
					feature_radius = radius * 0.72 / feature_span * (0.55 + 0.45 * intensity)
				end
				scope:drawCircle({
					color = source.color,
					center = feature_center,
					radius = feature_radius,
					alpha = 0.4 + 0.6 * intensity,
				})
			end
		end
	end

	scope:drawCircle({
		color = theme.color.on_surface_variant,
		center = center,
		radius = radius,
		style = canvas.DRAW_STYLE.STROKE,
		stroke_width = math.max(scope:dp(1), radius * 0.08),
		alpha = day.future and 0.25 or 0.55,
	})

	if index == data.today_index then
		scope:drawCircle({
			color = theme.color.primary,
			center = center,
			radius = radius,
			style = canvas.DRAW_STYLE.STROKE,
			stroke_width = math.max(scope:dp(1), radius * 0.14),
		})
	end
end

local function calendar(data, wide_layout)
	local modifier
	if wide_layout then
		modifier = ui.modifier(
			ui.fill_width(),
			ui.aspect_ratio((data.weeks + 1.5) / (days_per_week + 1))
		)
	else
		modifier = ui.modifier(
			ui.fill_width(),
			ui.height(ui.dp(24 + data.weeks * narrow_row_height_dp))
		)
	end
	return ui.canvas({
		key = "calendar",
		modifier = modifier,
		draw = function(scope)
			local label_width = scope:dp(wide_layout and 24 or 40)
			local label_gap = scope:dp(4)
			local header_height = scope:dp(wide_layout and 20 or 24)
			local grid_width = scope.size.width - label_width - label_gap
			local grid_height = scope.size.height - header_height
			local columns = wide_layout and data.weeks or days_per_week
			local rows = wide_layout and days_per_week or data.weeks
			local cell_width = grid_width / columns
			local cell_height = grid_height / rows
			local grid_left = label_width + label_gap
			local grid_top = header_height
			if wide_layout then
				local cell_size = math.min(cell_width, cell_height)
				cell_width = cell_size
				cell_height = cell_size
				grid_left = grid_left + (grid_width - cell_size * columns) / 2
				grid_top = grid_top + (grid_height - cell_size * rows) / 2
			end
			local radius = wide_layout and cell_width * 0.35 or scope:dp(narrow_marker_radius_dp)

			if wide_layout then
				for week = 1, data.weeks do
					draw_label(
						scope,
						data.month_labels[week] or "",
						theme.color.secondary_text,
						{ x = grid_left + (week - 1) * cell_width, y = 0 },
						{ width = cell_width * 4, height = header_height },
						canvas.TEXT_ALIGN.START
					)
				end
				for weekday = 1, days_per_week do
					draw_label(
						scope,
						data.weekday_labels[weekday] or "",
						theme.color.secondary_text,
						{ x = 0, y = grid_top + (weekday - 1) * cell_height },
						{ width = label_width, height = cell_height }
					)
				end
			else
				for weekday = 1, days_per_week do
					draw_label(
						scope,
						data.weekday_labels[weekday] or "",
						theme.color.secondary_text,
						{ x = grid_left + (weekday - 1) * cell_width, y = 0 },
						{ width = cell_width, height = header_height },
						nil,
						14
					)
				end
				for week = 1, data.weeks do
					draw_label(
						scope,
						data.month_labels[week] or "",
						theme.color.secondary_text,
						{ x = 0, y = grid_top + (week - 1) * cell_height },
						{ width = label_width, height = cell_height },
						nil,
						14
					)
				end
			end

			for weekday = 1, days_per_week do
				for week = 1, data.weeks do
					local index = (week - 1) * days_per_week + weekday
					local day = data.days[index]
					local column = wide_layout and week or weekday
					local row = wide_layout and weekday or week
					draw_day(scope, data, day, index, {
						x = grid_left + (column - 0.5) * cell_width,
						y = grid_top + (row - 0.5) * cell_height,
					}, radius)
				end
			end
		end,
	})
end

local function legend(data)
	local items = {}
	for index, source in ipairs(data.sources) do
		items[index] = {
			key = "source_" .. index,
			color = source.color,
			label = source.name,
		}
	end
	return graphui.legend({ key = "legend", items = items })
end

return function(context, state, data)
	local available_width_dp = context.constraints.max_width / context.density
	local wide_layout = available_width_dp >= wide_layout_min_width_dp
	return ui.column({
		key = "calendar_heatmap",
		spacing = theme.spacing.small,
		modifier = ui.modifier(ui.fill_width()),
		children = {
			calendar(data, wide_layout),
			legend(data),
		},
	})
end
