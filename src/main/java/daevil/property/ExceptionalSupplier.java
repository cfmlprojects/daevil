package daevil.property;

public interface ExceptionalSupplier<T, E extends Exception> {
    T supply() throws E;
}