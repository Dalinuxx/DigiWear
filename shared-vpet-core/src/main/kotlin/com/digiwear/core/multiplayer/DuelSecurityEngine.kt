package com.digiwear.core.multiplayer

import com.digiwear.core.model.BattleFighter
import com.digiwear.core.model.PetSpecies
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Zero-Trust Cryptographic & Anti-Cheat Engine for DigiWear Live Duels.
 * 
 * Features:
 * 1. Ephemeral ECDH (secp256r1) key agreement with AES-256-GCM authenticated encryption.
 * 2. Strict canonical Bandai fighter stat validation to prevent memory injection / hacked stats.
 * 3. Commit-Reveal cryptographic fairness protocol using SHA-256.
 * 4. Temporal reflex timing validation enforcing physiological human latency limits.
 */
object DuelSecurityEngine {

    const val MIN_HUMAN_REFLEX_MS = 120L
    const val MAX_QTE_WINDOW_MS = 6000L
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    private val secureRandom = SecureRandom()

    // =========================================================================
    // 1. Zero-Trust Fighter Stat Validation
    // =========================================================================

    data class StatValidationResult(
        val isValid: Boolean,
        val violationReason: String? = null
    )

    /**
     * Validates an incoming opponent's fighter stats against canonical Bandai rules.
     * Prevents tampered APKs or malicious packets from claiming illegal HP, power, or stages.
     */
    fun validateFighterIntegrity(fighter: BattleFighter): StatValidationResult {
        val canonical = runCatching { PetSpecies.getById(fighter.speciesId) }.getOrNull()
            ?: return StatValidationResult(false, "Unknown species ID #${fighter.speciesId}")

        if (fighter.stage != canonical.stage) {
            return StatValidationResult(
                false,
                "Stage mismatch: claimed ${fighter.stage} but canonical #${canonical.id} is ${canonical.stage}"
            )
        }

        if (fighter.attribute != canonical.attribute) {
            return StatValidationResult(
                false,
                "Attribute mismatch: claimed ${fighter.attribute} but canonical #${canonical.id} is ${canonical.attribute}"
            )
        }

        // Max HP cannot exceed canonical baseHp + 50% training/weight bonus + 50 HP floor
        val maxAllowedHp = canonical.baseHp + (canonical.baseHp / 2) + 60
        if (fighter.maxHp < (canonical.baseHp - 30) || fighter.maxHp > maxAllowedHp) {
            return StatValidationResult(
                false,
                "Illegal Max HP: ${fighter.maxHp} (Allowed range: ${canonical.baseHp - 30}..$maxAllowedHp)"
            )
        }

        // Base power must strictly match canonical definition
        if (fighter.basePower != canonical.basePower) {
            return StatValidationResult(
                false,
                "Base power mismatch: claimed ${fighter.basePower} but canonical #${canonical.id} is ${canonical.basePower}"
            )
        }

        if (fighter.effort !in 0..4) {
            return StatValidationResult(false, "Effort gauge out of bounds: ${fighter.effort} (Must be 0..4)")
        }

        if (fighter.victories < 0) {
            return StatValidationResult(false, "Negative historical victories: ${fighter.victories}")
        }

        return StatValidationResult(true)
    }

    // =========================================================================
    // 2. Commit-Reveal Protocol (Cryptographic Fairness & Anti-Peek)
    // =========================================================================

    fun generateRandomSalt(bytes: Int = 16): ByteArray {
        val salt = ByteArray(bytes)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun computeCommitment(choice: Int, reflexMs: Long, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(choice.toByte())
        md.update(reflexMs.toString().toByteArray(Charsets.UTF_8))
        md.update(salt)
        return toHex(md.digest())
    }

    fun verifyCommitment(expectedHashHex: String, choice: Int, reflexMs: Long, salt: ByteArray): Boolean {
        val computed = computeCommitment(choice, reflexMs, salt)
        return computed.equals(expectedHashHex, ignoreCase = true)
    }

    // =========================================================================
    // 3. Temporal Anti-Cheat
    // =========================================================================

    fun validateReflexTime(reflexMs: Long): Boolean {
        return reflexMs in MIN_HUMAN_REFLEX_MS..MAX_QTE_WINDOW_MS
    }

    // =========================================================================
    // 4. Ephemeral ECDH & AES-256-GCM End-to-End Encryption (E2EE)
    // =========================================================================

    fun generateEcKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), secureRandom)
        return kpg.generateKeyPair()
    }

    fun deriveSharedAesKey(myPrivateKey: PrivateKey, peerPublicKeyBytes: ByteArray): SecretKeySpec {
        val kf = KeyFactory.getInstance("EC")
        val peerPublicKey: PublicKey = kf.generatePublic(X509EncodedKeySpec(peerPublicKeyBytes))

        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(myPrivateKey)
        ka.doPhase(peerPublicKey, true)
        val rawSecret = ka.generateSecret()

        // Derive 256-bit symmetric key via SHA-256 of the raw ECDH secret
        val md = MessageDigest.getInstance("SHA-256")
        val aesKeyBytes = md.digest(rawSecret)
        return SecretKeySpec(aesKeyBytes, "AES")
    }

    fun encryptAesGcm(key: SecretKeySpec, plaintext: ByteArray): EncryptedDuelPayload {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedDuelPayload(
            ivHex = toHex(iv),
            ciphertextHex = toHex(ciphertext)
        )
    }

    fun decryptAesGcm(key: SecretKeySpec, encrypted: EncryptedDuelPayload): ByteArray {
        val iv = fromHex(encrypted.ivHex)
        val ciphertext = fromHex(encrypted.ciphertextHex)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        return cipher.doFinal(ciphertext)
    }

    // =========================================================================
    // 5. Dual-Signed Duel Receipt & Proof-of-Victory
    // =========================================================================

    fun computeDuelHash(
        duelId: String,
        winnerTamer: String,
        loserTamer: String,
        winnerSpeciesId: Int,
        loserSpeciesId: Int,
        totalRounds: Int
    ): String {
        val md = MessageDigest.getInstance("SHA-256")
        val content = "$duelId:$winnerTamer:$loserTamer:$winnerSpeciesId:$loserSpeciesId:$totalRounds"
        return toHex(md.digest(content.toByteArray(Charsets.UTF_8)))
    }

    fun signDuelReceipt(duelHash: String, secretKey: SecretKeySpec): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        val sig = mac.doFinal(duelHash.toByteArray(Charsets.UTF_8))
        return toHex(sig)
    }

    // Hex helpers
    fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun fromHex(hex: String): ByteArray {
        val clean = hex.trim()
        val len = clean.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
