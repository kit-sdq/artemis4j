/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading.metajson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.kit.kastel.sdq.artemis4j.client.ArtemisClient;
import edu.kit.kastel.sdq.artemis4j.client.FeedbackDTO;
import edu.kit.kastel.sdq.artemis4j.client.FeedbackType;
import edu.kit.kastel.sdq.artemis4j.grading.Annotation;
import edu.kit.kastel.sdq.artemis4j.grading.MismatchedGradingConfigException;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.GradingConfig;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.MistakeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaFeedbackMapper {
    private static final Logger log = LoggerFactory.getLogger(MetaFeedbackMapper.class);
    private static final String METAJSON_TEXT = "CLIENT_DATA";

    private MetaFeedbackMapper() {

    }

    public static List<Annotation> parseMetaFeedbacks(List<FeedbackDTO> allFeedbacks, GradingConfig config) throws AnnotationMappingException {
        var annotations = new ArrayList<Annotation>();
        for (var feedback : allFeedbacks) {
            if (feedback.type() == FeedbackType.MANUAL_UNREFERENCED && METAJSON_TEXT.equals(feedback.text())) {
                annotations.addAll(deserializeAnnotations(feedback.detailText(), config));
                log.info("Found meta feedback with {} annotations", annotations.size());
            }
        }

        return annotations;
    }

    public static List<FeedbackDTO> createMetaFeedbacks(List<Annotation> annotations) throws AnnotationMappingException {
        String text = serializeAnnotations(annotations);

        if (text.length() < FeedbackDTO.DETAIL_TEXT_MAX_CHARACTERS) {
            // The text fits in a single feedback
            log.info("Created meta feedback with {} characters", text.length());
            return List.of(FeedbackDTO.newInvisibleManualUnreferenced(0.0, METAJSON_TEXT, text));
        }

        if (annotations.size() == 1) {
            // No good, we cannot further split a single annotation
            throw new AnnotationMappingException("Annotation too long to be split for the meta feedback: " + text);
        }

        // Recursively split the list in half
        List<FeedbackDTO> feedbacks = new ArrayList<>();
        feedbacks.addAll(createMetaFeedbacks(annotations.subList(0, annotations.size() / 2)));
        feedbacks.addAll(createMetaFeedbacks(annotations.subList(annotations.size() / 2, annotations.size())));
        return feedbacks;
    }

    public static String serializeAnnotations(List<Annotation> annotations) throws AnnotationMappingException {
        var dtos = annotations.stream().map(Annotation::toDTO).toList();
        try {
            return ArtemisClient.MAPPER.writeValueAsString(dtos);
        } catch (JsonProcessingException ex) {
            throw new AnnotationMappingException(ex);
        }
    }

    public static List<Annotation> deserializeAnnotations(String text, GradingConfig config) throws AnnotationMappingException {
        var mistakes = config.getMistakeTypes().stream().collect(Collectors.toMap(MistakeType::getId, Function.identity()));

        try {
            var dtos = ArtemisClient.MAPPER.readValue(text, AnnotationDTO[].class);
            List<Annotation> annotations = new ArrayList<>();
            for (var dto : dtos) {
                var mistake = mistakes.get(dto.mistakeTypeId());
                if (mistake == null) {
                    throw new MismatchedGradingConfigException("MistakeType with id " + dto.mistakeTypeId() + " not found in grading config");
                }
                annotations.add(new Annotation(dto, mistake));
            }
            return annotations;
        } catch (JsonProcessingException ex) {
            throw new AnnotationMappingException(ex);
        }
    }
}
