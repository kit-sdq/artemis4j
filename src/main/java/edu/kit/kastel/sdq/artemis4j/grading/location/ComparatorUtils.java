/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.grading.location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;

final class ComparatorUtils {
    private ComparatorUtils() {}

    /**
     * This is essentially {@link Comparator#comparing(Function)}, but for collections, which don't implement {@link Comparable} by default.
     * <p>
     * Make sure that the provided collection has some kind of order, like a {@link java.util.TreeSet} or a {@link java.util.List}.
     * The behavior is undefined if the collection is unordered.
     *
     * @param keyExtractor the function to extract the key from the object
     * @return a comparator that compares the objects based on the extracted keys
     * @param <T> the type of the objects to compare
     * @param <V> the type of the keys to compare
     */
    static <T, V extends Comparable<V>> Comparator<T> comparing(
            Function<? super T, ? extends Collection<V>> keyExtractor) {
        return (left, right) -> {
            var leftList = new ArrayList<>(keyExtractor.apply(left));
            var rightList = new ArrayList<>(keyExtractor.apply(right));

            for (int i = 0; i < Math.min(leftList.size(), rightList.size()); i++) {
                int comparison = leftList.get(i).compareTo(rightList.get(i));
                if (comparison != 0) {
                    return comparison;
                }
            }

            return Integer.compare(leftList.size(), rightList.size());
        };
    }
}
