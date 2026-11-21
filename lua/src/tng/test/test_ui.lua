#!/usr/bin/env lua

package.path = package.path .. ";src/?.lua;src/?/init.lua"

local theme = require("tng.theme")
local ui = require("tng.ui")
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test

test("dimensions are serializable values", function()
	local width = ui.dp(16)
	assert(width.unit == ui.UNIT.DP)
	assert(width.value == 16)
	assert(ui.px(2).unit == ui.UNIT.PX)
	assert(ui.sp(14).unit == ui.UNIT.SP)
end)

test("scrolling and native date-picker descriptions retain their options and callbacks", function()
	assert(ui.vertical_scroll().type == "VERTICAL_SCROLL")
	local selected, dismissed
	local date = { year = 2026, month = 1, day = 1 }
	local dialog = ui.date_picker_dialog({
		key = "date", visible = true, initial_date = date, allow_past_dates = false,
		on_date_selected = function(value) selected = value end,
		on_dismiss_request = function() dismissed = true end,
	})
	assert(dialog.type == "DATE_PICKER_DIALOG" and dialog.initial_date == date)
	assert(dialog.visible and not dialog.allow_past_dates)
	dialog.on_date_selected(date)
	dialog.on_dismiss_request()
	assert(selected == date and dismissed)
end)

test("text accepts translated strings", function()
	local translations = { en = "Calendar", fr = "Calendrier" }
	local text = ui.text({ text = translations })
	assert(text.text == translations)
end)

test("text fields preserve incomplete numeric edits for Lua validation", function()
	local state = { minimum = ui.text_field_value("0") }
	local field = ui.text_field({
		key = "minimum",
		value = state.minimum,
		label = { en = "Minimum" },
		on_value_change = function(value) state.minimum = value end,
	})
	field.on_value_change({ text = "-", selection = { start = 1, ["end"] = 1 }, revision = 1 })
	assert(state.minimum.text == "-")
	assert(state.minimum.revision == 1)
	assert(type(state.minimum.mutate) == "function")
	field.on_value_change({ text = "-3.1253", selection = { start = 7, ["end"] = 7 }, revision = 2 })
	assert(tonumber(state.minimum.text) == -3.1253)
end)

test("text field values restore selection and revisions and mutate without aliasing", function()
	local original = ui.text_field_value({
		text = "Hello", selection = { start = 4, ["end"] = 1 }, revision = 37,
	})
	assert(ui.text_field_value(original) == original)
	local changed = original:mutate(function(value)
		value.text = "Reset"
		value.selection.start = 0
	end)
	assert(original.text == "Hello" and original.selection.start == 4)
	assert(changed.text == "Reset" and changed.selection.start == 0)
	assert(changed.selection["end"] == 1)
	assert(changed.base_revision == 37 and changed.revision == 38)
	local twice = changed:mutate(function(value) value.text = "Again" end)
	assert(twice.base_revision == 37 and twice.revision == 39)
	local restored = ui.text_field_value({
		text = twice.text, selection = twice.selection, revision = twice.revision,
		base_revision = twice.base_revision,
	})
	assert(restored.selection.start == 0 and restored.selection["end"] == 1)
	assert(restored.revision == 39 and restored.base_revision == 37)
	assert(restored:mutate(function(value) value.text = "Restored" end).revision == 40)
end)

test("new text field cursor offsets count UTF-16 rather than UTF-8 bytes", function()
	local value = ui.text_field_value("Aé😀")
	assert(value.selection.start == 4 and value.selection["end"] == 4)
	assert(value.revision == 0 and value.base_revision == nil)
end)

test("icons and icon buttons retain semantic tokens and interaction", function()
	local icons = require("tng.icons")
	local clicked = false
	local icon = ui.icon({
		icon = icons.ICON.DELETE,
		content_description = { en = "Remove feature" },
		tint = theme.color.error,
	})
	local button = ui.icon_button({
		child = icon,
		on_click = function() clicked = true end,
		enabled = false,
	})

	assert(icon.type == ui.COMPONENT_TYPE.ICON)
	assert(icon.icon == "ICON_DELETE")
	assert(icon.content_description.en == "Remove feature")
	assert(icon.tint == theme.color.error)
	assert(button.type == ui.COMPONENT_TYPE.ICON_BUTTON)
	assert(button.child == icon)
	assert(button.enabled == false)
	button.on_click()
	assert(clicked)
end)

