package edu.kit.kastel.sdq.artemis4j.dto.artemis;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisClient;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisRequest;

import java.util.List;

/**
 * Corresponds to an annotation as part of an assessment
 * @param type
 * @param id null for manual feedback
 * @param credits
 * @param positive null for manual feedback
 * @param visibility null for manual feedback
 * @param text null for unreferenced manual & automatic feedback, of form "File src/edu/kit/informatik/BubbleSort.java at line 13"
 * @param reference null for unreferenced manual & automatic feedback, of form "file:src/edu/kit/informatik/BubbleSort.java_line:12"
 * @param detailText null for automatic feedback
 * @param hasLongFeedbackText
 * @param testCase
 */
public record FeedbackDTO(
        @JsonProperty FeedbackType type,
        @JsonProperty Integer id,
        @JsonProperty double credits,
        @JsonProperty Boolean positive,
        @JsonProperty String visibility,
        @JsonProperty String text,
        @JsonProperty String reference,
        @JsonProperty String detailText,
        @JsonProperty Boolean hasLongFeedbackText,
        @JsonProperty TestCaseDTO testCase
) {
    /**
     * keep this up to date with
     * https://github.com/ls1intum/Artemis/blob/develop/src/main/java/de/tum/in/www1/artemis/config/Constants.java#L137
     */
    public static int DETAIL_TEXT_MAX_CHARACTERS = 5000;

    public static FeedbackDTO newManual(double credits, String text, String reference, String detailText) {
        return new FeedbackDTO(FeedbackType.MANUAL, null, credits, null, null, text, reference, detailText, null, null);
    }

    public static FeedbackDTO newVisibleManualUnreferenced(double credits, String text, String detailText) {
        return new FeedbackDTO(FeedbackType.MANUAL_UNREFERENCED, null, credits, null, null, text, null, detailText, null, null);
    }

    public static FeedbackDTO newInvisibleManualUnreferenced(double credits, String text, String detailText) {
        return new FeedbackDTO(FeedbackType.MANUAL_UNREFERENCED, null, credits, null, "NEVER", text, null, detailText, null, null);
    }

    public static String fetchLongFeedback(ArtemisClient client, long resultId, long feedbackId) throws ArtemisNetworkException {
        return ArtemisRequest.get()
                .path(List.of("results", resultId, "feedbacks", feedbackId, "long-feedback"))
                .executeAndDecode(client, String.class);
    }
}
