/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.grading.location;

import java.util.List;

interface PathFormatter {
    String formatLocation(Location location);

    String formatFile(String name, List<Location> locations);

    String formatFolder(String name, List<String> segments);
}
