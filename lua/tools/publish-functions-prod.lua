#!/usr/bin/env lua
-- publish-functions-prod.lua
-- Signs the community functions catalog for production with password-protected private key

local signing = require("tools.lib.signing")
local file_traversal = require("tools.lib.file-traversal")

-- Configuration
local CATALOG_PATH = "catalog/community-functions.lua"
local SIGNATURE_OUTPUT = "catalog/community-functions.sig.json"
local SIGNATURE_BYTES = "catalog/community-functions.sig"
local PRIVATE_KEY_PATH_FILE = ".private-key-path"
local PUBLIC_KEY_PATH = "../app/src/main/assets/functions-catalog/production.pub"
local KEY_ID = "prod-key-1"

-- Main function
local function main()
	print("Publishing functions catalog for production...\n")

	-- Phase 1: Check catalog exists
	print("==> Phase 1: Check Catalog")
	if not file_traversal.file_exists(CATALOG_PATH) then
		error("Catalog not found at " .. CATALOG_PATH .. ". Run pack-functions.lua first.")
	end
	print("✓ Found catalog: " .. CATALOG_PATH)

	-- Phase 2: Locate private key
	print("\n==> Phase 2: Locate Private Key")
	if not file_traversal.file_exists(PRIVATE_KEY_PATH_FILE) then
		error(string.format(
			"Private key path file not found.\n\n"
				.. "Expected file: %s\n"
				.. "This file should contain the absolute path to your password-protected private key.\n",
			PRIVATE_KEY_PATH_FILE
		))
	end

	local private_key_path_content = file_traversal.read_file(PRIVATE_KEY_PATH_FILE)
	local private_key_path = private_key_path_content:match("^%s*(.-)%s*$")

	if not file_traversal.file_exists(private_key_path) then
		error(string.format(
			"Private key file not found at path specified in %s\n" .. "Path from file: %s\n",
			PRIVATE_KEY_PATH_FILE,
			private_key_path
		))
	end
	print("✓ Found private key: " .. private_key_path)

	-- Phase 3: Check public key in assets
	print("\n==> Phase 3: Check Public Key")
	if not file_traversal.file_exists(PUBLIC_KEY_PATH) then
		error(string.format(
			"Public key not found in app assets.\n" .. "Expected location: %s\n",
			PUBLIC_KEY_PATH
		))
	end
	print("✓ Found public key: " .. PUBLIC_KEY_PATH)

	-- Phase 4: Sign catalog (OpenSSL will prompt for password)
	print("\n==> Phase 4: Sign Catalog")
	signing.sign_catalog(CATALOG_PATH, private_key_path, SIGNATURE_BYTES)
	print("✓ Signed catalog")

	-- Phase 5: Create signature JSON
	print("\n==> Phase 5: Create Signature JSON")
	local signature_bytes = file_traversal.read_file(SIGNATURE_BYTES)
	local json = signing.create_signature_json(KEY_ID, signature_bytes)
	file_traversal.write_file(SIGNATURE_OUTPUT, json)
	print("✓ Created signature JSON: " .. SIGNATURE_OUTPUT)

	-- Phase 6: Verify signature
	print("\n==> Phase 6: Verify Signature")
	if not signing.verify_signature(CATALOG_PATH, PUBLIC_KEY_PATH, SIGNATURE_BYTES) then
		error("Signature verification failed! Public key may not match private key.")
	end
	print("✓ Verified signature")

	-- Phase 7: Cleanup signature bytes file
	print("\n==> Phase 7: Cleanup")
	os.remove(SIGNATURE_BYTES)
	print("✓ Removed temporary signature file")

	print("\n✓ Successfully published production catalog with signature")
	print("  Catalog: " .. CATALOG_PATH)
	print("  Signature: " .. SIGNATURE_OUTPUT)
	print("  Public Key: " .. PUBLIC_KEY_PATH)
	print("  Key ID: " .. KEY_ID)
	print("\nNOTE: Commit both the catalog and signature files together.")
end

-- Run with error handling
local success, err = pcall(main)
if not success then
	io.stderr:write("\nERROR: " .. tostring(err) .. "\n")
	os.exit(1)
end
