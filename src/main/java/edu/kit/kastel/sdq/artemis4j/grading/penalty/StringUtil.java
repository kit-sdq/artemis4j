package edu.kit.kastel.sdq.artemis4j.grading.penalty;

public class StringUtil {
    public static boolean matchMaybe(String s, String pattern) {
        return pattern == null || pattern.matches(s);
    }
}
