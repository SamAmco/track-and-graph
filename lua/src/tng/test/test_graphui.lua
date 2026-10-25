#!/usr/bin/env lua

package.path = package.path .. ";src/?.lua;src/?/init.lua"

local core = require("tng.core")
local graphui = require("tng.graphui")
local theme = require("tng.theme")
local ui = require("tng.ui")
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test

test("legend item matches the standard graph legend layout", function()
	local item = graphui.legend_item({ key = "steps", color = core.COLOR.BLUE, label = "Steps" })

	assert(item.type == ui.COMPONENT_TYPE.ROW)
	assert(item.key == "steps")
	assert(item.alignment == ui.ALIGNMENT.CENTER)
	assert(#item.children == 3)
	local circle = item.children[1]
	assert(circle.type == ui.COMPONENT_TYPE.SPACER)
	assert(circle.modifier[1].value == theme.spacing.large)
	assert(circle.modifier[2].value == theme.spacing.large)
	assert(circle.modifier[3].color == core.COLOR.BLUE)
	assert(circle.modifier[3].shape == theme.shape.circle)
	assert(item.children[2].modifier[1].value.value == 2)
	assert(item.children[3].text == "Steps")
	assert(item.children[3].style == theme.text_style.body_medium)
end)

test("legend is full width centered and separates every item", function()
	local custom_modifier = ui.padding(ui.dp(4))
	local legend = graphui.legend({
		key = "legend",
		modifier = ui.modifier(custom_modifier),
		items = {
			{ color = core.COLOR.RED, label = "Red" },
			{ color = "#00ff00", label = "Green" },
		},
	})

	assert(legend.type == ui.COMPONENT_TYPE.FLOW_ROW)
	assert(legend.key == "legend")
	assert(legend.horizontal_arrangement == ui.ARRANGEMENT.CENTER)
	assert(legend.modifier[1].type == ui.MODIFIER_TYPE.FILL_WIDTH)
	assert(legend.modifier[2] == custom_modifier)
	assert(#legend.children == 4)
	assert(legend.children[1].children[3].text == "Red")
	assert(legend.children[2].modifier[1].value == theme.spacing.medium)
	assert(legend.children[2].modifier[2].value == theme.spacing.medium)
	assert(legend.children[3].children[3].text == "Green")
end)

test("colored feature input card matches the compact graph input layout", function()
	local selected
	local color
	local deleted = false
	local card = graphui.colored_feature_input_card({
		key = "steps",
		state = {},
		title = "Select a feature",
		placeholder = "Choose feature",
		selected = { id = 42, path = "/Health/Steps" },
		selected_color = core.COLOR.BLUE,
		picker_scope = "features",
		on_selected = function(value) selected = value end,
		on_color_selected = function(value) color = value end,
		on_delete = function() deleted = true end,
		delete_content_description = { en = "Remove feature" },
	})

	assert(card.type == ui.COMPONENT_TYPE.SURFACE)
	assert(card.color == theme.color.surface)
	assert(card.content_color == theme.color.on_surface)
	assert(card.shape == theme.shape.medium)
	assert(card.shadow_elevation.value == 4)
	assert(card.modifier[1].type == ui.MODIFIER_TYPE.FILL_WIDTH)
	local row = card.child
	assert(row.type == ui.COMPONENT_TYPE.ROW)
	assert(row.alignment == ui.ALIGNMENT.CENTER)
	assert(row.spacing == theme.spacing.small)
	assert(row.modifier[2].all == theme.spacing.medium)
	assert(row.children[1].type == ui.COMPONENT_TYPE.BOX)
	assert(row.children[2].key == "steps_feature")
	assert(row.children[2].children[1].child.children[1].text == "/Health/Steps")
	local delete_button = row.children[3]
	assert(delete_button.type == ui.COMPONENT_TYPE.ICON_BUTTON)
	assert(delete_button.child.icon == "ICON_DELETE")
	assert(delete_button.child.content_description.en == "Remove feature")
	delete_button.on_click()
	assert(deleted)

	row.children[1].children[2].children[8].on_click()
	assert(color == 7)
	row.children[2].children[2].on_feature_selected({ id = 9, path = "/Mood" })
	assert(selected.id == 9)
end)

test("graph UI wrappers are available through the API level gate", function()
	local api_gate = require("community.test-utils.api-gate")
	local spec = dofile("src/tng/graphui.apispec.lua")
	api_gate.reset()
	local gated = api_gate.gate(graphui, spec)

	assert(gated.legend({ items = {} }).type == ui.COMPONENT_TYPE.FLOW_ROW)
	assert(gated.colored_feature_input_card({
		key = "source",
		state = {},
		title = "Select",
		placeholder = "Choose",
		selected_color = 0,
		on_selected = function() end,
		on_color_selected = function() end,
		on_delete = function() end,
		delete_content_description = "Remove",
	}).type == ui.COMPONENT_TYPE.SURFACE)
	api_gate.validate(5)
end)

helpers.finish_tests()
