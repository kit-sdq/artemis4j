/* Licensed under EPL-2.0 2023. */
package edu.kit.kastel.sdq.artemis4j.api.artemis.assessment;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LongFeedbackText {
	@JsonProperty
	private int id;
	@JsonProperty
	private String text;

	public String getText() {
		return text;
	}
}
