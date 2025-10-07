#!/usr/bin/env lua
-- pack-functions.lua
-- Packs community Lua functions into a single distributable file

local serpent = require("serpent")

-- Find all non-test lua files in a directory
local function find_lua_files(dir)
    local files = {}
    local handle = io.popen("find " .. dir .. " -type f -name '*.lua' ! -name 'test_*'")
    if not handle then
        error("Failed to scan directory: " .. dir)
    end

    for file in handle:lines() do
        table.insert(files, file)
    end
    handle:close()

    return files
end

-- Read entire file
local function read_file(path)
    local file = io.open(path, "r")
    if not file then
        error("Could not open file: " .. path)
    end
    local content = file:read("a")
    file:close()
    return content
end

-- Validate semver format (basic check)
local function is_semver(version)
    if type(version) ~= "string" then
        return false
    end
    -- Basic semver pattern: X.Y.Z with optional pre-release and build metadata
    return version:match("^%d+%.%d+%.%d+") ~= nil
end


-- Write content to file
local function write_file(path, content)
    local file = io.open(path, "w")
    if not file then
        error("Could not open output file: " .. path)
    end
    file:write(content)
    file:close()
end

-- Main function
local function main()
    local functions = {}
    local seen = {}

    -- Find all lua files
    local files = find_lua_files("src/community/functions")

    if #files == 0 then
        error("No function files found in src/community/functions")
    end

    print("Found " .. #files .. " function file(s)")

    for _, file_path in ipairs(files) do
        print("Processing: " .. file_path)

        -- Read file content
        local content = read_file(file_path)

        -- Load and execute the file to get the module
        local chunk, load_err = load(content, file_path, "t")
        if not chunk then
            error("Failed to load " .. file_path .. ": " .. load_err)
        end

        local success, module = pcall(chunk)
        if not success then
            error("Failed to execute " .. file_path .. ": " .. module)
        end

        -- Validate structure
        if type(module) ~= "table" then
            error(file_path .. " must return a table, got " .. type(module))
        end

        if type(module.id) ~= "string" then
            error(file_path .. " must have 'id' field as string, got " .. type(module.id))
        end

        if type(module.version) ~= "string" then
            error(file_path .. " must have 'version' field as string, got " .. type(module.version))
        end

        if not is_semver(module.version) then
            error(file_path .. " has invalid semver 'version': " .. tostring(module.version))
        end

        if type(module.generator) ~= "function" then
            error(file_path .. " must have 'generator' field as function, got " .. type(module.generator))
        end

        -- Check for duplicates
        local key = module.id .. "@" .. module.version
        if seen[key] then
            error("Duplicate id/version pair: " .. key .. " in " .. file_path .. " (already seen in " .. seen[key] .. ")")
        end
        seen[key] = file_path

        print("  ✓ id=" .. module.id .. " version=" .. module.version)

        -- Add to functions list
        table.insert(functions, {
            id = module.id,
            version = module.version,
            script = content
        })
    end

    -- Build output
    local output = {
        functions = functions
    }

    -- Create catalog directory if it doesn't exist
    os.execute("mkdir -p catalog")

    -- Serialize and write using Serpent
    local output_path = "catalog/community-functions.lua"
    local output_content = "return " .. serpent.block(output, {
        comment = false,      -- No comments
        sortkeys = true,      -- Sort keys for reproducibility
        compact = true,       -- Remove unnecessary spaces for smaller output
        fatal = true,         -- Raise errors on non-serializable values
        nocode = true,        -- Disable bytecode serialization for safety
        nohuge = true         -- Disable undefined/huge number checking
    })
    write_file(output_path, output_content)

    print("\n✓ Successfully packed " .. #functions .. " function(s) to " .. output_path)
end

-- Run with error handling
local success, err = pcall(main)
if not success then
    io.stderr:write("ERROR: " .. tostring(err) .. "\n")
    os.exit(1)
end
