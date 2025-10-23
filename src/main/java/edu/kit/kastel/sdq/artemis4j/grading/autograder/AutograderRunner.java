/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.grading.autograder;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import de.firemage.autograder.api.AbstractCodePosition;
import de.firemage.autograder.api.AbstractLinter;
import de.firemage.autograder.api.AbstractProblem;
import de.firemage.autograder.api.CheckConfiguration;
import de.firemage.autograder.api.FailureInformation;
import de.firemage.autograder.api.JavaVersion;
import de.firemage.autograder.api.LinterException;
import de.firemage.autograder.api.Translatable;
import de.firemage.autograder.api.loader.AutograderLoader;
import edu.kit.kastel.sdq.artemis4j.grading.Assessment;
import edu.kit.kastel.sdq.artemis4j.grading.ClonedProgrammingSubmission;
import edu.kit.kastel.sdq.artemis4j.grading.location.LineColumn;
import edu.kit.kastel.sdq.artemis4j.grading.location.Location;

public final class AutograderRunner {
    private AutograderRunner() {}

    public static AutograderStats runAutograder(
            Assessment assessment,
            ClonedProgrammingSubmission submission,
            Locale locale,
            int threads,
            Consumer<? super String> statusConsumer)
            throws AutograderFailedException {
        return runAutograderFallible(
                assessment, submission, locale, threads, statusConsumer, FailureInformation.failFastConsumer());
    }

    public static AutograderStats runAutograderFallible(
            Assessment assessment,
            ClonedProgrammingSubmission submission,
            Locale locale,
            int threads,
            Consumer<? super String> statusConsumer,
            Consumer<FailureInformation> failureConsumer)
            throws AutograderFailedException {
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

        var problemTypesMap = assessment.getConfig().getMistakeTypes().stream()
                .flatMap(m -> m.getAutograderProblemTypes().stream().map(p -> Map.entry(p, m)))
                .distinct()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var checkConfiguration = CheckConfiguration.fromProblemTypes(problemTypesMap.keySet().stream()
                .map(AutograderLoader::convertProblemType)
                .toList());

        try (var tempLocation = AutograderLoader.instantiateTempLocation()) {
            var autograderBuilder = AbstractLinter.builder(locale)
                    .threads(threads)
                    .tempLocation(tempLocation)
                    .maxProblemsPerCheck(-1);
            var autograder = AutograderLoader.instantiateLinter(autograderBuilder);

            Consumer<Translatable> statusConsumerWrapper =
                    status -> statusConsumer.accept(autograder.translateMessage(status));

            var problems = autograder.checkFileFallible(
                    submission.getSubmissionSourcePath(),
                    JavaVersion.JAVA_21,
                    checkConfiguration,
                    statusConsumerWrapper,
                    failureConsumer);

            for (AbstractProblem problem : problems) {
                var mistakeType = problemTypesMap.get(problem.getType());
                var position = problem.getPosition();

                assessment.addAutograderAnnotation(
                        mistakeType,
                        translateToLocation(position),
                        autograder.translateMessage(problem.getExplanation()),
                        problem.getCheckName(),
                        problem.getType(),
                        problem.getMaximumProblemsForCheck().orElse(null));
            }

            return new AutograderStats(problems.size());
        } catch (IOException | LinterException e) {
            throw new AutograderFailedException(e);
        }
    }

    private static Location translateToLocation(AbstractCodePosition position) {
        // The Autograder emits a CodePosition which is based on the SourcePosition type defined by spoon.
        //
        // For some things the autograder emits a code position where the start/end line and start/end column
        // are equal like L10:5 - L10:5.
        //
        // For everything else, it will simply emit the positions defined in spoon.
        //
        // For the SourcePosition type the start and end line/column are 1-based.
        // In addition to that, the end line and column are inclusive as well.
        //
        // The translation to Location is just converting the 1-based start and end line/column to 0-based
        var start = new LineColumn(position.startLine() - 1, position.startColumn() - 1);
        var end = new LineColumn(position.endLine() - 1, position.endColumn() - 1);

        return new Location("src/" + position.path().toString(), start, end);
    }
}
