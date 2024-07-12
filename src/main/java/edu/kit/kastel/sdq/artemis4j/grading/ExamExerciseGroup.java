/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.client.ExerciseGroupDTO;

import java.util.List;

public class ExamExerciseGroup extends ArtemisConnectionHolder {
	private final ExerciseGroupDTO dto;
	private final Exam exam;

	private final List<ProgrammingExercise> exercises;

	public ExamExerciseGroup(ExerciseGroupDTO dto, Exam exam) {
		super(exam);
		this.dto = dto;
		this.exam = exam;
		this.exercises = dto.exercises().stream().map(exerciseDto -> new ProgrammingExercise(exerciseDto, exam.getCourse())).toList();
	}

	public Exam getExam() {
		return exam;
	}

	public long getId() {
		return this.dto.id();
	}

	public String getTitle() {
		return this.dto.title();
	}

	public List<ProgrammingExercise> getProgrammingExercises() {
		return this.exercises;
	}

	public ProgrammingExercise getProgrammingExerciseById(long id) {
		return this.exercises.stream().filter(exercise -> exercise.getId() == id).findFirst().orElseThrow();
	}

	@Override
	public String toString() {
		return this.getTitle();
	}
}
