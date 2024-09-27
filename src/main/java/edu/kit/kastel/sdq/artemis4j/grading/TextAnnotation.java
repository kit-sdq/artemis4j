/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.util.Collection;

import edu.kit.kastel.sdq.artemis4j.client.FeedbackDTO;
import edu.kit.kastel.sdq.artemis4j.client.TextBlockDTO;

public record TextAnnotation(double credits, String detailText, int startIndex, int endIndex) {
    static TextAnnotation unpack(FeedbackDTO feedbackDTO, Collection<TextBlockDTO> textBlocks) {
        TextBlockDTO textBlockDTO = textBlocks.stream()
                .filter(block -> block.id().equals(feedbackDTO.reference()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No text block found for feedback reference " + feedbackDTO.reference()));

        return new TextAnnotation(
                feedbackDTO.credits(), feedbackDTO.detailText(), textBlockDTO.startIndex(), textBlockDTO.endIndex());
    }
}
