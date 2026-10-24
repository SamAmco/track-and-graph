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

test("text accepts translated strings", function()
	local translations = { en = "Calendar", fr = "Calendrier" }
	local text = ui.text({ text = translations })
	assert(text.text == translations)
end)

test("nested UI constants are available through the API level gate", function()
	local api_gate = require("community.test-utils.api-gate")
	local spec = dofile("src/tng/ui.apispec.lua")
	api_gate.reset()
	local gated = api_gate.gate(ui, spec)

	assert(gated.BOX_ALIGNMENT.BOTTOM_END == "BOTTOM_END")
	assert(gated.COMPONENT_TYPE.FLOW_ROW == "FLOW_ROW")
	assert(gated.MODIFIER_TYPE.FILL_MAX_ROW_HEIGHT == "FILL_MAX_ROW_HEIGHT")
	assert(gated.fill_max_row_height(0.5).fraction == 0.5)
	assert(gated.flow_row({}).type == "FLOW_ROW")
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
