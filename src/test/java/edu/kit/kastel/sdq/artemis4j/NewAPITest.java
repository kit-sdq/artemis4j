package edu.kit.kastel.sdq.artemis4j;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.new_api.Annotation;
import edu.kit.kastel.sdq.artemis4j.new_api.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.new_api.penalty.GradingConfig;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class NewAPITest {
    private static final String ARTEMIS_URL = "artemis-test.sdq.kastel.kit.edu";
    private static final String ARTEMIS_USERNAME = System.getenv("ARTEMIS_USER");
    private static final String ARTEMIS_PASSWORD = System.getenv("ARTEMIS_PASSWORD");

    @Test
    void testLogin() throws ArtemisClientException, IOException {
        var gradingConfig = GradingConfig.readFromString(Files.readString(Path.of("src/test/resources/grading-config-sheet1taskB.json")));

        var artemis = new ArtemisInstance(ARTEMIS_URL);
        var client = ArtemisClient.fromUsernamePassword(artemis, ARTEMIS_USERNAME, ARTEMIS_PASSWORD);
        System.out.println("User is " + client.getAssessor().getLogin());

        var course = client.getCourses().get(4);
        System.out.println(course.getTitle());

        var exercise = course.getExercises().get(0);
        System.out.println(exercise.getTitle());

        var assessment = exercise.tryLockSubmission(523, 0, gradingConfig).orElseThrow();
        assessment.clearAnnotations();

        var mistakeType = gradingConfig.getMistakeTypeById("hardcodedLogic").get();
        for (int i = 0; i < 5; i++) {
            String customMessage = "aaa" + "h".repeat(1000);
            assessment.addAnnotation(new Annotation(mistakeType, "src/edu/kit/kastel/StringUtility.java", 12, 12, customMessage, null, Annotation.AnnotationSource.MANUAL_FIRST_ROUND));
        }

        try {
            assessment.saveOrSubmit(true, Locale.GERMANY);
        } catch (Exception ex) {
            assessment.cancel();
            throw ex;
        }
    }
}
