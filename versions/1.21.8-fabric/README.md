# QuickShulkerMore 1.0.0 / Minecraft 1.21.8 / Fabric

Independent target for Minecraft 1.21.8, Yarn 1.21.8+build.1, Fabric API 0.136.1+1.21.8, Fabric Loader 0.16.14 and Java 21. Fabric API's unmodified published metadata permits this game and Loader >=0.16.13.

Source precedence is local overrides, 1.21.6 overrides, 1.21.5 overrides, 1.21.2 overrides, then common root sources. No local Java override is currently needed. Compiling and testing against the real 1.21.8 game is required; successful tests from 1.21.6/1.21.7 are not substituted.

Build from the repository root:

```powershell
./build.ps1 -Version 1.21.8 -JavaHome '<JDK 21>' -Tasks @('build','printArtifactName') -GradleArguments @('-PlocalCompatDependencies=true','--console=plain','--no-daemon')
```

Local compatibility mode reuses the official, unmodified Mod Menu 15.0.0, Reinforced Core 4.0.7+1.21.8, Reinforced Shulker Boxes 3.4.3+1.21.8 and Cloth Config 19.0.147 JARs in `../1.21.6-fabric/libs/`. Their published compatibility covers this game. Default mode uses normal Maven coordinates. Local test filenames include a timestamp; GitHub Actions/official builds omit it.

The full build passed on 2026-10-19: 68 component/protocol checks and five mod GameTest groups plus the vanilla control. The installation JAR is `../../mod-builds/20261019-205951/quickshulkermore-1.0.0-1.21.8-test-20261019-205415.jar`; its SHA-256 manifest and XML report are saved alongside it. Metadata and test/AMS API signature exclusion were checked. A separate local GITHUB_ACTIONS=true naming check returned `quickshulkermore-1.0.0-1.21.8.jar`; it was not a remote CI run.

Detailed results are recorded in `../../docs/testing/1.0.0-fabric-port-progress.md`. Native server GameTests use embedded players, not real client login or mouse input. Actual AMS joint-runtime, mixed-client login and client GUI behavior are not verified here.
