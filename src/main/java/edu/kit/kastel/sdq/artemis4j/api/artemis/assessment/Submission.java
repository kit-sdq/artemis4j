/* Licensed under EPL-2.0 2022-2024. */
package edu.kit.kastel.sdq.artemis4j.api.artemis.assessment;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.api.client.IFeedbackClient;

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

	public Participation getParticipation() {
		return this.participation;
	}

	public String getRepositoryUrl() {
		return this.participation.getRepositoryUrl();
	}

	public int getSubmissionId() {
		return this.submissionId;
	}

	public boolean isBuildFailed() {
		return this.buildFailed;
	}

	public void init(IFeedbackClient client, int correctionRound) {
		this.correctionRound = correctionRound;

		if (this.results == null) {
			this.results = new Result[0];
		}

		for (Result result : this.results) {
			result.init(client);
		}
	}

	public int getCorrectionRound() {
		return this.correctionRound;
	}

	public Result getResult(int correctionRound) {
		if (results != null && results.length > correctionRound) {
			return results[correctionRound];
		}
		return null;
	}

	public Result getLatestResult() {
		if (results != null && results.length > 0) {
			return results[results.length - 1];
		}
		return null;
	}
}
