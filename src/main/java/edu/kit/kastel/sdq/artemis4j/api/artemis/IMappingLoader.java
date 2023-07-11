/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.artemis;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.assessment.Submission;
import edu.kit.kastel.sdq.artemis4j.api.artemis.exam.Exam;
import edu.kit.kastel.sdq.artemis4j.api.artemis.exam.ExerciseGroup;

import java.util.List;

public interface IMappingLoader {

	List<ExerciseGroup> getExerciseGroupsForExam(Exam artemisExam, Course course) throws ArtemisClientException;

	List<Exam> getExamsForCourse(Course course) throws ArtemisClientException;

	List<Exercise> getGradingExercisesForCourse(Course course) throws ArtemisClientException;

	Submission getSubmissionById(Exercise exercise, int submissionId) throws ArtemisClientException;

}
