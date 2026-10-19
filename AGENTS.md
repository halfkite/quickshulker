# Development baseline

- Primary development Minecraft version: **26.3** (Java 25).
- Default build entry: `./build.ps1`; Fabric target: `versions/26.3-fabric`; NeoForge target: `versions/26.3-neoforge`.
- The root `src/`, Gradle build and wrapper remain the legacy Minecraft 1.21.1 / Java 21 maintenance target. Do not treat them as the default for new feature work.
- Preserve both loaders' behavior when changing 26.3. Use the existing source-reference validation and capacity-preserving item inventory for all storage writes.
- AMS is optional. Read its rules without injecting into AMS methods. Never truncate serialized container components on rule changes.
- Build results are not gameplay verification. Archive every successful build under a new timestamp directory with a SHA-256 manifest. Do not claim AMS joint-runtime or multiplayer tests unless actually run.
