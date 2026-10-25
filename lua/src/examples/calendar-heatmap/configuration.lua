local strings = require("strings")
local commonui = require("tng.commonui")
local theme = require("tng.theme")
local ui = require("tng.ui")

local function initial_state(state)
	state = state or {}
	if state.weeks == nil then state.weeks = 53 end
	if state.layout == nil then state.layout = "AUTO" end
	if state.show_legend == nil then state.show_legend = true end
	if state.sources == nil then state.sources = {} end
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

local function source_row(state, index, source)
	return ui.row({
		key = "source_" .. index,
		spacing = theme.spacing.small,
		alignment = ui.ALIGNMENT.CENTER,
		children = {
			commonui.feature_selector({
				key = "source_feature_" .. index,
				state = state,
				title = strings.select_feature,
				placeholder = strings.choose_feature,
				selected = source.selection,
				picker_scope = "feature_inputs",
				on_selected = function(selection) source.selection = selection end,
				modifier = ui.modifier(ui.fill_width(), ui.weight(1)),
			}),
			commonui.color_spinner({
				key = "source_color_" .. index,
				state = state,
				selected_color = source.color,
				on_color_selected = function(color) source.color = color end,
			}),
			commonui.text_chip({
				key = "remove_source_" .. index,
				text = strings.remove,
				on_click = function() table.remove(state.sources, index) end,
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
	local feature_inputs = {}
	local source_controls = {}
	for index, source in ipairs(state.sources) do
		feature_inputs[index] = { name = "source_" .. index, feature_id = source.selection.id }
		source_controls[index] = source_row(state, index, source)
	end
	table.insert(source_controls, commonui.feature_selector({
		key = "add_source",
		state = state,
		title = strings.select_feature,
		placeholder = strings.add_feature,
		picker_scope = "feature_inputs",
		on_selected = function(selection)
			local index = #state.sources + 1
			state.sources[index] = { selection = selection, color = ((index - 1) * 7) % 12 }
		end,
		modifier = ui.modifier(ui.fill_width()),
	}))
	return {
		ui = ui.column({
			key = "calendar_heatmap_configuration",
			spacing = theme.spacing.large,
			modifier = ui.modifier(ui.fill_width()),
			children = {
				ui.column({ spacing = theme.spacing.small, children = source_controls }),
				setting(strings.range, {
					option(state, "weeks", 13, strings.weeks_13),
					option(state, "weeks", 26, strings.weeks_26),
					option(state, "weeks", 53, strings.weeks_53),
				}),
				setting(strings.layout, {
					option(state, "layout", "AUTO", strings.automatic),
					option(state, "layout", "WIDE", strings.wide),
					option(state, "layout", "NARROW", strings.narrow),
				}),
				setting(strings.show_legend, {
					option(state, "show_legend", true, strings.yes),
					option(state, "show_legend", false, strings.no),
				}),
			},
		}),
		state = state,
		feature_inputs = feature_inputs,
		configuration_error = #state.sources == 0 and strings.feature_required or nil,
	}
end
