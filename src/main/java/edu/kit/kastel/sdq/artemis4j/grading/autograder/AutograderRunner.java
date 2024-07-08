/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading.autograder;

import de.firemage.autograder.core.CheckConfiguration;
import de.firemage.autograder.core.Linter;
import de.firemage.autograder.core.LinterException;
import de.firemage.autograder.core.LinterStatus;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.compiler.JavaVersion;
import de.firemage.autograder.core.file.TempLocation;
import de.firemage.autograder.core.file.UploadedFile;
import edu.kit.kastel.sdq.artemis4j.grading.Assessment;
import edu.kit.kastel.sdq.artemis4j.grading.ClonedProgrammingSubmission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class AutograderRunner {
	private AutograderRunner() {
	}

	public static AutograderStats runAutograder(Assessment assessment, ClonedProgrammingSubmission submission, Locale locale, int threads,
			Consumer<String> statusConsumer) throws AutograderFailedException {
		if (!assessment.getSubmission().equals(submission.getSubmission())) {
			throw new IllegalArgumentException("The assessment and submission do not match");
		}

		var problemTypesMap = assessment.getConfig().getMistakeTypes().stream().flatMap(m -> m.getAutograderProblemTypes().stream().map(p -> Map.entry(p, m)))
				.distinct().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		var checkConfiguration = CheckConfiguration.fromProblemTypes(new ArrayList<>(problemTypesMap.keySet()));

		try (TempLocation tempLocation = TempLocation.of(".autograder-tmp")) {
			Linter autograder = Linter.builder(locale).threads(threads).tempLocation(tempLocation).maxProblemsPerCheck(-1).build();

			Consumer<LinterStatus> statusConsumerWrapper = status -> statusConsumer.accept(autograder.translateMessage(status.getMessage()));

			List<Problem> problems;
			try (UploadedFile uploadedFile = UploadedFile.build(submission.getSubmissionSourcePath(), JavaVersion.JAVA_17, tempLocation, statusConsumerWrapper,
					null)) {
				problems = autograder.checkFile(uploadedFile, checkConfiguration, statusConsumerWrapper);
			}

			for (var problem : problems) {
				var mistakeType = problemTypesMap.get(problem.getProblemType());
				var position = problem.getPosition();
				assessment.addAutograderAnnotation(mistakeType, position.file().toString(), position.startLine(), position.endLine(),
						autograder.translateMessage(problem.getExplanation()));
			}

			return new AutograderStats(problems.size());
		} catch (IOException | LinterException e) {
			throw new AutograderFailedException(e);
		}
	}
}