test("select item dialog preserves picker options and callbacks", function()
	local selected = function() end
	local dismissed = function() end
	local dialog = ui.select_item_dialog({
		key = "source_dialog",
		visible = true,
		title = { en = "Select a feature" },
		picker_scope = "feature_inputs",
		selectable_types = { ui.SELECTABLE_ITEM_TYPE.FEATURE },
		hidden_items = { { type = ui.SELECTABLE_ITEM_TYPE.FEATURE, id = 1 } },
		disabled_items = { { type = ui.SELECTABLE_ITEM_TYPE.FEATURE, id = 2 } },
		on_feature_selected = selected,
		on_dismiss_request = dismissed,
		reset_on_close = true,
	})

	assert(dialog.type == ui.COMPONENT_TYPE.SELECT_ITEM_DIALOG)
	assert(dialog.picker_scope == "feature_inputs")
	assert(dialog.selectable_types[1] == ui.SELECTABLE_ITEM_TYPE.FEATURE)
	assert(dialog.hidden_items[1].id == 1)
	assert(dialog.disabled_items[1].id == 2)
	assert(dialog.on_feature_selected == selected)
	assert(dialog.on_dismiss_request == dismissed)
	assert(dialog.reset_on_close == true)
end)

test("dropdown menu preserves controlled visibility and arbitrary item content", function()
	local dismissed = function() end
	local selected = function() end
	local item_content = ui.row({ children = { ui.text({ text = "Choice" }) } })
	local menu = ui.dropdown_menu({
		key = "choices",
		expanded = true,
		on_dismiss_request = dismissed,
		children = {
			ui.dropdown_menu_item({
				key = "choice",
				child = item_content,
				on_click = selected,
				enabled = false,
			}),
		},
	})

	assert(menu.type == ui.COMPONENT_TYPE.DROPDOWN_MENU)
	assert(menu.expanded == true)
	assert(menu.on_dismiss_request == dismissed)
	assert(menu.children[1].type == ui.COMPONENT_TYPE.DROPDOWN_MENU_ITEM)
	assert(menu.children[1].child == item_content)
	assert(menu.children[1].on_click == selected)
	assert(menu.children[1].enabled == false)
end)

test("nested UI constants are available through the API level gate", function()
	local api_gate = require("community.test-utils.api-gate")
	local spec = dofile("src/tng/ui.apispec.lua")
	api_gate.reset()
	local gated = api_gate.gate(ui, spec)
	assert(gated.text_field_value("text").text == "text")

	assert(gated.BOX_ALIGNMENT.BOTTOM_END == "BOTTOM_END")
	assert(gated.COMPONENT_TYPE.FLOW_ROW == "FLOW_ROW")
	assert(gated.COMPONENT_TYPE.DROPDOWN_MENU_ITEM == "DROPDOWN_MENU_ITEM")
	assert(gated.SELECTABLE_ITEM_TYPE.FEATURE == "FEATURE")
	assert(gated.TEXT_OVERFLOW.START_ELLIPSIS == "START_ELLIPSIS")
	assert(gated.MODIFIER_TYPE.FILL_MAX_ROW_HEIGHT == "FILL_MAX_ROW_HEIGHT")
	assert(gated.fill_max_row_height(0.5).fraction == 0.5)
	assert(gated.flow_row({}).type == "FLOW_ROW")
	assert(gated.dropdown_menu({
		expanded = false,
		on_dismiss_request = function() end,
	}).type == "DROPDOWN_MENU")
	api_gate.validate(5)
end)

