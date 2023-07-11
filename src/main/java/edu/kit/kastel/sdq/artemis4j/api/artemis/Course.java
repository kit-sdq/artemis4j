/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.artemis;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.exam.Exam;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

public class Course implements Serializable {
	@Serial
	private static final long serialVersionUID = -2658127210041804941L;

	@JsonProperty(value = "id")
	private int courseId;
	@JsonProperty
	private String title;
	@JsonProperty
	private String shortName;

	@JsonProperty("instructorGroupName")
	private String instructorGroup;

	private transient List<Exercise> exercises;
	private transient List<Exam> exams;
	private transient IMappingLoader client;

	/**
	 * For Auto-Deserialization Need to call this::init thereafter!
	 */
	public Course() {
		// NOP
	}

	public int getCourseId() {
		return this.courseId;
	}

	public List<Exam> getExams() throws ArtemisClientException {
		if (this.exams == null) {
			this.exams = this.client.getExamsForCourse(this);
			this.exams.sort(Comparator.comparing(Exam::getTitle));
		}
		return this.exams;
	}

	public List<Exercise> getExercises() throws ArtemisClientException {
		if (this.exercises == null) {
			this.exercises = this.client.getGradingExercisesForCourse(this);
			this.exercises.sort(Comparator.comparing(Exercise::getShortName));
		}
		return this.exercises;
	}

	public String getShortName() {
		return this.shortName;
	}

	public String getTitle() {
		return title;
	}

	public boolean isInstructor(User assessor) {
		return (assessor != null) && assessor.getGroups().contains(this.instructorGroup);
	}

	public void init(IMappingLoader client) {
		this.client = client;
	}

}
