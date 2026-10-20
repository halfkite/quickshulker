package net.kyrptonaught.quickshulker.util.update;

import java.util.Optional;

/** Keep the 1.x More release line separate from historical QuickShulker 3.x tags. */
public final class MoreReleaseTags {
    private MoreReleaseTags() {}
    public static Optional<String> version(String tag, String loader, String minecraft) {
        String prefix = "quickshulkermore-" + loader + "-";
        String suffix = "-" + minecraft;
        if (tag == null || !tag.startsWith(prefix) || !tag.endsWith(suffix)) return Optional.empty();
        int end = tag.length() - suffix.length();
        if (end <= prefix.length()) return Optional.empty();
        String version = tag.substring(prefix.length(), end);
        return version.matches("[0-9]{1,6}(\\.[0-9]{1,6}){2}(?:-[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?")
            && Version.isValidVersion(version) ? Optional.of(version) : Optional.empty();
    }
}
