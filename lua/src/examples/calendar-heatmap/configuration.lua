local strings = require("strings")
local graphui = require("tng.graphui")
local theme = require("tng.theme")
local ui = require("tng.ui")

local function initial_state(state)
	state = state or {}
	if state.weeks == nil then state.weeks = 53 end
	if state.layout == nil then state.layout = "AUTO" end
	if state.show_legend == nil then state.show_legend = true end
	return state
end

local function option(state, field, value, label)
	local selected = state[field] == value
	return ui.surface({
		key = field .. "_" .. tostring(value),
		modifier = ui.modifier(
			ui.clip(theme.shape.medium),
			ui.on_click(function() state[field] = value end)
		),
		color = selected and theme.color.primary or theme.color.surface_variant,
		content_color = selected and theme.color.on_primary or theme.color.on_surface_variant,
		shape = theme.shape.medium,
		child = ui.text({
			text = label,
			style = theme.text_style.label_large,
			modifier = ui.modifier(ui.padding({
				horizontal = theme.spacing.medium,
				vertical = theme.spacing.small,
			})),
		}),
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

return function(params)
	local state = params.state
	state = initial_state(state)
	if state.feature_inputs_initialized ~= true then
		state.source = params.feature_map and params.feature_map.source or nil
		state.feature_inputs_initialized = true
	end
	local feature_inputs = {}
	if state.source ~= nil then
		feature_inputs[1] = { name = "source", feature_id = state.source.id }
	end
	return {
		ui = ui.column({
			key = "calendar_heatmap_configuration",
			spacing = theme.spacing.large,
			modifier = ui.modifier(ui.fill_width()),
			children = {
				graphui.feature_selector({
					key = "source_feature",
					state = state,
					title = strings.select_feature,
					placeholder = strings.choose_feature,
					selected = state.source,
					picker_scope = "feature_inputs",
					on_selected = function(selection) state.source = selection end,
					modifier = ui.modifier(ui.fill_width()),
				}),
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
		configuration_error = state.source == nil and strings.feature_required or nil,
	}
end
