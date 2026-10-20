/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.util.List;

import edu.kit.kastel.sdq.artemis4j.client.ExamExerciseDTO;
import edu.kit.kastel.sdq.artemis4j.client.ExerciseGroupDTO;
import edu.kit.kastel.sdq.artemis4j.client.ExerciseType;
import org.jspecify.annotations.Nullable;

public class ExamExerciseGroup extends ArtemisConnectionHolder {
    private final ExerciseGroupDTO dto;
    private final Exam exam;

    private final List<ProgrammingExercise> exercises;

    public ExamExerciseGroup(ExerciseGroupDTO dto, Exam exam) {
        super(exam);
        this.dto = dto;
        this.exam = exam;
        var exerciseDTOs = dto.exercises() == null ? List.<ExamExerciseDTO>of() : dto.exercises();
        this.exercises = exerciseDTOs.stream()
                .filter(exercise -> exercise.type() == ExerciseType.PROGRAMMING)
                .map(exerciseDto -> ProgrammingExercise.fromExamExerciseDTO(exerciseDto, exam))
                .toList();
    }

    public Exam getExam() {
        return exam;
    }

    public long getId() {
        return this.dto.id();
    }

    public @Nullable String getTitle() {
        return this.dto.title();
    }

    public List<ProgrammingExercise> getProgrammingExercises() {
        return this.exercises;
    }

    public ProgrammingExercise getProgrammingExerciseById(long id) {
        return this.exercises.stream()
                .filter(exercise -> exercise.getId() == id)
                .findFirst()
                .orElseThrow();
    }

    @Override
    public String toString() {
        return this.getTitle();
    }
}
