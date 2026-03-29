# Secure-Keystore

A Kotlin/Android library for secure key management using Android's hardware-backed keystore. Provides encryption, decryption, signing, and HMAC operations with biometric authentication support.

---

## 🆕 What's New in v2

The `SecureKeystoreV2` interface was introduced to improve the Generic Key Storage API. The following changes were made:

| Change | v1 (Deprecated) | v2 (Current) |
|--------|----------------|--------------|
| Store a key pair | `storeGenericKey(publicKey, privateKey, account)` | `storeKeyPair(publicKey, privateKey, alias)` |
| Retrieve a key pair | `retrieveGenericKey(account, context)` | `retrieveKeyPair(alias, context, isBiometricRequiredToFetch)` |

> **Note:** `SecureKeystoreV2` extends `SecureKeystore`. The deprecated v1 methods (`storeGenericKey`, `retrieveGenericKey`) remain available for backwards compatibility but delegate to their v2 equivalents. They will be removed in a future release. Migrate to the new methods as soon as possible.

---

## Feature Support

The Secure-Keystore library provides hardware-backed key management for symmetric, asymmetric, and HMAC keys using Android’s Keystore system.

- **AES keys** support encryption and decryption with optional biometric protection.
- **RSA and EC P-256 keys** support digital signing operations. Public keys can be exported, while private keys remain non-exportable within the keystore.
- **HMAC keys** support HMAC-SHA256 message authentication using hardware-protected symmetric keys.
- All primary cryptographic keys (AES, RSA, EC P-256, HMAC) are generated and stored within the Android hardware-backed keystore when available.
- **Generic key storage** is supported for externally provided key pairs in encrypted preferences (e.g., OKP, secp256k1), allowing storage and retrieval of both public and private keys outside keystore.

This architecture ensures strong key isolation, optional biometric enforcement, and a clear separation between hardware-protected cryptographic keys and application-managed generic keys.

## 🔐 Key Type Capability Matrix

| Key Type                  | Signing | Encryption / Decryption | HMAC | Hardware-Backed | Storage | Retrieval            |
|---------------------------|----------|--------------------------|------|---------------|----------|----------------------|
| **AES (Symmetric)**       | ❌ | ✅ | ❌    | ✅ | ✅ | Public Key           |
| **RSA (Asymmetric)**      | ✅ | ❌ | ❌    | ✅ | ✅ | Public Key           |
| **EC P-256 (Asymmetric)** | ✅ | ❌ | ❌    | ✅ | ✅ | Public Key           |
| **HMAC (SHA-256)**        | ❌ | ❌ | ✅    | ✅ | ✅ | ❌                    |
| **OKP**                   | ❌ | ❌ | ❌     | ❌ | ✅ | Public & Private Key |
| **EC secp256k1**          | ❌ | ❌ | ❌    | ❌ | ✅ | Public & Private Key |
---

## 📦 Installation

### For Native Android (Kotlin)

Add Maven Central and Sonatype snapshots repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}
```

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.mosip:secure-keystore:0.5.0-SNAPSHOT")
}
```

### For React Native

```sh
npm install @mosip/secure-keystore
```

---

## 📖 API Documentation

### Device Capability

#### deviceSupportsHardware

Check if the device supports hardware keystore.

**Signature:**
```kotlin
fun deviceSupportsHardware(): Boolean
```

**Returns:**

| Type | Description |
|------|-------------|
| Boolean | `true` if hardware keystore is supported, `false` otherwise |

---

### Key Management

#### hasAlias

Check if the given alias is present in the keystore.

**Signature:**
```kotlin
fun hasAlias(alias: String): Boolean
```

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| alias | String | Yes | The key identifier to check |

**Returns:**

| Type | Description |
|------|-------------|
| Boolean | `true` if key exists, `false` otherwise |

---

#### removeKey

Removes a key associated with the alias from the keystore.

**Signature:**
```kotlin
fun removeKey(alias: String)
```

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| alias | String | Yes | The key identifier to remove |

---

#### removeAllKeys

Removes all keys stored in the keystore.

