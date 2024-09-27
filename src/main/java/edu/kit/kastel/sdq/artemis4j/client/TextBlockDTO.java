/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TextBlockDTO(
        @JsonProperty String id,
        @JsonProperty String text,
        @JsonProperty int startIndex,
        @JsonProperty int endIndex,
        @JsonProperty int numberOfAffectedSubmissions,
        @JsonProperty FeedbackType type) {
    public TextBlockDTO(
            long submissionId,
            String text,
            int startIndex,
            int endIndex,
            int numberOfAffectedSubmissions,
            FeedbackType type) {
        this(
                computeId(submissionId, text, startIndex, endIndex),
                text,
                startIndex,
                endIndex,
                numberOfAffectedSubmissions,
                type);
    }

    // This is from:
    // https://github.com/ls1intum/Artemis/blob/e56f7375d711c0fa0e791980b32ea9bd775162ad/src/main/webapp/app/entities/text-block.model.ts#L21
    public static String computeId(long submissionId, String text, int startIndex, int endIndex) {
        return computeSha1("%d;%d-%d;%s".formatted(submissionId, startIndex, endIndex, text));
    }

    private static String computeSha1(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hex = "0" + hex;
                }

                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to compute SHA1 hash", e);
        }
    }
}
