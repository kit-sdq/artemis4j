/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.grading.config;

import edu.kit.kastel.sdq.artemis4j.grading.model.MistakeType;
import edu.kit.kastel.sdq.artemis4j.grading.model.RatingGroup;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.List;
import java.util.Optional;

/**
 * Used by {@link JsonFileConfig} to add rating Group - mistake type
 * associations.
 */
public final class ExerciseConfigConverter extends StdConverter<ExerciseConfig, ExerciseConfig> {

	@Override
	public ExerciseConfig convert(final ExerciseConfig exerciseConfig) throws IllegalStateException {
		List<RatingGroup> ratingGroups = exerciseConfig.getRatingGroups();
		for (MistakeType mistakeType : exerciseConfig.getMistakeTypes()) {
			// find rating group
			Optional<RatingGroup> ratingGroupOptional = ratingGroups.stream()
					.filter(ratingGroup -> ratingGroup.getIdentifier().equals(mistakeType.getAppliesTo())).findFirst();
			if (ratingGroupOptional.isEmpty()) {
				throw new IllegalStateException("No RatingGroup could be associated with MistakeType " + mistakeType.getIdentifier() + " with appliesTo := "
						+ mistakeType.getAppliesTo() + " and available RatingGroups := " + ratingGroups);
			}
			final RatingGroup ratingGroup = ratingGroupOptional.get();
			// set both associations
			mistakeType.setRatingGroup(ratingGroup);
			ratingGroup.addMistakeType(mistakeType);
		}
		return exerciseConfig;
	}

}
