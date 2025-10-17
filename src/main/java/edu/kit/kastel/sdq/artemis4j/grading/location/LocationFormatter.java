/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.grading.location;

import java.util.Comparator;
import java.util.List;
import java.util.SequencedSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

/**
 * Makes multiple locations more readable by intelligently merging them.
 */
public class LocationFormatter implements Comparable<LocationFormatter> {
    private final SequencedSet<PathSegment> segments;
    private @Nullable Function<Location, String> locationToString;
    private Predicate<String> shouldRemoveSharedPrefix;
    private boolean shouldRemoveExtension;
    private boolean shouldMergeLines;

    /**
     * Creates a new location formatter.
     */
    public LocationFormatter() {
        this.segments = new TreeSet<>();
        this.locationToString = null;
        this.shouldRemoveSharedPrefix = prefix -> false;
        this.shouldRemoveExtension = true;
        this.shouldMergeLines = false;
    }

    /**
     * Adds a location to the formatter.
     *
     * @param location the location to add
     * @return this formatter
     */
    public LocationFormatter addLocation(Location location) {
        // separate the path into its components and create a segment for the last one
        List<String> path = List.of(location.filePath().split("/"));
        PathSegment segment = new PathSegment(path.getLast(), new TreeSet<>(), new TreeSet<>(Set.of(location)));

        // now create the segment for the remaining path components
        for (int i = path.size() - 2; i >= 0; i--) {
            segment = new PathSegment(path.get(i), new TreeSet<>(Set.of(segment)), new TreeSet<>());
        }

        // try to find a segment that is already in the tree and add the segment to it if possible
        boolean hasBeenAdded = false;
        for (PathSegment treeSegment : this.segments) {
            if (treeSegment.addIfPossible(segment)) {
                hasBeenAdded = true;
                break;
            }
        }

        if (!hasBeenAdded) {
            this.segments.add(segment);
        }

        return this;
    }

    /**
     * Whether to remove a common prefix between all locations or not.
     * <p>
     * By default, the common prefix is not removed.
     *
     * @param shouldRemove true if the common prefix should be removed, false otherwise
     * @return this formatter
     */
    public LocationFormatter removeSharedPrefix(boolean shouldRemove) {
        this.shouldRemoveSharedPrefix = prefix -> shouldRemove;
        return this;
    }

    /**
     * Whether to remove a common prefix between all locations or not.
     * <p>
     * By default, the common prefix is not removed.
     *
     * @param shouldRemove the predicate is passed the current shared prefix and should return true if the prefix should still be removed or false otherwise
     * @return this formatter
     */
    public LocationFormatter removeSharedPrefix(Predicate<String> shouldRemove) {
        this.shouldRemoveSharedPrefix = shouldRemove;
        return this;
    }

    /**
     * Whether to remove the extension of the files or not.
     * <p>
     * By default, the extension is removed.
     *
     * @param shouldRemove true if the extension should be removed, false otherwise
     * @return this formatter
     */
    public LocationFormatter removeExtension(boolean shouldRemove) {
        this.shouldRemoveExtension = shouldRemove;
        return this;
    }

    /**
     * Sets the function that is used to convert a {@link Location} to a {@link String}.
     * @param formatter the function that converts a location to a string
     * @return this formatter
     */
    public LocationFormatter setLocationToString(Function<Location, String> formatter) {
        this.locationToString = formatter;
        return this;
    }

    /**
     * Enables the merging of locations. Instead of displaying L1, L2, L3, L5, L6, L7, it will display L1-7.
     * <p>
     * Note that this ignores the column information, because for the correct merging of the columns,
     * the source file would be necessary.
     *
     * @return this formatter
     */
    public LocationFormatter enableLineMerging() {
        this.shouldMergeLines = true;
        return this;
    }

    private SequencedSet<PathSegment> segments() {
        return this.segments;
    }

    @Override
    public final boolean equals(Object object) {
        if (!(object instanceof LocationFormatter formatter)) {
            return false;
        }

        return this.segments.equals(formatter.segments);
    }

    @Override
    public int hashCode() {
        return this.segments.hashCode();
    }

    @Override
    public int compareTo(LocationFormatter other) {
        // Comparable is mostly implemented for convenience in intelligrade.
        return Comparator.comparing(LocationFormatter::segments, ComparatorUtils.compareByElement())
                .compare(this, other);
    }

    /**
     * Produces a formatted string of the locations.
     *
     * @return the formatted string
     */
    public String format() {
        PathFormatter formatter = this.getActualPathFormatter();

        // if there is only one segment, all locations share some common prefix and this can be removed if desired
        if (this.segments.size() == 1
                && this.shouldRemoveSharedPrefix.test(this.segments.getFirst().name())) {
            PathSegment segment = this.segments.getFirst();
            StringJoiner currentPrefix = new StringJoiner("/");
            currentPrefix.add(segment.name());
            // the paths might share a common prefix, for example when one has a path src/main/java/File.java
            // and the other src/main/java/other/File.java
            //
            // this loop calls the predicate with the current prefix:
            // 1. src
            // 2. src/main
            // 3. src/main/java
            //
            // and removes it if the predicate returns true.
            while (segment.elements().size() == 1
                    && this.shouldRemoveSharedPrefix.test(
                            currentPrefix + "/" + segment.elements().getFirst().name())) {
                segment = segment.elements().getFirst();
                currentPrefix.add(segment.name());
            }

            // if all locations are in the same file, it is unnecessary to display the filename:
            // File:(L1, L2, L3) -> L1, L2, L3
            if (segment.elements().isEmpty()) {
                return segment.toString(formatter.showFilePath(false));
            }

            return segment.elements().stream()
                    .map(subSegment -> subSegment.toString(formatter))
                    .collect(Collectors.joining(", "));
        }

        return this.segments.stream()
                .map(segment -> segment.toString(formatter))
                .collect(Collectors.joining(", "));
    }

    private PathFormatter getActualPathFormatter() {
        return new PathFormatter() {
            @Override
            public String formatLocation(Location location) {
                if (LocationFormatter.this.locationToString == null) {
                    return super.formatLocation(location);
                }

                return LocationFormatter.this.locationToString.apply(location);
            }

            @Override
            public String formatFile(String name, List<Location> locations) {
                if (LocationFormatter.this.shouldRemoveExtension) {
                    return super.formatFile(getFilenameWithoutExtension(name), locations);
                }

                return super.formatFile(name, locations);
            }

            private static String getFilenameWithoutExtension(String path) {
                String[] parts = path.split("[\\\\\\/]");
                String file = parts[parts.length - 1];

                return file.split("\\.")[0];
            }
        }.shouldMergeLines(this.shouldMergeLines);
    }
}
