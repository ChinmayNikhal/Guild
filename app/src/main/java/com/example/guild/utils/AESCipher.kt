package com.example.guild.utils

import android.util.Base64
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

object AESCipher {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_SIZE = 16 // 128-bit key


    // Generate a random AES key (store this securely, per user or group)
    fun generateKey(): SecretKey {
        val keyBytes = ByteArray(KEY_SIZE)
        SecureRandom().nextBytes(keyBytes)
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(plainText: String, secretKey: SecretKey): String {
        val iv = ByteArray(KEY_SIZE)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)

        Log.d("AESCipher", "IV (Base64): ${Base64.encodeToString(iv, Base64.DEFAULT)}")

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray())
        val ivAndCipher = iv + encryptedBytes
        return Base64.encodeToString(ivAndCipher, Base64.DEFAULT)

    }

    fun decrypt(encryptedData: String, secretKey: SecretKey): String {
        val ivAndCipher = Base64.decode(encryptedData, Base64.DEFAULT)
        val iv = ivAndCipher.sliceArray(0 until KEY_SIZE)
        val cipherBytes = ivAndCipher.sliceArray(KEY_SIZE until ivAndCipher.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        val decryptedBytes = cipher.doFinal(cipherBytes)
        return String(decryptedBytes)
    }
}