**Signature:**
```kotlin
fun removeAllKeys()
```

---

#### retrieveKey

Retrieves the public key in PEM format for the given alias from the Android Keystore.

**Signature:**
```kotlin
fun retrieveKey(alias: String): String
```

**Parameters:**

| Name  | Type   | Required | Description         |
|-------|--------|----------|---------------------|
| alias | String | Yes      | The key identifier  |

**Returns:**

| Type   | Description              |
|--------|--------------------------|
| String | Public key in PEM format |

**Example:**
```kotlin
val publicKeyPem = SecureKeystore.retrieveKey("rsa_key")
println(publicKeyPem) // -----BEGIN PUBLIC KEY-----...
```

---

### Symmetric Key Operations

#### generateKey

Generates a symmetric key for encryption and decryption.

**Signature:**
```kotlin
fun generateKey(
    alias: String,
    isAuthRequired: Boolean,
    authTimeout: Int? = null
)
```

**Parameters:**

| Name | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| alias | String | Yes | N/A | Unique identifier for the key |
| isAuthRequired | Boolean | Yes | N/A | Whether biometric/device authentication is required |
| authTimeout | Int? | No | null | Authentication timeout in seconds |

**Example:**
```kotlin
// Generate key without authentication
SecureKeystore.generateKey(
    alias = "my_encryption_key",
    isAuthRequired = false
)

// Generate key with authentication and 30-second timeout
SecureKeystore.generateKey(
    alias = "secure_key",
    isAuthRequired = true,
    authTimeout = 30
)
```

---

#### encryptData

Encrypts the given data (encoded in Base64) using the key assigned to the alias.

**Signature:**
```kotlin
fun encryptData(
    alias: String,
    data: String,
    onSuccess: (encryptedText: String) -> Unit,
    onFailure: (code: Int, message: String) -> Unit,
    context: Context
)
```

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| alias | String | Yes | Key identifier for encryption |
| data | String | Yes | Plain text data to encrypt (Base64 encoded) |
| onSuccess | (String) -> Unit | Yes | Callback with encrypted text |
| onFailure | (Int, String) -> Unit | Yes | Callback with error code and message |
| context | Context | Yes | Android context for authentication UI |

**Example:**
```kotlin
SecureKeystore.encryptData(
    alias = "my_key",
    data = "Sensitive information",
    onSuccess = { encryptedText ->
        println("Encrypted: $encryptedText")
    },
    onFailure = { code, message ->
        println("Encryption failed: $code - $message")
    },
    context = this
)
```

---

#### decryptData

Decrypts the given encrypted text using the key assigned to the alias.

**Signature:**
```kotlin
fun decryptData(
    alias: String,
    encryptedText: String,
    onSuccess: (data: String) -> Unit,
    onFailure: (code: Int, message: String) -> Unit,
    context: Context
)
```

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| alias | String | Yes | Key identifier for decryption |
| encryptedText | String | Yes | Encrypted data to decrypt |
| onSuccess | (String) -> Unit | Yes | Callback with decrypted text |
| onFailure | (Int, String) -> Unit | Yes | Callback with error code and message |
| context | Context | Yes | Android context for authentication UI |

**Example:**
```kotlin
SecureKeystore.decryptData(
    alias = "my_key",
    encryptedText = encryptedData,
    onSuccess = { decryptedText ->
        println("Decrypted: $decryptedText")
    },
    onFailure = { code, message ->
        println("Decryption failed: $code - $message")
    },
    context = this
)
```

---

### Asymmetric Key Operations

#### generateKeyPair

Generates an asymmetric RSA or EC (P-256) key pair for signing.

**Signature:**
```kotlin
fun generateKeyPair(
    type: String,
    alias: String,
    isAuthRequired: Boolean,
    authTimeout: Int? = null
): String
```

**Parameters:**

| Name | Type | Required | Default | Description                                    |
|------|------|----------|---------|------------------------------------------------|
| type | String | Yes | N/A | Key type: `"RS256"` or `"ES256"`               |
| alias | String | Yes | N/A | Unique identifier for the key pair             |
| isAuthRequired | Boolean | Yes | N/A | Whether authentication is required for signing |
| authTimeout | Int? | No | null | Authentication timeout in seconds              |

