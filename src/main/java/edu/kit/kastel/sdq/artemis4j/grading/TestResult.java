/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.client.FeedbackDTO;
import edu.kit.kastel.sdq.artemis4j.client.FeedbackType;

public class TestResult {
	private final long id;
	private final FeedbackDTO dto;

	public TestResult(FeedbackDTO dto) {
		if (dto.type() != FeedbackType.AUTOMATIC) {
			throw new IllegalArgumentException("Feedback type must be automatic for test results");
		}

		this.id = dto.id();
		this.dto = dto;
	}

	public String getTestName() {
		return this.dto.testCase().testName();
	}

	public double getPoints() {
		return this.dto.credits();
	}

	protected FeedbackDTO getDto() {
		return this.dto;
	}

	@Override
	public String toString() {
		return "%s: %.3fP".formatted(this.getTestName(), this.getPoints());
	}
}
