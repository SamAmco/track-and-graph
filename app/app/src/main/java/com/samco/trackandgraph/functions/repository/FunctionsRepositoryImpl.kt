/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.functions.repository

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import com.samco.trackandgraph.data.assetreader.AssetReader
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.functions.service.FunctionsService
import com.samco.trackandgraph.functions.service.SignatureData
import com.samco.trackandgraph.util.Stopwatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject

@OptIn(ExperimentalEncodingApi::class)
class FunctionsRepositoryImpl @Inject constructor(
    private val functionsService: FunctionsService,
    private val luaEngine: LuaEngine,
    private val assetReader: AssetReader,
) : FunctionsRepository {

    override suspend fun fetchFunctions(): List<LuaFunctionMetadata> = withContext(Dispatchers.IO) {
        val stopwatch = Stopwatch()
        try {
            val catalogData = functionsService.fetchFunctionsCatalog()
            stopwatch.start()
            val scriptBytes = catalogData.luaScriptBytes

            // Verify signature before processing - throws SignatureVerificationException on any failure
            verifySignature(scriptBytes, catalogData.signatureData)

            // Convert to string only after signature verification
            val script = String(scriptBytes, Charsets.UTF_8)

            val vmLock = luaEngine.acquireVM()
            try {
                luaEngine.runLuaCatalogue(vmLock, script).functions.also {
                    Timber.d("Returning ${it.size} functions from the catalogue")
                }
            } finally {
                luaEngine.releaseVM(vmLock)
            }
        } catch (t: Throwable) {
            Timber.e(t, "Failed to fetch functions catalog")
            throw t
        } finally {
            stopwatch.stop()
            Timber.d("fetchFunctions took ${stopwatch.elapsedMillis}ms to parse the catalogue")
        }
    }

    private fun verifySignature(luaScriptBytes: ByteArray, signatureData: SignatureData) {
        try {
            // Read the public key file based on keyId
            val keyFileName = "${signatureData.keyId}"
            val keyFileContent = assetReader.readAssetToString("functions-catalog/$keyFileName")

            // Extract the base64 key from PEM format
            val publicKeyBase64 = extractPublicKeyFromPem(keyFileContent)

            // Verify the signature
            val isValid = verifyECDSAP256Sha256(
                manifestBytes = luaScriptBytes,
                signatureDer = Base64.decode(signatureData.signature),
                publicKeySpkiBase64 = publicKeyBase64
            )

            if (!isValid) {
                throw SignatureVerificationException("Signature verification failed for functions catalog")
            }

            Timber.d("Signature verification successful for keyId: ${signatureData.keyId}")
        } catch (e: SignatureVerificationException) {
            // Re-throw our own exceptions
            throw e
        } catch (e: Throwable) {
            // Wrap any other exceptions (Base64 decode, crypto operations, file reading, etc.)
            throw SignatureVerificationException("Signature verification failed due to: ${e.message}", e)
        }
    }

    private fun extractPublicKeyFromPem(pemContent: String): String {
        return pemContent
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
    }

    private fun verifyECDSAP256Sha256(
        manifestBytes: ByteArray,
        signatureDer: ByteArray,
        publicKeySpkiBase64: String
    ): Boolean {
        val keyBytes = Base64.decode(publicKeySpkiBase64)
        val kf = KeyFactory.getInstance("EC")
        val pubKey = kf.generatePublic(X509EncodedKeySpec(keyBytes))
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initVerify(pubKey)
        sig.update(manifestBytes)
        return sig.verify(signatureDer)
    }
}
