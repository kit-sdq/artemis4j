/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.grading.penalty;

import org.jspecify.annotations.Nullable;

public final class StringUtil {
    private StringUtil() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Optionally matches the string s against the given pattern if the pattern is
     * not null
     *
     * @param s       The string to match. Must not be null
     * @param pattern The pattern to match against, or null
     * @return True if the pattern is null or the string s matches the pattern,
     *         false otherwise
     */
    public static boolean matchMaybe(String s, @Nullable String pattern) {
        return pattern == null || s.matches(pattern);
    }
}
