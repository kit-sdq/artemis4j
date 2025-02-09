/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.grading.location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.SequencedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A segment of a path, which can either be a folder or a file.
 *
 * @param name the name of the segment
 * @param elements the elements of the segment, if it is a folder
 * @param locations the locations of the segment, if it is a file
 */
record PathSegment(String name, SequencedSet<PathSegment> elements, SequencedSet<Location> locations)
        implements Comparable<PathSegment> {
    PathSegment {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }

        if (!elements.isEmpty() && !locations.isEmpty()) {
            throw new IllegalArgumentException("a path segment cannot have both elements and locations");
        }

        // The file path is described by the position of the pathsegment, therefore it is removed from the locations
        locations = locations.stream()
                .map(location -> new Location("", location.start(), location.end()))
                .collect(Collectors.toCollection(TreeSet::new));

        elements = new TreeSet<>(elements);
    }

    boolean addIfPossible(PathSegment segment) {
        if (!this.name().equals(segment.name())) {
            return false;
        }

        // if the segment has locations, add them to the shared segment
        if (!segment.locations().isEmpty()) {
            this.locations.addAll(segment.locations());
            return true;
        }

        // the current name is shared, therefore try to merge the children:
        Collection<PathSegment> remaining = new ArrayList<>(segment.elements());
        for (var elem : this.elements) {
            remaining.removeIf(elem::addIfPossible);
        }

        this.elements.addAll(remaining);
        return true;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof PathSegment otherSegment
                        && this.name.equals(otherSegment.name())
                        && this.elements.equals(otherSegment.elements())
                        && this.locations.equals(otherSegment.locations());
    }

    @Override
    public int hashCode() {
        int result = this.name().hashCode();
        result = 31 * result + this.locations().hashCode();
        result = 31 * result + this.elements().hashCode();
        return result;
    }

    @Override
    public int compareTo(PathSegment other) {
        // For sorting:
        // 1. compare folders by name, then their contents
        // 2. if one is a folder and the other is a file, the file comes first
        //    ^ because the file does not have elements, it should be considered smaller
        // 3. if both are files, compare by locations
        return Comparator.comparing(PathSegment::name)
                .thenComparing(PathSegment::elements, ComparatorUtils.compareByElement())
                .thenComparing(PathSegment::locations, ComparatorUtils.compareByElement())
                .compare(this, other);
    }

    String toString(PathFormatter pathFormatter) {
        // if the segment has locations in a file, merge them via the location merger
        if (!this.locations.isEmpty()) {
            return pathFormatter.formatFile(this.name, new ArrayList<>(this.locations));
        }

        return pathFormatter.formatFolder(
                this.name,
                this.elements.stream()
                        .map(pathSegment -> pathSegment.toString(pathFormatter))
                        .toList());
    }
}
