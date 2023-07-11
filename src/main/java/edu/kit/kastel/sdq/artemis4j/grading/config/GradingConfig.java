/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.grading.config;

import edu.kit.kastel.sdq.artemis4j.api.artemis.Exercise;

import java.io.IOException;

/**
 * Encapsulates read access to config sources, which might come from a file
 */
public interface GradingConfig {
	ExerciseConfig getExerciseConfig(Exercise exercise) throws IOException, IllegalStateException;
}
