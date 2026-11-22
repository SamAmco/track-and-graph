local canvas = require("tng.canvas")
local graphui = require("tng.graphui")
local theme = require("tng.theme")
local ui = require("tng.ui")
local layout = require("calendar_layout")
local month_layout = require("month_layout")

local function label(scope, text, x, y, width, height, align)
	scope:drawText({
		text = text, color = theme.color.secondary_text,
		top_left = { x = x, y = y }, size = { width = width, height = height },
		font_size = scope:sp(12), text_align = align or canvas.TEXT_ALIGN.START,
		vertical_align = canvas.TEXT_VERTICAL_ALIGN.CENTER, max_lines = 1, soft_wrap = false,
	})
end

local function draw_day(scope, data, index, x, y, tile)
	if data.days[index].outside_year then return end
	local blank = theme.color.data_vis_blank
	local position = { x = x, y = y }
	local size = { width = tile, height = tile }
	local function fill(color, alpha)
		scope:drawRect({ color = color, alpha = alpha, top_left = position, size = size })
	end
	fill(blank, 1)
	if data.days[index].future then return end
	local count = 0
	for _, source in ipairs(data.sources) do
		local alpha = source.values[data.days[index].key] or 0
		if alpha > 0 then
			count = count + 1
			-- Running arithmetic mean of each active source's final colour.
			if alpha < 1 then fill(blank, (1 - alpha) / (count - alpha)) end
			fill(source.color, alpha / count)
		end
	end
end

-- The list card remains a compact strip of the most recent weeks.
local function draw_calendar(scope, data, font_scale)
	local geometry = layout(scope.size.width, scope.density, font_scale)
	if geometry == nil then return end
	local calendar = data.calendar(geometry.weeks)
	calendar.sources = data.sources
	local top = math.floor(math.max(0, scope.size.height - geometry.height) / 2)
	local grid_width = geometry.weeks * geometry.step - 1
	local group_left = math.floor((scope.size.width - grid_width - geometry.label_gap - geometry.label_width) / 2)
	local left = group_left + geometry.label_gap + geometry.label_width
	local right = left + grid_width
	local headings = {}
	for week = 1, geometry.weeks do
		local day = calendar.days[(week - 1) * 7 + 1]
		local month, year = calendar.month_labels[week], calendar.year_labels[week]
		if week == 1 and month == nil then
			month = day.month
			if day.month_number == 1 then year = tostring(day.year) end
		end
		local x = left + (week - 1) * geometry.step
		if month then
			local minimum = scope:dp((year and 32 or 26) * font_scale)
			headings[#headings + 1] = { month = month, year = year,
				x = math.max(left, math.min(x, right - minimum)), minimum = minimum }
		end
		for weekday = 1, 7 do
			draw_day(scope, calendar, (week - 1) * 7 + weekday, x,
				top + geometry.header + (weekday - 1) * geometry.step, geometry.tile)
		end
	end
	-- Give month boundaries priority over an initial partial-month heading.
	local label_right = math.min(scope.size.width, math.max(right, left + scope:dp(40 * font_scale)))
	for index = #headings, 1, -1 do
		local heading = headings[index]
		if heading.x + heading.minimum <= label_right then
			local width = math.min(scope:dp(40 * font_scale), label_right - heading.x)
			if heading.year then label(scope, heading.year, heading.x, top, width, geometry.header / 2) end
			label(scope, heading.month, heading.x, top + geometry.header / 2, width, geometry.header / 2)
			label_right = heading.x
		end
	end
	for weekday = 1, 7 do
		label(scope, calendar.weekday_labels[weekday], group_left,
			top + geometry.header + (weekday - 1) * geometry.step, geometry.label_width, geometry.tile)
	end
end

local function draw_month_row(scope, row, geometry, sources)
	local left = math.floor(math.max(0, scope.size.width - row.width) / 2)
	for _, entry in ipairs(row.months) do
		local month = entry.month
		local x = left + entry.x
		label(scope, month.title, x, 0, scope:dp(52), geometry.header)
		local calendar = { days = month.days, sources = sources }
		for index, day in ipairs(month.days) do
			draw_day(scope, calendar, index, x + (day.week - 1) * geometry.step,
				geometry.header + (day.weekday - 1) * geometry.step, geometry.tile)
		end
	end
	if row.gutter > 0 then
		for weekday = 1, 7 do
			label(scope, row.months[1].month.weekday_labels[weekday], left,
				geometry.header + (weekday - 1) * geometry.step,
				row.gutter - scope:dp(6), geometry.tile)
		end
	end
end

return function(params)
	local data, context, state = params.data, params.context, params.state
	local density, font_scale = context.density or 1, context.font_scale or 1
	local full_screen = params.view_mode == "FULL_SCREEN"
	local column_modifier = ui.modifier(ui.fill_width())
	local children = {}
	if full_screen then
		table.insert(column_modifier, ui.fill_height())
		table.insert(column_modifier, ui.padding({ horizontal = ui.dp(16) }))
		local width = math.max(0, (context.constraints and context.constraints.max_width or 320 * density)
			- 2 * math.floor(16 * density + 0.5))
		local sections = {}
		for year_index, section in ipairs(data.years) do
			local section_children = {}
			if #data.years > 1 then
				section_children[#section_children + 1] = ui.text({
					key = "year_heading_" .. section.year, text = tostring(section.year),
					style = theme.text_style.title_medium, font_weight = ui.FONT_WEIGHT.BOLD,
				})
			end
			local geometry = month_layout(section.months, width, density, font_scale, year_index == 1)
			local rows = {}
			for index, row in ipairs(geometry.rows) do
				rows[#rows + 1] = ui.canvas({
					key = "month_row_" .. section.year .. "_" .. index,
					modifier = ui.modifier(ui.fill_width(), ui.height(ui.px(geometry.height))),
					draw = function(scope) draw_month_row(scope, row, geometry, data.sources) end,
				})
			end
			section_children[#section_children + 1] = ui.column({
				key = "months_" .. section.year, modifier = ui.modifier(ui.fill_width()),
				spacing = theme.spacing.medium, children = rows,
			})
			sections[#sections + 1] = ui.column({
				key = "year_" .. section.year, spacing = theme.spacing.small,
				modifier = ui.modifier(ui.fill_width()), children = section_children,
			})
		end
		-- Independent month blocks avoid split weeks and per-row scroll positions.
		children[1] = ui.column({
			key = "calendar_years", arrangement = ui.ARRANGEMENT.CENTER,
			modifier = ui.modifier(ui.fill_width(), ui.weight(1), ui.vertical_scroll()),
			children = { ui.column({ spacing = theme.spacing.large, children = sections }) },
		})
	else
		children[1] = ui.canvas({
			key = "calendar",
			modifier = ui.modifier(ui.fill_width(), ui.height(ui.dp(36 * font_scale + 7 * 14 + 6 / density))),
			draw = function(scope) draw_calendar(scope, data, font_scale) end,
		})
	end
	if state == nil or state.show_legend ~= false then
		local items = {}
		for index, source in ipairs(data.sources) do
			items[index] = { key = "source_" .. index, color = source.color, label = source.name }
		end
		table.insert(children, graphui.legend({ key = "legend", items = items }))
	end
	return {
		ui = ui.column({
			key = "calendar_heatmap", spacing = theme.spacing.small,
			modifier = column_modifier, children = children,
		}),
		state = state,
	}
end
