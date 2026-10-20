# Minecraft 1.21 / Fabric / QuickShulkerMore 1.0.0

This target compiles the root's audited 1.21.1 source and tests against **Minecraft 1.21**, Yarn **1.21+build.9**, Fabric API **0.102.0+1.21**, Java 21. Common-source reuse is intentional; this is not a renamed 1.21.1 JAR. The resource-processing task pins the output metadata to Minecraft 1.21.

On 2026-10-19 the actual build passed compilation, remapping, access-widener validation, 68 component/protocol checks and all 5 native server GameTest groups. The GameTest world enabled experimental bundles. The shared regression runner's historical console label says 1.21.1, but Fabric's startup log and installed Minecraft metadata identify 1.21; do not infer the tested game version from that label.

Artifact: `../../mod-builds/20261019-193238/quickshulkermore-1.0.0-1.21-test-20261019-193038.jar`.

SHA-256: see the adjacent `build-manifest.json`.

This rebuild also passes full crafter item CODEC round trips, reopening disabled slots and preserving unrelated block entity fields. It fixes the missing block entity type ID when editing crafter locks; earlier test JARs did not cover that serialization path and are superseded.

Build from repository root:

```powershell
./build.ps1 -Version 1.21 -JavaHome '<JDK 21>' -Tasks @('build','printArtifactName') -GradleArguments @('-PlocalCompatDependencies=true','--console=plain','--no-daemon')
```

`localCompatDependencies` uses the root `libs/` official third-party JARs; otherwise Maven dependencies are resolved normally. Reinforced Core 4.0.2 and Reinforced Shulker Boxes 3.2.1 retain their published `~1.21` compatibility metadata; metadata is not patched. New portable menus do not add custom registry IDs on 1.21.

No real 1.21 AMS joint runtime or real-client mixed login/UI automation was performed. Do not reuse the 1.21.1 AMS test result as proof for this version.
