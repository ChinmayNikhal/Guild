package com.example.guild.encryption

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object AESHelper {
    private const val secretKey = "very_secure_password"
    private const val salt = "some_random_salt"
    private const val ivString = "1234567890123456" // 16 chars = 128 bits IV

    private fun generateKey(): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(secretKey.toCharArray(), salt.toByteArray(), 65536, 256)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    fun encrypt(input: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val key = generateKey()
        val iv = IvParameterSpec(ivString.toByteArray())
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val encrypted = cipher.doFinal(input.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    fun decrypt(encrypted: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val key = generateKey()
        val iv = IvParameterSpec(ivString.toByteArray())
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        val decodedBytes = Base64.decode(encrypted, Base64.DEFAULT)
        val decrypted = cipher.doFinal(decodedBytes)
        return String(decrypted)
        }
}