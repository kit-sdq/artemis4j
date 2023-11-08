/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.artemis.assessment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Used for deserializing assessmentResults in lock calls into feedbacks.
 */
public class LockCallAssessmentResult {
	private final int id;
	private List<Feedback> feedbacks;

	@JsonCreator
	public LockCallAssessmentResult(@JsonProperty("id") int id, @JsonProperty("feedbacks") Feedback[] feedbacks) {
		this.id = id;
		if (feedbacks != null) {
			this.feedbacks = Arrays.asList(feedbacks);
			this.feedbacks.forEach(Feedback::init);
		}
	}

	public int getId() {
		return id;
	}

	public List<Feedback> getFeedbacks() {
		if (this.feedbacks != null) {
			return new ArrayList<>(this.feedbacks);
		}
		return List.of();
	}
}
