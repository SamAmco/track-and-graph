local canvas = require("tng.canvas")
local graphui = require("tng.graphui")
local theme = require("tng.theme")
local ui = require("tng.ui")

local function label(scope, text, x, y, width, height, align)
	scope:drawText({
		text = text,
		color = theme.color.secondary_text,
		top_left = { x = x, y = y },
		size = { width = width, height = height },
		font_size = scope:sp(12),
		text_align = align or canvas.TEXT_ALIGN.START,
		vertical_align = canvas.TEXT_VERTICAL_ALIGN.CENTER,
		max_lines = 1,
		soft_wrap = false,
	})
end

local function draw_day(scope, data, index, x, y, tile)
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
			-- Running arithmetic mean of the source's final colour:
			-- C' = (1 - 1/n) C + ((1-a) blank + a source) / n.
			-- Two ordinary source-over fills implement it without resolving theme
			-- or palette tokens in Lua, and give every active source equal weight.
			if alpha < 1 then fill(blank, (1 - alpha) / (count - alpha)) end
			fill(source.color, alpha / count)
		end
	end
end

return function(params)
	local data = params.data
	local context = params.context
	local state = params.state
	local density = context.density or 1
	local font_scale = context.font_scale or 1
	local tile_dp = 14
	-- Labels grow with accessibility text scaling; squares retain their size.
	local header_dp = 22 * font_scale
	local children = {
		ui.canvas({
			key = "calendar",
			modifier = ui.modifier(ui.fill_width(), ui.height(ui.dp(header_dp + 7 * tile_dp + 6 / density))),
			draw = function(scope)
				local tile = math.max(1, math.floor(scope:dp(tile_dp)))
				local step = tile + 1 -- one physical pixel between adjacent squares
				local label_width = scope:dp(34 * font_scale)
				local header = math.floor(scope:dp(header_dp))
				local weeks = math.max(0, math.floor((scope.size.width - label_width + 1) / step))
				if weeks == 0 then return end
				local calendar_data = data.calendar(weeks)
				calendar_data.sources = data.sources
				local first_week = 1
				local left = math.floor(scope.size.width) - (weeks * step - 1)
				local row_step = step
				for week = first_week, weeks do
					local x = left + (week - first_week) * step
					local month = calendar_data.month_labels[week]
					if month ~= nil and month ~= "" then
						-- Skip a trailing partial label rather than clipping it at the right edge.
						local label_right = scope.size.width
						for next_week = week + 1, weeks do
							if (calendar_data.month_labels[next_week] or "") ~= "" then
								label_right = left + (next_week - first_week) * step
								break
							end
						end
						if x + scope:dp(26 * font_scale) <= label_right then
							label(scope, month, x, 0, math.min(scope:dp(40 * font_scale), scope.size.width - x), header)
						end
					end
					for weekday = 1, 7 do
						local y = header + (weekday - 1) * row_step
						draw_day(scope, calendar_data, (week - 1) * 7 + weekday, x, y, tile)
					end
				end
				for weekday = 1, 7 do
					-- Keep labels anchored independently of the spare width before the first whole week.
					label(scope, calendar_data.weekday_labels[weekday], 0, header + (weekday - 1) * row_step,
						label_width - scope:dp(4), tile, canvas.TEXT_ALIGN.START)
				end
			end,
		}),
	}
	if state == nil or state.show_legend ~= false then
		local items = {}
		for index, source in ipairs(data.sources) do
			items[index] = { key = "source_" .. index, color = source.color, label = source.name }
		end
		table.insert(children, graphui.legend({ key = "legend", items = items }))
	end
	return {
		ui = ui.column({
			key = "calendar_heatmap",
			spacing = theme.spacing.small,
			modifier = ui.modifier(ui.fill_width()),
			children = children,
		}),
		state = state,
	}
end
