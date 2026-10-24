#!/usr/bin/env lua

package.path = package.path .. ";src/?.lua;src/?/init.lua"

local theme = require("tng.theme")
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test

test("colors, text styles, spacing, and shapes are serializable tokens", function()
	assert(theme.color.selector_button == "THEME_SELECTOR_BUTTON")
	assert(theme.color.hyperlink == "THEME_HYPERLINK")
	assert(theme.color.good == "THEME_GOOD")
	assert(theme.text_style.display_large == "TEXT_STYLE_DISPLAY_LARGE")
	assert(theme.text_style.body_medium == "TEXT_STYLE_BODY_MEDIUM")
	assert(theme.text_style.label_small == "TEXT_STYLE_LABEL_SMALL")
	assert(theme.text_style.code == "TEXT_STYLE_CODE")
	assert(theme.spacing.medium == "SPACING_MEDIUM")
	assert(theme.shape.small == "SHAPE_SMALL")
	assert(theme.shape.circle == "SHAPE_CIRCLE")
end)

test("nested theme tokens are available through the API level gate", function()
	local api_gate = require("community.test-utils.api-gate")
	local spec = dofile("src/tng/theme.apispec.lua")
	api_gate.reset()
	local gated = api_gate.gate(theme, spec)

	assert(gated.color.good == "THEME_GOOD")
	assert(gated.text_style.body_medium == "TEXT_STYLE_BODY_MEDIUM")
	assert(gated.text_style.code == "TEXT_STYLE_CODE")
	assert(gated.spacing.medium == "SPACING_MEDIUM")
	assert(gated.shape.medium == "SHAPE_MEDIUM")
	api_gate.validate(5)
end)

helpers.finish_tests()