**Returns:**

| Type | Description |
|------|-------------|
| String | Public key in PEM format |

**Example:**
```kotlin
// Generate RSA key pair
val rsaPublicKey = SecureKeystore.generateKeyPair(
    type = "RS256",
    alias = "rsa_key",
    isAuthRequired = false
)

// Generate EC key pair with authentication
val ecPublicKey = SecureKeystore.generateKeyPair(
    type = "ES256",
    alias = "ec_key",
    isAuthRequired = true,
    authTimeout = 60
)
```

---

#### sign

Creates a signature for the given data and signing algorithm using the key assigned to the alias.

**Signature:**
```kotlin
fun sign(
    signAlgorithm: String,
    alias: String,
    data: String,
    onSuccess: (signature: String) -> Unit,
    onFailure: (code: Int, message: String) -> Unit,
    context: Context
)
```

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| signAlgorithm | String | Yes | Signing algorithm: `"SHA256withRSA"` or `"SHA256withECDSA"` |
| alias | String | Yes | Key pair identifier for signing |
| data | String | Yes | Data to sign (Base64 encoded) |
| onSuccess | (String) -> Unit | Yes | Callback with signature (Base64 encoded) |
| onFailure | (Int, String) -> Unit | Yes | Callback with error code and message |
| context | Context | Yes | Android context for authentication UI |

**Example:**
```kotlin
SecureKeystore.sign(
    signAlgorithm = "SHA256withRSA",
    alias = "rsa_key",
    data = "data to sign",
    onSuccess = { signature ->
        println("Signature: $signature")
    },
    onFailure = { code, message ->
        println("Signing failed: $code - $message")
    },
    context = this
)
```

**Note on ECDSA Signatures:**

For `SHA256withECDSA` as `signAlgorithm`, the output is in standard ASN.1 format. In the case of certain verifiers like jwt.io, conversion to RS format is necessary.

```kotlin
private fun convertDerToRsFormat(derSignature: ByteArray): ByteArray {
    val asn1InputStream = ASN1InputStream(ByteArrayInputStream(derSignature))
    val seq = asn1InputStream.readObject() as ASN1Sequence
    val r = (seq.getObjectAt(0) as ASN1Integer).value
    val s = (seq.getObjectAt(1) as ASN1Integer).value

    val rBytes = r.toByteArray()
    val sBytes = s.toByteArray()

    val rPadded = ByteArray(32)
    val sPadded = ByteArray(32)

    val rTrimmed = if (rBytes.size > 32) {
        rBytes.copyOfRange(rBytes.size - 32, rBytes.size)
    } else rBytes
    
    val sTrimmed = if (sBytes.size > 32) {
        sBytes.copyOfRange(sBytes.size - 32, sBytes.size)
    } else sBytes

    System.arraycopy(rTrimmed, 0, rPadded, 32 - rTrimmed.size, rTrimmed.size)
    System.arraycopy(sTrimmed, 0, sPadded, 32 - sTrimmed.size, sTrimmed.size)

    return rPadded + sPadded
}
```

---

### HMAC Operations

#### generateHmacSha256Key

Generates a symmetric key specifically for HMAC-SHA256 operations.

**Signature:**
```kotlin
fun generateHmacSha256Key(alias: String)
```

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| alias | String | Yes | Unique identifier for the HMAC key |

**Example:**
```kotlin
SecureKeystore.generateHmacSha256Key("hmac_key")
```

---

#### generateHmacSha

Generates an HMAC signature for the given data using the key assigned to the alias.

**Signature:**
```kotlin
fun generateHmacSha(
    alias: String,
    data: String,
    onSuccess: (signature: String) -> Unit,
    onFailure: (code: Int, message: String) -> Unit
)
```

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| alias | String | Yes | HMAC key identifier |
| data | String | Yes | Data to generate HMAC for |
| onSuccess | (String) -> Unit | Yes | Callback with HMAC signature (Base64 encoded) |
| onFailure | (Int, String) -> Unit | Yes | Callback with error code and message |

