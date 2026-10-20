# QuickShulkerMore 1.0.0 / Minecraft 1.21.7 / Fabric

Independent target for the actual Minecraft 1.21.7 game, Yarn 1.21.7+build.8, Fabric API 0.129.0+1.21.7, Fabric Loader 0.16.14 and Java 21. Upstream's latest 1.21.7 tag is configured for 1.21.8, so it is not used as proof of 1.21.7 compatibility.

This target selects local overrides first, then the audited 1.21.6, 1.21.5, 1.21.2 and root source layers. No version-specific Java override is currently needed. It independently compiles, remaps and runs the shared tests against 1.21.7; the installation JAR metadata pins Minecraft 1.21.7.

Build from the repository root:

```powershell
./build.ps1 -Version 1.21.7 -JavaHome '<JDK 21>' -Tasks @('build','printArtifactName') -GradleArguments @('-PlocalCompatDependencies=true','--console=plain','--no-daemon')
```

Local compatibility mode reuses the unmodified official dependency JARs in `../1.21.6-fabric/libs/`. Mod Menu 15.0.0, Reinforced Core 4.0.7+1.21.8 and Reinforced Shulker Boxes 3.4.3+1.21.8 declare `~1.21.6`, covering 1.21.7; no dependency metadata is patched. Default mode uses normal Maven coordinates. Test filenames include a timestamp; GitHub Actions/official builds omit it.

On 2026-10-19 the complete build passed, including 68 component/protocol checks and all five mod GameTest groups plus the vanilla control. The first missing-Cloth-Config runtime was rejected; the successful rerun loaded Cloth Config 19.0.147. The installation JAR is `../../mod-builds/20261019-205345/quickshulkermore-1.0.0-1.21.7-test-20261019-205121.jar`, with a SHA-256 manifest and XML test report alongside it.

See `../../docs/1.0.0-fabric-port-progress.md` for detailed results. A build or embedded-player GameTest is not real client UI, mixed-client login or actual AMS joint-runtime verification.
