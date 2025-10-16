/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j;

import org.jspecify.annotations.Nullable;

public class LazyNetworkValue<T> {
    private final NetworkSupplier<T> supplier;
    // Must be volatile to avoid reordering of the checks in get()
    private volatile @Nullable T value;

    public LazyNetworkValue(NetworkSupplier<T> supplier) {
        this.supplier = supplier;
    }

    public @Nullable T get() throws ArtemisNetworkException {
        // Store the value in a local variable to avoid invalidation between the null check and the return
        T localValue = this.value;

        // First un-synchronized check
        if (localValue == null) {
            synchronized (this) {
                // Second synchronized check to avoid double initialization
                if (this.value == null) {
                    this.value = this.supplier.get();
                }
                localValue = this.value;
            }
        }

        return localValue;
    }

    public void invalidate() {
        synchronized (this) {
            this.value = null;
        }
    }

    @FunctionalInterface
    public interface NetworkSupplier<T> {
        T get() throws ArtemisNetworkException;
    }
}
