package com.reactnativesecurekeystore

import android.content.Context;
import java.security.PrivateKey

interface SecureKeystore {
    fun hasAlias(alias: String): Boolean
    fun generateKeyPair(type:String, alias: String, isAuthRequired: Boolean, authTimeout: Int?): String
    fun generateKey(alias: String, isAuthRequired: Boolean, authTimeout: Int?)
    fun removeKey(alias: String)

    fun encryptData(
      alias: String,
      data: String,
      onSuccess: (encryptedText: String) -> Unit,
      onFailure: (code: Int, message: String) -> Unit,
      context: Context,
    )

    fun decryptData(
      alias: String, encryptedText: String,
      onSuccess: (data: String) -> Unit,
      onFailure: (code: Int, message: String) -> Unit,
      context: Context,
    )

    fun sign(
      signAlgorithm: String,
      alias: String,
      data: String,
      onSuccess: (signature: String) -> Unit,
      onFailure: (code: Int, message: String) -> Unit,
      context: Context,
    )

    fun generateHmacSha(
        alias: String, data: String,
        onSuccess: (signature: String) -> Unit,
        onFailure: (code: Int, message: String) -> Unit,
    )

    fun generateHmacSha256Key(alias: String)

    @Deprecated(
        message = "Use retrieveKeyPair instead",
        replaceWith = ReplaceWith("retrieveKeyPair(account, context, isBiometricRequiredToFetch)"),
    )
    fun  retrieveGenericKey(account: String, context: Any): List<String>

    fun removeAllKeys()

    @Deprecated(
        message = "Use storeKeyPair instead",
        replaceWith = ReplaceWith("storeKeyPair(publicKey, privateKey, alias)")
    )
    fun storeGenericKey(publicKey: String,privateKey: String,account: String)


    fun retrieveKey(alias: String):String
}
