/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.artemis.exam;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.Course;
import edu.kit.kastel.sdq.artemis4j.api.artemis.IMappingLoader;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Exam implements Serializable {

	@Serial
	private static final long serialVersionUID = 97898730702942861L;

	@JsonProperty(value = "id")
	private int examId;
	@JsonProperty
	private String title;
	@JsonProperty("numberOfCorrectionRoundsInExam")
	private int numberOfCorrectionRounds;
	@JsonProperty
	private Date startDate;
	@JsonProperty
	private Date endDate;
	@JsonIgnore
	private transient Course course;
	@JsonIgnore
	private transient List<ExerciseGroup> exerciseGroups;
	@JsonIgnore
	private transient IMappingLoader client;

	/**
	 * For Auto-Deserialization Need to call this::init thereafter!
	 */
	public Exam() {
		// NOP
	}

	public int getExamId() {
		return this.examId;
	}

	public List<ExerciseGroup> getExerciseGroups() throws ArtemisClientException {
		if (this.exerciseGroups == null) {
			this.exerciseGroups = this.client.getExerciseGroupsForExam(this, this.course);
		}
		return this.exerciseGroups;
	}

	public String getTitle() {
		return this.title;
	}

	public void init(IMappingLoader client, Course course) {
		this.course = course;
		this.client = client;
	}

	public boolean hasSecondCorrectionRound() {
		return this.numberOfCorrectionRounds >= 2;
	}

}
