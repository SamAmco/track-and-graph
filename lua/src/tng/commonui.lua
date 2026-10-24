local theme = require("tng.theme")
local ui = require("tng.ui")

local M = {}

--- @since (API level 5)
--- @class SelectorButtonParams
--- @field key? string: Stable identity across renders.
--- @field text string|table<string,string>: Button label.
--- @field on_click? fun()
--- @field enabled? boolean: Defaults to true.
--- @field modifier? table[]
--- @field text_color? Color|string
--- @field max_lines? integer: Defaults to one.
M.SelectorButtonParams = {}

--- @since (API level 5)
--- @class TextChipParams
--- @field key? string: Stable identity across renders.
--- @field text string|table<string,string>: Chip label.
--- @field selected? boolean: Defaults to false.
--- @field enabled? boolean: Defaults to true.
--- @field on_click? fun()
--- @field modifier? table[]
M.TextChipParams = {}

--- @since (API level 5)
--- @class FeatureSelectorParams
--- @field key string: Stable key used for this selector and its visibility state.
--- @field state table: Configuration state retained by the renderer.
--- @field title string|table<string,string>: Select-item dialog title.
--- @field placeholder string|table<string,string>: Text shown before a feature is selected.
--- @field selected? table: Current feature selection returned by `on_feature_selected`.
--- @field picker_scope? string: Scope shared with other selectors that should retain picker navigation.
--- @field on_selected fun(selection): Updates the caller's state with the selected feature.
--- @field modifier? table[]
M.FeatureSelectorParams = {}

local function append_modifiers(first, second)
	local result = {}
	for _, modifier in ipairs(first or {}) do table.insert(result, modifier) end
	for _, modifier in ipairs(second or {}) do table.insert(result, modifier) end
	return result
end

local function has_modifier(modifiers, modifier_type)
	for _, modifier in ipairs(modifiers or {}) do
		if modifier.type == modifier_type then return true end
	end
	return false
end

local function clickable_modifiers(params, minimum_width, minimum_height, border)
	local modifiers = append_modifiers(params.modifier, ui.modifier(ui.clip(theme.shape.medium)))
	if params.enabled ~= false and params.on_click ~= nil then
		table.insert(modifiers, ui.on_click(params.on_click))
	end
	table.insert(modifiers, ui.min_width(ui.dp(minimum_width)))
	if minimum_height ~= nil then table.insert(modifiers, ui.min_height(ui.dp(minimum_height))) end
	if border ~= nil then table.insert(modifiers, border) end
	return modifiers
end

--- Creates the Lua equivalent of the app's SelectorButton.
--- @param params SelectorButtonParams
M.selector_button = function(params)
	assert(type(params) == "table", "selector button params must be a table")
	assert(type(params.text) == "string" or type(params.text) == "table", "selector button text is required")
	assert(params.on_click == nil or type(params.on_click) == "function", "selector button on_click must be a function")
	local content_modifiers = ui.modifier(
		ui.min_width(ui.dp(58)),
		ui.min_height(ui.dp(40)),
		ui.padding({ horizontal = ui.dp(8), vertical = ui.dp(6) })
	)
	if has_modifier(params.modifier, ui.MODIFIER_TYPE.FILL_WIDTH) then
		table.insert(content_modifiers, 1, ui.fill_width())
	end
	return ui.surface({
		key = params.key,
		modifier = clickable_modifiers(params, 58, 40),
		color = theme.color.selector_button,
		content_color = params.text_color or theme.color.on_surface,
		shape = theme.shape.medium,
		child = ui.row({
			modifier = content_modifiers,
			arrangement = ui.ARRANGEMENT.CENTER,
			alignment = ui.ALIGNMENT.CENTER,
			children = {
				ui.text({
					text = params.text,
					style = theme.text_style.title_small,
					color = params.text_color or theme.color.on_surface,
					max_lines = params.max_lines or 1,
					overflow = ui.TEXT_OVERFLOW.START_ELLIPSIS,
				}),
			},
		}),
	})
end

--- Creates the Lua equivalent of the app's TextChip.
--- @param params TextChipParams
M.text_chip = function(params)
	assert(type(params) == "table", "text chip params must be a table")
	assert(type(params.text) == "string" or type(params.text) == "table", "text chip text is required")
	assert(params.on_click == nil or type(params.on_click) == "function", "text chip on_click must be a function")
	local selected = params.selected == true
	local container_color = selected and theme.color.primary or theme.color.surface
	local content_color = selected and theme.color.on_primary or theme.color.on_surface
	local border_color = selected and theme.color.primary or theme.color.selector_button
	return ui.surface({
		key = params.key,
		modifier = clickable_modifiers(
			params,
			60,
			nil,
			ui.border({ width = ui.dp(2), color = border_color, shape = theme.shape.medium })
		),
		color = container_color,
		content_color = content_color,
		shape = theme.shape.medium,
		child = ui.row({
			modifier = ui.modifier(
				ui.min_width(ui.dp(60)),
				ui.padding({ horizontal = ui.dp(10), vertical = ui.dp(8) })
			),
			arrangement = ui.ARRANGEMENT.CENTER,
			alignment = ui.ALIGNMENT.CENTER,
			children = {
				ui.text({
					text = params.text,
					style = theme.text_style.title_small,
					color = content_color,
					modifier = ui.modifier(ui.padding({ horizontal = ui.dp(4) })),
				}),
			},
		}),
	})
end

--- Creates a SelectorButton backed by the app's standard feature-selection dialog.
--- @param params FeatureSelectorParams
M.feature_selector = function(params)
	assert(type(params) == "table", "feature selector params must be a table")
	assert(type(params.key) == "string", "feature selector key is required")
	assert(type(params.state) == "table", "feature selector state is required")
	assert(type(params.on_selected) == "function", "feature selector on_selected is required")
	params.state.__commonui = params.state.__commonui or {}
	params.state.__commonui.dialogs = params.state.__commonui.dialogs or {}
	local dialogs = params.state.__commonui.dialogs
	local close = function() dialogs[params.key] = false end
	local selected = params.selected
	return ui.box({
		key = params.key,
		children = {
			M.selector_button({
				key = params.key .. "_button",
				modifier = params.modifier,
				text = selected and selected.path or params.placeholder,
				on_click = function() dialogs[params.key] = true end,
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
