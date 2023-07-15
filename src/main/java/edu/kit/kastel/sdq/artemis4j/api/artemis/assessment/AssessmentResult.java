/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.artemis.assessment;

import edu.kit.kastel.sdq.artemis4j.api.artemis.User;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Represents the result of an assessment.
 */
public class AssessmentResult implements Serializable {
	@Serial
	private static final long serialVersionUID = -1703764424474018461L;

	@JsonProperty
	public final int id;
	@JsonProperty
	private final String assessmentType;
	@JsonProperty
	private final double score;
	@JsonProperty
	private final boolean rated;
	@JsonProperty
	private final boolean hasFeedback;
	@JsonProperty
	private final User assessor;
	@JsonProperty
	private final List<Feedback> feedbacks;
	@JsonProperty
	private final int codeIssueCount;
	@JsonProperty
	private final int passedTestCaseCount;
	@JsonProperty
	private final int testCaseCount;

	/**
	 * Creates a new assessment result.
	 *
	 * @param id                  the id of the assessment result
	 * @param assessmentType      the type of the assessment
	 * @param score               the score of the assessment
	 * @param rated               whether the assessment is rated
	 * @param hasFeedback         whether the assessment has feedback
	 * @param assessor            the assessor of the assessment
	 * @param feedbacks           the feedbacks of the assessment
	 * @param codeIssueCount      the number of code issues
	 * @param passedTestCaseCount the number of passed test cases
	 * @param testCaseCount       the number of test cases
	 */
	public AssessmentResult(int id, String assessmentType, double score, boolean rated, boolean hasFeedback, User assessor, List<Feedback> feedbacks,
			int codeIssueCount, int passedTestCaseCount, int testCaseCount) {
		this.id = id;
		this.assessmentType = assessmentType;
		this.score = score;
		this.rated = rated;
		this.hasFeedback = hasFeedback;
		this.assessor = assessor;
		this.feedbacks = feedbacks;
		this.codeIssueCount = codeIssueCount;
		this.passedTestCaseCount = passedTestCaseCount;
		this.testCaseCount = testCaseCount;
	}
}
