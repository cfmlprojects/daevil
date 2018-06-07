package daevil.property;

import java.util.function.Function;
import java.util.function.Supplier;

public class Property<T> {
    private final Function<T, T> setter;
    private Supplier<T> getter;

    public Property(Supplier<T> getter, Function<T, T> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    public static <T> Writeonly<T> set(Function<T, T> setter) {
        Property<T> prop = new Property<T>(() -> {
            throw new UnsupportedOperationException();
        }, setter);
        return prop::set;
    }

    public static <T> PropertyBuilder<T> get(Supplier<T> getter) {
        return new PropertyBuilder<T>() {
            public Property<T> set(Function<T, T> setter) {
                return new Property<T>(getter, setter);
            }

            public Readonly<T> readonly() {
                Property<T> prop = new Property<T>(getter, Function.<T>identity());
                return prop::get;
            }
        };
    }

    public T set(T value) {
        return setter.apply(value);
    }

    public T get() {
        return getter.get();
    }

    public Named<T> named() {
        return new GuessesName<T>(this.getter, this.setter);
    }

    public Named<T> named(String name) {
        return new ExplicitName<T>(this.getter, this.setter, name);
    }


    public interface PropertyBuilder<T> {
        public Property<T> set(Function<T, T> setter);

        public Readonly<T> readonly();
    }

    public String toString(){
        Object value = getter.get();
        if(value instanceof String){
            return value.toString();
        }
        return getter.get().toString();
    }


}