package edu.kit.kastel.sdq.artemis4j;

public class LazyNetworkValue<T> {
    private final NetworkSupplier<T> supplier;
    // Must be volatile to avoid reordering the checks in get()
    private volatile T value;

    public LazyNetworkValue(NetworkSupplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() throws ArtemisNetworkException {
        // First un-synchronized check
        if (this.value == null) {
            synchronized (this) {
                // Second synchronized check to avoid double initialization
                if (this.value == null) {
                    this.value = this.supplier.get();
                }
            }
        }
        return this.value;
    }

    @FunctionalInterface
    public interface NetworkSupplier<T> {
        T get() throws ArtemisNetworkException;
    }
}
