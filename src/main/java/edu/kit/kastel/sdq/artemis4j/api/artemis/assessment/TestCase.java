/* Licensed under EPL-2.0 2023. */
package edu.kit.kastel.sdq.artemis4j.api.artemis.assessment;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestCase {
	@JsonProperty
	private int id;
	@JsonProperty
	private String testName;
	@JsonProperty
	private Double weight;
	@JsonProperty
	private Boolean active;
	@JsonProperty
	private String visibility;
	@JsonProperty
	private Double bonusMultiplier;
	@JsonProperty
	private Double bonusPoints;
	@JsonProperty
	private String type;

	public String getTestName() {
		return testName;
	}
}
