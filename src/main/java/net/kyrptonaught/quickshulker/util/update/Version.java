package net.kyrptonaught.quickshulker.util.update;

import java.math.BigInteger;
import java.util.Optional;

public class Version implements Comparable<Version> {
    private final String name;

    public Version(String name){
        this.name = name;
    }

    public String getFriendlyString() {
        return this.name;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Version)) return false;
        if(this == obj) return true;
        return this.name.equals(((Version) obj).name);
    }

    @Override
    public int compareTo(Version o) {
        String[] self = splitModAndMinecraft(this.name);
        String[] other = splitModAndMinecraft(o.getFriendlyString());
        int result = compareString(self[1], other[1]);
        return result == 0 ? compareString(self[0], other[0]) : result;
    }

    public static int compareString(String self, String other){
        ParsedVersion first = parse(self);
        ParsedVersion second = parse(other);
        int coreLength = Math.max(first.core().length, second.core().length);
        for (int index = 0; index < coreLength; index++) {
            BigInteger left = index < first.core().length ? new BigInteger(first.core()[index]) : BigInteger.ZERO;
            BigInteger right = index < second.core().length ? new BigInteger(second.core()[index]) : BigInteger.ZERO;
            int result = left.compareTo(right);
            if (result != 0) return result;
        }
        if (first.preRelease().length == 0 || second.preRelease().length == 0) {
            return Integer.compare(second.preRelease().length, first.preRelease().length);
        }
        int preLength = Math.min(first.preRelease().length, second.preRelease().length);
        for (int index = 0; index < preLength; index++) {
            String left = first.preRelease()[index];
            String right = second.preRelease()[index];
            boolean leftNumeric = left.matches("[0-9]+");
            boolean rightNumeric = right.matches("[0-9]+");
            int result;
            if (leftNumeric && rightNumeric) {
                result = new BigInteger(left).compareTo(new BigInteger(right));
            } else if (leftNumeric != rightNumeric) {
                result = leftNumeric ? -1 : 1;
            } else {
                result = left.compareTo(right);
            }
            if (result != 0) return result;
        }
        return Integer.compare(first.preRelease().length, second.preRelease().length);
    }

    public static Optional<String> modVersionForMinecraft(String fullVersion, String minecraftVersion) {
        if (fullVersion == null || minecraftVersion == null || minecraftVersion.isBlank()) return Optional.empty();
        String suffix = "-" + minecraftVersion;
        if (!fullVersion.endsWith(suffix)) return Optional.empty();
        String modVersion = fullVersion.substring(0, fullVersion.length() - suffix.length());
        return isValidVersion(modVersion) ? Optional.of(modVersion) : Optional.empty();
    }

    public static boolean isValidVersion(String version) {
        try {
            parse(version);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String[] splitModAndMinecraft(String fullVersion) {
        int separator = fullVersion.lastIndexOf('-');
        if (separator <= 0 || separator == fullVersion.length() - 1) {
            throw new IllegalArgumentException("Expected mod-version-minecraft-version: " + fullVersion);
        }
        return new String[]{fullVersion.substring(0, separator), fullVersion.substring(separator + 1)};
    }

    private static ParsedVersion parse(String version) {
        if (version == null || version.isBlank()) throw new IllegalArgumentException("Version is empty");
        String[] buildParts = version.split("\\+", 2);
        if (buildParts.length == 2 && !buildParts[1].matches("[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*")) {
            throw new IllegalArgumentException("Invalid build metadata: " + version);
        }
        String[] preReleaseParts = buildParts[0].split("-", 2);
        if (!preReleaseParts[0].matches("[0-9]+(?:\\.[0-9]+)*")) {
            throw new IllegalArgumentException("Invalid numeric version: " + version);
        }
        String[] preRelease = preReleaseParts.length == 1 ? new String[0] : preReleaseParts[1].split("\\.", -1);
        for (String identifier : preRelease) {
            if (!identifier.matches("[0-9A-Za-z-]+")) throw new IllegalArgumentException("Invalid pre-release: " + version);
        }
        return new ParsedVersion(preReleaseParts[0].split("\\."), preRelease);
    }

    private record ParsedVersion(String[] core, String[] preRelease) {}

    @Override
    public String toString() {
        return this.name;
    }
}
