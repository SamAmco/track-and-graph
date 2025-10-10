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

import android.content.Context
import android.util.Base64
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.dto.LuaFunctionMetadata
import com.samco.trackandgraph.functions.service.FunctionsService
import com.samco.trackandgraph.functions.service.SignatureData
import com.samco.trackandgraph.util.Stopwatch
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject

class FunctionsRepositoryImpl @Inject constructor(
    private val functionsService: FunctionsService,
    private val luaEngine: LuaEngine,
    @ApplicationContext private val appContext: Context,
) : FunctionsRepository {

    override fun triggerFetchFunctions() {
        // No-op for now, as requested. We'll fetch on demand in fetchFunctions().
    }

    override suspend fun fetchFunctions(): List<LuaFunctionMetadata> = withContext(Dispatchers.IO) {
        val stopwatch = Stopwatch()
        try {
            val catalogData = functionsService.fetchFunctionsCatalog()
            stopwatch.start()
            val scriptBytes = catalogData.luaScriptBytes

            // Verify signature before processing
            val isSignatureValid = verifySignature(scriptBytes, catalogData.signatureData)
            if (!isSignatureValid) {
                throw SignatureVerificationException("Signature verification failed for functions catalog")
            }

            if (scriptBytes.isEmpty()) {
                Timber.w("Received empty Lua script from functions service")
                return@withContext emptyList()
            }

            // Convert to string only after signature verification
            val script = String(scriptBytes, Charsets.UTF_8)

            val vmLock = luaEngine.acquireVM()
            try {
                luaEngine.runLuaCatalogue(vmLock, script).also {
                    Timber.d("Returning ${it.size} functions from the catalogue")
                }
            } catch (t: Throwable) {
                Timber.e(t, "Failed to parse/execute community functions script for metadata")
                emptyList()
            } finally {
                luaEngine.releaseVM(vmLock)
            }
        } catch (t: Throwable) {
            Timber.e(t, "Failed to fetch functions catalog")
            emptyList()
        } finally {
            stopwatch.stop()
            Timber.d("fetchFunctions took ${stopwatch.elapsedMillis}ms to parse the catalogue")
        }
    }

    private fun verifySignature(luaScriptBytes: ByteArray, signatureData: SignatureData): Boolean {
        // Read the public key file based on keyId
        val keyFileName = "${signatureData.keyId}.pub"
        val keyFileContent = appContext.assets
            .open("functions-catalog/$keyFileName")
            .bufferedReader()
            .use { it.readText() }

        // Extract the base64 key from PEM format
        val publicKeyBase64 = extractPublicKeyFromPem(keyFileContent)

        // Verify the signature
        val isValid = verifyECDSAP256Sha256(
            manifestBytes = luaScriptBytes,
            signatureDer = Base64.decode(signatureData.signature, Base64.DEFAULT),
            publicKeySpkiBase64 = publicKeyBase64
        )

        if (isValid) {
            Timber.d("Signature verification successful for keyId: ${signatureData.keyId}")
        }

        return isValid
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
        val keyBytes = Base64.decode(publicKeySpkiBase64, Base64.DEFAULT)
        val kf = KeyFactory.getInstance("EC")
        val pubKey = kf.generatePublic(X509EncodedKeySpec(keyBytes))
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initVerify(pubKey)
        sig.update(manifestBytes)
        return sig.verify(signatureDer)
    }
}
