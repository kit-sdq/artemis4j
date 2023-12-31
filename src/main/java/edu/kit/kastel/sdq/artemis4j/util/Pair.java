/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.util;

public record Pair<A, B>(A first, B second) {
	private static final Pair<?, ?> EMPTY = createEmptyPair();

	private static Pair<?, ?> createEmptyPair() {
		return new Pair<>(null, null);
	}

	@SuppressWarnings("unchecked")
	public static <A, B> Pair<A, B> empty() {
		return (Pair<A, B>) EMPTY;
	}

	public boolean isEmpty() {
		return this.first == null && this.second == null;
	}
}
