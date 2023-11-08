/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.artemis.assessment;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

public class Result implements Serializable {
	@Serial
	private static final long serialVersionUID = 6637703343535347213L;

	@JsonProperty
	public int id;
	@JsonProperty
	public Date completionDate;
	/**
	 * {@code null} when accessed via
	 * {@link edu.kit.kastel.sdq.artemis4j.api.client.ISubmissionsArtemisClient#getSubmissions()
	 * ISubmissionsArtemisClient#getSubmissions()} in combination with
	 * {@link Submission#getLatestResult()}.
	 */
	@JsonProperty
	public Feedback[] feedbacks;
	@JsonProperty
	public Boolean successful;
	@JsonProperty
	public double score;
	@JsonProperty
	public Boolean rated;

	public Result() {
		// NOP
	}

	public void init() {
		if (this.feedbacks == null)
			return;
		for (Feedback feedback : this.feedbacks) {
			feedback.init();
		}
	}
}
