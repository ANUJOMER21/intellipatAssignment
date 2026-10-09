package com.example.intellipatassignment.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Log

interface SessionStorage {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clear()
}

class KeystoreSessionStorage(context: Context) : SessionStorage {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    override fun saveToken(token: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val payload = cipher.iv + cipher.doFinal(token.toByteArray())
        prefs.edit().putString(KEY_TOKEN, Base64.encodeToString(payload, Base64.NO_WRAP)).apply()
        Log.d(TAG, "session token stored (encrypted)")
    }

    override fun getToken(): String? {
        val encoded = prefs.getString(KEY_TOKEN, null) ?: return null
        return runCatching {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = payload.copyOfRange(0, IV_SIZE)
            val cipher = Cipher.getInstance(TRANSFORMATION)
                .apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv)) }
            String(cipher.doFinal(payload.copyOfRange(IV_SIZE, payload.size)))
        }.getOrElse {
            Log.d(TAG, "stored token could not be decrypted; clearing session")
            clear()
            null
        }
    }

    override fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
        Log.d(TAG, "session token cleared")
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val TAG = "SessionStorage"
        const val KEY_ALIAS = "session_token_key"
        const val KEY_TOKEN = "token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
