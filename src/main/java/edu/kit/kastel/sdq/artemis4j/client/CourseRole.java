/* Licensed under EPL-2.0 2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CourseRole {
    STUDENT("students"),
    // With new artemis versions the tutor has been
    // renamed to teaching_assistant (in some contexts)
    // in others, it will still be called tutor
    @JsonProperty("TEACHING_ASSISTANT")
    TUTOR("tutors"),
    EDITOR("editors"),
    INSTRUCTOR("instructors");

    private final String artemisEndpoint;

    CourseRole(String artemisEndpoint) {
        this.artemisEndpoint = artemisEndpoint;
    }

    @Override
    public String toString() {
        return this.artemisEndpoint;
    }
}
