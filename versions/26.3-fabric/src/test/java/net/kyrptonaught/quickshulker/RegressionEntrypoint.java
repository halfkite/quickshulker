package net.kyrptonaught.quickshulker;
public final class RegressionEntrypoint implements net.fabricmc.api.ModInitializer {
    public void onInitialize() {
        if (!Boolean.getBoolean("quickshulker.regression")) return;
        try {
            ContainerRegressionTest.main(new String[0]);
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
