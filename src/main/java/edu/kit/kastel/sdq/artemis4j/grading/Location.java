/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.util.Comparator;
import java.util.Objects;

public record Location(String filePath, LineColumn start, LineColumn end) implements Comparable<Location> {
    public Location {
        if (start.compareTo(end) > 0) {
            throw new IllegalArgumentException("start %s must be before end %s".formatted(start, end));
        }

        // In the past there were problems with paths that contained backslashes. This ensures that this will never
        // happen again.
        filePath = filePath.replace("\\", "/");
    }

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
