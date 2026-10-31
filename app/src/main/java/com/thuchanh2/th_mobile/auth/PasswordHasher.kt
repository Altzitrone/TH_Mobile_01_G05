package com.thuchanh2.th_mobile.auth

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PBKDF2-HMAC-SHA1 is supported on every Android version allowed by minSdk 24. */
object PasswordHasher {
    private const val ITERATIONS = 120_000
    private fun encode(value: ByteArray) = Base64.encodeToString(value, Base64.NO_WRAP)

    fun hash(password: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return "pbkdf2_sha1\$$ITERATIONS\$${encode(salt)}\$${encode(derive(password, salt, ITERATIONS))}"
    }

    fun verify(password: String, encoded: String): Boolean = try {
        val parts = encoded.split('$')
        if (parts.size != 4 || parts[0] != "pbkdf2_sha1") false else {
            val iterations = parts[1].toInt()
            if (iterations !in 1..1_000_000) false else MessageDigest.isEqual(
                Base64.decode(parts[3], Base64.NO_WRAP),
                derive(password, Base64.decode(parts[2], Base64.NO_WRAP), iterations)
            )
        }
    } catch (_: IllegalArgumentException) { false }

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
