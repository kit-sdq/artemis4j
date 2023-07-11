/* Licensed under EPL-2.0 2022. */
package edu.kit.kastel.sdq.artemis4j.api.artemis.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Objects;

public record Stats(@JsonProperty Timing numberOfSubmissions, //
		@JsonProperty Timing[] numberOfAssessmentsOfCorrectionRounds, //
		@JsonProperty int totalNumberOfAssessmentLocks //
) {
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Stats stats = (Stats) o;

		if (totalNumberOfAssessmentLocks != stats.totalNumberOfAssessmentLocks)
			return false;
		if (!Objects.equals(numberOfSubmissions, stats.numberOfSubmissions))
			return false;
		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		return Arrays.equals(numberOfAssessmentsOfCorrectionRounds, stats.numberOfAssessmentsOfCorrectionRounds);
	}

	@Override
	public int hashCode() {
		int result = numberOfSubmissions != null ? numberOfSubmissions.hashCode() : 0;
		result = 31 * result + Arrays.hashCode(numberOfAssessmentsOfCorrectionRounds);
		result = 31 * result + totalNumberOfAssessmentLocks;
		return result;
	}

	@Override
	public String toString() {
		return "Stats{" + "numberOfSubmissions=" + numberOfSubmissions + ", numberOfAssessmentsOfCorrectionRounds="
				+ Arrays.toString(numberOfAssessmentsOfCorrectionRounds) + ", totalNumberOfAssessmentLocks=" + totalNumberOfAssessmentLocks + '}';
	}
}
