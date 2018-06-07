package daevil.property;

public interface ExceptionalVoid<E extends Exception> {
    void apply() throws E;
}