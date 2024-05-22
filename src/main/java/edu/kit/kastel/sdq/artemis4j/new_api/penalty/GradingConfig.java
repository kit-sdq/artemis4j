package edu.kit.kastel.sdq.artemis4j.new_api.penalty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record GradingConfig(List<RatingGroup> ratingGroups) {
    public static GradingConfig readFromString(String config) throws InvalidGradingConfigException {
        ObjectMapper mapper = new ObjectMapper();

        try {
            var configDTO = mapper.readValue(config, GradingConfigDTO.class);
            var ratingGroups = configDTO.ratingGroups.stream().map(RatingGroup::new).toList();
            var ratingGroupsById = ratingGroups.stream().collect(Collectors.toMap(RatingGroup::getId, Function.identity()));
            configDTO.mistakeTypes.forEach(dto -> new MistakeType(dto, ratingGroupsById.get(dto.appliesTo())));
            return new GradingConfig(ratingGroups);
        } catch (JsonProcessingException e) {
            throw new InvalidGradingConfigException(e);
        }
    }

    public List<MistakeType> mistakeTypes() {
        return this.streamMistakeTypes().toList();
    }

    public Optional<MistakeType> getMistakeTypeById(String id) {
        return this.streamMistakeTypes()
                .filter(mistakeType -> mistakeType.getId().equals(id))
                .findFirst();
    }

    private Stream<MistakeType> streamMistakeTypes() {
        return ratingGroups.stream()
                .map(RatingGroup::getMistakeTypes)
                .flatMap(List::stream);
    }

    /* package-private */ record GradingConfigDTO(String shortName,
                                                  List<Integer> allowedExercises,
                                                  List<RatingGroup.RatingGroupDTO> ratingGroups,
                                                  List<MistakeType.MistakeTypeDTO> mistakeTypes) {
    }
}
