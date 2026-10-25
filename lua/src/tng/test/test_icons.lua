#!/usr/bin/env lua

package.path = package.path .. ";src/?.lua;src/?/init.lua"

local icons = require("tng.icons")
local helpers = require("src.tng.test.test_helpers")
local test = helpers.test

test("common app icons have stable semantic tokens", function()
	assert(icons.ICON.ADD == "ICON_ADD")
	assert(icons.ICON.ADD_RECORD == "ICON_ADD_RECORD")
	assert(icons.ICON.DELETE == "ICON_DELETE")
	assert(icons.ICON.MORE == "ICON_MORE")
	assert(icons.ICON.TRACKER == "ICON_TRACKER")
	assert(icons.ICON.VISIBILITY_OFF == "ICON_VISIBILITY_OFF")
end)

test("icon tokens are available through the API level gate", function()
	local api_gate = require("community.test-utils.api-gate")
	local spec = dofile("src/tng/icons.apispec.lua")
	api_gate.reset()
	local gated = api_gate.gate(icons, spec)

	assert(gated.ICON.DELETE == "ICON_DELETE")
	assert(gated.ICON.GRAPH == "ICON_GRAPH")
	assert(gated.ICON.WARNING == "ICON_WARNING")
	api_gate.validate(5)
end)

helpers.finish_tests()