test("modifier order and values are preserved", function()
	local clicked = function() end
	local modifiers = ui.modifier(
		ui.fill_width(),
		ui.height(ui.dp(200)),
		ui.weight(2, false),
		ui.padding({ horizontal = theme.spacing.medium, vertical = ui.dp(4) }),
		ui.background(theme.color.surface, theme.shape.medium),
		ui.border({ width = ui.dp(1), color = theme.color.outline, shape = theme.shape.medium }),
		ui.clip(theme.shape.medium),
		ui.elevation(ui.dp(4)),
		ui.align(ui.ALIGNMENT.CENTER),
		ui.on_click(clicked)
	)

	assert(#modifiers == 10)
	assert(modifiers[1].type == ui.MODIFIER_TYPE.FILL_WIDTH)
	assert(modifiers[2].type == ui.MODIFIER_TYPE.HEIGHT)
	assert(modifiers[3].fill == false)
	assert(modifiers[4].horizontal == "SPACING_MEDIUM")
	assert(modifiers[5].color == "THEME_SURFACE")
	assert(modifiers[6].width.value == 1)
	assert(modifiers[7].shape == "SHAPE_MEDIUM")
	assert(modifiers[8].value.value == 4)
	assert(modifiers[9].value == ui.ALIGNMENT.CENTER)
	assert(modifiers[10].type == ui.MODIFIER_TYPE.ON_CLICK)
	assert(modifiers[10].callback == clicked)
end)

test("flow row preserves wrapping policy and child modifiers", function()
	local flow = ui.flow_row({
		key = "legend",
		horizontal_arrangement = ui.ARRANGEMENT.CENTER,
		vertical_arrangement = ui.ARRANGEMENT.SPACE_BETWEEN,
		horizontal_spacing = ui.dp(4),
		vertical_spacing = theme.spacing.small,
		item_vertical_alignment = ui.ALIGNMENT.CENTER,
		max_items_in_each_row = 3,
		max_lines = 2,
		children = {
			ui.text({
				text = "Item",
				modifier = ui.modifier(ui.weight(1), ui.fill_max_row_height(0.5)),
			}),
		},
	})

	assert(flow.type == ui.COMPONENT_TYPE.FLOW_ROW)
	assert(flow.key == "legend")
	assert(flow.horizontal_arrangement == ui.ARRANGEMENT.CENTER)
	assert(flow.vertical_arrangement == ui.ARRANGEMENT.SPACE_BETWEEN)
	assert(flow.horizontal_spacing.value == 4)
	assert(flow.vertical_spacing == theme.spacing.small)
	assert(flow.item_vertical_alignment == ui.ALIGNMENT.CENTER)
	assert(flow.max_items_in_each_row == 3)
	assert(flow.max_lines == 2)
	assert(flow.children[1].modifier[2].type == ui.MODIFIER_TYPE.FILL_MAX_ROW_HEIGHT)
	assert(flow.children[1].modifier[2].fraction == 0.5)
end)

test("layout components retain keys, layout policies, and surface styling", function()
	local tree = ui.surface({
		key = "card",
		color = theme.color.surface,
		content_color = theme.color.on_surface,
		shape = theme.shape.large,
		tonal_elevation = ui.dp(1),
		shadow_elevation = ui.dp(4),
		child = ui.column({
			spacing = theme.spacing.medium,
			arrangement = ui.ARRANGEMENT.CENTER,
			alignment = ui.ALIGNMENT.STRETCH,
			children = {
				ui.text({
					key = "title",
					text = "Calendar",
					style = theme.text_style.headline_small,
					font_size = ui.sp(20),
					font_weight = ui.FONT_WEIGHT.BOLD,
					text_align = ui.TEXT_ALIGN.CENTER,
					max_lines = 1,
					overflow = ui.TEXT_OVERFLOW.ELLIPSIS,
				}),
				ui.row({
					arrangement = ui.ARRANGEMENT.SPACE_BETWEEN,
					alignment = ui.ALIGNMENT.CENTER,
					children = {
						ui.box({
							alignment = ui.BOX_ALIGNMENT.CENTER,
							children = { ui.text({ text = "Previous" }) },
						}),
						ui.spacer({ modifier = ui.modifier(ui.weight(1)) }),
						ui.text({ text = "Next" }),
					},
				}),
			},
		}),
	})

	assert(tree.type == ui.COMPONENT_TYPE.SURFACE)
	assert(tree.key == "card")
	assert(tree.child.type == ui.COMPONENT_TYPE.COLUMN)
	assert(tree.child.arrangement == ui.ARRANGEMENT.CENTER)
	assert(tree.child.alignment == ui.ALIGNMENT.STRETCH)
	assert(tree.child.children[1].key == "title")
	assert(tree.child.children[1].style == "TEXT_STYLE_HEADLINE_SMALL")
	assert(tree.child.children[2].type == ui.COMPONENT_TYPE.ROW)
	assert(tree.child.children[2].children[1].type == ui.COMPONENT_TYPE.BOX)
	assert(tree.child.children[2].children[1].alignment == ui.BOX_ALIGNMENT.CENTER)
	assert(tree.child.children[2].children[2].type == ui.COMPONENT_TYPE.SPACER)
	assert(ui.row({}).alignment == ui.ALIGNMENT.START)
	assert(ui.box({}).alignment == ui.BOX_ALIGNMENT.TOP_START)
end)

helpers.finish_tests()
