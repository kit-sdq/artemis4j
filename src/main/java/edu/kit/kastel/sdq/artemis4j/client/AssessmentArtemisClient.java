/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.client;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.Exercise;
import edu.kit.kastel.sdq.artemis4j.api.artemis.ExerciseStats;
import edu.kit.kastel.sdq.artemis4j.api.artemis.assessment.*;
import edu.kit.kastel.sdq.artemis4j.api.artemis.stats.Stats;
import edu.kit.kastel.sdq.artemis4j.api.artemis.stats.Timing;
import edu.kit.kastel.sdq.artemis4j.api.client.IAssessmentArtemisClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class AssessmentArtemisClient extends AbstractArtemisClient implements IAssessmentArtemisClient {
	private static final Logger log = LoggerFactory.getLogger(AssessmentArtemisClient.class);

	private static final String PROGRAMMING_SUBMISSION_WIHOUT_ASSESSMENT_PATH = "programming-submission-without-assessment";
	private static final String PARTICIPATIONS_PATH_PART = "participations";
	private static final String MANUAL_RESULTS_PATH_PART = "manual-results";
	private static final String CORRECTION_ROUND_QUERY_PARAM = "correction-round";
	private static final String LOCK_QUERY_PARAM = "lock";
	private static final String SUBMIT_QUERY_PARAM = "submit";
	protected static final String STATS_FOR_ASSESSMENT_DASHBOARD_PATH = "stats-for-assessment-dashboard";

	private final OkHttpClient client;

	public AssessmentArtemisClient(final String hostname, String token) {
		super(hostname);
		this.client = this.createClient(token);
	}

	@Override
	public void saveAssessment(int participationId, boolean submit, AssessmentResult assessment) throws ArtemisClientException {
		String assessmentPayload = this.payload(assessment);
		log.info("Saving assessment for submission {} with json: {}", assessment.id, assessmentPayload);

		Request request = new Request.Builder() //
				.url(this.path(PARTICIPATIONS_PATH_PART, participationId, MANUAL_RESULTS_PATH_PART).newBuilder()
						.addQueryParameter(SUBMIT_QUERY_PARAM, String.valueOf(submit)).build())
				.put(RequestBody.create(assessmentPayload, JSON)).build();

		this.call(this.client, request, null);
	}

	@Override
	public LockResult startAssessment(Submission submission) throws ArtemisClientException {
		Request request = new Request.Builder() //
				.url(this.path(PROGRAMMING_SUBMISSIONS_PATHPART, submission.getSubmissionId(), LOCK_QUERY_PARAM).newBuilder()
						.addQueryParameter(CORRECTION_ROUND_QUERY_PARAM, String.valueOf(submission.getCorrectionRound())).build())
				.get().build();

		LockResult result = this.call(this.client, request, LockResult.class);
		assert result != null;
		result.init(this);
		return result;
	}

	@Override
	public Optional<LockResult> startNextAssessment(Exercise exercise, int correctionRound) throws ArtemisClientException {
		Request request = new Request.Builder() //
				.url(this.path(EXERCISES_PATHPART, exercise.getExerciseId(), PROGRAMMING_SUBMISSION_WIHOUT_ASSESSMENT_PATH).newBuilder()
						.addQueryParameter(CORRECTION_ROUND_QUERY_PARAM, String.valueOf(correctionRound))
						.addQueryParameter(LOCK_QUERY_PARAM, String.valueOf(true)).build())
				.get().build();

		try (Response response = this.client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				return Optional.empty();
			}
			assert response.body() != null;
			var lockResult = this.read(response.body().string(), LockResult.class);
			lockResult.init(this);
			return Optional.of(lockResult);
		} catch (IOException e) {
			throw new ArtemisClientException(e.getMessage(), e);
		}

	}

	@Override
	public ExerciseStats getStats(Exercise exercise) throws ArtemisClientException {
		Request request = new Request.Builder() //
				.url(this.path(EXERCISES_PATHPART, exercise.getExerciseId(), STATS_FOR_ASSESSMENT_DASHBOARD_PATH)).get().build();

		Stats stats = null;
		try (Response response = this.client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				return null;
			}
			assert response.body() != null;
			stats = this.read(response.body().string(), Stats.class);
		} catch (IOException e) {
			throw new ArtemisClientException(e.getMessage(), e);
		}

		int submissionsInRound1 = this.countSubmissions(exercise, 0);
		int submissionsInRound2 = 0;
		if (exercise.hasSecondCorrectionRound()) {
			submissionsInRound2 = this.countSubmissions(exercise, 1);
		}

		return new ExerciseStats( //
				this.countInRounds(stats.numberOfAssessmentsOfCorrectionRounds()), //
				stats.numberOfSubmissions().inTime(), //
				stats.totalNumberOfAssessmentLocks(), //
				submissionsInRound1 + submissionsInRound2 //
		);

	}

	private int countInRounds(Timing[] rounds) {
		int countInTime = 0;
		for (var round : rounds) {
			countInTime += round.inTime();
		}
		return countInTime;
	}

	private int countSubmissions(Exercise exercise, int correctionRound) throws ArtemisClientException {
		Request request = new Request.Builder() //
				.url(this.path(EXERCISES_PATHPART, exercise.getExerciseId(), PROGRAMMING_SUBMISSIONS_PATHPART).newBuilder()
						.addQueryParameter("assessedByTutor", String.valueOf(true))
						.addQueryParameter(CORRECTION_ROUND_QUERY_PARAM, String.valueOf(correctionRound)).build())
				.get().build();

		try (Response response = this.client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				return 0;
			}
			assert response.body() != null;
			Submission[] submissionsArray = this.read(response.body().string(), Submission[].class);
			return submissionsArray.length;
		} catch (IOException e) {
			throw new ArtemisClientException(e.getMessage(), e);
		}
	}

	@Override
	public List<Feedback> getFeedbacks(Submission submission, Result result) throws ArtemisClientException {
		Request request = new Request.Builder()
				.url(this.path(PARTICIPATIONS_PATHPART, submission.getParticipation().getParticipationId(),
						RESULTS_PATHPART, result.id, DETAILS_PATHPART))
				.get().build();

		Feedback[] feedbacksArray = this.call(this.client, request, Feedback[].class);
		assert feedbacksArray != null;

		return Arrays.asList(feedbacksArray);
	}

	@Override
	public LongFeedbackText getLongFeedback(int resultId, Feedback feedback) throws ArtemisClientException {
		Request request = new Request.Builder()//
				.url(this.path("results", resultId, "feedbacks", feedback.getId(), "long-feedback")).get().build();

		return this.call(client, request, LongFeedbackText.class);
	}
}
