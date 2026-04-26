/* Licensed under EPL-2.0 2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

public record UserPublicInfoDTO(
        @JsonProperty long id,
        @JsonProperty String login,
        @JsonProperty @Nullable String name,
        @JsonProperty @Nullable String firstName,
        @JsonProperty @Nullable String lastName,
        @JsonProperty @Nullable Boolean isInstructor,
        @JsonProperty @Nullable Boolean isEditor,
        @JsonProperty @Nullable Boolean isTeachingAssistant,
        @JsonProperty @Nullable Boolean isStudent) {

    /**
     * Returns the course roles of the user.
     * <p>
     * A user can have multiple roles, or none if the user is not in the course.
     * Note that there are endpoints that do not provide the information for
     * the roles of the user, in which case this function will return an empty set.
     * @return the roles of the user or an empty set if there is no information
     *         or the user is not in the course
     */
    public Set<CourseRole> getRoles() {
        Set<CourseRole> result = new HashSet<>();

        if (Boolean.TRUE.equals(this.isInstructor)) {
            result.add(CourseRole.INSTRUCTOR);
        }

        if (Boolean.TRUE.equals(this.isEditor)) {
            result.add(CourseRole.EDITOR);
        }

        if (Boolean.TRUE.equals(this.isTeachingAssistant)) {
            result.add(CourseRole.TUTOR);
        }

        if (Boolean.TRUE.equals(this.isStudent)) {
            result.add(CourseRole.STUDENT);
        }

        return result;
    }

    public UserDTO toUserDTO(CourseDTO courseDTO) {
        return new UserDTO(
                this.id,
                this.login,
                this.name,
                this.firstName,
                this.lastName,
                null,
                true,
                null,
                new ArrayList<>(courseDTO.mapToGroupNames(this.getRoles())),
                null,
                null);
    }
}
