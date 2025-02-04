/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.grading.location;

import java.util.Comparator;
import java.util.Objects;

/**
 * Represents a location for an annotation. The location consists of a file path and a start and end inside the file.
 *
 * @param filePath the path to the file, must not be null, but can be a non-existing path like "unknown" or ""
 * @param start the start of the location (inclusive), must not be null, must be before end
 * @param end the end of the location (inclusive), must not be null, must be after start or equal to start
 */
public record Location(String filePath, LineColumn start, LineColumn end) implements Comparable<Location> {
    /**
     * Constructs a location with a file path and a start and end.
     *
     * @param filePath the path to the file
     * @param start the start of the location
     * @param end the end of the location
     */
    public Location {
        if (start.compareTo(end) > 0) {
            throw new IllegalArgumentException("start %s must be before end %s".formatted(start, end));
        }

        // In the past there were problems with paths that contained backslashes. This ensures that this will never
        // happen again.
        filePath = filePath.replace("\\", "/");
    }

    /**
     * Constructs a location with a file path and a start and end line. The start and end are entire lines.
     * @param filePath the path to the file
     * @param startLine the start line, must be >= 0, 0-indexed
     * @param endLine the end line, must be >= 0, 0-indexed
     */
    public Location(String filePath, int startLine, int endLine) {
        this(filePath, LineColumn.entireLine(startLine), LineColumn.entireLine(endLine));
    }

    @Override
    public int compareTo(Location other) {
        return Comparator.comparing(Location::filePath)
                .thenComparing(Location::start)
                .thenComparing(Location::end)
                .compare(this, other);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Location location)) {
            return false;
        }

        return Objects.equals(this.end(), location.end())
                && Objects.equals(this.filePath(), location.filePath())
                && Objects.equals(this.start(), location.start());
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(this.filePath());
        result = 31 * result + Objects.hashCode(this.start());
        result = 31 * result + Objects.hashCode(this.end());
        return result;
    }
}
