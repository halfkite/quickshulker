# QuickShulkerMore 1.0.0 / Minecraft 1.21.10 / Fabric

Independent target for the actual Minecraft 1.21.10 game, Yarn 1.21.10+build.3, Fabric API 0.138.4+1.21.10, Loader 0.17.3, Java 21 and Loom 1.13.6. Local overrides take precedence over the audited 1.21.9, 1.21.6, 1.21.5, 1.21.2 and common root layers; no local Java override is currently needed.

The 1.21.9 API layer includes typed block entity item data, modern input events/key categories, all copper chest/shelf variants, real container slot roles and More's protected storage/policy flow. That behavior is independently compiled and tested against 1.21.10, not assumed from 1.21.9 results.

```powershell
./build.ps1 -Version 1.21.10 -JavaHome '<JDK 21>' -Tasks @('build','printArtifactName') -GradleArguments @('-PlocalCompatDependencies=true','--console=plain','--no-daemon')
```

Local compatibility mode reuses unmodified official dependencies from `../1.21.9-fabric/libs/`; default mode uses upstream Maven coordinates. Reinforced Shulker Boxes 3.4.4-alpha+1.21.10 is an upstream alpha dependency, not a stable release. Test JARs retain timestamps; official builds omit them.

The complete build passed on 2026-10-20: 72 component/protocol checks, six mod GameTest groups and the vanilla control. A local test override adapts the command helper to Yarn build.3's `parseAndExecute`; production code is unchanged from the approved 1.21.9 layer. The installation JAR is `../../mod-builds/20261020-074831/quickshulkermore-1.0.0-1.21.10-test-20261020-074731.jar`, with a SHA-256 manifest and XML report. See `../../docs/1.0.0-fabric-port-progress.md` for detailed results. No real client UI/mixed-client login or actual 1.21.10 AMS joint-runtime verification has been performed.
