#!/usr/bin/env lua
-- publish-functions-debug.lua
-- Signs the community functions catalog and publishes to debug assets

-- Configuration
local CATALOG_PATH = "catalog/community-functions.lua"
local DEBUG_CATALOG_DIR = "../app/src/debug/assets/functions-catalog"
local TMP_DIR = "tmp"

-- Read entire file
local function read_file(path)
	local file = io.open(path, "rb")
	if not file then
		error("Could not open file: " .. path)
	end
	local content = file:read("*all")
	file:close()
	return content
end

-- Write content to file
local function write_file(path, content)
	local file = io.open(path, "wb")
	if not file then
		error("Could not open output file: " .. path)
	end
	file:write(content)
	file:close()
end

-- Base64 encode bytes
local function base64_encode(data)
	-- Use openssl to base64 encode
	local tmp_in = TMP_DIR .. "/encode_input"
	local tmp_out = TMP_DIR .. "/encode_output"

	write_file(tmp_in, data)
	local cmd = string.format("openssl base64 -in %s -out %s", tmp_in, tmp_out)
	local result = os.execute(cmd)

	if not result then
		error("Failed to base64 encode")
	end

	local encoded = read_file(tmp_out)
	-- Remove newlines from openssl output
	encoded = encoded:gsub("\n", "")

	os.remove(tmp_in)
	os.remove(tmp_out)

	return encoded
end

-- Run shell command and check success
local function run(cmd, error_msg)
	local result = os.execute(cmd)
	if not result then
		error(error_msg or "Command failed: " .. cmd)
	end
end

-- Main function
local function main()
	print("Publishing functions catalog for debug...\n")

	-- Check catalog exists
	local catalog_file = io.open(CATALOG_PATH, "r")
	if not catalog_file then
		error("Catalog not found at " .. CATALOG_PATH .. ". Run pack-functions.lua first.")
	end
	catalog_file:close()

	-- Check debug catalog directory exists
	local catalog_dir_check = io.open(DEBUG_CATALOG_DIR, "r")
	if not catalog_dir_check then
		error("Debug catalog directory not found: " .. DEBUG_CATALOG_DIR)
	end
	catalog_dir_check:close()

	print("==> Phase 1: Setup")
	-- Create and clean tmp directory
	os.execute("rm -rf " .. TMP_DIR)
	os.execute("mkdir -p " .. TMP_DIR)
	print("Created temporary directory: " .. TMP_DIR)

	print("\n==> Phase 2: Generate Keypair")
	local private_key = TMP_DIR .. "/private.key"
	local public_key = TMP_DIR .. "/public.pub"

	-- Generate ECDSA P-256 private key
	run(
		"openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -out " .. private_key,
		"Failed to generate private key"
	)
	print("✓ Generated private key")

	-- Extract public key (SubjectPublicKeyInfo format)
	run(
		"openssl pkey -in " .. private_key .. " -pubout -out " .. public_key,
		"Failed to extract public key"
	)
	print("✓ Extracted public key")

	print("\n==> Phase 3: Sign Catalog")
	local signature_file = TMP_DIR .. "/catalog.sig"

	-- Sign the catalog (produces DER-encoded signature)
	run(
		"openssl dgst -sha256 -sign " .. private_key .. " -out " .. signature_file .. " " .. CATALOG_PATH,
		"Failed to sign catalog"
	)
	print("✓ Signed catalog")

	-- Verify signature (sanity check)
	run(
		"openssl dgst -sha256 -verify "
			.. public_key
			.. " -signature "
			.. signature_file
			.. " "
			.. CATALOG_PATH,
		"Signature verification failed"
	)
	print("✓ Verified signature")

	print("\n==> Phase 4: Encode Signature")
	-- Read and base64 encode signature
	local signature_bytes = read_file(signature_file)
	local signature_b64 = base64_encode(signature_bytes)
	print("✓ Encoded signature (" .. #signature_b64 .. " chars)")

	print("\n==> Phase 5: Create Signature JSON")
	local timestamp = os.date("!%Y%m%dT%H%M%SZ")
	local key_id = "debug-" .. timestamp

	-- Build JSON manually (simple structure, no need for JSON library)
	local json = string.format(
		[[{
  "keyId": "%s",
  "algorithm": "ECDSA-P256-SHA256",
  "signature": "%s"
}]],
		key_id,
		signature_b64
	)

	local json_path = TMP_DIR .. "/community-functions.sig.json"
	write_file(json_path, json)
	print("✓ Created signature JSON")
	print("  Key ID: " .. key_id)

	print("\n==> Phase 6: Copy to Debug Assets")
	-- Clean out the catalog directory
	run("rm -rf " .. DEBUG_CATALOG_DIR .. "/*", "Failed to clean catalog directory")
	print("✓ Cleaned catalog directory")

	-- Copy catalog
	run("cp " .. CATALOG_PATH .. " " .. DEBUG_CATALOG_DIR .. "/community-functions.lua", "Failed to copy catalog")
	print("✓ Copied catalog to " .. DEBUG_CATALOG_DIR .. "/community-functions.lua")

	-- Copy signature JSON
	run("cp " .. json_path .. " " .. DEBUG_CATALOG_DIR .. "/community-functions.sig.json", "Failed to copy signature JSON")
	print("✓ Copied signature JSON to " .. DEBUG_CATALOG_DIR .. "/community-functions.sig.json")

	-- Copy public key
	local public_key_dest = DEBUG_CATALOG_DIR .. "/" .. key_id .. ".pub"
	run("cp " .. public_key .. " " .. public_key_dest, "Failed to copy public key")
	print("✓ Copied public key to " .. public_key_dest)

	print("\n==> Phase 7: Cleanup")
	os.execute("rm -rf " .. TMP_DIR)
	print("✓ Removed temporary directory")

	print("\n✓ Successfully published debug catalog with signature")
	print("  Catalog: " .. DEBUG_CATALOG_DIR .. "/community-functions.lua")
	print("  Signature: " .. DEBUG_CATALOG_DIR .. "/community-functions.sig.json")
	print("  Public Key: " .. public_key_dest)
	print("  Key ID: " .. key_id)
end

-- Run with error handling
local success, err = pcall(main)
if not success then
	io.stderr:write("\nERROR: " .. tostring(err) .. "\n")
	-- Cleanup tmp directory on error
	os.execute("rm -rf " .. TMP_DIR .. " 2>/dev/null")
	os.exit(1)
end
