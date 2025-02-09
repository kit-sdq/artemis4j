/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.grading.location;

import java.util.ArrayList;
import java.util.List;
import java.util.SequencedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

class PathFormatter {
    private boolean shouldMergeLines;
    private boolean showFilePath;

    PathFormatter() {
        this.shouldMergeLines = false;
        this.showFilePath = true;
    }

    PathFormatter shouldMergeLines(boolean shouldMergeLines) {
        this.shouldMergeLines = shouldMergeLines;
        return this;
    }

    PathFormatter showFilePath(boolean showFilePath) {
        this.showFilePath = showFilePath;
        return this;
    }

    private static List<Location> mergeLocations(List<Location> locations) {
        // merges locations in the format L\d+(-\d+)?
        if (locations.size() == 1) {
            return locations;
        }

        // NOTE: lines are 0-indexed
        SequencedSet<Integer> lines = new TreeSet<>();

        for (var location : locations) {
            int start = location.start().line();
            lines.add(start);
            int end = location.end().line();

            if (start != end) {
                for (int i = start + 1; i <= end; i++) {
                    lines.add(i);
                }
            }
        }

        // L1, L2, L3, L5, L6, L7 can be merged to L1-7
        List<Location> result = new ArrayList<>();
        while (!lines.isEmpty()) {
            int start = lines.getFirst();
            int end = start;
            // this merges the lines that are consecutive
            while (lines.remove(end + 1)) {
                end += 1;
            }

            // the filepath is different for the merged locations, so it is left empty
            // (does not matter, because the file path is indicated through the location of the parent PathSegment)
            result.add(new Location("", start, end));
            lines.remove(start);
        }

        return result;
    }

    public String formatLocation(Location location) {
        LineColumn start = location.start();
        LineColumn end = location.end();

        if (start.line() == end.line()) {
            return "L%d".formatted(start.line() + 1);
        }

        return "L%d-%d".formatted(start.line() + 1, end.line() + 1);
    }

    public String formatFile(String name, List<Location> locations) {
        List<Location> mergedLocations = locations;
        if (this.shouldMergeLines) {
            mergedLocations = mergeLocations(locations);
        }

        String result = mergedLocations.stream().map(this::formatLocation).collect(Collectors.joining(", "));
        if (mergedLocations.size() > 1 && this.showFilePath) {
            result = "(%s)".formatted(result);
        }

        if (this.showFilePath) {
            return name + ":" + result;
        }

        return result;
    }

    String formatFolder(String name, List<String> segments) {
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
