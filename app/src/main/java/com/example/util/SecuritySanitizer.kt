package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Defensive input sanitization, URL/MIME validation, and constant-time PBKDF2-HMAC-SHA256
 * cryptographic utilities for SkillConnect.
 */
object SecuritySanitizer {

    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val MAX_ATTACHMENT_SIZE_BYTES = 5L * 1024L * 1024L // 5 MB

    private val CONTROL_CHAR_REGEX = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]")
    private val SCRIPT_TAG_REGEX = Regex("<\\s*/?\\s*script\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val DANGEROUS_TAG_REGEX = Regex(
        "<\\s*/?\\s*(iframe|object|embed|svg|math|style|link|meta|base|form)\\b[^>]*>",
        RegexOption.IGNORE_CASE
    )
    private val EVENT_HANDLER_REGEX = Regex("\\bon[a-z]+\\s*=", RegexOption.IGNORE_CASE)
    private val DANGEROUS_SCHEME_REGEX = Regex("(javascript|vbscript|data)\\s*:", RegexOption.IGNORE_CASE)
    private val SAFE_HEX_64_REGEX = Regex("^[a-fA-F0-9]{64}$")

    private val ALLOWED_MIME_TYPES = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp",
        "application/pdf"
    )

    /**
     * Strips control characters, HTML script/iframe tags, inline event attributes,
     * and dangerous URI schemes (`javascript:`, `vbscript:`, `data:`) before persisting to Firestore.
     */
    fun sanitizeText(input: String, maxLength: Int): String {
        if (input.isBlank()) return ""
        return input
            .replace(CONTROL_CHAR_REGEX, "")
            .replace(SCRIPT_TAG_REGEX, "")
            .replace(DANGEROUS_TAG_REGEX, "")
            .replace(EVENT_HANDLER_REGEX, "")
            .replace(DANGEROUS_SCHEME_REGEX, "")
            .trim()
            .take(maxLength)
    }

    /**
     * Sanitizes a list of skill/language tags.
     */
    fun sanitizeTagList(items: List<String>, maxItems: Int = 10, maxItemLength: Int = 50): List<String> {
        return items
            .map { sanitizeText(it, maxItemLength) }
            .filter { it.isNotBlank() }
            .take(maxItems)
    }

    /**
     * Validates and sanitizes an attachment or avatar URL.
     * Rejects `javascript:`, `data:`, `vbscript:`, `file:`, and non-HTTPS external schemes.
     */
    fun sanitizeUrl(url: String, maxLength: Int = 500): String {
        val clean = url.trim().replace(CONTROL_CHAR_REGEX, "")
        if (clean.isBlank()) return ""
        val lower = clean.lowercase()
        if (lower.startsWith("javascript:") ||
            lower.startsWith("vbscript:") ||
            lower.startsWith("data:") ||
            lower.startsWith("file:")
        ) {
            return ""
        }
        return if (lower.startsWith("https://") || lower.startsWith("content://")) {
            clean.take(maxLength)
        } else {
            ""
        }
    }

    /**
     * Sanitizes uploaded file names against path traversal (`..`, `/`, `\`) and null bytes.
     */
    fun sanitizeFileName(fileName: String, maxLength: Int = 100): String {
        val clean = sanitizeText(fileName, maxLength)
            .replace("..", "")
            .replace("/", "_")
            .replace("\\", "_")
        return clean.ifBlank { "attachment_file" }.take(maxLength)
    }

    /**
     * Validates MIME type against the allowed upload list (JPEG, PNG, WebP, PDF).
     */
    fun sanitizeMimeType(mimeType: String): String {
        val normalized = mimeType.trim().lowercase()
        return if (normalized in ALLOWED_MIME_TYPES) normalized else "application/pdf"
    }

    /**
     * Enforces the 5 MB attachment size ceiling.
     */
    fun clampAttachmentSizeBytes(sizeBytes: Long): Long {
        return sizeBytes.coerceIn(0L, MAX_ATTACHMENT_SIZE_BYTES)
    }

    /**
     * Generates a cryptographically random 128-bit salt (`sc_salt_<32-hex-chars>`).
     */
    fun generateCryptographicSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return "sc_salt_$hex"
    }

    /**
     * Derives a key-stretched PBKDF2-HMAC-SHA256 password verifier (`pbkdf2$120000$<64-char-hex>`).
     */
    fun hashAdminPasswordPbkdf2(password: String, salt: String, iterations: Int = PBKDF2_ITERATIONS): String {
        val cleanPassword = password.trim()
        val spec = PBEKeySpec(
            cleanPassword.toCharArray(),
            salt.toByteArray(Charsets.UTF_8),
            iterations,
            KEY_LENGTH_BITS
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derivedBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        val hex = derivedBytes.joinToString("") { "%02x".format(it) }
        return "pbkdf2$$iterations$$hex"
    }

    /**
     * Legacy single-round SHA-256 hex digest (used only for verifying existing legacy admin records
     * before upgrading them to PBKDF2/scrypt).
     */
    fun legacySha256Hex(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:${password.trim()}".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Constant-time administrative password verification using `MessageDigest.isEqual`.
     * Returns Pair(isValid, needsUpgradeToKeyStretchedHash).
     */
    fun verifyAdminPasswordConstantTime(
        enteredPassword: String,
        salt: String,
        storedVerifier: String
    ): Pair<Boolean, Boolean> {
        val cleanPassword = enteredPassword.trim()
        val cleanStored = storedVerifier.trim()
        if (cleanPassword.isEmpty() || salt.isEmpty() || cleanStored.isEmpty()) {
            return false to false
        }

        if (cleanStored.startsWith("pbkdf2$")) {
            val parts = cleanStored.split("$")
            if (parts.size != 3) return false to false
            val iterations = (parts[1].toIntOrNull() ?: PBKDF2_ITERATIONS).coerceIn(10_000, 600_000)
            val expectedHex = parts[2].lowercase()
            val computed = hashAdminPasswordPbkdf2(cleanPassword, salt, iterations)
            val computedHex = computed.substringAfterLast("$").lowercase()
            val matches = MessageDigest.isEqual(
                expectedHex.toByteArray(Charsets.UTF_8),
                computedHex.toByteArray(Charsets.UTF_8)
            )
            return matches to false
        }

        if (SAFE_HEX_64_REGEX.matches(cleanStored)) {
            val expectedBytes = cleanStored.lowercase().toByteArray(Charsets.UTF_8)
            val computedBytes = legacySha256Hex(cleanPassword, salt).lowercase().toByteArray(Charsets.UTF_8)
            val matches = MessageDigest.isEqual(expectedBytes, computedBytes)
            // Upgrade legacy single-round SHA-256 to PBKDF2-HMAC-SHA256 on successful match
            return matches to matches
        }

        return false to false
    }
}
