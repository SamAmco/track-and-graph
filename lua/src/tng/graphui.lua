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

--- @since (API level 5)
--- @class FeatureSelectorParams
--- @field key string: Stable key used for this selector's visibility state.
--- @field state table: Configuration state retained by the renderer.
--- @field title string|table<string,string>: Select-item dialog title.
--- @field placeholder string|table<string,string>: Text shown before a feature is selected.
--- @field selected? table: Current feature selection returned by `on_feature_selected`.
--- @field picker_scope? string: Scope shared with other selectors that should retain picker navigation.
--- @field on_selected fun(selection): Updates the graph's configuration state.
--- @field modifier? table[]
M.FeatureSelectorParams = {}

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

--- Creates a graph feature selector backed by the standard select-item dialog.
--- @param params FeatureSelectorParams
M.feature_selector = function(params)
	assert(type(params) == "table", "feature selector params must be a table")
	assert(type(params.key) == "string", "feature selector key is required")
	assert(type(params.state) == "table", "feature selector state is required")
	assert(type(params.on_selected) == "function", "feature selector on_selected is required")
	params.state.__graphui = params.state.__graphui or {}
	params.state.__graphui.dialogs = params.state.__graphui.dialogs or {}
	local dialogs = params.state.__graphui.dialogs
	local close = function() dialogs[params.key] = false end
	local selected = params.selected
	return ui.box({
		key = params.key,
		children = {
			ui.surface({
				modifier = append_modifiers(params.modifier, ui.modifier(
					ui.clip(theme.shape.medium),
					ui.on_click(function() dialogs[params.key] = true end)
				)),
				color = theme.color.surface_variant,
				content_color = theme.color.on_surface_variant,
				shape = theme.shape.medium,
				child = ui.text({
					text = selected and selected.path or params.placeholder,
					style = theme.text_style.title_small,
					max_lines = 1,
					overflow = ui.TEXT_OVERFLOW.ELLIPSIS,
					modifier = ui.modifier(ui.padding({ horizontal = ui.dp(8), vertical = ui.dp(6) })),
				}),
			}),
			ui.select_item_dialog({
				key = params.key .. "_dialog",
				visible = dialogs[params.key] == true,
				title = params.title,
				picker_scope = params.picker_scope,
				selectable_types = { ui.SELECTABLE_ITEM_TYPE.FEATURE },
				on_feature_selected = function(selection)
					params.on_selected(selection)
					close()
				end,
				on_dismiss_request = close,
			}),
		},
	})
end

return M
