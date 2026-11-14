local strings = require("strings")
local commonui = require("tng.commonui")
local icons = require("tng.icons")
local theme = require("tng.theme")
local ui = require("tng.ui")

local function initial_state(state)
	state = state or {}
	if state.show_legend == nil then state.show_legend = true end
	if state.sources == nil then state.sources = {} end
	if state.next_source_key == nil then state.next_source_key = 0 end
	return state
end

local function option(state, field, value, label)
	local selected = state[field] == value
	return commonui.text_chip({
		key = field .. "_" .. tostring(value),
		text = label,
		selected = selected,
		on_click = function() state[field] = value end,
	})
end

local function setting(label, options)
	return ui.column({
		spacing = theme.spacing.small,
		children = {
			ui.text({ text = label, style = theme.text_style.title_small }),
			ui.flow_row({
				horizontal_spacing = theme.spacing.small,
				vertical_spacing = theme.spacing.small,
				children = options,
			}),
		},
	})
end

local function initial_sources(feature_map)
	local sources = {}
	if feature_map == nil then return sources end
	if feature_map.source ~= nil then
		sources[1] = { selection = feature_map.source, color = 0 }
		return sources
	end
	local index = 1
	while feature_map["source_" .. index] ~= nil do
		sources[index] = {
			selection = feature_map["source_" .. index],
			color = ((index - 1) * 7) % 12,
		}
		index = index + 1
	end
	return sources
end

local function valid_range(source)
	local minimum, maximum = tonumber(source.minimum.text), tonumber(source.maximum.text)
	return minimum ~= nil and maximum ~= nil
		and minimum > -math.huge and maximum < math.huge and maximum > minimum
end

local function range_field(source, field, label)
	return ui.text_field({
		key = field,
		value = source[field],
		label = label,
		-- Some decimal keyboards omit minus; the text keyboard permits signed ranges.
		keyboard_type = "TEXT",
		is_error = not valid_range(source),
		on_value_change = function(value) source[field] = value end,
		modifier = ui.modifier(ui.weight(1)),
	})
end

local function source_row(state, index, source)
	if source.key == nil then
		state.next_source_key = state.next_source_key + 1
		source.key = state.next_source_key
	end
	local source_key = source.key
	source.mode = source.mode or "BINARY"
	source.minimum = ui.text_field_value(source.minimum or "0")
	source.maximum = ui.text_field_value(source.maximum or "100")
	local mode_menu = {}
	for _, mode in ipairs({ "BINARY", "RANGE" }) do
		mode_menu[#mode_menu + 1] = ui.dropdown_menu_item({
			on_click = function()
				source.mode = mode
				source.mode_menu_visible = false
			end,
			child = ui.text({ text = mode == "BINARY" and strings.binary or strings.range }),
		})
	end
	local children = {
		ui.row({
			alignment = ui.ALIGNMENT.CENTER,
			spacing = theme.spacing.small,
			children = {
				commonui.color_spinner({
					key = "source_color_" .. source_key,
					state = state,
					selected_color = source.color,
					on_color_selected = function(color) source.color = color end,
				}),
				commonui.feature_selector({
					key = "source_feature_" .. source_key,
					state = state,
					title = strings.select_feature,
					placeholder = strings.choose_feature,
					selected = source.selection,
					picker_scope = "feature_inputs",
					on_selected = function(selection) source.selection = selection end,
					modifier = ui.modifier(ui.weight(1)),
				}),
				ui.icon_button({
					on_click = function() table.remove(state.sources, index) end,
					child = ui.icon({ icon = icons.ICON.DELETE, content_description = strings.remove_feature }),
				}),
			},
		}),
		ui.row({
			alignment = ui.ALIGNMENT.CENTER,
			spacing = theme.spacing.medium,
			children = {
				ui.text({ text = strings.intensity, style = theme.text_style.body_medium }),
				ui.box({
					children = {
						commonui.selector_button({
							text = source.mode == "RANGE" and strings.range or strings.binary,
							on_click = function() source.mode_menu_visible = true end,
						}),
						ui.dropdown_menu({
							expanded = source.mode_menu_visible == true,
							on_dismiss_request = function() source.mode_menu_visible = false end,
							children = mode_menu,
						}),
					},
				}),
			},
		}),
	}
	if source.mode == "RANGE" then
		children[#children + 1] = ui.row({
			spacing = theme.spacing.medium,
			children = {
				range_field(source, "minimum", strings.minimum),
				range_field(source, "maximum", strings.maximum),
			},
		})
	end
	return ui.surface({
		key = "source_" .. source_key,
		modifier = ui.modifier(ui.fill_width()),
		color = theme.color.surface,
		shape = theme.shape.medium,
		shadow_elevation = ui.dp(4),
		child = ui.column({
			modifier = ui.modifier(ui.padding(theme.spacing.medium)),
			spacing = theme.spacing.medium,
			children = children,
		}),
	})
end

local function add_source_control(state)
	local close = function() state.add_source_dialog_visible = false end
	return ui.box({
		key = "add_source_control",
		modifier = ui.modifier(ui.fill_width()),
		alignment = ui.BOX_ALIGNMENT.TOP_CENTER,
		children = {
			commonui.add_bar_button({
				key = "add_source_button",
				content_description = strings.add_feature,
				on_click = function() state.add_source_dialog_visible = true end,
			}),
			ui.select_item_dialog({
				key = "add_source_dialog",
				visible = state.add_source_dialog_visible == true,
				title = strings.select_feature,
				picker_scope = "feature_inputs",
				selectable_types = { ui.SELECTABLE_ITEM_TYPE.FEATURE },
				on_feature_selected = function(selection)
					local index = #state.sources + 1
					state.sources[index] = { selection = selection, color = ((index - 1) * 7) % 12 }
					close()
				end,
				on_dismiss_request = close,
			}),
		},
	})
end

return function(params)
	local state = params.state
	state = initial_state(state)
	if state.feature_inputs_initialized ~= true then
		state.sources = initial_sources(params.feature_map)
		state.feature_inputs_initialized = true
	end
	local configuration_error = #state.sources == 0 and strings.feature_required or nil
	local feature_inputs = {}
	local source_controls = {}
	for index, source in ipairs(state.sources) do
		feature_inputs[index] = { name = "source_" .. index, feature_id = source.selection.id }
		source_controls[index] = source_row(state, index, source)
		if source.mode == "RANGE" and not valid_range(source) then configuration_error = strings.invalid_range end
	end
	table.insert(source_controls, add_source_control(state))
	return {
		ui = ui.column({
			key = "calendar_heatmap_configuration",
			spacing = theme.spacing.large,
			modifier = ui.modifier(ui.fill_width()),
			children = {
				ui.column({
					spacing = theme.spacing.small,
					children = {
						ui.text({ text = strings.data_sources, style = theme.text_style.title_small }),
						ui.column({ spacing = theme.spacing.medium, children = source_controls }),
					},
				}),
				setting(strings.show_legend, {
					option(state, "show_legend", true, strings.yes),
					option(state, "show_legend", false, strings.no),
				}),
			},
		}),
		state = state,
		feature_inputs = feature_inputs,
		configuration_error = configuration_error,
	}
end
