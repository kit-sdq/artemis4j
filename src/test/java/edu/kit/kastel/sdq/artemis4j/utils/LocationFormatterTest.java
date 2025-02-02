/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import edu.kit.kastel.sdq.artemis4j.grading.LineColumn;
import edu.kit.kastel.sdq.artemis4j.grading.Location;
import org.junit.jupiter.api.Test;

class LocationFormatterTest {
    @Test
    void testSortingPathSegments() {
        LocationFormatter formatter = new LocationFormatter()
                .addLocation(new Location("src/fs/example/test/ExampleTest.java", 0, 0))
                .addLocation(new Location("src/edu/kit/kastel/QuickSort.java", 0, 0))
                .addLocation(new Location("src/edu/kit/kastel/BubbleSort.java", 0, 0))
                .addLocation(new Location("src/fs/example/Example.java", new LineColumn(0, 0), new LineColumn(1, 30)))
                .addLocation(new Location("src/fs/example/Example.java", new LineColumn(0, 0), new LineColumn(2, 20)))
                .addLocation(new Location(
                        "src/fs/example/Example.java", new LineColumn(0, 0), new LineColumn(2, Optional.empty())))
                .addLocation(new Location("src/fs/example/Example.java", new LineColumn(0, 10), new LineColumn(2, 5)))
                // the default formatter does not display the columns
                .setPathFormatter(new DefaultPathFormatter() {
                    @Override
                    public String formatLocation(Location location) {
                        LineColumn start = location.start();
                        LineColumn end = location.end();

                        if (start.equals(end)) {
                            return "L%d".formatted(start.line() + 1);
                        }

                        return start + "-" + end.toString();
                    }
                });

        assertEquals(
                "src/(edu/kit/kastel/(BubbleSort:L1, QuickSort:L1), fs/example/(Example:(L1:1-L2:31, L1:1-L3:21, L1:1-L3, L1:11-L3:6), test/ExampleTest:L1))",
                formatter.format());
    }

    @Test
    void testSortingFilesFirst() {
        LocationFormatter formatter = new LocationFormatter()
                .addLocation(new Location("src/edu/kit/kastel/BubbleSort.java", 0, 0))
                .addLocation(new Location("src/edu/kit/kastel/alpha/Beta.java", 0, 0));

        assertEquals("src/edu/kit/kastel/(BubbleSort:L1, alpha/Beta:L1)", formatter.format());
    }

    @Test
    void testShortestFirst() {
        LocationFormatter formatter = new LocationFormatter()
                .addLocation(new Location("src/edu/kit/kastel/QuickSort.java", 0, 0))
                .addLocation(new Location("src/edu/kit/kastel/extras/BubbleSort.java", 0, 0))
                .addLocation(new Location("src\\edu\\kit\\kastel\\extras\\InsertionSort.java", 0, 0));

        assertEquals("src/edu/kit/kastel/(QuickSort:L1, extras/(BubbleSort:L1, InsertionSort:L1))", formatter.format());
    }

    @Test
    void testComparable() {
        List<LocationFormatter> list = new ArrayList<>(List.of(
                new LocationFormatter()
                        .addLocation(new Location("src/edu/kit/kastel/QuickSort.java", 0, 0))
                        .addLocation(new Location("src/edu/kit/kastel/extras/BubbleSort.java", 0, 0))
                        .addLocation(new Location("src\\edu\\kit\\kastel\\extras\\InsertionSort.java", 0, 0)),
                new LocationFormatter()
                        .addLocation(new Location("src/edu/kit/kastel/QuickSort.java", 0, 0))
                        .addLocation(new Location("src/edu/kit/kastel/extras/BubbleSort.java", 0, 0)),
                new LocationFormatter().addLocation(new Location("other/Example.java", 0, 0))));

        Collections.sort(list);

        assertEquals(
                List.of(
                        new LocationFormatter().addLocation(new Location("other/Example.java", 0, 0)),
                        new LocationFormatter()
                                .addLocation(new Location("src/edu/kit/kastel/QuickSort.java", 0, 0))
                                .addLocation(new Location("src/edu/kit/kastel/extras/BubbleSort.java", 0, 0)),
                        new LocationFormatter()
                                .addLocation(new Location("src/edu/kit/kastel/QuickSort.java", 0, 0))
                                .addLocation(new Location("src/edu/kit/kastel/extras/BubbleSort.java", 0, 0))
                                .addLocation(new Location("src\\edu\\kit\\kastel\\extras\\InsertionSort.java", 0, 0))),
                list);
    }

