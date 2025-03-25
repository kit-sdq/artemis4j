/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.grading.location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public final class ComparatorUtils {
    private ComparatorUtils() {}

    /**
     * Returns a comparator that compares collections based on their elements.
     * <p>
     * The comparator will first compare the elements element-wise, and if they are equal, the collection with fewer elements
     * is considered smaller.
     *
     * @param comparator the comparator to compare the elements with
     * @return a comparator that compares collections based on their elements
     * @param <T> the type of the elements in the collections
     * @param <U> the type of the collections
     */
    public static <T, U extends Collection<T>> Comparator<U> compareByElement(Comparator<? super T> comparator) {
        return (left, right) -> {
            var leftList = new ArrayList<>(left);
            var rightList = new ArrayList<>(right);

            for (int i = 0; i < Math.min(leftList.size(), rightList.size()); i++) {
                int comparison = comparator.compare(leftList.get(i), rightList.get(i));
                if (comparison != 0) {
                    return comparison;
                }
            }

            return Integer.compare(leftList.size(), rightList.size());
        };
    }

    static <T extends Comparable<T>, U extends Collection<T>> Comparator<U> compareByElement() {
        return compareByElement(Comparator.naturalOrder());
    }

    /**
     * Returns a comparator that compares collections based on their size.
     *
     * @param comparator the comparator to compare collections with the same size
     * @return a comparator that compares collections based on their size
     * @param <T> the type of the elements in the collections
     * @param <U> the type of the collections
     */
    public static <T, U extends Collection<T>> Comparator<U> shortestFirst(Comparator<? super U> comparator) {
        // NOTE: does no longer compile if the lambda is replaced with Collection::size
        return Comparator.comparingInt((U collection) -> collection.size()).thenComparing(comparator);
    }
}
