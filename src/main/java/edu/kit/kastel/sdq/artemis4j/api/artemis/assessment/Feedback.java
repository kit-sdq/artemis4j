/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.artemis.assessment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.client.IFeedbackClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Artemis Object Mapping: Represents kinds of "sub assessments", each
 * contributing to the total credit sum. Types:
 * <ul>
 * <li>{@link FeedbackType#MANUAL} represents single line annotations</li>
 * <li>{@link FeedbackType#MANUAL_UNREFERENCED} represents remarks that are
 * shown below the code</li>
 * <li>{@link FeedbackType#AUTOMATIC} represents e.g. unit test results</li>
 * </ul>
 * Whether a method returns null or not depends on the FeedbackType
 * {@link Feedback#getFeedbackType()} }!
 */
@JsonInclude(Include.NON_NULL)
public class Feedback implements Comparable<Feedback>, Serializable {
	@Serial
	private static final long serialVersionUID = 4531964872375020131L;

	private static final Logger log = LoggerFactory.getLogger(Feedback.class);

	@JsonProperty("type")
	private String type;
	@JsonProperty("credits")
	private Double credits;
	@JsonProperty("id")
	private Integer id; // null for all manual feedback
	@JsonProperty("positive")
	private Boolean positive; // null for all manual feedback
	@JsonProperty("visibility")
	private String visibility; // null for all manual feedback

	// "File src/edu/kit/informatik/BubbleSort.java at line 13"
	@JsonProperty("text")
	private String codeLocationHumanReadable; // null for UNREFERENCED manual feedback and also for test cases
	// "file:src/edu/kit/informatik/BubbleSort.java_line:12"
	@JsonProperty("reference")
	private String codeLocation; // null for UNREFERENCED manual feedback and auto feedback
	@JsonProperty("detailText")
	private String detailText; // null for auto feedback
	private transient String detailTextComplete = null;
	@JsonProperty("hasLongFeedbackText")
	private Boolean hasLongFeedbackText;

	@JsonProperty("testCase")
	private TestCase testCase;

	/**
	 * Only for Jackson deserialization.
	 */
	public Feedback() {
		// NOP
	}

	/**
	 * This constructor is used for manual feedback.
	 *
	 * @param type                      the type of the feedback
	 * @param credits                   the credits of the feedback
	 * @param id                        the id of the feedback
	 * @param positive                  whether the feedback is positive
	 * @param visibility                the visibility of the feedback
	 * @param codeLocationHumanReadable the text of the feedback
	 * @param codeLocation              the reference of the feedback
	 * @param detailText                the detail text of the feedback
	 */
	public Feedback(String type, Double credits, Integer id, Boolean positive, String visibility, String codeLocationHumanReadable, String codeLocation,
			String detailText) {
		this.type = type;
		this.credits = credits;
		this.id = id;
		this.positive = positive;
		this.visibility = visibility;
		this.codeLocationHumanReadable = codeLocationHumanReadable;
		this.codeLocation = codeLocation;
		this.detailText = detailText;
	}

	public void init(IFeedbackClient feedbackClient, int resultId) {
		// Init Long FeedbackTexts
		if (this.hasLongFeedbackText == null || !this.hasLongFeedbackText) {
			return;
		}
		try {
			this.detailTextComplete = feedbackClient.getLongFeedback(resultId, this);
		} catch (ArtemisClientException e) {
			log.error("Could not get long feedback for feedback with id {} and result id {}.", this.getId(), resultId, e);
		}
	}

	/**
	 * @return this Feedbacks contribution to the total credit sum. Can be positive
	 *         or negative.
	 */
	public Double getCredits() {
		if (Objects.equals("NEVER", this.visibility)) {
			// Bugfix for wrong Artemis points for NEVER visibility
			return 0.0;
		}
		return this.credits;
	}

	/**
	 * @return detail text shown in the Artemis GUI on viewing the assessment: Comes
	 *         after {@link #getCodeLocationHumanReadable()}, if that is not
	 *         <b>null</b>.<br/>
	 *         <b>null</b> for {@link FeedbackType#AUTOMATIC}
	 */
	public String getDetailText() {
		return this.detailTextComplete == null ? this.detailText : this.detailTextComplete;
	}

	/**
	 * @return {@link #getType()} , but typed ;)
	 */
	public FeedbackType getFeedbackType() {
		return FeedbackType.valueOfIgnoreCase(this.type);
	}

	/**
	 * @return <b>null</b> for {@link FeedbackType#MANUAL} and
	 *         {@link FeedbackType#MANUAL_UNREFERENCED}
	 */
	public Integer getId() {
		return this.id;
	}

	/**
	 * @return not sure what. Unimportant for now.<br/>
	 *         <b>null</b> for {@link FeedbackType#MANUAL} and
	 *         {@link FeedbackType#MANUAL_UNREFERENCED}
	 */
	public Boolean getPositive() {
		return this.positive;
	}

	/**
	 * @return
	 *         <ul>
	 *         <li>code reference string like so:
	 *         "file:${CLASS_FILE_PATH}.java_line:${START_LINE}".</li>
	 *         <li>Note that Artemis does only consider single lines.</li>
	 *         <li><b>null</b> for {@link FeedbackType#AUTOMATIC} and
	 *         {@link FeedbackType#MANUAL_UNREFERENCED}</li>
	 *         </ul>
	 */
	public String getCodeLocation() {
		return this.codeLocation;
	}

	/**
	 * @return text shown in the Artemis GUI on viewing the assessment.<br/>
	 *         <b>null</b> for {@link FeedbackType#MANUAL_UNREFERENCED}
	 */
	public String getCodeLocationHumanReadable() {
		return this.codeLocationHumanReadable;
	}

	public String getType() {
		return this.type;
	}

	/**
	 * <b>null</b> for {@link FeedbackType#MANUAL} and
	 * {@link FeedbackType#MANUAL_UNREFERENCED}
	 */
	public String getVisibility() {
		return this.visibility;
	}

	@Override
	public int compareTo(Feedback o) {
		// Sort (1): Automatic before Manual
		// Sort (2): Tests with name containing "Mandatory" before any other test

		boolean similarType = this.getFeedbackType() == o.getFeedbackType()
				|| this.getFeedbackType() != FeedbackType.AUTOMATIC && o.getFeedbackType() != FeedbackType.AUTOMATIC;

		if (!similarType) {
			return this.getFeedbackType() == FeedbackType.AUTOMATIC ? -1 : 1;
		}

		if (this.isMandatoryTest() != o.isMandatoryTest()) {
			return this.isMandatoryTest() ? -1 : 1;
		}

		String thisName = this.getCodeLocationHumanReadable() == null ? "" : this.getCodeLocationHumanReadable();
		String otherName = o.getCodeLocationHumanReadable() == null ? "" : o.getCodeLocationHumanReadable();
		return thisName.compareToIgnoreCase(otherName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.credits, this.detailText, this.id, this.positive, this.codeLocation, this.codeLocationHumanReadable, this.type,
				this.visibility);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		Feedback other = (Feedback) obj;
		return Objects.equals(this.credits, other.credits) && Objects.equals(this.detailText, other.detailText) && Objects.equals(this.id, other.id)
				&& Objects.equals(this.positive, other.positive) && Objects.equals(this.codeLocation, other.codeLocation)
				&& Objects.equals(this.codeLocationHumanReadable, other.codeLocationHumanReadable) && Objects.equals(this.type, other.type)
				&& Objects.equals(this.visibility, other.visibility);
	}

	@JsonIgnore
	public boolean isStaticCodeAnalysis() {
		return this.codeLocationHumanReadable != null && this.codeLocationHumanReadable.startsWith("SCAFeedbackIdentifier");
	}

	@JsonIgnore
	public boolean isMandatoryTest() {
		// Only by naming convention so far, since Artemis has no "mandatory" tests.
		return isTest() && getTestName().toLowerCase().contains("mandatory");
	}

	@JsonIgnore
	public boolean isTest() {
		return this.testCase != null;
	}

	@JsonIgnore
	public String getTestName() {
		return this.testCase == null ? "Unknown Test" : this.testCase.getTestName();
	}
}
