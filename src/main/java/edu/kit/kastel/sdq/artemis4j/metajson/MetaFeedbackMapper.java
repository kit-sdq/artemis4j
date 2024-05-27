package edu.kit.kastel.sdq.artemis4j.metajson;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.kit.kastel.sdq.artemis4j.dto.artemis.FeedbackDTO;
import edu.kit.kastel.sdq.artemis4j.dto.artemis.FeedbackType;
import edu.kit.kastel.sdq.artemis4j.new_api.Annotation;
import edu.kit.kastel.sdq.artemis4j.new_api.penalty.GradingConfig;
import edu.kit.kastel.sdq.artemis4j.new_api.penalty.MistakeType;
import edu.kit.kastel.sdq.artemis4j.new_client.AnnotationMappingException;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisClient;
import edu.kit.kastel.sdq.artemis4j.new_client.MismatchedGradingConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MetaFeedbackMapper {
    private static final Logger log = LoggerFactory.getLogger(MetaFeedbackMapper.class);
    private static final String METAJSON_TEXT = "CLIENT_DATA";

    private MetaFeedbackMapper() {

    }

    public static List<Annotation> parseMetaFeedbacks(FeedbackDTO[] allFeedbacks, GradingConfig config) throws AnnotationMappingException {
        var mistakeTypeMap = config.mistakeTypes().stream().collect(Collectors.toMap(MistakeType::getId, Function.identity()));

        var annotations = new ArrayList<Annotation>();
        for (var feedback : allFeedbacks) {
            if (feedback.type() == FeedbackType.MANUAL_UNREFERENCED && METAJSON_TEXT.equals(feedback.text())) {
                annotations.addAll(unpackAnnotationsFromMetaFeedback(feedback, mistakeTypeMap));
                log.info("Found meta feedback with {} annotations", annotations.size());
            }
        }

        return annotations;
    }

    public static List<FeedbackDTO> createMetaFeedbacks(List<Annotation> annotations) throws AnnotationMappingException {
        var dtos = annotations.stream().map(Annotation::toDTO).toList();
        try {
            String text = ArtemisClient.MAPPER.writeValueAsString(dtos);

            if (text.length() < FeedbackDTO.DETAIL_TEXT_MAX_CHARACTERS) {
                // The text fits in a single feedback
                log.info("Created meta feedback with {} characters", text.length());
                return List.of(FeedbackDTO.newInvisibleManualUnreferenced(0.0, METAJSON_TEXT, text));
            }

            if (dtos.size() == 1) {
                // No good, we cannot further split a single annotation
                throw new AnnotationMappingException("Annotation too long to be split for the meta feedback: " + text);
            }

            // Recursively split the list in half
            List<FeedbackDTO> feedbacks = new ArrayList<>();
            feedbacks.addAll(createMetaFeedbacks(annotations.subList(0, annotations.size() / 2)));
            feedbacks.addAll(createMetaFeedbacks(annotations.subList(annotations.size() / 2, annotations.size())));
            return feedbacks;
        } catch (JsonProcessingException ex) {
            throw new AnnotationMappingException(ex);
        }
    }

    private static List<Annotation> unpackAnnotationsFromMetaFeedback(FeedbackDTO feedback, Map<String, MistakeType> mistakes) throws AnnotationMappingException {
        try {
            var dtos = ArtemisClient.MAPPER.readValue(feedback.detailText(), AnnotationDTO[].class);
            List<Annotation> annotations = new ArrayList<>();
            for (var dto : dtos) {
                var mistake = mistakes.get(dto.mistakeTypeId());
                if (mistake == null) {
                    throw new MismatchedGradingConfigException("MistakeType with id " + dto.mistakeTypeId() + " not found in GradingConfig");
                }
                annotations.add(new Annotation(dto, mistakes.get(dto.mistakeTypeId())));
            }
            return annotations;
        } catch (JsonProcessingException ex) {
            throw new AnnotationMappingException("Failed to parse CLIENT_DATA", ex);
        }
    }
}
