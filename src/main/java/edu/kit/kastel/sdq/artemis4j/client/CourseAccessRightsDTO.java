/* Licensed under EPL-2.0 2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CourseAccessRightsDTO(
        @JsonProperty long courseId, @JsonProperty Set<CourseRole> roles) {
    public CourseAccessRightsDTO {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
