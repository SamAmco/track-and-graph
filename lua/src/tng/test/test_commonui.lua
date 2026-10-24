#!/usr/bin/env lua

package.path = package.path .. ";src/?.lua;src/?/init.lua"

local commonui = require("tng.commonui")
local theme = require("tng.theme")
local ui = require("tng.ui")
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test

local function modifier_of_type(modifiers, modifier_type)
	for _, modifier in ipairs(modifiers) do
		if modifier.type == modifier_type then return modifier end
	end
	return nil
end

test("selector button matches the native shared component", function()
	local clicked = function() end
	local button = commonui.selector_button({
		key = "selector",
		text = "Choose feature",
		on_click = clicked,
		modifier = ui.modifier(ui.fill_width()),
	})

	assert(button.type == ui.COMPONENT_TYPE.SURFACE)
	assert(button.key == "selector")
	assert(button.color == theme.color.selector_button)
	assert(button.content_color == theme.color.on_surface)
	assert(button.shape == theme.shape.medium)
	assert(modifier_of_type(button.modifier, ui.MODIFIER_TYPE.ON_CLICK).callback == clicked)
	assert(modifier_of_type(button.modifier, ui.MODIFIER_TYPE.MIN_WIDTH).value.value == 58)
	assert(modifier_of_type(button.modifier, ui.MODIFIER_TYPE.MIN_HEIGHT).value.value == 40)
	local content = button.child
	assert(content.type == ui.COMPONENT_TYPE.ROW)
	assert(content.arrangement == ui.ARRANGEMENT.CENTER)
	assert(content.alignment == ui.ALIGNMENT.CENTER)
	assert(content.modifier[1].type == ui.MODIFIER_TYPE.FILL_WIDTH)
	assert(modifier_of_type(content.modifier, ui.MODIFIER_TYPE.PADDING).horizontal.value == 8)
	assert(content.children[1].style == theme.text_style.title_small)
	assert(content.children[1].overflow == ui.TEXT_OVERFLOW.START_ELLIPSIS)
end)

test("text chip matches native selected and unselected styling", function()
	local unselected = commonui.text_chip({ text = "Auto" })
	assert(unselected.color == theme.color.surface)
	assert(unselected.content_color == theme.color.on_surface)
	assert(unselected.shape == theme.shape.medium)
	assert(modifier_of_type(unselected.modifier, ui.MODIFIER_TYPE.MIN_WIDTH).value.value == 60)
	local border = modifier_of_type(unselected.modifier, ui.MODIFIER_TYPE.BORDER)
	assert(border.width.value == 2)
	assert(border.color == theme.color.selector_button)
	assert(unselected.child.arrangement == ui.ARRANGEMENT.CENTER)
	assert(unselected.child.alignment == ui.ALIGNMENT.CENTER)
	local padding = modifier_of_type(unselected.child.modifier, ui.MODIFIER_TYPE.PADDING)
	assert(padding.horizontal.value == 10)
	assert(padding.vertical.value == 8)
	assert(unselected.child.children[1].modifier[1].horizontal.value == 4)
	assert(unselected.child.children[1].style == theme.text_style.title_small)

	local selected = commonui.text_chip({ text = "Auto", selected = true })
	assert(selected.color == theme.color.primary)
	assert(selected.content_color == theme.color.on_primary)
	assert(modifier_of_type(selected.modifier, ui.MODIFIER_TYPE.BORDER).color == theme.color.primary)
	assert(selected.child.children[1].color == theme.color.on_primary)
end)

test("feature selector stores visibility in configuration state and updates selection", function()
	local state = {}
	local selected = nil
	local selector = commonui.feature_selector({
		key = "source",
		state = state,
		title = "Select a feature",
		placeholder = "Choose feature",
		picker_scope = "inputs",
		on_selected = function(value) selected = value end,
	})

	modifier_of_type(selector.children[1].modifier, ui.MODIFIER_TYPE.ON_CLICK).callback()
	assert(state.__commonui.dialogs.source == true)
	local rerendered = commonui.feature_selector({
		key = "source",
		state = state,
		title = "Select a feature",
		placeholder = "Choose feature",
		picker_scope = "inputs",
		on_selected = function(value) selected = value end,
	})
	local dialog = rerendered.children[2]
	assert(dialog.visible == true)
	assert(dialog.picker_scope == "inputs")
	dialog.on_feature_selected({ id = 42, path = "/Health/Steps" })
	assert(selected.id == 42)
	assert(state.__commonui.dialogs.source == false)
end)

test("common UI wrappers are available through the API level gate", function()
	local api_gate = require("community.test-utils.api-gate")
	local spec = dofile("src/tng/commonui.apispec.lua")
	api_gate.reset()
	local gated = api_gate.gate(commonui, spec)

	assert(gated.selector_button({ text = "Select" }).type == ui.COMPONENT_TYPE.SURFACE)
	assert(gated.text_chip({ text = "Chip" }).type == ui.COMPONENT_TYPE.SURFACE)
	api_gate.validate(5)
end)

helpers.finish_tests()
