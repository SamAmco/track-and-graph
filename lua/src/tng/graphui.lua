local ui = require("tng.ui")
local theme = require("tng.theme")
local icons = require("tng.icons")
local commonui = require("tng.commonui")

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

--- @since (API level 5)
--- @class ColoredFeatureInputCardParams
--- @field key string: Stable key for the card and its retained child controls.
--- @field state table: Configuration state retained by the renderer.
--- @field title string|table<string,string>: Select-item dialog title.
--- @field placeholder string|table<string,string>: Text shown before a feature is selected.
--- @field selected? table: Current feature selection returned by `on_feature_selected`.
--- @field selected_color integer: Zero-based data-visualisation colour index.
--- @field picker_scope? string: Scope shared with other selectors that should retain picker navigation.
--- @field on_selected fun(selection): Updates the caller's selected feature.
--- @field on_color_selected fun(index): Updates the caller's zero-based colour index.
--- @field on_delete fun(): Removes this feature input.
--- @field delete_content_description string|table<string,string>: Localized accessibility description.
--- @field modifier? table[]: Ordered modifiers applied after the default fill-width modifier.
M.ColoredFeatureInputCardParams = {}

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

--- Creates the compact graph input card used for a feature with an associated data-vis colour.
--- @param params ColoredFeatureInputCardParams
M.colored_feature_input_card = function(params)
	assert(type(params) == "table", "colored feature input card params must be a table")
	assert(type(params.key) == "string", "colored feature input card key is required")
	assert(type(params.state) == "table", "colored feature input card state is required")
	assert(type(params.selected_color) == "number", "selected_color must be a number")
	assert(type(params.on_selected) == "function", "on_selected is required")
	assert(type(params.on_color_selected) == "function", "on_color_selected is required")
	assert(type(params.on_delete) == "function", "on_delete is required")
	assert(
		type(params.delete_content_description) == "string"
			or type(params.delete_content_description) == "table",
		"delete_content_description is required"
	)
	return ui.surface({
		key = params.key,
		modifier = append_modifiers(ui.modifier(ui.fill_width()), params.modifier),
		color = theme.color.surface,
		content_color = theme.color.on_surface,
		shape = theme.shape.medium,
		shadow_elevation = ui.dp(4),
		child = ui.row({
			modifier = ui.modifier(ui.fill_width(), ui.padding(theme.spacing.medium)),
			spacing = theme.spacing.small,
			alignment = ui.ALIGNMENT.CENTER,
			children = {
				commonui.color_spinner({
					key = params.key .. "_color",
					state = params.state,
					selected_color = params.selected_color,
					on_color_selected = params.on_color_selected,
				}),
				commonui.feature_selector({
					key = params.key .. "_feature",
					state = params.state,
					title = params.title,
					placeholder = params.placeholder,
					selected = params.selected,
					picker_scope = params.picker_scope,
					on_selected = params.on_selected,
					modifier = ui.modifier(ui.fill_width(), ui.weight(1)),
				}),
				ui.icon_button({
					key = params.key .. "_delete",
					on_click = params.on_delete,
					child = ui.icon({
						icon = icons.ICON.DELETE,
						content_description = params.delete_content_description,
					}),
				}),
			},
		}),
	})
end

return M
