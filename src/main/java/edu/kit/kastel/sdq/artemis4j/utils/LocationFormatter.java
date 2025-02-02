/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.utils;

import java.util.List;
import java.util.SequencedSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import edu.kit.kastel.sdq.artemis4j.grading.Location;

/**
 * Makes multiple locations more readable by intelligently merging them.
 */
public class LocationFormatter implements Comparable<LocationFormatter> {
    private final SequencedSet<PathSegment> segments;
    private PathFormatter pathFormatter;
    private Predicate<String> shouldRemoveSharedPrefix;
    private boolean shouldRemoveExtension;

    /**
     * Creates a new location formatter.
     */
    public LocationFormatter() {
        this.segments = new TreeSet<>();
        this.pathFormatter = new DefaultPathFormatter();
        this.shouldRemoveSharedPrefix = prefix -> false;
        this.shouldRemoveExtension = true;
    }

    private void addPathSegment(String filePath, SequencedSet<Location> locations) {
        // separate the path into its components and create a segment for the last one
        List<String> path = List.of(filePath.replace("\\", "/").split("/"));
        PathSegment segment = new PathSegment(path.getLast(), new TreeSet<>(), locations);

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
    }

    /**
     * Adds a location to the formatter.
     *
     * @param location the location to add
     * @return this formatter
     */
    public LocationFormatter addLocation(Location location) {
        this.addPathSegment(location.filePath(), new TreeSet<>(Set.of(location)));
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
     * Sets the function that is used to format a path.
     *
     * @param formatter the formatter to use for paths
     * @return this formatter
     */
    public LocationFormatter setPathFormatter(PathFormatter formatter) {
        this.pathFormatter = formatter;
        return this;
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
        return ComparatorUtils.comparing(LocationFormatter::segments).compare(this, other);
    }

    private SequencedSet<PathSegment> segments() {
        return this.segments;
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
            String currentPrefix = segment.name();
            while (segment.elements().size() == 1
                    && this.shouldRemoveSharedPrefix.test(
                            currentPrefix + "/" + segment.elements().getFirst().name())) {
                segment = segment.elements().getFirst();
                currentPrefix += "/" + segment.name();
            }

            // if all locations are in the same file, it is unnecessary to display the filename:
            // File:(L1, L2, L3) -> L1, L2, L3
            if (segment.elements().isEmpty()) {
                return segment.toString(new DelegatingPathFormatter(formatter) {
                    @Override
                    public String formatFile(String name, List<Location> locations) {
                        return locations.stream().map(this::formatLocation).collect(Collectors.joining(", "));
                    }
                });
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
        if (this.shouldRemoveExtension) {
            // this removes the extension from the filename:
            return new DelegatingPathFormatter(this.pathFormatter) {
                @Override
                public String formatFile(String name, List<Location> locations) {
                    return super.formatFile(getFilenameWithoutExtension(name), locations);
                }

                private static String getFilenameWithoutExtension(String path) {
                    String[] parts = path.split("[\\\\\\/]");
                    String file = parts[parts.length - 1];

                    return file.split("\\.")[0];
                }
            };
        }

        return this.pathFormatter;
    }
}
