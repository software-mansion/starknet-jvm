<h2 align="center">☕ starknet jvm ☕</h2>

[StarkNet](https://starkware.co/starknet/#:~:text=Live%20on%20Mainnet-,What%20is%20StarkNet%3F,-StarkNet%20is%20a) SDK for JVM languages:
- Java
- Kotlin
- Scala
- Clojure
- Groovy

## Development

### Hooks
Run
```
./gradlew addKtlintFormatGitPreCommitHook
./gradlew addKtlintCheckGitPreCommitHook
```


### Ensuring idiomatic Java code
We want this library to be used by both kotlin & java users. In order to ensure a nice API for java always follow those rules: 
1. When using file level functions use `@file:JvmName(NAME)` to ensure a nice name without `Kt` suffix.
2. When using a companion object mark every property/function with `@JvmStatic`. This way they are accessible as static from the class. Without it `Class.INSTANCE` would have to be used.
3. When defining an immutable constant use `@field:JvmField`. This makes them static properties without getters/setters in java.
4. If you are not sure how something would work in java just create a new java class, import your code and check yourself.
