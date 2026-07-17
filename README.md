# Dither Kit Compose

[![CI](https://github.com/ryumacodes/dither-kit-compose/actions/workflows/ci.yml/badge.svg)](https://github.com/ryumacodes/dither-kit-compose/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

Composable, **dithered** charts and UI for Compose Multiplatform — area, line,
bar, pie, radar, sparklines, avatars, buttons, and gradients rendered through a
shared ordered-dither canvas engine.

Original demos and documentation →
**[tripwire.sh/dither-kit](https://tripwire.sh/dither-kit)**

Compose examples →
**[ryumacodes.github.io/dither-kit-compose](https://ryumacodes.github.io/dither-kit-compose/)**

> This is an independent Compose implementation of
> [Dither Kit](https://github.com/Boring-Software-Inc/dither-kit). React and DOM
> concepts map to Compose modifiers, semantics, state, and composable content.

## Install

Until a public Maven coordinate is released, publish the library to your local
Maven repository:

```bash
./gradlew :dither-kit-compose:publishToMavenLocal
```

Then add the root multiplatform dependency:

```kotlin
commonMain.dependencies {
    implementation("io.github.ryumacodes:dither-kit-compose:0.1.0-SNAPSHOT")
}
```

To consume a checkout directly, include the module in your build and use a
project dependency:

```kotlin
// settings.gradle.kts
include(":dither-kit-compose")
project(":dither-kit-compose").projectDir =
    file("../dither-kit-compose/dither-kit-compose")
```

```kotlin
// shared/build.gradle.kts
commonMain.dependencies {
    implementation(project(":dither-kit-compose"))
}
```

## Components

| Component | What it provides |
| --- | --- |
| `AreaChart` | Area and mixed area/line series, stacking, dots, scrub tooltip |
| `LineChart` | Line-focused cartesian chart with the shared chart DSL |
| `BarChart` | Grouped, stacked, and percent-stacked bars |
| `PieChart` | Pie and donut charts with slice selection |
| `RadarChart` | Multi-series radar charts with interactive legends |
| `Sparkline` | Compact numeric series without chart chrome |
| `DitherAvatar` | Deterministic mirrored pixel avatars |
| `DitherButton` | Interactive dithered button surface |
| `DitherGradient` | Transparent and two-colour dither washes |
| `BlockLegend` | In-flow legend for layouts where overlays would collide |

## Usage

```kotlin
val data = listOf(
    ChartDatum("Jan", mapOf("desktop" to 186.0, "mobile" to 80.0)),
    ChartDatum("Feb", mapOf("desktop" to 240.0, "mobile" to 118.0)),
)

AreaChart(
    data = data,
    modifier = Modifier.fillMaxWidth().height(260.dp),
    bloom = Bloom.Aura,
) {
    grid()
    xAxis()
    yAxis()
    legend(isClickable = true)
    tooltip(TooltipVariant.FrostedGlass)
    area(
        dataKey = "desktop",
        label = "Desktop",
        color = DitherColor.Blue,
        variant = DitherVariant.Gradient,
        activeDot = DotStyle(DotVariant.ColoredBorder, radius = 3f),
    )
    line(
        dataKey = "mobile",
        label = "Mobile",
        color = DitherColor.Purple,
        strokeVariant = StrokeVariant.Dashed,
    )
    referenceLine(value = 200, label = "Target")
}
```

### Other charts

```kotlin
BarChart(data, stackType = StackType.Stacked) {
    grid()
    xAxis()
    yAxis()
    legend(isClickable = true)
    tooltip()
    bar("desktop", color = DitherColor.Green)
    bar("mobile", color = DitherColor.Orange, variant = DitherVariant.Dotted)
}

PieChart(
    data = browserData,
    dataKey = "visitors",
    colors = browserColors,
    innerRadius = .5f,
) {
    legend(isClickable = true)
    tooltip()
    pie(DitherVariant.Gradient)
}

RadarChart(skillData) {
    legend(isClickable = true)
    tooltip()
    radar("desktop", color = DitherColor.Blue)
    radar("mobile", color = DitherColor.Pink, variant = DitherVariant.Hatched)
}
```

### Standalone components

```kotlin
DitherAvatar(name = "dan", size = 64.dp, bloom = Bloom.Aura)

DitherButton(onClick = ::save, color = DitherColor.Blue) {
    Text("save changes")
}

DitherGradient(
    from = DitherColor.Purple,
    to = DitherColor.Blue,
    direction = GradientDirection.Right,
    modifier = Modifier.fillMaxSize(),
)

Sparkline(
    data = listOf(3, 7, 5, 9, 8, 12),
    color = DitherColor.Green,
    modifier = Modifier.size(128.dp, 44.dp),
)
```

## Options

| Option | Values |
| --- | --- |
| `variant` | `Gradient`, `Dotted`, `Hatched`, `Solid` |
| `bloom` | `Off`, `Low`, `High`, `Aura`, or a custom `Bloom` |
| `stackType` | `Default`, `Stacked`, `Percent` |
| `animate` | Enables the entrance animation |
| `animationDurationMillis` | Controls entrance duration |
| `replayToken` | Changing the value replays the entrance |
| `interactive` | Disables pointer scrubbing for decorative charts |
| `markerIndex` | Controls the visible crosshair position |
| `hovered` | Applies parent-controlled hover lift |
| `bloomOnHover` | Shows bloom only while hovered |
| `onHoverChange` | Reports the currently scrubbed index |
| `onSelectionChange` | Reports legend or series selection |

Colours: `Green`, `Blue`, `Purple`, `Pink`, `Orange`, `Red`, and `Grey`.

## Platforms

| Target | Minimum / architecture |
| --- | --- |
| Android | API 21+ |
| Desktop | JVM 11+, macOS, Windows, and Linux |
| iOS device | ARM64 |
| iOS Simulator | Apple Silicon ARM64 |

Compose Multiplatform 1.11.1 does not publish an iOS x64 artifact, so Intel iOS
simulators are not included.

## Samples

Browse the interactive
[examples gallery](https://ryumacodes.github.io/dither-kit-compose/) or run the
native gallery locally.

The shared gallery is in
[`sample/shared`](sample/shared/src/commonMain/kotlin/com/ditherkit/compose/sample/SampleApp.kt).

Desktop:

```bash
./gradlew :sample:shared:run
```

Android:

```bash
./gradlew :sample:androidApp:installDebug
```

The iOS entry point is
[`MainViewController`](sample/shared/src/iosMain/kotlin/com/ditherkit/compose/sample/MainViewController.kt).
Link the generated shared framework from an Xcode host app to run it.

## Repository layout

- [`dither-kit-compose`](dither-kit-compose/src/commonMain/kotlin/com/ditherkit/compose)
  — public library and common canvas engine.
- [`sample/shared`](sample/shared/src/commonMain/kotlin/com/ditherkit/compose/sample)
  — shared component gallery.
- [`sample/androidApp`](sample/androidApp)
  — Android sample host.
- [`gradle/libs.versions.toml`](gradle/libs.versions.toml)
  — dependency and plugin versions.
- [`.github/workflows/ci.yml`](.github/workflows/ci.yml)
  — formatting, tests, and platform compilation.
- [`docs`](docs)
  — interactive browser gallery published through
  [`.github/workflows/pages.yml`](.github/workflows/pages.yml).

## Development

```bash
./gradlew spotlessCheck
./gradlew :dither-kit-compose:desktopTest
./gradlew :dither-kit-compose:compileAndroidMain
./gradlew :dither-kit-compose:compileKotlinIosSimulatorArm64
```

Formatting is enforced with Spotless and ktfmt. Run `./gradlew spotlessApply`
before committing. See [CONTRIBUTING.md](CONTRIBUTING.md) for the full workflow.

## Compatibility

The Compose API tracks Dither Kit `main` at commit
[`1e7faee`](https://github.com/Boring-Software-Inc/dither-kit/commit/1e7faee9aa252e499651e6736ed65f7a07d9a6bd)
(2026-07-14).

## Publishing

Maven Central-compatible POM metadata, sources publications, credential hooks,
and in-memory PGP signing are configured in
[`dither-kit-compose/build.gradle.kts`](dither-kit-compose/build.gradle.kts).

Required Gradle properties or environment variables:

- `mavenCentralUsername` / `MAVEN_CENTRAL_USERNAME`
- `mavenCentralPassword` / `MAVEN_CENTRAL_PASSWORD`
- `signingKey` / `SIGNING_KEY`
- `signingPassword` / `SIGNING_PASSWORD`
- `GROUP` and `VERSION_NAME` to override the defaults

## Credit

[Dither Kit](https://github.com/Boring-Software-Inc/dither-kit) is by ripgrim /
Boring Software Inc. It was inspired by
[Evil Charts](https://evilcharts.com) by legions-developer.

## License

Apache-2.0. See [LICENSE](LICENSE).
