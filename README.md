# JVN Architectury ToucanLib Template

Reusable starter template for JVN multi-loader Minecraft mods.

## Target

- Minecraft `1.21.1`
- Fabric
- NeoForge
- Java `21`
- Architectury common/fabric/neoforge source split
- Shared common resources included in both loader builds
- Optional ToucanLib support through published CurseMaven artifacts by default

## Project Structure

- `common` contains shared mod code and shared resources.
- `fabric` contains the Fabric entrypoint and `fabric.mod.json`.
- `neoforge` contains the NeoForge entrypoint and `neoforge.mods.toml`.
- `.github/workflows/build.yml` runs a CI-safe Gradle build.

## Starting A New Mod

After copying this template, update these values in `gradle.properties`:

- `mod_id`, for example `examplemod`
- `mod_name`, for example `Example Mod`
- `mod_description`
- `mod_authors`
- `mod_version`
- `maven_group`, usually `com.jvn.<modname>`
- `archives_base_name`, usually the same as `mod_id`
- `mod_display_url` and `mod_issue_tracker`

Then rename the Java package folders and classes. The normal JVN package pattern is:

```text
com.jvn.examplemod
```

Also update these template locations:

- `common/src/main/java/com/jvn/examplemod/common/ExampleModCommon.java`
- `fabric/src/main/java/com/jvn/examplemod/ExampleMod.java`
- `fabric/src/main/java/com/jvn/examplemod/client/ExampleModClient.java`
- `neoforge/src/main/java/com/jvn/examplemod/ExampleMod.java`
- `common/src/main/resources/assets/examplemod`
- `fabric/src/main/resources/fabric.mod.json`
- `neoforge/src/main/resources/META-INF/neoforge.mods.toml`

If you add mixins, access wideners, or access transformers, name their files with the new `mod_id` and wire them in the loader metadata.

## ToucanLib

ToucanLib is configured as an optional shared helper/API library. By default it is enabled and resolves from published CurseMaven artifacts:

```gradle
curse.maven:toucanlib-1542666:8116258 // Fabric 0.1.5
curse.maven:toucanlib-1542666:8158370 // NeoForge 0.2.0
```

Normal development, CI, and release builds use published artifacts and do not require local ToucanLib jars. `toucanlib_version` is set to `0.2.0` for local ToucanLib development and NeoForge metadata. Fabric remains pinned to the newest published Fabric artifact, `0.1.5`, until a Fabric `0.2.0` jar is available.

To disable ToucanLib for a mod that does not need it:

```properties
enable_toucanlib=false
```

That removes the Gradle dependency. Also remove the `toucanlib` dependency entries from `fabric.mod.json` and `neoforge.mods.toml` before shipping a mod without ToucanLib.

For active ToucanLib development only, you can opt into `mavenLocal()`:

```bash
./gradlew build -PuseLocalToucanLib=true
```

On Windows:

```bat
gradlew.bat build -PuseLocalToucanLib=true
```

Keep this local. CI and release builds must not pass `-PuseLocalToucanLib=true`.
When enabled, the template switches ToucanLib dependencies to local `com.jvn.toucanlib` artifacts for the matching loader. The property defaults to `false`.

## Build

On Windows:

```bat
gradlew.bat build
```

On macOS/Linux:

```bash
./gradlew build
```

Useful run tasks:

```bat
gradlew.bat :fabric:runClient
gradlew.bat :neoforge:runClient
```

To inspect ToucanLib resolution:

```bat
gradlew.bat :fabric:dependencyInsight --configuration modImplementation --dependency toucanlib
gradlew.bat :neoforge:dependencyInsight --configuration modImplementation --dependency toucanlib
```

## CI And Release Safety

The included GitHub Actions workflow uses Java `21`, Gradle setup, and `./gradlew build`. It does not enable local ToucanLib and does not rely on local jars, included builds, `flatDir`, or relative paths.

Fabric includes a lightweight client bootstrap and ModMenu suggestion entry so new mods can add client-only features or config screens without reshaping the template first.

## Cleanup Checklist

- Update `gradle.properties`.
- Rename package folders and entrypoint classes.
- Rename `common/src/main/resources/assets/examplemod`.
- Update language keys such as `modmenu.descriptionTranslation.examplemod`.
- Replace `logo.png`.
- Review Fabric and NeoForge metadata.
- Add mixin, access widener, or access transformer files only if the new mod needs them.
- Run `gradlew.bat build`.
- Confirm CI still runs without local ToucanLib.
