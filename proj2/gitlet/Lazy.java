package gitlet;

import java.util.function.Supplier;

/**
 * Adapted from Guava Suppliers.memoize
 *
 * @param <T> Type of the value
 */
public class Lazy<T> implements Supplier<T> {

    private volatile Supplier<T> delegate;

    private volatile boolean initialized;

    private T value;

    public Lazy(Supplier<T> delegate) {
        this.delegate = delegate;
    }

    public static <T> Lazy<T> of(Supplier<T> delegate) {
        return new Lazy<>(delegate);
    }

    @Override
    public T get() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    T t = delegate.get();
                    value = t;
                    initialized = true;
                    delegate = null;
                    return t;
                }
            }
        }
        return value;
    }
}
