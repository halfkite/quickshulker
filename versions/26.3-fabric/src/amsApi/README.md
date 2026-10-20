# Compile-only AMS public API signatures

This source set contains only two public boolean field declarations for optional static linking. It is not an AMS implementation, replacement or runtime mock. It must never be packaged in a More JAR or included in the game runtime classpath.

Verified public signatures: `carpetamsaddition.CarpetAMSAdditionSettings.largeShulkerBox` and `largeEnderChest` in the original AMS settings source:

https://github.com/Minecraft-AMS/Carpet-AMS-Addition/blob/master/src/main/java/carpetamsaddition/CarpetAMSAdditionSettings.java

More checks Fabric's installed-mod metadata and the presence of the public settings class file before entering a separate direct-link helper. It then reads the actual AMS public static fields; fields are deliberately non-final to prevent compiler constant inlining. There is no class/member discovery, Class.forName, Method/Field access or AMS method injection.

An unsupported AMS API is explicitly warned about and does not overwrite existing item capacity. This adapter does not claim support for renamed/private/lazy settings APIs without a verified compile-time API. The original user-supplied AMS ZIP is no longer present at its earlier path; the master public source above was used for signature verification, not Carpet-FGA code. Actual 26.3 AMS joint-runtime validation is still required.
