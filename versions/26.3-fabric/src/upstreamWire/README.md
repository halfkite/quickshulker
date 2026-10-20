# Independent upstream wire fixture

`java/net/kyrptonaught/quickshulker/network/OpenShulkerPacket.java` is copied from MoRanpcy/quickshulker, branch `26.3`, commit `3fb956eb9ee1abf3e8df4cbe9174289c64aa6a02`, path `src/main/java/net/kyrptonaught/quickshulker/network/OpenShulkerPacket.java`, Git blob `e63f1a2333bc5d59e7701bb0b5c1deb5e7a1ff57`. Only the package declaration is changed to `net.kyrptonaught.quickshulker.upstreamwire` (plus line endings/final newline), so both original and More codecs can be called directly at compile time without reflection or a custom classloader.

Upstream: https://github.com/MoRanpcy/quickshulker/blob/3fb956eb9ee1abf3e8df4cbe9174289c64aa6a02/src/main/java/net/kyrptonaught/quickshulker/network/OpenShulkerPacket.java

Original author credits and MIT license are retained in this repository's LICENSE and README. Do not edit the fixture to match a broken More codec; review/pin another upstream revision explicitly when updating it.

Gradle compiles it as a separate test-only source set. MixedWireRegression directly calls both independently compiled codecs. Neither this fixture nor its compiled class is included in the distributable mod. This proves packet-format interoperability only, not handshake, client rendering, GUI clicks, AMS item safety or actual mixed-mod player login.
