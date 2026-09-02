package net.craftcitizen.imagemaps.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class Utils {
    public static final int ELEMENTS_PER_PAGE = 10;

    private Utils() {
    }

    public static int parseIntegerOrDefault(String string, int defaultValue) {
        try {
            return Integer.parseInt(string);
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean isBetween(float value, double min, double max) {
        return value >= min && value < max;
    }

    public static List<String> getMatches(String prefix, String[] values) {
        if (values == null)
            return Collections.emptyList();

        return getMatches(prefix, Arrays.asList(values));
    }

    public static List<String> getMatches(String prefix, List<String> values) {
        if (values == null)
            return Collections.emptyList();

        String search = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);

        return values.stream().filter(a -> a.toLowerCase(Locale.ROOT).startsWith(search))
                     .sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
    }

    public static List<String> paginate(String[] data, long page) {
        if (data == null || data.length == 0)
            return Collections.emptyList();

        int start = (int) Math.max(0, page) * ELEMENTS_PER_PAGE;

        if (start >= data.length)
            return Collections.emptyList();

        String[] copy = Arrays.copyOfRange(data, start, Math.min(start + ELEMENTS_PER_PAGE, data.length));
        Arrays.sort(copy, String.CASE_INSENSITIVE_ORDER);
        return Arrays.asList(copy);
    }
}
