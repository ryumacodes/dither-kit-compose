# Contributing

Contributions are welcome. Keep changes focused, preserve multiplatform
behavior, and include tests for reusable rendering or data logic.

## Requirements

- JDK 21
- Android SDK 36 for Android compilation
- macOS with Xcode for iOS compilation

Set `ANDROID_HOME` or add `sdk.dir` to an ignored `local.properties` file when
building Android locally.

## Development loop

Format the repository:

```bash
./gradlew spotlessApply
```

Run the library tests and platform compiles:

```bash
./gradlew spotlessCheck
./gradlew :dither-kit-compose:desktopTest
./gradlew :dither-kit-compose:compileAndroidMain
./gradlew :dither-kit-compose:compileKotlinIosSimulatorArm64
./gradlew :sample:shared:compileKotlinDesktop
./gradlew :sample:shared:compileKotlinIosSimulatorArm64
```

The iOS tasks require an Apple Silicon macOS host. CI runs Desktop and Android
checks on Linux and the iOS compile on macOS.

## Pull requests

- Keep public API changes explicit and documented.
- Add or update tests when changing deterministic algorithms or chart math.
- Verify the shared sample when changing rendering or interaction behavior.
- Do not commit build output, IDE state, credentials, signing material, or
  local SDK paths.
- Run `spotlessCheck` before submitting.

By contributing, you agree that your contribution is licensed under the
[Apache License 2.0](LICENSE).
