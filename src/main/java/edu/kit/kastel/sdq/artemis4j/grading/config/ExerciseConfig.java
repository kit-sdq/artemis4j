/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.grading.config;

import edu.kit.kastel.sdq.artemis4j.api.artemis.Exercise;
import edu.kit.kastel.sdq.artemis4j.api.grading.IMistakeType;
import edu.kit.kastel.sdq.artemis4j.api.grading.IRatingGroup;
import edu.kit.kastel.sdq.artemis4j.grading.model.MistakeType;
import edu.kit.kastel.sdq.artemis4j.grading.model.RatingGroup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A mapped config file (from {@link JsonFileConfig})
 */
@JsonDeserialize(converter = ExerciseConfigConverter.class)
public class ExerciseConfig {

	@JsonProperty("shortName")
	private String shortName;
	@JsonProperty("allowedExercises")
	private List<Integer> allowedExercises;
	@JsonProperty("ratingGroups")
	private List<RatingGroup> ratingGroups;
	@JsonProperty("mistakeTypes")
	private List<MistakeType> mistakeTypes;
	@JsonProperty("positiveFeedbackAllowed")
	private Boolean isPositiveFeedbackAllowed;

	public List<Integer> getAllowedExercises() {
		return Collections.unmodifiableList(this.allowedExercises == null ? List.of() : this.allowedExercises);
	}

	public List<IMistakeType> getIMistakeTypes() {
		return new ArrayList<>(this.mistakeTypes);
	}

	public List<IRatingGroup> getIRatingGroups() {
		return new ArrayList<>(this.ratingGroups);
	}

	public List<MistakeType> getMistakeTypes() {
		return this.mistakeTypes;
	}

	public List<RatingGroup> getRatingGroups() {
		return this.ratingGroups;
	}

	public String getShortName() {
		return this.shortName;
	}

	/**
	 * Modify mistakeTypes of config for the current exercise
	 *
	 * @param exercise the exercise
	 */
	public void initialize(Exercise exercise) {
		this.mistakeTypes.forEach(e -> e.initialize(exercise));
	}

	public boolean isPositiveFeedbackAllowed() {
		return this.isPositiveFeedbackAllowed == null || this.isPositiveFeedbackAllowed;
	}

}