-- signing.lua
-- Common signing utilities for catalog publishing

local file_traversal = require("tools.lib.file-traversal")

local M = {}

--- Base64 encode bytes using openssl
--- @param data string: Data to encode
--- @return string: Base64 encoded string (single line)
function M.base64_encode(data)
	-- Write to temp files for openssl
	local tmp_in = os.tmpname()
	local tmp_out = os.tmpname()

	file_traversal.write_file(tmp_in, data)

	local cmd = string.format("openssl base64 -in %s -out %s", tmp_in, tmp_out)
	local result = os.execute(cmd)

	if not result then
		os.remove(tmp_in)
		os.remove(tmp_out)
		error("Failed to base64 encode")
	end

	local encoded = file_traversal.read_file(tmp_out)

	-- Remove newlines from openssl output
	encoded = encoded:gsub("\n", "")

	os.remove(tmp_in)
	os.remove(tmp_out)

	return encoded
end

--- Run shell command and check success
--- @param cmd string: Command to run
--- @param error_msg string?: Error message if command fails
function M.run(cmd, error_msg)
	local result = os.execute(cmd)
	if not result then
		error(error_msg or ("Command failed: " .. cmd))
	end
end

--- Sign a file with ECDSA P-256 SHA256
--- OpenSSL will prompt for password if the private key is encrypted
--- @param catalog_path string: Path to catalog to sign
--- @param private_key_path string: Path to private key
--- @param signature_output_path string: Where to write signature
function M.sign_catalog(catalog_path, private_key_path, signature_output_path)
	M.run(
		"openssl dgst -sha256 -sign " .. private_key_path .. " -out " .. signature_output_path .. " " .. catalog_path,
		"Failed to sign catalog"
	)
end

--- Verify a signature
--- @param catalog_path string: Path to catalog that was signed
--- @param public_key_path string: Path to public key
--- @param signature_path string: Path to signature file
--- @return boolean: true if signature is valid
function M.verify_signature(catalog_path, public_key_path, signature_path)
	local cmd = "openssl dgst -sha256 -verify " .. public_key_path .. " -signature " .. signature_path .. " " .. catalog_path
	local result = os.execute(cmd)
	return result ~= nil and result ~= false
end

--- Generate an ephemeral ECDSA P-256 keypair
--- @return string: Path to temporary private key file
--- @return string: Path to temporary public key file
function M.generate_ephemeral_keypair()
	local private_key = os.tmpname()
	local public_key = os.tmpname()

	M.run(
		"openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -out " .. private_key,
		"Failed to generate private key"
	)

	M.run("openssl pkey -in " .. private_key .. " -pubout -out " .. public_key, "Failed to extract public key")

	return private_key, public_key
end

--- Create signature JSON content
--- @param key_id string: Key identifier
--- @param signature_bytes string: Raw signature bytes
--- @return string: JSON formatted signature
function M.create_signature_json(key_id, signature_bytes)
	local signature_b64 = M.base64_encode(signature_bytes)

	return string.format(
		[[{
  "keyId": "%s",
  "algorithm": "ECDSA-P256-SHA256",
  "signature": "%s"
}]],
		key_id,
		signature_b64
	)
end

return M
