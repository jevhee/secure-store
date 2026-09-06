# SecureStore

![SecureStore — Encrypted key-value storage for Android](docs/images/secure-store-banner.png)

SecureStore is a Kotlin-first encrypted key-value library for Android. It automatically serializes,
encrypts, stores, decrypts, and decodes small values through a coroutine-friendly API.

- Values use AES-256-GCM with keys held by Android Keystore.
- Logical identifiers use HMAC-SHA-256 obfuscation by default.
- Storage is isolated by application package and namespace.
- The library has no telemetry, network calls, or network permission.

> The API `key` is a logical identifier, not a cryptographic key. In the default configuration it
> is HMAC-obfuscated rather than reversibly encrypted. Both the original identifier and plaintext
> value are absent from persistent storage managed by SecureStore.

## Status

This project is under active development and is not yet recommended for production or
security-critical data. The current minimum Android version is API 23.

## Installation

Packages published by this repository use:

```text
io.github.jevhee:secure-store:<version>
```

Add GitHub Packages to the consuming project's `settings.gradle.kts`. GitHub Packages requires
credentials for package downloads; use Gradle properties or environment variables and do not
commit a personal access token.

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/jevhee/secure-store")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orNull
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orNull
            }
        }
    }
}
```

Then add the library dependency:

```kotlin
dependencies {
    implementation("io.github.jevhee:secure-store:<version>")
}
```

## Quick start

```kotlin
import io.github.jevhee.securestore.SecureStore
import io.github.jevhee.securestore.SecureStoreResult

val secureStore = SecureStore.open(
    context = applicationContext,
    namespace = "authentication",
)

when (val result = secureStore.put("access_token", token)) {
    is SecureStoreResult.Success -> Unit
    is SecureStoreResult.Failure -> handleSecureStorageFailure(result.error)
}

val storedToken: String? = when (val result = secureStore.getString("access_token")) {
    is SecureStoreResult.Success -> result.value
    is SecureStoreResult.Failure -> null // Apply an explicit recovery policy.
}
```

All storage operations are `suspend` functions and must run in a coroutine. `Success(null)`
means the entry does not exist. Decryption, integrity, type, key, and storage problems return
`SecureStoreResult.Failure`.

Supported values are `String`, `Int`, `Long`, `Float`, `Double`, `Boolean`, and
`ByteArray`. Use `contains`, `remove`, and `clear` to manage entries. Values are limited to
1 MiB before encryption, and logical keys are limited to 256 UTF-8 bytes.

## Configuration

Defaults use the supported AES-256-GCM profile, obfuscated identifiers, lazy migration, and
explicit failures for corrupt data:

```kotlin
import io.github.jevhee.securestore.IdentifierProtection
import io.github.jevhee.securestore.MigrationPolicy
import io.github.jevhee.securestore.SecureStoreConfig

val secureStore = SecureStore.open(
    context = applicationContext,
    namespace = "authentication",
    config = SecureStoreConfig(
        identifierProtection = IdentifierProtection.Obfuscated,
        migrationPolicy = MigrationPolicy.Lazy,
    ),
)
```

`IdentifierProtection.Plain` stores logical identifiers as visible metadata and should only be
selected when that disclosure is acceptable. A namespace cannot be reopened with a different
configuration in the same application process.

Rotate the active value-encryption key with `rotateKey()`. With lazy migration, entries encrypted
by an older key are rewritten only after a successful read. Explicit `migrate()` is available for
plain identifiers; obfuscated identifiers cannot currently be enumerated back to their logical
keys and are reported as skipped.

## Security boundary

SecureStore protects data at rest written by this library. It does not protect plaintext already in
application memory, an application process under attacker control, or a compromised device.
Secure physical deletion on flash storage cannot be guaranteed.

SecureStore uses the `SST1` envelope and `securestore` internal identifiers. Data written by
pre-release builds under the former `secure-kv` identity is incompatible and is not migrated
automatically.

Android Keystore keys are device-local. Host applications must define backup and device-transfer
rules that exclude SecureStore storage; restoring ciphertext without its Keystore key makes the data
unreadable. The sample application disables Android backup. Multi-process access is not supported.

## Sample application

The `:sample` module contains a runnable scenario dashboard. It covers primitive and binary
round-trips, overwrite, missing entries, type mismatch, removal, namespace clearing and isolation,
concurrent writes, key rotation with lazy migration, and invalid input.

Run it from Android Studio or install it on a connected device:

```shell
./gradlew :sample:installDebug
```

## Build and verification

The project uses Java 17 and Gradle Kotlin DSL.

```shell
./gradlew check lint
```

Publish a development snapshot to Maven Local:

```shell
./gradlew :secure-store:publishToMavenLocal
```

## Publishing from GitHub Actions

The repository owner can open **Actions → Publish library → Run workflow**, enter a semantic
version such as `0.1.0`, and start the workflow. It runs checks and lint before publishing
`io.github.jevhee:secure-store:<version>` to GitHub Packages. Authentication uses the workflow's
short-lived `GITHUB_TOKEN`; no publishing token needs to be stored as a repository secret.

## License

Copyright 2026 jevhee.

SecureStore is available under the [Apache License 2.0](LICENSE).
