/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;

/**
 * There are many more fields in the actual entity that are added as needed
 *
 * @param sshCloneURLTemplate Base URL for cloning repositories via SSH
 */
public record ManagementInfoDTO(@JsonProperty String sshCloneURLTemplate) {
    public static ManagementInfoDTO fetch(ArtemisClient client) throws ArtemisNetworkException {
        return ArtemisRequest.get()
                .path(List.of("management", "info"))
                .managementRequest(true)
                .executeAndDecode(client, ManagementInfoDTO.class);
    }
}
