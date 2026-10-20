package carpetamsaddition;

/**
 * Compile-only public API signatures, not an AMS implementation.
 * Verified against Minecraft-AMS/Carpet-AMS-Addition public settings API.
 * Never include this source set in runtimeClasspath or the distributable JAR.
 * Fields intentionally are NOT final: callers must read actual AMS runtime values.
 */
public class CarpetAMSAdditionSettings {
    public static boolean largeShulkerBox;
    public static boolean largeEnderChest;
}
