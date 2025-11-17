package com.example.momolabfe.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALIAS = "momolab_token_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ACCESS_TOKEN_CIPHER = "access_token_cipher"
        private const val ACCESS_TOKEN_IV = "access_token_iv"
        private const val REFRESH_TOKEN_CIPHER = "refresh_token_cipher"
        private const val REFRESH_TOKEN_IV = "refresh_token_iv"
    }

    init {
        ensureKey()
    }

    private fun ensureKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return ks.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun encrypt(data: String): Pair<String, String> {
        val key = getSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val ciphertext = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP) to
                Base64.encodeToString(iv, Base64.NO_WRAP)
    }

    private fun decrypt(ciphertextB64: String, ivB64: String): String? {
        return try {
            val ciphertext = Base64.decode(ciphertextB64, Base64.NO_WRAP)
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            val key = getSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val plaintext = cipher.doFinal(ciphertext)
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("TokenManager", "토큰 복호화에 실패했습니다.", e)
            clearTokens()
            null
        }
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        val (accessCipher, accessIv) = encrypt(accessToken)
        val (refreshCipher, refreshIv) = encrypt(refreshToken)

        prefs.edit().apply {
            putString(ACCESS_TOKEN_CIPHER, accessCipher)
            putString(ACCESS_TOKEN_IV, accessIv)
            putString(REFRESH_TOKEN_CIPHER, refreshCipher)
            putString(REFRESH_TOKEN_IV, refreshIv)
            apply()
        }
    }

    fun getAccessToken(): String? {
        val cipher = prefs.getString(ACCESS_TOKEN_CIPHER, null) ?: return null
        val iv = prefs.getString(ACCESS_TOKEN_IV, null) ?: return null
        return decrypt(cipher, iv)
    }

    fun getRefreshToken(): String? {
        val cipher = prefs.getString(REFRESH_TOKEN_CIPHER, null) ?: return null
        val iv = prefs.getString(REFRESH_TOKEN_IV, null) ?: return null
        return decrypt(cipher, iv)
    }

    fun clearTokens() {
        prefs.edit().clear().apply()
    }
}