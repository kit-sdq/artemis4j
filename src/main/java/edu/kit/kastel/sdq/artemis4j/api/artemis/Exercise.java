/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.artemis;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.assessment.Submission;
import edu.kit.kastel.sdq.artemis4j.api.artemis.exam.Exam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

public class Exercise implements Serializable {
	@Serial
	private static final long serialVersionUID = 5892461865571113106L;

	@JsonProperty(value = "id")
	private int exerciseId;
	@JsonProperty
	private String title;
	@JsonProperty
	private String shortName;
	@JsonProperty("testRepositoryUri")
	private String testRepositoryUrl;
	@JsonProperty
	private Boolean secondCorrectionEnabled;
	@JsonProperty
	private String type;
	// assessmentType == AUTOMATIC it shall not be present in Grading Tool
	@JsonProperty
	private String assessmentType;
	@JsonProperty
	private double maxPoints;
	@JsonProperty
	private final Date dueDate = null;
	@JsonProperty
	private final Date startDate = null;

	private transient IMappingLoader client;
	private transient Course course;
	private transient Exam exam;

	/**
	 * For Auto-Deserialization Need to call this::init thereafter!
	 */
	public Exercise() {
		// NOP
	}

	public int getExerciseId() {
		return this.exerciseId;
	}

	public boolean isSecondCorrectionEnabled() {
		return this.secondCorrectionEnabled != null && this.secondCorrectionEnabled;
	}

	public String getShortName() {
		if (this.shortName == null) {
			return this.title;
		}
		return this.shortName;
	}

	public String getTestRepositoryUrl() {
		return this.testRepositoryUrl;
	}

	public String getTitle() {
		return this.title;
	}

	public double getMaxPoints() {
		return this.maxPoints;
	}

	public Course getCourse() {
		return this.course;
	}

	public void init(IMappingLoader client, Course course, Exam exam) {
		this.client = client;
		this.course = course;
		this.exam = exam;
	}

	public void init(IMappingLoader client, Course course) {
		this.init(client, course, null);
	}

	public Submission getSubmission(int id) throws ArtemisClientException {
		return this.client.getSubmissionById(this, id);
	}

	public boolean hasSecondCorrectionRound() {
		if (this.exam == null) {
			return false;
		}
		return this.exam.hasSecondCorrectionRound();
	}

	@JsonIgnore
	public boolean isAutomaticAssessment() {
		return "AUTOMATIC".equals(assessmentType);
	}

	@JsonIgnore
	public boolean isProgramming() {
		return "programming".equals(type);
	}

	public Date getDueDate() {
		return dueDate;
	}

	public Date getStartDate() {
		return startDate;
	}
}