    @Test
    void testLineLocationMerger() {
        LocationFormatter formatter = new LocationFormatter()
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 9, 9))
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 10, 19))
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 29, 29))
                .addLocation(new Location("src/edu/kit/kastel/extras/BubbleSort", 1, 1))
                .addLocation(new Location("src\\edu/kit/kastel/extras/InsertionSort", 1, 1))
                .addLocation(new Location("src\\edu/kit/kastel/extras/InsertionSort", 0, 0))
                .addLocation(new Location("src\\edu/kit/kastel/extras/InsertionSort", 1, 1))
                .setPathFormatter(new LocationMergingPathFormatter(new DefaultPathFormatter()));

        assertEquals(
                "src/edu/kit/kastel/(QuickSort:(L10-20, L30), extras/(BubbleSort:L2, InsertionSort:L1-2))",
                formatter.format());
    }

    @Test
    void testDefaultMerger() {
        LocationFormatter formatter = new LocationFormatter()
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 9, 9))
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 10, 19))
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 29, 29))
                .addLocation(new Location("src/edu/kit/kastel/extras/BubbleSort", 1, 1))
                .addLocation(new Location("src\\edu/kit/kastel/extras/InsertionSort", 1, 1))
                .addLocation(new Location("src\\edu/kit/kastel/extras/InsertionSort", 0, 0))
                .addLocation(new Location("src\\edu/kit/kastel/extras/InsertionSort", 1, 1))
                .setPathFormatter(new DefaultPathFormatter());

        assertEquals(
                "src/edu/kit/kastel/(QuickSort:(L10, L11-20, L30), extras/(BubbleSort:L2, InsertionSort:(L1, L2)))",
                formatter.format());
    }

    @Test
    void testRemoveSharedPrefixFolder() {
        LocationFormatter formatter = new LocationFormatter()
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 9, 9))
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 10, 19))
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 29, 29))
                .addLocation(new Location("src/edu/kit/kastel/extras/BubbleSort", 1, 1))
                .addLocation(new Location("src\\edu/kit/kastel/extras/InsertionSort", 1, 1))
                .addLocation(new Location("src\\edu/kit/kastel/extras/InsertionSort", 0, 0))
                .addLocation(new Location("src\\edu/kit/kastel/extras/InsertionSort", 1, 1))
                .removeSharedPrefix(true);

        assertEquals(
                "QuickSort:(L10, L11-20, L30), extras/(BubbleSort:L2, InsertionSort:(L1, L2))", formatter.format());
    }

    @Test
    void testRemoveSharedPrefixSingleFile() {
        LocationFormatter formatter = new LocationFormatter()
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 9, 9))
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 10, 19))
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 29, 29))
                .removeSharedPrefix(true);

        assertEquals("L10, L11-20, L30", formatter.format());
    }

    @Test
    void testRemoveSharedPrefixCustom() {
        String prefixToRemove = "src/edu/kit/kastel/";

        LocationFormatter formatter = new LocationFormatter()
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 9, 9))
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 10, 19))
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 29, 29))
                .removeSharedPrefix(prefixToRemove::startsWith);

        assertEquals("QuickSort:(L10, L11-20, L30)", formatter.format());
    }

    @Test
    void testRemoveSharedPrefixIncludesFile() {
        String prefixToRemove = "src/edu/kit/kastel/QuickSort";

        LocationFormatter formatter = new LocationFormatter()
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 9, 9))
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 10, 19))
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 29, 29))
                .removeSharedPrefix(prefixToRemove::startsWith);

        assertEquals("L10, L11-20, L30", formatter.format());
    }

    @Test
    void testRemoveSharedPrefixWithExtra() {
        String prefixToRemove = "src/edu/kit/kastel/QuickSort";

        LocationFormatter formatter = new LocationFormatter()
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 9, 9))
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 10, 19))
                .addLocation(new Location("src/edu/kit/kastel/QuickSort", 29, 29))
                .addLocation(new Location("src/edu/kit/kastel/Other", 29, 29))
                .removeSharedPrefix(prefixToRemove::startsWith);

        assertEquals("Other:L30, QuickSort:(L10, L11-20, L30)", formatter.format());
    }
}
