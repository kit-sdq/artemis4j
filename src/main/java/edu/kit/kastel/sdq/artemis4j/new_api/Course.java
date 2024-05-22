package edu.kit.kastel.sdq.artemis4j.new_api;

import edu.kit.kastel.sdq.artemis4j.dto.artemis.CourseDTO;
import edu.kit.kastel.sdq.artemis4j.dto.artemis.ExerciseDTO;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisClient;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.new_client.LazyNetworkValue;

import java.util.List;

public class Course extends ArtemisClientHolder {
    private final CourseDTO dto;
    private final LazyNetworkValue<List<Exercise>> exercises;

    public Course(CourseDTO dto, ArtemisClient client) {
        super(client);

        this.dto = dto;
        this.exercises = new LazyNetworkValue<>(() -> ExerciseDTO.fetchAll(client, dto.id())
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

    public List<Exercise> getExercises() throws ArtemisNetworkException {
        return this.exercises.get();
    }
}
