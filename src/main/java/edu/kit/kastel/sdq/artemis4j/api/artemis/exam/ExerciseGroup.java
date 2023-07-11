/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.artemis.exam;

import edu.kit.kastel.sdq.artemis4j.api.artemis.Course;
import edu.kit.kastel.sdq.artemis4j.api.artemis.Exercise;
import edu.kit.kastel.sdq.artemis4j.api.artemis.IMappingLoader;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ExerciseGroup implements Serializable {
	@Serial
	private static final long serialVersionUID = 1797252671567588724L;

	@JsonProperty(value = "id")
	private int exerciseGroupId;
	@JsonProperty
	private String title;
	@JsonProperty
	private boolean isMandatory;
	@JsonProperty
	private List<Exercise> exercises;

	/**
	 * For Auto-Deserialization Need to call this::init thereafter!
	 */
	public ExerciseGroup() {
		// NOP
	}

	public List<Exercise> getExercises() {
		return new ArrayList<>(this.exercises);
	}

	public void init(IMappingLoader client, Course course, Exam exam) {
		if (this.exercises == null) {
			this.exercises = List.of();
			return;
		}
		this.exercises = this.exercises.stream().filter(exercise -> exercise.getShortName() != null).toList();
		this.exercises = this.exercises.stream().filter(Exercise::isProgramming).toList();

		for (Exercise exercise : this.exercises) {
			exercise.init(client, course, exam);
		}
	}
}
