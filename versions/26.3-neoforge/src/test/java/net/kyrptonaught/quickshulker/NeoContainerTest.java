package net.kyrptonaught.quickshulker;
@org.junit.jupiter.api.extension.ExtendWith(net.neoforged.testframework.junit.EphemeralTestServerProvider.class)
public final class NeoContainerTest {
    @org.junit.jupiter.api.Test
    void componentAndSourceRegressions(net.minecraft.server.MinecraftServer server) {
        ContainerRegressionTest.main(new String[0]);
    }
}
