package edu.kit.kastel.sdq.artemis4j.new_client;

public class StringUtil {
    public static boolean matchMaybe(String s, String pattern) {
        return pattern == null || pattern.matches(s);
    }
}
