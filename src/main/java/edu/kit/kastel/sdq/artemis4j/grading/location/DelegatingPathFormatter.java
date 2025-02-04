/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.grading.location;

import java.util.List;

abstract class DelegatingPathFormatter implements PathFormatter {
    private final PathFormatter delegate;

    protected DelegatingPathFormatter(PathFormatter delegate) {
        this.delegate = delegate;
    }

    @Override
    public String formatFile(String name, List<Location> locations) {
        return this.delegate.formatFile(name, locations);
    }

    @Override
    public String formatFolder(String name, List<String> segments) {
        return this.delegate.formatFolder(name, segments);
    }

    @Override
    public String formatLocation(Location location) {
        return this.delegate.formatLocation(location);
    }
}
