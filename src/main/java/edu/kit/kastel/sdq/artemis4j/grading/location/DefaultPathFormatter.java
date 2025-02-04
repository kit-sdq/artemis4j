/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.grading.location;

import java.util.List;
import java.util.stream.Collectors;

class DefaultPathFormatter implements PathFormatter {
    @Override
    public String formatLocation(Location location) {
        LineColumn start = location.start();
        LineColumn end = location.end();

        if (start.line() == end.line()) {
            return "L%d".formatted(start.line() + 1);
        }

        return "L%d-%d".formatted(start.line() + 1, end.line() + 1);
    }

    @Override
    public String formatFile(String name, List<Location> locations) {
        if (locations.size() == 1) {
            return name + ":" + this.formatLocation(locations.getFirst());
        }

        return "%s:(%s)"
                .formatted(name, locations.stream().map(this::formatLocation).collect(Collectors.joining(", ")));
    }

    @Override
    public String formatFolder(String name, List<String> segments) {
        if (segments.isEmpty()) {
            return name;
        }

        // if the segment has elements, display the name and the elements
        String result = name + "/";

        if (segments.size() == 1) {
            return result + segments.getFirst();
        }

        return "%s(%s)".formatted(result, String.join(", ", segments));
    }
}
