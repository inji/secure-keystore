package com.reactnativesecurekeystore


/** Android Key store helps to store keys specific to app */
const val KEYSTORE_TYPE = "AndroidKeyStore"

/** Test alias to check the hardware supports TEE or strongbox */
const val CHECK_HARDWARE_SUPPORT_KEY_ALIAS = "$KEYSTORE_TYPE#checkHardwareSupport"

/** Key size. */
const val ENCRYPTION_KEY_SIZE = 256

const val PUBLIC_KEY_STORING_ID = "_publicKey"
const val PRIVATE_KEY_STORING_ID = "_privateKey"

enum class SigningAlgorithm(val value: String) {
    RSA("RS256"),
    ES256("ES256"),
    EDDSA("EdDSA"),
    ES256K("ES256K");

    companion object {
        fun contains(value: String): Boolean {
            return values().any { it.value.equals(value, ignoreCase = true) }
        }
    }
}

