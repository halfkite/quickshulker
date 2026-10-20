# QuickShulkerMore 1.0.0 / Minecraft 1.21.11 / Fabric

Actual target: Minecraft 1.21.11, Yarn 1.21.11+build.6, Fabric API 0.141.3+1.21.11, Loader 0.18.5, Java 21, Gradle 9.3.1 and the upstream Loom remap plugin (1.15-SNAPSHOT, currently resolving 1.15.5).

Local version overrides precede the 1.21.10, 1.21.9, 1.21.6, 1.21.5, 1.21.2 and root sources. Upstream 1.21.11 commit `d2507939d25a0b63a736294008f20877986133ef` was reviewed for new Screen initialization/button rendering and toolchain APIs. More's protected storage, complete item serialization, child switches and negotiated menus remain shared.

The new permission system is used directly: both the OP command root and the policy-update packet use vanilla `GAMEMASTERS_CHECK`, retaining the old level-2 threshold. Tests cover every native permission level, NONE/ALL predicates and the actual registered command gate. No reflection, AMS method injection or permission bypass is introduced.

Local compatibility dependencies are unmodified official Mod Menu 17.0.0, Reinforced Core 4.0.9+1.21.11, Reinforced Shulker Boxes 3.5.0-beta+1.21.11 (Curse 7585962), plus Core's bundled Cloth Config. The upstream Reinforced dependency is explicitly a beta. Default mode retains Maven coordinates and declares the compile-only JetBrains annotations required by shared older sources; these are not embedded into the mod JAR.

```powershell
./build.ps1 -Version 1.21.11 -JavaHome '<JDK 21>' -Tasks @('build','printArtifactName') -GradleArguments @('-PlocalCompatDependencies=true','--offline','--console=plain','--no-daemon')
```

The complete build passed on 2026-10-20: 79 component/protocol checks and seven mod GameTest groups plus the vanilla control. The installation JAR is `../../mod-builds/20261020-075914/quickshulkermore-1.0.0-1.21.11-test-20261020-075815.jar`, with its SHA-256 manifest and XML report. Metadata, test/API/library exclusion and generated language JSON were checked.

Gradle 9's `test` task depends on both actual Fabric test runners. Empty JUnit discovery is expected because these are not JUnit classes; a component/GameTest failure still fails the build. Local compatibility mode additionally needs the original datafixerupper 9.0.19 and netty-codec-http 4.2.7.Final POM/JAR mirror under `libs/official-maven/`, working around this host's TLS/cache-origin failures. Its JAR checksums were verified against the official Minecraft manifest. Default Maven mode does not use that local mirror.

Full results are recorded in `../../docs/testing/1.0.0-fabric-port-progress.md`. Client GUI/mixed-client login and actual 1.21.11 AMS joint-runtime are not verified here.
