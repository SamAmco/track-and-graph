#!/usr/bin/env lua
-- publish-functions-debug.lua
-- Signs the community functions catalog and publishes to debug assets

local signing = require("tools.lib.signing")
local file_traversal = require("tools.lib.file-traversal")

-- Configuration
local CATALOG_PATH = "catalog/community-functions.lua"
local DEBUG_CATALOG_DIR = "../app/src/debug/assets/functions-catalog"

-- Main function
local function main()
	print("Publishing functions catalog for debug...\n")

	-- Phase 1: Check catalog exists
	print("==> Phase 1: Check Catalog")
	if not file_traversal.file_exists(CATALOG_PATH) then
		error("Catalog not found at " .. CATALOG_PATH .. ". Run pack-functions.lua first.")
	end
	print("✓ Found catalog: " .. CATALOG_PATH)

	-- Phase 2: Check debug catalog directory exists
	print("\n==> Phase 2: Check Debug Assets Directory")
	if not file_traversal.file_exists(DEBUG_CATALOG_DIR) then
		error("Debug catalog directory not found: " .. DEBUG_CATALOG_DIR)
	end
	print("✓ Found directory: " .. DEBUG_CATALOG_DIR)

	-- Phase 3: Generate ephemeral keypair
	print("\n==> Phase 3: Generate Ephemeral Keypair")
	local timestamp = os.date("!%Y%m%dT%H%M%SZ")
	local key_id = "debug-" .. timestamp

	local private_key, public_key = signing.generate_ephemeral_keypair()
	print("✓ Generated ephemeral keypair")

	-- Phase 4: Sign catalog
	print("\n==> Phase 4: Sign Catalog")
	local signature_file = os.tmpname()
	signing.sign_catalog(CATALOG_PATH, private_key, signature_file)
	print("✓ Signed catalog")

	-- Verify signature
	if not signing.verify_signature(CATALOG_PATH, public_key, signature_file) then
		os.remove(private_key)
		os.remove(public_key)
		os.remove(signature_file)
		error("Signature verification failed")
	end
	print("✓ Verified signature")

	-- Phase 5: Create signature JSON
	print("\n==> Phase 5: Create Signature JSON")
	local signature_bytes = file_traversal.read_file(signature_file)

	local json = signing.create_signature_json(key_id, signature_bytes)
	local json_path = os.tmpname()
	file_traversal.write_file(json_path, json)
	print("✓ Created signature JSON (Key ID: " .. key_id .. ")")

	-- Phase 6: Copy to debug assets
	print("\n==> Phase 6: Copy to Debug Assets")
	signing.run("rm -rf " .. DEBUG_CATALOG_DIR .. "/*", "Failed to clean catalog directory")
	print("✓ Cleaned catalog directory")

	local catalog_dest = DEBUG_CATALOG_DIR .. "/community-functions.lua"
	signing.run("cp " .. CATALOG_PATH .. " " .. catalog_dest, "Failed to copy catalog")
	print("✓ Copied catalog to " .. catalog_dest)

	local json_dest = DEBUG_CATALOG_DIR .. "/community-functions.sig.json"
	signing.run("cp " .. json_path .. " " .. json_dest, "Failed to copy signature JSON")
	print("✓ Copied signature JSON to " .. json_dest)

	local public_key_dest = DEBUG_CATALOG_DIR .. "/" .. key_id .. ".pub"
	signing.run("cp " .. public_key .. " " .. public_key_dest, "Failed to copy public key")
	print("✓ Copied public key to " .. public_key_dest)

	-- Phase 7: Cleanup temp files
	print("\n==> Phase 7: Cleanup")
	os.remove(private_key)
	os.remove(public_key)
	os.remove(signature_file)
	os.remove(json_path)
	print("✓ Removed temporary files")

	print("\n✓ Successfully published debug catalog with signature")
	print("  Catalog: " .. catalog_dest)
	print("  Signature: " .. json_dest)
	print("  Public Key: " .. public_key_dest)
	print("  Key ID: " .. key_id)
end

-- Run with error handling
local success, err = pcall(main)
if not success then
	io.stderr:write("\nERROR: " .. tostring(err) .. "\n")
	os.exit(1)
end
