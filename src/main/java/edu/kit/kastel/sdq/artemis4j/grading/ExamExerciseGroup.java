package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.client.ExerciseGroupDTO;

import java.util.List;

public class ExamExerciseGroup extends ArtemisConnectionHolder {
    private final ExerciseGroupDTO dto;
    private final Exam exam;

    private final List<Exercise> exercises;

    public ExamExerciseGroup(ExerciseGroupDTO dto, Exam exam) {
        super(exam);
        this.dto = dto;
        this.exam = exam;
        this.exercises = dto.exercises().stream()
                .map(exerciseDto -> new Exercise(exerciseDto, exam.getCourse()))
                .toList();
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

    public List<Exercise> getExercises() {
        return this.exercises;
    }
}