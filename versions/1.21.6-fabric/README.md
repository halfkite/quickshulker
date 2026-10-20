# QuickShulkerMore 1.0.0 / Minecraft 1.21.6 / Fabric

This target compiles against the actual Minecraft 1.21.6 game, Yarn 1.21.6+build.1, Fabric API 0.128.2+1.21.6 and Java 21. It is not the upstream 1.21.6 release tag relabelled: that tag currently points at a build configured for 1.21.8.

Source precedence is local 1.21.6 overrides, 1.21.5 overrides, 1.21.2 overrides, then the audited common root sources. The selected file with highest priority is compiled exactly once. Resources remain shared and output metadata pins the true game version.

The upstream 1.21.8 API changes were reviewed for world access, GUI render pipelines, ARGB text and `HandledScreen.renderMain`. Only matching API adaptations are applied; More's protected storage, source validation, negotiated menus, old four-byte opening protocol and server-authoritative policy remain intact. No reflection or AMS method injection is added.

Dependencies use upstream's published Mod Menu 15.0.0, Reinforced Core 4.0.7+1.21.8 and Reinforced Shulker Boxes Curse file 6813003. Their actual metadata must allow 1.21.6; dependency metadata must not be patched. Local compatibility mode uses unmodified official JARs to avoid this machine's Maven TLS issues. Default mode retains normal published Maven coordinates.

Build from the repository root:

```powershell
./build.ps1 -Version 1.21.6 -JavaHome '<JDK 21>' -Tasks @('build','printArtifactName') -GradleArguments @('-PlocalCompatDependencies=true','--offline','--console=plain','--no-daemon')
```

The full build includes component/protocol regression checks and native Fabric server GameTests, including complete crafter item serialization/reopening. Build status and archived installation JAR are recorded in `../../docs/testing/1.0.0-fabric-port-progress.md`; this README alone is not evidence of a passing build. No real 1.21.6 AMS runtime, client UI or mixed-client login has been verified.
