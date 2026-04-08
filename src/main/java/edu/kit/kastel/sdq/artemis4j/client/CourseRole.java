/* Licensed under EPL-2.0 2026. */
package edu.kit.kastel.sdq.artemis4j.client;

public enum CourseRole {
    STUDENT("students"),
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
