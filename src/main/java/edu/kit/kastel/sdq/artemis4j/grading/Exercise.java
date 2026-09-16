/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.AssessmentStatsDTO;
import org.jspecify.annotations.Nullable;

public interface Exercise {
    long getId();

    String getTitle();

    @Nullable
    String getShortName();

    Course getCourse();

    AssessmentStatsDTO fetchAssessmentStats() throws ArtemisNetworkException;
}
