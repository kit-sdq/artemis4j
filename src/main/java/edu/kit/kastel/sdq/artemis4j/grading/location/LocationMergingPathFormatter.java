/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.grading.location;

import java.util.ArrayList;
import java.util.List;
import java.util.SequencedSet;
import java.util.TreeSet;

class LocationMergingPathFormatter extends DelegatingPathFormatter {
    LocationMergingPathFormatter(PathFormatter delegate) {
        super(delegate);
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

    @Override
    public String formatFile(String name, List<Location> locations) {
        return super.formatFile(name, mergeLocations(locations));
    }
}
