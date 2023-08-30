/* Licensed under EPL-2.0 2023. */
package edu.kit.kastel.sdq.artemis4j.api.artemis.exam;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.api.artemis.User;

public class StudentExam {
	@JsonProperty
	private int id;
	@JsonProperty
	private boolean submitted;
	@JsonProperty("user")
	private User student;

	public int getId() {
		return id;
	}

	public User getStudent() {
		return student;
	}

	public boolean isSubmitted() {
		return submitted;
	}
}
