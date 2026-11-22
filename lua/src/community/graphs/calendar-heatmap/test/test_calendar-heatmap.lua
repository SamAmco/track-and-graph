local core = require("tng.core")
local ui = require("tng.ui")

local M = {}
local portrait = { density = 2, font_scale = 1, theme = "LIGHT", layout_direction = "LTR",
    constraints = { max_width = 764, max_height = 1200 } }

local function point(year, month, day, value)
    return { timestamp = core.time({ year = year, month = month, day = day, hour = 12 }).timestamp,
        value = value, label = "Test" }
end

local function visit(node, action)
    action(node)
    if node.child then visit(node.child, action) end
    for _, child in ipairs(node.children or {}) do visit(child, action) end
end

local function matching(node, predicate)
    local found
    visit(node, function(child) if found == nil and predicate(child) then found = child end end)
    return assert(found, "No matching component")
end

M.configuration_requires_an_input = function(h)
    local screen = h:configure({ context = portrait })
    assert(screen.result.configuration_error.en == "Select a feature for the calendar heatmap")
    assert(#screen.result.feature_inputs == 0)
    screen:invoke("add_source_button", "on_click")
    assert(screen:find("add_source_dialog").visible)
    screen:invoke("add_source_dialog", "on_feature_selected", { id = 42, path = "/Health/Steps" })
    assert(screen.result.configuration_error == nil)
    assert(screen.result.feature_inputs[1].feature_id == 42)
end

M.configuration_callbacks_and_json_round_trip = function(h)
    local screen = h:configure({ context = portrait,
        feature_map = { source = { id = 42, path = "/Health/Steps" } } })
    screen:invoke("source_feature_1_dialog", "on_feature_selected", { id = 99, path = "/Health/Sleep" })
    screen:invoke("source_color_1_color_7", "on_click")
    screen:invoke(matching(screen.ui, function(node)
        return node.type == "DROPDOWN_MENU_ITEM" and type(node.child.text) == "table"
            and node.child.text.en == "Range"
    end), "on_click")
    -- LuaK currently parses a lone minus as zero, unlike standard Lua. Use an
    -- unambiguously nonnumeric value here; that engine discrepancy is separate.
    screen:invoke("minimum", "on_value_change", ui.text_field_value("invalid"))
    assert(screen.result.configuration_error ~= nil, "Nonnumeric range is invalid")
    screen:invoke("minimum", "on_value_change", ui.text_field_value("-3.1253"))
    screen:invoke("maximum", "on_value_change", ui.text_field_value("234.12"))
    screen:invoke("show_legend_false", "on_click")
    assert(screen.result.configuration_error == nil, "Complete range is valid")
    local saved = screen:save()
    assert(type(saved.sources[1].minimum.text) == "string", "Numeric text persists as text")
    local reopened = h:configure({ context = portrait, state = saved })
    assert(reopened.state.sources[1].color == 7, "Saved source colour")
    assert(reopened.result.feature_inputs[1].feature_id == 99, "Saved feature selection")
    assert(reopened.state.sources[1].minimum.text == "-3.1253", "Saved range minimum")
    assert(type(reopened.state.sources[1].minimum.mutate) == "function", "Restored text-field methods")
    assert(reopened.state.show_legend == false, "Saved legend preference")
    reopened:invoke("add_source_button", "on_click")
    reopened:invoke("add_source_dialog", "on_feature_selected", { id = 123, path = "/Mood" })
    local delete = matching(reopened:find("source_1"), function(node) return node.type == "ICON_BUTTON" end)
    reopened:invoke(delete, "on_click")
    assert(#reopened.result.feature_inputs == 1 and reopened.result.feature_inputs[1].feature_id == 123,
        "Deleting the first source preserves the second")
    assert(reopened:find("source_2") ~= nil, "Source keys remain stable")
end

M.sampling_controls_support_cancellation_and_selected_dates = function(h)
    local screen = h:configure({ context = portrait })
    screen:invoke("ending_at_NOW", "on_click")
    screen:invoke("ending_at_CUSTOM", "on_click")
    assert(screen:find("ending_at_date").visible)
    screen:invoke("ending_at_date", "on_dismiss_request")
    assert(screen.state.sample_ending_at == "NOW")
    screen:invoke("ending_at_CUSTOM", "on_click")
    local dialog = screen:find("ending_at_date")
    -- Native date selection and dismissal run in order before the next root render.
    h:call(dialog.on_date_selected, { year = 2024, month = 1, day = 3 })
    h:call(dialog.on_dismiss_request)
    screen:render()
    assert(screen.state.sample_ending_at == "CUSTOM" and not screen.state.ending_at_date_visible)
    assert(screen:find("ending_at_button").child.children[1].text == "3 Jan 2024")
end

M.preparation_uses_binary_nonzero_and_signed_range_sums = function(h)
    local prepared = h:prepare({
        sources = {
            source_1 = { point(2026, 11, 14, 5), point(2026, 11, 14, -5) },
            source_2 = { point(2026, 11, 14, -2), point(2026, 11, 14, 3) },
        },
        state = { sources = {
            { color = 0, mode = "BINARY" },
            { color = 7, mode = "RANGE", minimum = { text = "-3" }, maximum = { text = "5" } },
        } },
    })
    assert(prepared.data.sources[1].values["2026-11-14"] == 1)
    assert(prepared.data.sources[2].values["2026-11-14"] == 0.5)
    assert(prepared.data.sources[2].color == 7)
end

M.sampling_endpoints_filter_future_data_and_keep_all_history = function(h)
    local sources = { source_1 = {
        point(2026, 12, 1, 1), point(2026, 11, 14, 1), point(2024, 1, 1, 0),
    } }
    local latest = h:prepare({ sources = sources })
    assert(latest.data.sources[1].values["2026-12-01"] == 1)
    assert(latest.data.years[1].months[#latest.data.years[1].months].month == 12)
    local now = h:prepare({ sources = sources, state = { sample_ending_at = "NOW" } })
    assert(now.data.sources[1].values["2026-12-01"] == nil)
    assert(#now.data.years == 3 and now.data.years[3].year == 2024)
    local custom = h:prepare({ sources = sources, state = {
        sample_ending_at = "CUSTOM", sample_end_date = { year = 2026, month = 11, day = 13 },
    } })
    assert(custom.data.sources[1].values["2026-11-14"] == nil)
    assert(custom.data.sources[1].values["2024-01-01"] == 0)
end

M.list_mode_records_fixed_tiles_and_optional_legend = function(h)
    local graph = h:prepare({ sources = { source_1 = { point(2026, 11, 14, 1) } },
        state = { show_legend = false } }):render({ context = portrait, view_mode = "LIST" })
    assert(#graph.ui.children == 1)
    local recording = graph:draw("calendar", { bounds = { width = 700, height = 275 }, density = 2 })
    local blanks = {}
    for _, instruction in ipairs(recording.instructions) do
        if instruction.type == "RECT" and instruction.color == "THEME_DATA_VIS_BLANK" then
            blanks[#blanks + 1] = instruction
            assert(instruction.size.width == 28 and instruction.size.height == 28)
        end
    end
    assert(#blanks > 7)
    assert(blanks[2].top_left.y - blanks[1].top_left.y == 29)
    assert(blanks[8].top_left.x - blanks[1].top_left.x == 29)
end

M.full_screen_packs_whole_months_and_centres_year_sections = function(h)
    local graph = h:prepare({ sources = { source_1 = {
        point(2026, 11, 14, 1), point(2024, 1, 1, 1),
    } } }):render({ context = portrait, view_mode = "FULL_SCREEN" })
    local years = graph.ui.children[1].children[1].children
    assert(#years == 3 and years[1].children[1].text == "2026" and years[3].children[1].text == "2024")
    assert(graph.ui.children[1].arrangement == "CENTER")
    local weekday_count, blank_count, boundary_count, canvas_count = 0, 0, 0, 0
    local weekdays = { Mon = true, Tue = true, Wed = true, Thu = true, Fri = true, Sat = true, Sun = true }
    visit(graph.ui, function(node)
        if node.type ~= "CANVAS" then return end
        canvas_count = canvas_count + 1
        local recording = graph:draw(node.key, { bounds = { width = 700, height = 250 }, density = 2 })
        local rows = {}
        for _, instruction in ipairs(recording.instructions) do
            if instruction.type == "TEXT" and weekdays[instruction.text] then weekday_count = weekday_count + 1 end
            if instruction.type == "RECT" and instruction.color == "THEME_DATA_VIS_BLANK" then
                blank_count = blank_count + 1
                local x, y = instruction.top_left.x, instruction.top_left.y
                assert(x >= 0 and x + 28 <= 700 and y >= 48 and y + 28 <= 250)
                rows[y] = rows[y] or {}
                rows[y][#rows[y] + 1] = x
            end
        end
        for _, row in pairs(rows) do
            table.sort(row)
            for index = 2, #row do
                local gap = row[index] - row[index - 1] - 28
                assert(gap == 1 or gap == 5)
                if gap == 5 then boundary_count = boundary_count + 1 end
            end
        end
    end)
    assert(weekday_count == 7 and blank_count == 366 + 365 + 334 and boundary_count > 0)
    local wide = { density = 2, font_scale = 1, theme = "DARK", constraints = { max_width = 1600, max_height = 700 } }
    graph:render({ context = wide, view_mode = "FULL_SCREEN" })
    local wide_count = 0
    visit(graph.ui, function(node) if node.type == "CANVAS" then wide_count = wide_count + 1 end end)
    assert(wide_count < canvas_count)
end

return M
