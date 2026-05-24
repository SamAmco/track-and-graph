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
package com.samco.trackandgraph.applock

import android.util.Base64
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockPasswordHasher @Inject constructor() {

    private val secureRandom = SecureRandom()

    fun createVerifier(password: String): PasswordVerifier {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        return PasswordVerifier(
            salt = salt.toBase64(),
            hash = hash(password, salt, ITERATIONS, DEFAULT_ALGORITHM).toBase64(),
            iterations = ITERATIONS,
            algorithm = DEFAULT_ALGORITHM,
        )
    }

    fun verify(
        password: String,
        salt: String,
        expectedHash: String,
        iterations: Int,
        algorithm: String,
    ): Boolean {
        val saltBytes = salt.fromBase64()
        val expectedHashBytes = expectedHash.fromBase64()
        val actualHash = hash(password, saltBytes, iterations, algorithm)
        return MessageDigest.isEqual(actualHash, expectedHashBytes)
    }

    private fun hash(
        password: String,
        salt: ByteArray,
        iterations: Int,
        algorithm: String,
    ): ByteArray {
        try {
            val spec = PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS)
            return SecretKeyFactory
                .getInstance(algorithm)
                .generateSecret(spec)
                .encoded
        } catch (e: GeneralSecurityException) {
            throw AppLockPasswordHashingException(e)
        }
    }

    data class PasswordVerifier(
        val salt: String,
        val hash: String,
        val iterations: Int,
        val algorithm: String,
    )

    private fun ByteArray.toBase64(): String =
        Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray {
        try {
            return Base64.decode(this, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            throw AppLockPasswordHashingException(e)
        }
    }

    class AppLockPasswordHashingException(cause: Throwable) : RuntimeException(cause)

    companion object {
        const val SALT_BYTES = 32
        const val ITERATIONS = 10_000
        const val HASH_BITS = 256
        const val DEFAULT_ALGORITHM = "PBKDF2WithHmacSHA1"
    }
}
