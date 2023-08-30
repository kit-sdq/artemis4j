/* Licensed under EPL-2.0 2023. */
package edu.kit.kastel.sdq.artemis4j.api.client;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.Course;
import edu.kit.kastel.sdq.artemis4j.api.artemis.exam.Exam;
import edu.kit.kastel.sdq.artemis4j.api.artemis.exam.StudentExam;

import java.util.Collections;
import java.util.List;

/**
 * REST-Client to execute calls concerning exams.
 */
public interface IExamArtemisClient {
	/**
	 * Mark all student exams as submitted.
	 *
	 * @param course the course of the exam
	 * @param exam   the exam
	 * @return the result of the toggle process
	 * @throws ArtemisClientException if some errors occur while interacting with
	 *                                artemis
	 */
	SubmitResult markAllExamsAsSubmitted(Course course, Exam exam) throws ArtemisClientException;

	/**
	 * Get all student exams for the given course and exam.
	 *
	 * @param course the course of the exam
	 * @param exam   the exam
	 * @return all student exams for the given course and exam
	 * @throws ArtemisClientException if some errors occur while interacting with
	 *                                artemis
	 */
	List<StudentExam> getStudentExams(Course course, Exam exam) throws ArtemisClientException;

	/**
	 * The result object of the toggle process.
	 *
	 * @param exams            all student exams
	 * @param toggleSuccessful the successfully toggled student exams
	 * @param toggleFailed     the failed toggled student exams
	 */
	record SubmitResult(List<StudentExam> exams, List<StudentExam> toggleSuccessful, List<StudentExam> toggleFailed) {
		public SubmitResult {
			exams = Collections.unmodifiableList(exams);
			toggleSuccessful = Collections.unmodifiableList(toggleSuccessful);
			toggleFailed = Collections.unmodifiableList(toggleFailed);
		}
	}
}
