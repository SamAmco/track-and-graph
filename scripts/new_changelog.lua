#!/usr/bin/env lua
--[[
Create a new changelog file for the current release version.
Extracts version from app/build.gradle.kts, creates a changelog file
in fastlane/metadata/android/en-GB/changelogs/, populates it with
git log since the last release, and opens it in neovim.
]]

--[[
 New requirements:
 1. Create a new sub-dir under changelogs for the current version e.g. changelogs/1.2.3/
 2. Create a template lua file that will be opened in nvim that contains
   - A lua data structure with:
         - keys for localisations (en-GB, fr-FR, de-DE, es-ES) and values representing the changelog md text
         - A boolean for publish that tells the script whether to publish the changelog to the changelogs dir or not
   - Each commit under this structure in comments
 3. When the file is written, read it with a lua interpreter to extract the changelog text for each localisation
    and write them to the appropriate changelog files under the new version sub-dir.
 4. Update the index file which is a Java Properties style file mapping version codes to locations in the changelogs dir.
--]]

local function get_versions_from_gradle()
	local gradle_file = "app/build.gradle.kts"
	local file = io.open(gradle_file, "r")
	if not file then
		print("Error: Could not open " .. gradle_file)
		os.exit(1)
	end
	local content = file:read("*a")
	file:close()

	local version_code = content:match("versionCode%s*=%s*(%d+)")
	if not version_code then
		print("Error: Could not find versionCode in app/build.gradle.kts")
		os.exit(1)
	end

	local version_name = content:match('versionName%s*=%s*"(.-)"')
	if not version_name then
		print("Error: Could not find versionName in app/build.gradle.kts")
		os.exit(1)
	end

	return version_code, version_name
end

