/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.artemis.assessment;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public class Participation implements Serializable {
	@Serial
	private static final long serialVersionUID = -9151262219739630658L;

	@JsonProperty("id")
	private int participationId;
	@JsonProperty
	private String participantIdentifier;
	@JsonProperty
	private String participantName;
	@JsonProperty
	private String repositoryUrl;
	@JsonProperty
	private Result[] results;

	public Participation() {
		// NOP
	}

	public int getParticipationId() {
		return this.participationId;
	}

	public String getParticipantIdentifier() {
		return this.participantIdentifier;
	}

	public String getRepositoryUrl() {
		return this.repositoryUrl;
	}

	public Result[] getResults() {
		return this.results;
	}

	public void init() {
		if (this.results == null)
			return;
		for (Result result : this.results) {
			result.init();
		}
	}
}