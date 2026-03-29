package com.reactnativesecurekeystore

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.fragment.app.FragmentActivity
import com.reactnativesecurekeystore.biometrics.Biometrics
import com.reactnativesecurekeystore.common.PemConverter
import com.reactnativesecurekeystore.common.Util
import com.reactnativesecurekeystore.common.Util.Companion.getLogTag
import com.reactnativesecurekeystore.dto.EncryptedOutput
import com.reactnativesecurekeystore.exception.InvalidEncryptionText
import com.reactnativesecurekeystore.exception.KeyNotFound
import kotlinx.coroutines.runBlocking
import java.security.Key
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import javax.crypto.SecretKey

const val BIOMETRIC_AUTH_TITLE = "Unlock App"
const val BIOMETRIC_AUTH_SUBTITLE = "Please use fingerprint to unlock the app"
const val BIOMETRIC_AUTH_CANCEL = "Cancel"

class SecureKeystoreImpl(
    private val keyGenerator: KeyGenerator,
    private val cipherBox: CipherBox,
    private val biometrics: Biometrics,
    private val preferences: Preferences,
) : SecureKeystoreV2 {
    private var ks: KeyStore = KeyStore.getInstance(KEYSTORE_TYPE)
    private val logTag = getLogTag(javaClass.simpleName)

    init {
        ks.load(null)
    }

    /** Generate secret key and store it in AndroidKeystore */
    override fun generateKey(alias: String, isAuthRequired: Boolean, authTimeout: Int?) {
        keyGenerator.generateKey(alias, isAuthRequired, authTimeout)
    }

    /** Generate a new key pair */
    override fun generateKeyPair(
        type: String,
        alias: String,
        isAuthRequired: Boolean,
        authTimeout: Int?,
    ): String {
        val keyPair: KeyPair = when (type) {
            SigningAlgorithm.RSA.value -> {
                keyGenerator.generateKeyPair(alias, isAuthRequired, authTimeout)
            }

            SigningAlgorithm.ES256.value -> keyGenerator.generateKeyPairEC(
                alias,
                isAuthRequired,
                authTimeout
            )

            else -> throw KeyNotFound("Given key type $type is not supported")
        }
        return PemConverter(keyPair.public).toPem()
    }

    override fun retrieveKey(
        alias: String,
    ): String {
        val publicKey = ks.getCertificate(alias).publicKey
        return PemConverter(publicKey).toPem()
    }

    fun generateKeyPairEC(
        alias: String,
        isAuthRequired: Boolean,
        authTimeout: Int?,
    ): String {
        val keyPair = keyGenerator.generateKeyPairEC(alias, isAuthRequired, authTimeout)
        return PemConverter(keyPair.public).toPem()
    }

    override fun generateHmacSha256Key(alias: String) {
        keyGenerator.generateHmacKey(alias)
    }

    /** Remove key with provided name from security storage.  */
    override fun removeKey(alias: String) {
        ks.load(null)

        if (ks.containsAlias(alias)) {
            ks.deleteEntry(alias)
        }
    }

    override fun hasAlias(alias: String): Boolean {
        ks.load(null)
        if (ks.containsAlias(alias))
            return true

        return preferences.hasAlias(Util.getPublicKeyId(alias))
    }

    override fun encryptData(
        alias: String,
        data: String,
        onSuccess: (encryptedText: String) -> Unit,
        onFailure: (code: Int, message: String) -> Unit,
        context: Context,
    ) {
        try {
            val key = getKeyOrThrow(alias)

            runBlocking {
                val createCryptoObject = {
                    CryptoObject(cipherBox.initEncryptCipher(key))
                }

                val action = { cryptoObject: CryptoObject ->
                    val encryptedText =
                        cipherBox.encryptData(cryptoObject.cipher!!, data).toString()
                    onSuccess(encryptedText)
                }

                biometrics.authenticateAndPerform(createCryptoObject, action, onFailure, context)
            }
        } catch (ex: Exception) {
            onFailure(ex.hashCode(), ex.message.toString())
        }
    }

    override fun decryptData(
        alias: String, encryptedText: String,
        onSuccess: (data: String) -> Unit,
        onFailure: (code: Int, message: String) -> Unit,
        context: Context,
    ) {
        try {
            val key = getKeyOrThrow(alias)

            if (!EncryptedOutput.validate(encryptedText)) {
                throw InvalidEncryptionText()
            }

            runBlocking {
                val encryptedOutput = EncryptedOutput(encryptedText)

                val createCryptoObject =
                    { CryptoObject(cipherBox.initDecryptCipher(key, encryptedOutput)) }

                val action = { cryptoObject: CryptoObject ->
                    val data = cipherBox.decryptData(cryptoObject.cipher!!, encryptedOutput)
                    onSuccess(String(data))
                }

                biometrics.authenticateAndPerform(createCryptoObject, action, onFailure, context)
            }
        } catch (ex: Exception) {
            onFailure(ex.hashCode(), ex.message.toString())
        }
    }

    override fun sign(
        signAlgorithm: String,
        alias: String, data: String,
        onSuccess: (signature: String) -> Unit, onFailure: (code: Int, message: String) -> Unit,
        context: Context,
    ) {
        try {
            val key = getKeyOrThrow(alias) as PrivateKey

            runBlocking {
                val createCryptoObject =
                    { CryptoObject(cipherBox.createSignature(key, signAlgorithm)) }

                val action = { cryptoObject: CryptoObject ->
                    val signatureText =
                        cipherBox.sign(cryptoObject.signature!!, data, signAlgorithm)
                    onSuccess(signatureText)
                }
                biometrics?.authenticateAndPerform(
                    createCryptoObject,
                    action,
                    onFailure,
                    context
                )
            }
        } catch (e: RuntimeException) {
            Log.e(logTag, "Exception in sign creation: ", e)
            onFailure(e.hashCode(), e.message.toString())
        }
    }

    override fun generateHmacSha(
        alias: String, data: String,
        onSuccess: (sha: String) -> Unit,
        onFailure: (code: Int, message: String) -> Unit,
    ) {
        try {
            val key = getKeyOrThrow(alias) as SecretKey
            val hmacSha: ByteArray = cipherBox.generateHmacSha(key, data)
            onSuccess(String(hmacSha))
        } catch (e: RuntimeException) {
            Log.e(logTag, "Exception in Hmac generation: ", e)
            onFailure(e.hashCode(), e.message.toString())
        }
    }

    public fun getKeyOrThrow(alias: String): Key {
        ks.load(null)
        if (!ks.containsAlias(alias)) {
            throw KeyNotFound("Key not found for the alias: $alias")
        }
        return ks.getKey(alias, null)
    }

    override fun removeAllKeys() {
        try {
            ks.load(null)
            val aliases = ks.aliases()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                ks.deleteEntry(alias)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Deprecated(
        "Use retrieveKeyPair instead",
        replaceWith = ReplaceWith("retrieveKeyPair(account, context, isBiometricRequiredToFetch)"),
    )
    override fun retrieveGenericKey(account: String, context: Any): List<String> {
        return retrieveKeyPair(account, context)
    }

    private fun authenticateBiometricallyBlocking(
        activity: FragmentActivity,
        keyAlias: String
    ): Boolean {
        val latch = CountDownLatch(1)
        var success = false

        activity.runOnUiThread {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(BIOMETRIC_AUTH_TITLE)
                .setSubtitle(BIOMETRIC_AUTH_SUBTITLE)
                .setNegativeButtonText(BIOMETRIC_AUTH_CANCEL)
                .build()

            val biometricPrompt = BiometricPrompt(
                activity,
                Executors.newSingleThreadExecutor(),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        success = true
                        latch.countDown()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        success = false
                        latch.countDown()
                    }

                    override fun onAuthenticationFailed() {
                        success = false
                        latch.countDown()
                    }
                }
            )

            biometricPrompt.authenticate(promptInfo)
        }

        latch.await()

        return success
    }


    @Deprecated(
        "Use storeKeyPair instead",
        replaceWith = ReplaceWith("storeKeyPair(publicKey, privateKey, alias)")
    )
    override fun storeGenericKey(
        publicKey: String,
        privateKey: String,
        account: String,
    ) {
        storeKeyPair(publicKey, privateKey, account)
    }

    override fun storeKeyPair(
        publicKey: String,
        privateKey: String,
        alias: String
    ) {
        preferences.savePreference(Util.getPublicKeyId(alias), publicKey)
        preferences.savePreference(Util.getPrivateKeyId(alias), privateKey)

    }

    override fun retrieveKeyPair(
        alias: String,
        context: Any,
        isBiometricRequiredToFetch: Boolean
    ): List<String> {
        try {
            val privateKeyAlias = Util.getPrivateKeyId(alias)
            val publicKeyAlias = Util.getPublicKeyId(alias)

            val keyPair = ArrayList<String>()

            val fragmentActivity = context as? FragmentActivity
                ?: throw IllegalArgumentException("Context must be a FragmentActivity for biometric authentication")
            if (SigningAlgorithm.contains(alias) || isBiometricRequiredToFetch) {

                val success = authenticateBiometricallyBlocking(fragmentActivity, privateKeyAlias)

                if (success) {
                    val privateKey = preferences.getPreference(privateKeyAlias, "")
                    val publicKey = preferences.getPreference(publicKeyAlias, "")
                    keyPair.add(privateKey)
                    keyPair.add(publicKey)
                } else {
                    Log.e("SecureKeystore", "Biometric authentication failed")
                }
            } else {
                val privateKey = preferences.getPreference(privateKeyAlias, "")
                val publicKey = preferences.getPreference(publicKeyAlias, "")
                keyPair.add(privateKey)
                keyPair.add(publicKey)
            }
            return keyPair
        } catch (e: Exception) {
            Log.e(
                "SecureKeystore",
                "Error during biometric authentication or retrieving key-data: ${e.message}"
            )
            throw Exception(e.message)
        }
    }
}
