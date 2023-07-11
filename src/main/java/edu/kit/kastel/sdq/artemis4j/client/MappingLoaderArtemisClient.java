/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.client;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.Course;
import edu.kit.kastel.sdq.artemis4j.api.artemis.Exercise;
import edu.kit.kastel.sdq.artemis4j.api.artemis.IMappingLoader;
import edu.kit.kastel.sdq.artemis4j.api.artemis.assessment.Submission;
import edu.kit.kastel.sdq.artemis4j.api.artemis.exam.Exam;
import edu.kit.kastel.sdq.artemis4j.api.artemis.exam.ExerciseGroup;
import edu.kit.kastel.sdq.artemis4j.api.client.ICourseArtemisClient;
import edu.kit.kastel.sdq.artemis4j.api.client.ISubmissionsArtemisClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MappingLoaderArtemisClient extends AbstractArtemisClient implements ICourseArtemisClient, IMappingLoader {
	private final ISubmissionsArtemisClient submissionClient;

	private final OkHttpClient client;

	public MappingLoaderArtemisClient(ISubmissionsArtemisClient submissionClient, String hostname, String token) {
		super(hostname);
		this.client = this.createClient(token);
		this.submissionClient = submissionClient;
	}

	@Override
	public List<Course> getCoursesForAssessment() throws ArtemisClientException {
		Request request = new Request.Builder().url(this.path(COURSES_PATHPART)).get().build();
		Course[] coursesArray = this.call(this.client, request, Course[].class);
		for (Course course : coursesArray) {
			course.init(this);
		}
		return Arrays.asList(coursesArray);
	}

	@Override
	public List<ExerciseGroup> getExerciseGroupsForExam(Exam artemisExam, Course course) throws ArtemisClientException {
		Request request = new Request.Builder() //
				.url(this.path(COURSES_PATHPART, course.getCourseId(), EXAMS_PATHPART, artemisExam.getExamId(), "exam-for-assessment-dashboard")).get().build();

		// need to retrieve the exerciseGroups array root node to deserialize it!
		List<ExerciseGroup> exerciseGroups = this.call(this.client, request, ExerciseGroupWrapper.class).getExerciseGroups();

		for (ExerciseGroup exerciseGroup : exerciseGroups) {
			exerciseGroup.init(this, course, artemisExam);
		}
		return new ArrayList<>(exerciseGroups);
	}

	@Override
	public List<Exam> getExamsForCourse(Course course) throws ArtemisClientException {
		Request request = new Request.Builder() //
				.url(this.path(COURSES_PATHPART, course.getCourseId(), EXAMS_PATHPART)).get().build();

		Exam[] examsArray = this.call(this.client, request, Exam[].class);
		for (Exam exam : examsArray) {
			exam.init(this, course);
		}
		return Arrays.asList(examsArray);
	}

	@Override
	public List<Exercise> getGradingExercisesForCourse(Course course) throws ArtemisClientException {
		Request request = new Request.Builder() //
				.url(this.path(COURSES_PATHPART, course.getCourseId(), "with-exercises")).get().build();

		// get the part of the json that we want to deserialize
		final List<Exercise> exercises = this.call(this.client, request, ExerciseWrapper.class).getExercises();

		for (Exercise exercise : exercises) {
			exercise.init(this, course, null);
		}

		// Here we filter all programming exercises
		return exercises.stream().filter(Exercise::isProgramming).collect(Collectors.toList());
	}

	@Override
	public Submission getSubmissionById(Exercise exercise, int submissionId) throws ArtemisClientException {
		return this.submissionClient.getSubmissionById(exercise, submissionId);
	}

	private static class ExerciseGroupWrapper {
		@JsonProperty
		private final List<ExerciseGroup> exerciseGroups = new ArrayList<>();

		public List<ExerciseGroup> getExerciseGroups() {
			return exerciseGroups == null ? List.of() : new ArrayList<>(exerciseGroups);
		}
	}

	private static class ExerciseWrapper {
		@JsonProperty
		private final List<Exercise> exercises = new ArrayList<>();

		public List<Exercise> getExercises() {
			return exercises == null ? List.of() : new ArrayList<>(exercises);
		}
	}

}
