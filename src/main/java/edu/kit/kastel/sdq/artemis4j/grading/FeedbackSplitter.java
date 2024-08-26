/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import edu.kit.kastel.sdq.artemis4j.client.FeedbackDTO;
import edu.kit.kastel.sdq.artemis4j.i18n.FormatString;
import edu.kit.kastel.sdq.artemis4j.i18n.TranslatableString;

final class FeedbackSplitter {
    private static final String LINE_SEPARATOR = "\n";
    private static final int SAFETY_MARGIN = 50;
    private static final FormatString HEADER_FEEDBACK_ID =
            new FormatString(new MessageFormat(" (feedback {0,number}/{1,number})"));

    private FeedbackSplitter() {}

    static List<String> splitLines(
            Collection<? extends TranslatableString> lines, TranslatableString header, Locale locale) {
        var translatedLines = lines.stream()
                .map(line -> line.translateTo(locale) + LINE_SEPARATOR)
                .toList();
        var translatedHeader = header.translateTo(
                locale); // Not adding the line sep here because may need to add the feedback id to the
        // line

        int maxFeedbackLength = FeedbackDTO.DETAIL_TEXT_MAX_CHARACTERS - translatedHeader.length() - SAFETY_MARGIN;
        List<List<String>> perFeedbackLines = new ArrayList<>();
        perFeedbackLines.add(new ArrayList<>());
        int currentLength = translatedHeader.length();
        for (var line : translatedLines) {
            if (currentLength + line.length() > maxFeedbackLength) {
                perFeedbackLines.add(new ArrayList<>());
                currentLength = translatedHeader.length();
            }
            perFeedbackLines.get(perFeedbackLines.size() - 1).add(line);
            currentLength += line.length();
        }

        if (perFeedbackLines.size() == 1) {
            return List.of(translatedHeader + LINE_SEPARATOR + String.join("", perFeedbackLines.get(0)));
        } else {
            // We have more than one feedback to create
            // To make it easier for students, each feedback gets a running index
            // (annotation 1/2, annotation 2/2)
            // appended to its header
            List<String> feedbacks = new ArrayList<>();
            for (int i = 0; i < perFeedbackLines.size(); i++) {
                String text = translatedHeader
                        + HEADER_FEEDBACK_ID
                                .format(i + 1, perFeedbackLines.size())
                                .translateTo(locale)
                        + LINE_SEPARATOR
                        + String.join("", perFeedbackLines.get(i));
                feedbacks.add(text);
            }
            return feedbacks;
        }
    }
}
