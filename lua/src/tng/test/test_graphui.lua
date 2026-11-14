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

test("graph UI wrappers are available through the API level gate", function()
	local api_gate = require("community.test-utils.api-gate")
	local spec = dofile("src/tng/graphui.apispec.lua")
	api_gate.reset()
	local gated = api_gate.gate(graphui, spec)

	assert(gated.legend({ items = {} }).type == ui.COMPONENT_TYPE.FLOW_ROW)
	api_gate.validate(5)
end)

helpers.finish_tests()
