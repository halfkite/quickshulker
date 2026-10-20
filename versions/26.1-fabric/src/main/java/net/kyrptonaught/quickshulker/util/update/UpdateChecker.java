package net.kyrptonaught.quickshulker.util.update;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyrptonaught.quickshulker.QuickShulkerMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {

    private static final Logger LOGGER = LogManager.getLogger("QuickShulkerMore Update Checker");
    private static final String CURRENT_VERSION = getCurrentVersion();
    private static final String VERSION_LIST_URL = "https://api.github.com/repos/halfkite/quickshulkermore/tags?per_page=100";
    private static final String DOWNLOAD_URL = "https://github.com/halfkite/quickshulkermore/releases";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().executor(Util.nonCriticalIoPool()).connectTimeout(Duration.ofSeconds(5)).build();
    private static UpdateInfo updateInfo;

    public static UpdateInfo checkForUpdates(boolean onCurrentThread){
        UpdateInfo info = getUpdateInfo();
        if(info != null) return info;
        return onCurrentThread ? checkWithCurrentThread() : checkWithNewThread();
    }

    public static UpdateInfo checkWithNewThread(){
        UpdateInfo info = null;
        ExecutorService executor = createCheckUpdateExecutor();
        try {
            info = executor.submit(UpdateChecker::checkWithCurrentThread).get();
        } catch (ExecutionException e) {
            LOGGER.error("Failed QuickShulkerMore update check!", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }finally {
            executor.shutdown();
        }
        return info;
    }

    public static UpdateInfo checkWithCurrentThread() {
        UpdateInfo info = null;
        try{
            info = updateInfo = checkUpdate0();
        } catch (IOException e) {
            LOGGER.error("Failed QuickShulkerMore update check!", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return info;
    }

    public static UpdateInfo checkUpdate() throws IOException, InterruptedException {
        return checkUpdate0();
    }

    public static UpdateInfo checkUpdate0() throws IOException, InterruptedException {
        String url = VERSION_LIST_URL;
        String lastVersion = null;
        String mcVersion = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow()
            .getMetadata().getVersion().getFriendlyString();
        Optional<String> currentModVersion = Version.modVersionForMinecraft(CURRENT_VERSION, mcVersion);
        if (currentModVersion.isEmpty()) {
            LOGGER.warn("Cannot parse mod version {} for Minecraft {}.", CURRENT_VERSION, mcVersion);
            return null;
        }
        String modVersion = currentModVersion.get();
        while(url != null){
            HttpRequest.Builder request = HttpRequest.newBuilder().GET().uri(URI.create(url));
            HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if(status != 200){
                LOGGER.warn("Mod version list responded with a non-200 status: {}!", status);
                return null;
            }
            JsonElement data = JsonParser.parseString(response.body());
            if(!data.isJsonArray()){
                LOGGER.warn("Received invalid data, aborting loader update check!");
                return null;
            }
            for(JsonElement child : data.getAsJsonArray()){
                if(!(child instanceof JsonObject)) continue;
                JsonObject obj = child.getAsJsonObject();
                Optional<String> name = getString(obj, "name");
                if(name.isEmpty()) continue;
                Optional<String> candidate = MoreReleaseTags.version(name.get(), "fabric", mcVersion);
                if (candidate.isPresent() && (lastVersion == null || Version.compareString(candidate.get(), lastVersion) > 0)) {
                    lastVersion = candidate.get();
                }
            }
            {
                String link = response.headers().firstValue("Link").orElse("");
                Pattern pattern = Pattern.compile("<([^>]+)>; rel=\"next\"");
                Matcher matcher = pattern.matcher(link);
                url = matcher.find() ? matcher.group(1) : null;
            }
        }
        if(lastVersion != null && isNewer(lastVersion, modVersion)){
            LOGGER.debug("QuickShulkerMore has a update available!");
            return new UpdateInfo(modVersion, lastVersion);
        }
        LOGGER.debug("QuickShulkerMore is up to date.");
        return null;
    }

    public static String getCurrentVersion() {
        return FabricLoader.getInstance().getModContainer(QuickShulkerMod.MOD_ID).get().getMetadata().getVersion().getFriendlyString();
    }

    public static Optional<String> getString(JsonObject obj, String key) {
        if(!obj.has(key)) return Optional.empty();
        JsonElement e = obj.get(key);
        if(!(e instanceof JsonPrimitive) || !((JsonPrimitive) e).isString()) return Optional.empty();
        return Optional.of(e.getAsString());
    }

    public static UpdateInfo getUpdateInfo(){
        return updateInfo;
    }

    public static boolean isNewer(String self, String other) {
        return Version.compareString(self, other) > 0;
    }

    private static <T> HttpResponse<T> send(HttpRequest.Builder request, HttpResponse.BodyHandler<T> handler) throws IOException, InterruptedException {
        request.timeout(Duration.ofSeconds(10));
        return HTTP_CLIENT.send(request.build(), handler);
    }

    private static <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest.Builder request, HttpResponse.BodyHandler<T> handler) throws IOException, InterruptedException {
        request.timeout(Duration.ofSeconds(10));
        return HTTP_CLIENT.sendAsync(request.build(), handler);
    }

    public static ExecutorService createCheckUpdateExecutor(){
        return Executors.newSingleThreadExecutor(r -> new Thread(r, "QuickShulkerMore Update Checker"));
    }

    public static class UpdateInfo {
        public String curVersion;
        public String lastVersion;

        public UpdateInfo(String curVersion, String lastVersion) {
            this.curVersion = curVersion;
            this.lastVersion = lastVersion;
        }

        public boolean isUpdateAvailable() {
            return true;
        }

        public Component getUpdateMessage() {
            return Component.translatable("key.quickshulker.update.updateText", Component.translatable("key.category.quickshulker.main").withStyle(ChatFormatting.BOLD)).append(" ")
                    .append(Component.translatable(
                            "key.quickshulker.update.version",
                            Component.literal(this.lastVersion)
                                    .withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE)
                                    .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.translatable("key.quickshulker.update.check"))))
                                    .withStyle(style -> style.withClickEvent(new ClickEvent.OpenUrl(URI.create(getDownloadLink()))))
                    ));
        }

        public String getDownloadLink() {
            return UpdateChecker.DOWNLOAD_URL;
        }
    }

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> {
            if(QuickShulkerMod.getConfig().checkUpdate) {
                Util.nonCriticalIoPool().execute(() -> {
                    UpdateInfo info = checkForUpdates(false);
					if(info == null) return;
                    client.execute(() -> client.gui.getChat().addClientSystemMessage(info.getUpdateMessage()));
                });
            }
        });
    }
}
