/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.artemis.assessment;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public class Submission implements Serializable {
	@Serial
	private static final long serialVersionUID = 4084879944629151733L;

	@JsonProperty(value = "id")
	private int submissionId;
	@JsonProperty
	private String commitHash;
	@JsonProperty
	private boolean buildFailed;

	// for constructing hasSubmittedAssessment and hasSavedAssessment
	@JsonProperty
	private Result[] results;

	// for getting participantIdentifier, participantName, repositoryUrl
	@JsonProperty(value = "participation", required = true)
	private Participation participation;

	private transient int correctionRound;

	/**
	 * For Auto-Deserialization Need to call this::init thereafter!
	 */
	public Submission() {
		// NOP
	}

	public String getParticipantIdentifier() {
		return this.participation.getParticipantIdentifier();
	}

	public String getRepositoryUrl() {
		String studentsUrl = this.participation.getRepositoryUrl();
		String studentId = this.participation.getParticipantIdentifier();

		int startIndexOfUID = studentsUrl.indexOf(studentId);
		int endIndexOfUID = studentsUrl.indexOf("@");

		assert startIndexOfUID < endIndexOfUID && startIndexOfUID >= 0;

		String newUrl = "";
		newUrl += studentsUrl.substring(0, startIndexOfUID);
		newUrl += studentsUrl.substring(endIndexOfUID + 1);
		return newUrl;
	}

	public int getSubmissionId() {
		return this.submissionId;
	}

	public boolean hasBuildFailed() {
		return this.buildFailed;
	}

	public void init(int correctionRound) {
		this.correctionRound = correctionRound;
	}

	public int getCorrectionRound() {
		return this.correctionRound;
	}

	public Result getLatestResult() {
		if (results != null && results.length > 0) {
			return results[results.length - 1];
		}
		return null;
	}
}
