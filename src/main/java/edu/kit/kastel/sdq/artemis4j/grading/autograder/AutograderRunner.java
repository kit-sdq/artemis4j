/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading.autograder;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import de.firemage.autograder.api.AbstractLinter;
import de.firemage.autograder.api.CheckConfiguration;
import de.firemage.autograder.api.JavaVersion;
import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.api.Translatable;
import de.firemage.autograder.api.loader.AutograderLoader;
import edu.kit.kastel.sdq.artemis4j.grading.Assessment;
import edu.kit.kastel.sdq.artemis4j.grading.ClonedProgrammingSubmission;

public final class AutograderRunner {
    private AutograderRunner() {
    }

    public static AutograderStats runAutograder(Assessment assessment, ClonedProgrammingSubmission submission, Locale locale, int threads,
            Consumer<String> statusConsumer) throws AutograderFailedException {
        if (!assessment.getSubmission().equals(submission.getSubmission())) {
            throw new IllegalArgumentException("The assessment and submission do not match");
        }

        try {
            if (!AutograderLoader.isAutograderLoaded()) {
                statusConsumer.accept("Downloading the latest Autograder release");
                AutograderLoader.loadFromGithubWithExtraChecks();
            } else if (!AutograderLoader.isCurrentVersionLoaded()) {
                throw new AutograderFailedException("There is a more recent version of the Autograder available");
            }
        } catch (IOException e) {
            throw new AutograderFailedException("Failed to check for or download the latest Autograder release", e);
        }

        var problemTypesMap = assessment.getConfig().getMistakeTypes().stream().flatMap(m -> m.getAutograderProblemTypes().stream().map(p -> Map.entry(p, m)))
                .distinct().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var checkConfiguration = CheckConfiguration.fromProblemTypes(problemTypesMap.keySet().stream().map(AutograderLoader::convertProblemType).toList());

        try (var tempLocation = AutograderLoader.instantiateTempLocation()) {
            var autograderBuilder = AbstractLinter.builder(locale).threads(threads).tempLocation(tempLocation).maxProblemsPerCheck(-1);
            var autograder = AutograderLoader.instantiateLinter(autograderBuilder);

            Consumer<Translatable> statusConsumerWrapper = status -> statusConsumer.accept(autograder.translateMessage(status));

            var problems = autograder.checkFile(submission.getSubmissionSourcePath(), JavaVersion.JAVA_21, checkConfiguration, statusConsumerWrapper);

            for (var problem : problems) {
                var mistakeType = problemTypesMap.get(problem.getType());
                var position = problem.getPosition();
                assessment.addAutograderAnnotation(mistakeType, position.path().toString(), position.startLine(), position.endLine(),
                        autograder.translateMessage(problem.getExplanation()));
            }

            return new AutograderStats(problems.size());
        } catch (IOException | LinterException e) {
            throw new AutograderFailedException(e);
        }
    }
}
