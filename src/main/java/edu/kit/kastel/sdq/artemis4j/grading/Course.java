package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.LazyNetworkValue;
import edu.kit.kastel.sdq.artemis4j.client.CourseDTO;
import edu.kit.kastel.sdq.artemis4j.client.ExamDTO;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingExerciseDTO;

import java.util.List;

/**
 * A course, containing exercises.
 */
public class Course extends ArtemisConnectionHolder {
    private final CourseDTO dto;
    private final LazyNetworkValue<List<Exercise>> exercises;
    private final LazyNetworkValue<List<Exam>> exams;

    public Course(CourseDTO dto, ArtemisConnection connection) {
        super(connection);

        this.dto = dto;
        this.exercises = new LazyNetworkValue<>(() -> ProgrammingExerciseDTO.fetchAll(connection.getClient(), dto.id())
                .stream()
                .map(exerciseDTO -> new Exercise(exerciseDTO, this))
                .toList());
        this.exams = new LazyNetworkValue<>(() -> ExamDTO.fetchAll(connection.getClient(), dto.id()).stream().map(examDTO -> new Exam(examDTO, this)).toList());
    }

    public int getId() {
        return this.dto.id();
    }

    public String getTitle() {
        return this.dto.title();
    }

    /**
     * Gets all exercises of this course. The result is fetched lazily and then cached.
     *
     * @return
     * @throws ArtemisNetworkException
     */
    public List<Exercise> getExercises() throws ArtemisNetworkException {
        return this.exercises.get();
    }
}
