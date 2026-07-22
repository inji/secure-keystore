# Secure-Keystore
A module to create and store keys in the Android hardware keystore, which helps with encryption, decryption, and HMAC calculation.

## Usage as a Kotlin library (for native android)
The secure-keystore kotlin artifact (.aar) has been published to Maven.
### Adding as a Maven dependency.
- In settings.gradle.kts of your app modify the following:
  ```
       dependencyResolutionManagement {
       repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
       repositories {
         google()
         mavenCentral()
         maven("https://oss.sonatype.org/content/repositories/snapshots/")
       }
     }
  ```

- In your app's `build.gradle.kts`, add the following:
  ```kotlin
  dependencies {
    implementation("io.mosip:secure-keystore:1.0-SNAPSHOT")
  }
  ```

The Kotlin library has been added to your project.

## Usage as a React-Native Wrapper

### Installation

```sh
npm install @mosip/secure-keystore
```

## API Documentation

### deviceSupportsHardware

`deviceSupportsHardware() => boolean`

Check if the device supports hardware keystore.

### hasAlias

`hasAlias(alias: String) => boolean`

Check if the given alias is present in the keystore.

### generateKey

`generateKey(alias: String, isAuthRequired: boolean, authTimeout?: number) => void`

Generates a symmetric key for encryption and decryption.

### generateKeyPair

`generateKeyPair(type: String, alias: String, isAuthRequired: boolean, authTimeout?: number) => String`

Generates an asymmetric RSA or EC (P-256) key pair for signing.

### removeKey

`removeKey(alias: String) => void`

Removes a key associated with the alias from the keystore.

### encryptData

```kotlin
encryptData(
  alias: String,
  data: String,
  onSuccess: (encryptedText: String) -> Unit,
  onFailure: (code: number, message: String) -> Unit,
  context: Context,
) => void
```

Encrypts the given data (encoded in Base64) using the key assigned to the alias. Returns the encrypted data as a String through the `onSuccess` callback.

### decryptData

```kotlin
decryptData(
  alias: String,
  encryptedText: String,
  onSuccess: (data: String) -> Unit,
  onFailure: (code: number, message: String) -> Unit,
  context: Context,
) => void
```

Decrypts the given `encryptedText` using the key assigned to the alias. Returns the decrypted data as a String through the `onSuccess` callback.

### sign

```kotlin
sign(
  signAlgorithm: String,
  alias: String,
  data: String,
  onSuccess: (signature: String) -> Unit,
  onFailure: (code: number, message: String) -> Unit,
  context: Context,
) => void
```

Creates a signature for the given data and signing algorithm using the key assigned to the alias. Returns the signature as a String through the `onSuccess` callback.

> For `SHA256withECDSA` as `signAlgorithm`, the output is in standard ASN.1 format. In the case of certain verifiers like jwt.io, conversion to RS format is necessary.

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

    val rTrimmed = if (rBytes.size > 32) rBytes.copyOfRange(rBytes.size - 32, rBytes.size) else rBytes
    val sTrimmed = if (sBytes.size > 32) sBytes.copyOfRange(sBytes.size - 32, sBytes.size) else sBytes

    System.arraycopy(rTrimmed, 0, rPadded, 32 - rTrimmed.size, rTrimmed.size)
    System.arraycopy(sTrimmed, 0, sPadded, 32 - sTrimmed.size, sTrimmed.size)

    return rPadded + sPadded
}
```

### generateHmacSha

```kotlin
generateHmacSha(
    alias: String,
    data: String,
    onSuccess: (signature: String) -> Unit,
    onFailure: (code: number, message: String) -> Unit,
) => void
```

Generates an HMAC signature for the given data using the key assigned to the alias. Returns the signature as a String through the `onSuccess` callback.

### generateHmacSha256Key

`generateHmacSha256Key(alias: String) => void`

Generates a symmetric key specifically for HMAC-SHA256 operations.

### retrieveGenericKey

`retrieveGenericKey(account: String) => String[]`

Retrieves a list of keys associated with the specified account.

### storeGenericKey

```kotlin
storeGenericKey(
  publicKey: String,
  privateKey: String,
  account: String,
) => void
```

Stores the specified public and private key pair associated with the account.

### retrieveKey

`retrieveKey(alias: String) => String`

Retrieves the key associated with the alias.

### removeAllKeys

`removeAllKeys() => void`

Removes all keys stored in the keystore.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

Apache 2.0
