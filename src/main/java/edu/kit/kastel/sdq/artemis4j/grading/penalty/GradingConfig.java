/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.grading.penalty;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingExercise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GradingConfig {
    private static final Logger log = LoggerFactory.getLogger(GradingConfig.class);
    private final String shortName;
    private final List<RatingGroup> ratingGroups;
    private final long validExerciseId;
    private final boolean positiveFeedbackAllowed;
    private final boolean review;

    private GradingConfig(
            String shortName,
            boolean positiveFeedbackAllowed,
            List<RatingGroup> ratingGroups,
            long validExerciseId,
            boolean review) {
        this.shortName = shortName;
        this.ratingGroups = ratingGroups;
        this.validExerciseId = validExerciseId;
        this.positiveFeedbackAllowed = positiveFeedbackAllowed;
        this.review = review;
    }

    public static GradingConfig fromDTO(GradingConfigDTO configDTO, ProgrammingExercise exercise)
            throws InvalidGradingConfigException {
        if (!configDTO.isAllowedForExercise(exercise.getId())) {
            throw new InvalidGradingConfigException(
                    "Grading config is not valid for exercise with id " + exercise.getId());
        }

        List<RatingGroup> ratingGroups = RatingGroup.createRatingGroups(configDTO.ratingGroups());
        for (MistakeType.MistakeTypeDTO dto : configDTO.mistakeTypes()) {
            if (!StringUtil.matchMaybe(exercise.getShortName(), dto.enabledForExercises())) {
                continue;
            }

            var group = ratingGroups.stream()
                    .flatMap(ratingGroup -> ratingGroup.findGroup(dto.appliesTo()).stream())
                    .findFirst()
                    .orElseThrow(() -> new InvalidGradingConfigException("No group found for mistake type %s with id %s"
                            .formatted(dto.shortName(), dto.appliesTo())));

            MistakeType.createAndAddToGroup(
                    dto, StringUtil.matchMaybe(exercise.getShortName(), dto.enabledPenaltyForExercises()), group);
        }

        var config = new GradingConfig(
                configDTO.shortName(),
                configDTO.positiveFeedbackAllowed(),
                ratingGroups,
                exercise.getId(),
                configDTO.review());
        log.info(
                "Parsed grading config for exercise '{}' and found {} mistake types",
                config.getShortName(),
                config.getMistakeTypes().size());
        return config;
    }

    public static GradingConfigDTO readDTOFromString(String configString) throws InvalidGradingConfigException {
        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(configString, GradingConfigDTO.class);
        } catch (JsonProcessingException e) {
            throw new InvalidGradingConfigException(e);
        }
    }

    public static GradingConfig readFromString(String configString, ProgrammingExercise exercise)
            throws InvalidGradingConfigException {
        return fromDTO(readDTOFromString(configString), exercise);
    }

    public List<MistakeType> getMistakeTypes() {
        return this.streamMistakeTypes().toList();
    }

    public MistakeType getMistakeTypeById(String id) {
        return this.streamMistakeTypes()
                .filter(mistakeType -> mistakeType.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No mistake type with id '%s' found".formatted(id)));
    }

    public String getShortName() {
        return shortName;
    }

    public List<RatingGroup> getRatingGroups() {
        return Collections.unmodifiableList(ratingGroups);
    }

    public boolean isValidForExercise(ProgrammingExercise exercise) {
        return exercise.getId() == validExerciseId;
    }

    public boolean isPositiveFeedbackAllowed() {
        return positiveFeedbackAllowed;
    }

    public boolean isReview() {
        return review;
    }

    @Override
    public String toString() {
        return "GradingConfig[" + "shortName=" + shortName + ", " + "ratingGroups=" + ratingGroups + ']';
    }

    private Stream<MistakeType> streamMistakeTypes() {
        return ratingGroups.stream().map(RatingGroup::getAllMistakeTypes).flatMap(List::stream);
    }

    public record GradingConfigDTO(
            String shortName,
            Boolean positiveFeedbackAllowed,
            List<Long> allowedExercises,
            List<RatingGroup.RatingGroupDTO> ratingGroups,
            List<MistakeType.MistakeTypeDTO> mistakeTypes,
            boolean review) {
        public boolean isAllowedForExercise(long exerciseId) {
            // no allowed exercises means it is valid for all exercises
            return this.allowedExercises() == null
                    || this.allowedExercises().isEmpty()
                    || this.allowedExercises().contains(exerciseId);
        }

        @Override
        public Boolean positiveFeedbackAllowed() {
            // allow positive feedback if nothing is specified
            return this.positiveFeedbackAllowed == null || this.positiveFeedbackAllowed;
        }
    }
}
