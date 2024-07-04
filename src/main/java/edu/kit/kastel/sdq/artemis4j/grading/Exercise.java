package edu.kit.kastel.sdq.artemis4j.grading;

public interface Exercise {
    long getId();

    String getTitle();

    String getShortName();

    Course getCourse();
}
