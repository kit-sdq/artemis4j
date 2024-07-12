/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingExerciseDTO;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingSubmissionDTO;
import edu.kit.kastel.sdq.artemis4j.grading.metajson.AnnotationMappingException;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.GradingConfig;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An exercise, containing many submissions.
 */
public class ProgrammingExercise extends ArtemisConnectionHolder implements Exercise {
	private final ProgrammingExerciseDTO dto;

	private final Course course;

	public ProgrammingExercise(ProgrammingExerciseDTO dto, Course course) {
		super(course);

		this.dto = dto;
		this.course = course;
	}

	@Override
	public long getId() {
		return this.dto.id();
	}

	public String getTestRepositoryUrl() {
		return this.dto.testRepositoryUri();
	}

	public double getMaxPoints() {
		return this.dto.maxPoints();
	}

	public ZonedDateTime getStartDate() {
		return this.dto.startDate();
	}

	public ZonedDateTime getDueDate() {
		return this.dto.dueDate();
	}

	@Override
	public String getTitle() {
		return this.dto.title();
	}

	@Override
	public String getShortName() {
		return this.dto.shortName();
	}

	@Override
	public Course getCourse() {
		return this.course;
	}

	public boolean hasSecondCorrectionRound() {
		return this.dto.secondCorrectionEnabled() != null && this.dto.secondCorrectionEnabled();
	}

	/**
	 * Fetches all submissions for this exercise. This may fetch *many* submissions,
	 * and does not cache the result, so be careful.
	 *
	 * @param correctionRound The correction round to fetch submissions for
	 * @param onlyOwn         Whether to only fetch submissions that the current
	 *                        user has assessed
	 * @return
	 * @throws ArtemisNetworkException
	 */
	public List<ProgrammingSubmission> fetchSubmissions(int correctionRound, boolean onlyOwn) throws ArtemisNetworkException {
		return ProgrammingSubmissionDTO.fetchAll(this.getConnection().getClient(), this.getId(), correctionRound, onlyOwn).stream()
				.map(dto -> new ProgrammingSubmission(dto, this, correctionRound)).toList();
	}

	public List<ProgrammingSubmission> fetchSubmissions(int correctionRound) throws ArtemisNetworkException {
		return this.fetchSubmissions(correctionRound, !this.getCourse().isInstructor(this.getConnection().getAssessor()));
	}

	/**
	 * Fetches all submissions from correction round 1 and 2 (if enabled).
	 * 
	 * @return
	 * @throws ArtemisNetworkException
	 */
	public List<ProgrammingSubmission> fetchSubmissions() throws ArtemisNetworkException {
		List<ProgrammingSubmission> submissions = new ArrayList<>(this.fetchSubmissions(0));
		if (this.hasSecondCorrectionRound()) {
			submissions.addAll(this.fetchSubmissions(1));
		}

		return submissions;
	}

	/**
	 * Tries to lock the next submission for this exercise. If successful, returns
	 * the assessment.
	 *
	 * @param correctionRound
	 * @param gradingConfig
	 * @return An empty optional if no submission was available to lock, otherwise
	 *         the assessment
	 * @throws AnnotationMappingException
	 * @throws ArtemisNetworkException
	 */
	public Optional<Assessment> tryLockNextSubmission(int correctionRound, GradingConfig gradingConfig)
			throws AnnotationMappingException, ArtemisNetworkException {
		this.assertGradingConfigValid(gradingConfig);

		// This line already locks the submission, but doesn't tell us what the relevant
		// ResultDTO is
		var dto = ProgrammingSubmissionDTO.lockNextSubmission(this.getConnection().getClient(), this.getId(), correctionRound);
		if (dto.isEmpty()) {
			return Optional.empty();
		}

		// Second lock call to get the ResultDTO
		try {
			var lockResult = this.tryLockSubmission(dto.get().id(), correctionRound, gradingConfig);
			return Optional.of(lockResult.orElseThrow(IllegalStateException::new));
		} catch (MoreRecentSubmissionException ex) {
			// The student has submitted a new submission between our two lock calls
			// We assume that this doesn't happen to make downstream error handling simpler
			throw new IllegalStateException("A new submission has been created between successive locking calls", ex);
		}
	}

	/**
	 * Tries to lock a specific submission for this exercise. Locking is reentrant,
	 * i.e. a single user may lock the same submission multiple times.
	 *
	 * @param submissionId
	 * @param correctionRound
	 * @param gradingConfig
	 * @return An empty optional if a *different* user has already locked the
	 *         submission, otherwise the assessment
	 * @throws AnnotationMappingException    If the annotations that were already
	 *                                       present could not be mapped given the
	 *                                       gradingConfig
	 * @throws ArtemisNetworkException       Generic network failure
	 * @throws MoreRecentSubmissionException If the requested submission is not the
	 *                                       most recent submission of the
	 *                                       corresponding student (i.e.
	 *                                       participation)
	 */
	public Optional<Assessment> tryLockSubmission(long submissionId, int correctionRound, GradingConfig gradingConfig)
			throws AnnotationMappingException, ArtemisNetworkException, MoreRecentSubmissionException {
		this.assertGradingConfigValid(gradingConfig);

		var locked = ProgrammingSubmissionDTO.lock(this.getConnection().getClient(), submissionId, correctionRound);

		if (locked.id() != submissionId) {
			// Artemis automatically returns the most recent submission associated with the
			// same participation
			// as the requested submission
			throw new MoreRecentSubmissionException(submissionId, locked.id(), locked.participation().id());
		}

		if (locked.results() == null) {
			throw new IllegalStateException("Locking returned a submission %d without results".formatted(submissionId));
		}

		if (locked.results().size() != 1) {
			throw new IllegalStateException("Locking returned %d results, expected 1".formatted(locked.results().size()));
		}
		var result = locked.results().getFirst();

		// Locking was successful if we are the assessor
		// The webui of Artemis does the same check
		if (result.assessor() == null || result.assessor().id() != this.getConnection().getAssessor().getId()) {
			return Optional.empty();
		}

		var submission = new ProgrammingSubmission(locked, this, correctionRound);
		return Optional.of(new Assessment(result, gradingConfig, submission, correctionRound));
	}

	@Override
	public String toString() {
		return this.getTitle();
	}

	private void assertGradingConfigValid(GradingConfig gradingConfig) {
		if (!gradingConfig.isValidForExercise(this)) {
			throw new IllegalArgumentException("Grading config is not valid for this exercise");
		}
	}
}
