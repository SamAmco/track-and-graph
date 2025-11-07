-- catalog-encoding.lua
-- Encodes catalog data structures into Lua code

local M = {}

-- Returns a valid Lua long string literal containing 's'
local function encode_lua_script_as_string(s)
    local eq = 0
    -- find the longest existing ]=...=] pattern
    for brackets in s:gmatch("%[(=*)%[") do
        eq = math.max(eq, #brackets)
    end
    for brackets in s:gmatch("%](=*)%]") do
        eq = math.max(eq, #brackets)
    end
    local eqs = string.rep("=", eq + 1)
    return string.format("[%s[\n%s]%s]", eqs, s, eqs)
end

-- Escape a regular string for use in quotes
local function escape_string(s)
    return s:gsub("\\", "\\\\"):gsub("\"", "\\\""):gsub("\n", "\\n"):gsub("\r", "\\r"):gsub("\t", "\\t")
end

-- Forward declaration
local function encode_table(tbl, indent) end

-- Encode a single value as Lua code
local function encode_value(value, indent)
    indent = indent or ""

    if type(value) == "string" then
        -- For script fields that contain Lua code, use long string literals
        if value:find("\n") and value:find("return") then
            return encode_lua_script_as_string(value)
        else
            return "\"" .. escape_string(value) .. "\""
        end
    elseif type(value) == "number" then
        return tostring(value)
    elseif type(value) == "boolean" then
        return tostring(value)
    elseif type(value) == "nil" then
        return "nil"
    elseif type(value) == "table" then
        return encode_table(value, indent)
    else
        error("Unsupported value type: " .. type(value))
    end
end

-- Encode a table as Lua code
encode_table = function(tbl, indent)
    indent = indent or ""
    local next_indent = indent .. "\t"

    -- Check if it's an array or associative table
    local is_array = true
    local max_index = 0
    local count = 0

    for k, v in pairs(tbl) do
        count = count + 1
        if type(k) ~= "number" or k <= 0 or k ~= math.floor(k) then
            is_array = false
            break
        end
        max_index = math.max(max_index, k)
    end

    if is_array and count == max_index then
        -- Array format
        local parts = { "{\n" }
        for i = 1, max_index do
            table.insert(parts, next_indent)
            table.insert(parts, encode_value(tbl[i], next_indent))
            table.insert(parts, ",\n")
        end
        table.insert(parts, indent .. "}")
        return table.concat(parts)
    else
        -- Associative table format
        local parts = { "{\n" }

        -- Sort keys for consistent output
        local keys = {}
        for k in pairs(tbl) do
            table.insert(keys, k)
        end
        table.sort(keys, function(a, b)
            -- Sort strings before numbers, then alphabetically/numerically
            if type(a) == type(b) then
                return a < b
            elseif type(a) == "string" then
                return true
            else
                return false
            end
        end)

        for _, k in ipairs(keys) do
            local v = tbl[k]
            table.insert(parts, next_indent)

            if type(k) == "string" and k:match("^[a-zA-Z_][a-zA-Z0-9_]*$") then
                -- Valid identifier, use dot notation
                table.insert(parts, k)
            else
                -- Need bracket notation
                table.insert(parts, "[" .. encode_value(k, next_indent) .. "]")
            end

            table.insert(parts, "=")
            table.insert(parts, encode_value(v, next_indent))
            table.insert(parts, ",\n")
        end

        table.insert(parts, indent .. "}")
        return table.concat(parts)
    end
end

-- Main function to encode a catalog as Lua code
function M.encode_catalog(catalog)
    return "return " .. encode_table(catalog, "")
end

return M
