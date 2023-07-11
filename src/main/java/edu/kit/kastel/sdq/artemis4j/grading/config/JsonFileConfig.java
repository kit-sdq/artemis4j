/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.grading.config;

import edu.kit.kastel.sdq.artemis4j.api.artemis.Exercise;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
 * Implementation of {@link GradingConfig} using a json file.
 */
public class JsonFileConfig implements GradingConfig {

	private ExerciseConfig exerciseConfig;

	private final File configFile;
	private final ObjectMapper oom = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	public JsonFileConfig(File configFile) {
		this.configFile = configFile;
	}

	@Override
	public ExerciseConfig getExerciseConfig(Exercise exercise) throws IOException, IllegalStateException {
		if (this.exerciseConfig == null) {
			this.parse();
		}
		this.exerciseConfig.initialize(exercise);
		return this.exerciseConfig;
	}

	private void parse() throws IOException, IllegalStateException {
		this.exerciseConfig = oom.readValue(this.configFile, ExerciseConfig.class);
	}
}