**Example:**
```kotlin
SecureKeystore.generateHmacSha(
    alias = "hmac_key",
    data = "message to authenticate",
    onSuccess = { signature ->
        println("HMAC: $signature")
    },
    onFailure = { code, message ->
        println("HMAC generation failed: $code - $message")
    }
)
```

---

### Generic Key Storage

> ⚠️ **Deprecation Notice:** `storeGenericKey` and `retrieveGenericKey` are deprecated as of v2. Use [`storeKeyPair`](#storekeypair) and [`retrieveKeyPair`](#retrievekeypair) instead.

#### storeKeyPair

Stores the specified public and private key pair associated with the alias. This method replaces the deprecated `storeGenericKey`.

**Signature:**
```kotlin
fun storeKeyPair(
    publicKey: String,
    privateKey: String,
    alias: String
)
```

**Parameters:**

| Name       | Type   | Required | Description          |
|------------|--------|----------|----------------------|
| publicKey  | String | Yes      | Public key to store  |
| privateKey | String | Yes      | Private key to store |
| alias      | String | Yes      | Key pair identifier  |

**Example:**
```kotlin
SecureKeystore.storeKeyPair(
    publicKey = "-----BEGIN PUBLIC KEY-----...",
    privateKey = "-----BEGIN PRIVATE KEY-----...",
    alias = "user@example.com"
)
```

---

#### retrieveKeyPair

Retrieves the public and private keys associated with the specified alias. For sensitive key types (`ES256K`, `EdDSA`), biometric authentication is triggered automatically. This method replaces the deprecated `retrieveGenericKey`.

**Signature:**
```kotlin
fun retrieveKeyPair(alias: String, context: Any, isBiometricRequiredToFetch: Boolean = false): List<String>
```

**Parameters:**

| Name                      | Type    | Required | Default | Description                                                                                  |
|---------------------------|---------|----------|---------|----------------------------------------------------------------------------------------------|
| alias                     | String  | Yes      | N/A     | Key pair identifier                                                                          |
| context                   | Any     | Yes      | N/A     | `FragmentActivity` context, required for biometric authentication prompts                    |
| isBiometricRequiredToFetch | Boolean | No      | `false` | When `true`, biometric authentication is enforced before the key pair is returned regardless of key type |

**Returns:**

| Type          | Description                                              |
|---------------|----------------------------------------------------------|
| List\<String\> | List containing `[privateKey, publicKey]` for the alias |

**Example:**
```kotlin
val keys = SecureKeystore.retrieveKeyPair("user@example.com", this)
val privateKey = keys[0]
val publicKey  = keys[1]
```

---

#### ~~storeGenericKey~~ *(Deprecated)*

> ⚠️ **Deprecated.** Use [`storeKeyPair`](#storekeypair) instead.

Stores a public and private key pair associated with the account. This method now delegates to `storeKeyPair` internally.

**Signature:**
```kotlin
@Deprecated("Use storeKeyPair instead")
fun storeGenericKey(
    publicKey: String,
    privateKey: String,
    account: String
)
```

**Migration:**
```kotlin
// Before (v1 - deprecated)
SecureKeystore.storeGenericKey(publicKey, privateKey, account)

// After (v2)
SecureKeystore.storeKeyPair(publicKey, privateKey, alias = account)
```

---

#### ~~retrieveGenericKey~~ *(Deprecated)*

> ⚠️ **Deprecated.** Use [`retrieveKeyPair`](#retrievekeypair) instead.

Retrieves keys associated with the specified account. This method now delegates to `retrieveKeyPair` internally.

**Signature:**
```kotlin
@Deprecated("Use retrieveKeyPair instead")
fun retrieveGenericKey(account: String, context: Any): List<String>
```

**Migration:**
```kotlin
// Before (v1 - deprecated)
val keys = SecureKeystore.retrieveGenericKey(account, context)

// After (v2)
val keys = SecureKeystore.retrieveKeyPair(alias = account, context = this)
```

---

## 🤝 Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

---

## 📄 License

MPL-2.0

---
