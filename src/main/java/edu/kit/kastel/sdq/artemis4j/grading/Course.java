package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.client.CourseDTO;
import edu.kit.kastel.sdq.artemis4j.client.ExerciseDTO;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.LazyNetworkValue;

import java.util.List;

/**
 * A course, containing exercises.
 */
public class Course extends ArtemisConnectionHolder {
    private final CourseDTO dto;
    private final LazyNetworkValue<List<Exercise>> exercises;

    public Course(CourseDTO dto, ArtemisConnection connection) {
        super(connection);

        this.dto = dto;
        this.exercises = new LazyNetworkValue<>(() -> ExerciseDTO.fetchAll(connection.getClient(), dto.id())
                .stream()
                .map(exerciseDTO -> new Exercise(exerciseDTO, this))
                .toList());
    }

    public int getId() {
        return this.dto.id();
    }

    public String getTitle() {
        return this.dto.title();
    }

    /**
     * Gets all exercises of this course. The result is fetched lazily and then cached.
     * @return
     * @throws ArtemisNetworkException
     */
    public List<Exercise> getExercises() throws ArtemisNetworkException {
        return this.exercises.get();
    }
}
