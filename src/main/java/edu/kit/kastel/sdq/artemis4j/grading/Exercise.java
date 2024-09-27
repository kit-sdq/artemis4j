/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.AssessmentStatsDTO;

public interface Exercise {
    long getId();

    String getTitle();

    String getShortName();

    Course getCourse();

    AssessmentStatsDTO fetchAssessmentStats() throws ArtemisNetworkException;
}
