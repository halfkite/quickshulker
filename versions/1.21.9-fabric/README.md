# QuickShulkerMore 1.0.0 / Minecraft 1.21.9 / Fabric

This target uses the real Minecraft 1.21.9 game, Yarn 1.21.9+build.1, Fabric API 0.134.1+1.21.9, Loader 0.17.3 and Java 21. Loom 1.13-SNAPSHOT currently resolves 1.13.6 with Gradle 8.14.3. Upstream's 1.21.9 tag is actually configured for 1.21.10, so a renamed upstream JAR is not used as validation.

Source precedence is local 1.21.9 overrides, then 1.21.6, 1.21.5, 1.21.2 and the common root. Upstream 1.21.10 commit `d5c8f3be29997870be40a85e7826fd529f579b11` was reviewed for Click/KeyInput/CharInput, key categories, world access and ContainerUser APIs. Version-specific overrides retain More's source protection, capacity preservation, server policies and old four-byte opening protocol. No reflection or AMS method injection is added.

Block entity item data now uses `TypedEntityData.create(BlockEntityType.CRAFTER, nbt)` and copies its payload without a raw ID. Existing unrelated data is retained. CopperChestBlock covers all eight native oxidation/wax variants; ShelfBlock covers all twelve native woods. Shelves have three real slots and use the negotiated UI envelope; pots remain one slot and brewing stands five native roles. Both new child policies are appended to the stable policy bit order, and share the existing GUI/OP configuration flow.

Local compatibility dependencies are unmodified Mod Menu 16.0.0, Reinforced Core 4.0.8+1.21.10, Reinforced Shulker Boxes 3.4.4-alpha+1.21.10 (Curse 7225066), and the Cloth Config JAR embedded in that Core release. Their published metadata permits 1.21.9; this does not replace actual runtime testing. Default mode uses the upstream Maven coordinates. The upstream alpha compatibility dependency is explicitly retained as such, not described as stable.

```powershell
./build.ps1 -Version 1.21.9 -JavaHome '<JDK 21>' -Tasks @('build','printArtifactName') -GradleArguments @('-PlocalCompatDependencies=true','--console=plain','--no-daemon')
```

On 2026-10-19 the complete build passed 72 component/protocol checks plus six mod GameTest groups and a vanilla control, including all copper/shelf variants, item serialization, source protection and child policies. The installation JAR is `../../mod-builds/20261019-211547/quickshulkermore-1.0.0-1.21.9-test-20261019-211317.jar`, with its SHA-256 manifest and XML report. See `../../docs/1.0.0-fabric-port-progress.md` for failure diagnosis and validation details. No real client UI/mixed-client login or real 1.21.9 AMS joint-runtime verification has been performed.
