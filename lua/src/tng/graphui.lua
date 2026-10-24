local ui = require("tng.ui")
local theme = require("tng.theme")

local M = {}

--- @since (API level 5)
--- @class LegendItem
--- @field key? string: Stable identity across renders.
--- @field color Color|string: Data-vis index, hex color, or theme color token.
--- @field label string
--- @field modifier? table[]: Ordered modifiers for the item row.
M.LegendItem = {}

--- @since (API level 5)
--- @class LegendParams
--- @field key? string: Stable identity across renders.
--- @field modifier? table[]: Ordered modifiers applied after the default fill-width modifier.
--- @field items LegendItem[]
M.LegendParams = {}

local function append_modifiers(first, second)
	local result = {}
	for _, modifier in ipairs(first or {}) do table.insert(result, modifier) end
	for _, modifier in ipairs(second or {}) do table.insert(result, modifier) end
	return result
end

--- Creates one standard graph legend item.
--- @param item LegendItem
M.legend_item = function(item)
	assert(type(item) == "table", "legend item must be a table")
	assert(item.color ~= nil, "legend item color is required")
	assert(type(item.label) == "string", "legend item label must be a string")
	return ui.row({
		key = item.key,
		modifier = item.modifier,
		alignment = ui.ALIGNMENT.CENTER,
		children = {
			ui.spacer({
				modifier = ui.modifier(
					ui.width(theme.spacing.large),
					ui.height(theme.spacing.large),
					ui.background(item.color, theme.shape.circle)
				),
			}),
			ui.spacer({ modifier = ui.modifier(ui.width(ui.dp(2))) }),
			ui.text({ text = item.label, style = theme.text_style.body_medium }),
		},
	})
end

--- Creates the standard full-width, centered, wrapping graph legend.
--- @param params LegendParams
M.legend = function(params)
	assert(type(params) == "table", "legend params must be a table")
	assert(type(params.items) == "table", "legend items must be a table")
	local children = {}
	for _, item in ipairs(params.items) do
		table.insert(children, M.legend_item(item))
		table.insert(children, ui.spacer({
			modifier = ui.modifier(
				ui.width(theme.spacing.medium),
				ui.height(theme.spacing.medium)
			),
		}))
	end
	return ui.flow_row({
		key = params.key,
		modifier = append_modifiers(ui.modifier(ui.fill_width()), params.modifier),
		horizontal_arrangement = ui.ARRANGEMENT.CENTER,
		children = children,
	})
end

return M