local function find_previous_tag()
	local handle = io.popen("git tag") or error("Failed to run git tag")
	local result = handle:read("*a")
	handle:close()
	local tags = {}
	for tag in result:gmatch("[^\n]+") do
		if tag:match("^v") or tag:match("^rc%-v") then
			table.insert(tags, tag)
		end
	end
	if #tags == 0 then
		return nil
	end
	table.sort(tags, function(a, b)
		local function version_tuple(tag)
			local v = tag:gsub("^rc%-v", ""):gsub("^v", "")
			local t = {}
			for num in v:gmatch("%d+") do
				table.insert(t, tonumber(num))
			end
			return t
		end
		local ta, tb = version_tuple(a), version_tuple(b)
		for i = 1, math.max(#ta, #tb) do
			local va, vb = ta[i] or 0, tb[i] or 0
			if va ~= vb then
				return va > vb
			end
		end
		return false
	end)
	return tags[1]
end

local function get_git_log_since_tag(tag)
	if not tag then
		print("Error: Can not determine previous tag")
		os.exit(1)
	end
	local cmd = string.format("git log --oneline %s..HEAD", tag)
	local handle = io.popen(cmd) or error("Failed to run git log command")
	local log_output = handle:read("*a")
	handle:close()
	log_output = log_output:gsub("^%s+", ""):gsub("%s+$", "")
	if log_output == "" then
		print("No commits since last tag, showing diff instead...")
		local diff_handle = io.popen("git diff HEAD") or error("Failed to run git diff command")
		local diff_output = diff_handle:read("*a")
		diff_handle:close()
		return diff_output:gsub("^%s+", ""):gsub("%s+$", "")
	end
	return log_output
end

local function ensure_dir(dir)
	local test = io.open(dir, "r")
	if test then
		test:close()
		return
	end
	os.execute("mkdir -p " .. dir)
end

local function create_temp_changelog_file(git_log)
	local temp_file = os.tmpname()
	local initial_content = [=[
return {
  publish = false,
  changelogs = {
    {
    	regional = "en-GB",
    	general = "en",
    	text = [[

			]]
    },
    {
    	regional = "es-ES",
    	general = "es",
    	text = [[

			]]
    },
    {
    	regional = "fr-FR",
    	general = "fr",
    	text = [[

			]]
    },
    {
    	regional = "de-DE",
    	general = "de",
    	text = [[

			]]
    },
  }
}

]=]
	-- Add git log as comments
	for line in git_log:gmatch("[^\r\n]+") do
		initial_content = initial_content .. string.format("-- %s\n", line)
	end

	local file = io.open(temp_file, "w") or error("Could not create temp changelog file")
	file:write(initial_content)
	file:close()
	return temp_file, initial_content
end

local function open_in_neovim(file_path, initial_content)
	os.execute("nvim " .. file_path)

	local file = io.open(file_path, "r")
	if not file then
		print("Error: Could not open " .. file_path .. " after editing")
		os.exit(1)
	end
	local content = file:read("*a")
	file:close()

	return content ~= initial_content
end

local function publish_to_fastlane(version_code, changelogs)
	local base_dir = "fastlane/metadata/android/"

	for _, changelog in pairs(changelogs) do
		local locale = changelog.regional
		local changelog_dir = base_dir .. locale .. "/changelogs/"
		ensure_dir(changelog_dir)

		local changelog_file = changelog_dir .. version_code .. ".txt"
		local file = io.open(changelog_file, "w") or error("Could not open " .. changelog_file .. " for writing")
		file:write(changelog.text)
		file:close()
		print("Wrote changelog for " .. locale .. " to " .. changelog_file)
	end
end

local function publish_to_public_changelogs(version_name, changelogs)
	print("\nCreating changelog directory for version " .. version_name .. "...")
	local base_dir = "changelogs/" .. version_name
	ensure_dir(base_dir)

	for _, changelog in pairs(changelogs) do
		local locale = changelog.general
		local changelog_file = base_dir .. "/" .. locale .. ".md"
		local file = io.open(changelog_file, "w") or error("Could not open " .. changelog_file .. " for writing")
		file:write(changelog.text)
		file:close()
		print("Wrote public changelog for " .. locale .. " to " .. changelog_file)
	end

	-- Update index file
	local index_file = "changelogs/index.properties"
	local file = io.open(index_file, "a") or error("Could not open " .. index_file .. " for appending")
	file:write(string.format("%s=changelogs/%s/\n", version_name, version_name))
	file:close()
end

local function main()
	print("Extracting version code from app/build.gradle.kts...")
	local version_code, version_name = get_versions_from_gradle()
	print("Current version code: " .. version_code .. ", version name: " .. version_name)

	print("\nFinding previous release tag...")
	local previous_tag = find_previous_tag()
	if previous_tag then
		print("Previous tag: " .. previous_tag)
	else
		print("No previous tag found, will show all commits")
	end

	print("\nGenerating git log...")
	local git_log = get_git_log_since_tag(previous_tag)

	print("\nCreating temp changelog file...")
	local temp_changelog_file, initial_content = create_temp_changelog_file(git_log)
	print("Created: " .. temp_changelog_file)

	print("\nOpening in neovim...")
	local made_changes = open_in_neovim(temp_changelog_file, initial_content)

	if not made_changes then
		print("No changes made to changelog, exiting.")
		os.remove(temp_changelog_file)
		return
	end

	print("\nProcessing changelog file...")
	local changelog_env = {}
	local chunk, err = loadfile(temp_changelog_file, "t", changelog_env)
	if not chunk then
		print("Error loading changelog file: " .. err)
		os.remove(temp_changelog_file)
		os.exit(1)
	end

	local changelog_meta = chunk()

	print("\nPublishing changelogs...")
	publish_to_fastlane(version_code, changelog_meta.changelogs)

	if changelog_meta.publish then
		publish_to_public_changelogs(version_name, changelog_meta.changelogs)
	else
		print("Publish flag is false, skipping public changelogs.")
	end

	print("\nCleaning up temporary file...")
	os.remove(temp_changelog_file)
	print("✓ Completed successfully")
end

main()
