/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.grading.location;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * A line-column pair representing the start or end of a {@link Location}.
 *
 * @param line the 0-indexed line in the source file on which the {@link Location} starts or ends (inclusive)
 * @param column the 0-indexed column in the source file on which the {@link Location} starts or ends (inclusive). If empty, the entire line is spanned.
 */
public record LineColumn(int line, Optional<Integer> column) implements Comparable<LineColumn> {
    public LineColumn {
        if (line < 0) {
            throw new IllegalArgumentException("line must be >= 0, but was %d".formatted(line));
        }

        if (column.isPresent() && column.get() < 0) {
            throw new IllegalArgumentException("column must be >= 0, but was %d".formatted(column.get()));
        }
    }

    /**
     * Constructs a {@link LineColumn} with a line and a column.
     *
     * @param line the 0-indexed line in the source file
     * @param column the 0-indexed column in the source file
     */
    public LineColumn(int line, int column) {
        this(line, Optional.of(column));
    }

    /**
     * Constructs a {@link LineColumn} spanning the entire line.
     *
     * @param line the line in the source file
     * @return a {@link LineColumn} spanning the entire line
     */
    public static LineColumn entireLine(int line) {
        return new LineColumn(line, Optional.empty());
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof LineColumn that)) {
            return false;
        }

        return this.line() == that.line() && this.column() == that.column();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.line, this.column);
    }

    @Override
    public int compareTo(LineColumn other) {
        return Comparator.comparingInt(LineColumn::line)
                .thenComparing(LineColumn::column, Comparator.comparingInt(value -> value.orElse(Integer.MAX_VALUE)))
                .compare(this, other);
    }

    @Override
    public String toString() {
        if (this.column.isEmpty()) {
            return "L%d".formatted(this.line + 1);
        }

        return "L%d:%d".formatted(this.line + 1, this.column.get() + 1);
    }
}
