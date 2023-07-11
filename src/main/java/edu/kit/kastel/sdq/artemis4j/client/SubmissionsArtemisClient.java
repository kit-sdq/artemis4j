/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.client;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.Exercise;
import edu.kit.kastel.sdq.artemis4j.api.artemis.User;
import edu.kit.kastel.sdq.artemis4j.api.artemis.assessment.Submission;
import edu.kit.kastel.sdq.artemis4j.api.client.ISubmissionsArtemisClient;

import java.util.Arrays;
import java.util.List;

public class SubmissionsArtemisClient extends AbstractArtemisClient implements ISubmissionsArtemisClient {
	private final OkHttpClient client;
	private final User assessor;

	public SubmissionsArtemisClient(final String hostname, String token, User assessor) {
		super(hostname);
		this.client = this.createClient(token);
		this.assessor = assessor;
	}

	@Override
	public List<Submission> getSubmissions(Exercise exercise, int correctionRound) throws ArtemisClientException {
		boolean isInstructor = exercise.getCourse().isInstructor(this.assessor);
		Request request = new Request.Builder() //
				.url(this.path(EXERCISES_PATHPART, exercise.getExerciseId(), PROGRAMMING_SUBMISSIONS_PATHPART).newBuilder()
						.addQueryParameter("assessedByTutor", String.valueOf(!isInstructor))
						.addQueryParameter("correction-round", String.valueOf(correctionRound)).build())
				.get().build();

		Submission[] submissionsArray = this.call(this.client, request, Submission[].class);
		for (Submission submission : submissionsArray) {
			submission.init(correctionRound);
		}

		return Arrays.asList(submissionsArray);
	}

	@Override
	public Submission getSubmissionById(Exercise exercise, int submissionId) throws ArtemisClientException {
		List<Submission> submissions = this.getSubmissions(exercise);
		Submission target = submissions.stream().filter(s -> s.getSubmissionId() == submissionId).findFirst().orElse(null);
		if (target == null) {
			throw new ArtemisClientException(String.format("No submission found for SubmissionId '%d'", submissionId));
		}
		return target;
	}

}
