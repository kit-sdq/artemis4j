/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.grading;

public record Location(String filePath, LineColumn start, LineColumn end) {
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
}
