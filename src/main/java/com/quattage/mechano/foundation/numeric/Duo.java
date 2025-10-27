package com.quattage.mechano.foundation.numeric;

import java.util.Objects;

public class Duo<T> {

    private final T a;
    private final T b;

    public static <T> Duo<T> of(T a, T b) {
        return new Duo<T>(a, b);
    }

    private Duo(T a, T b) {
        this.a = a; this.b = b;
    }

    /** @return {@link #a() member A} */
    public T a() { return a; }
    /** @return {@link #a() member A} */
    public T x() { return a(); }
    /** @return {@link #a() member A} */
    public T first() { return a(); }
    /** @return {@link #a() member A} */
    public T key() { return a(); }
    /** @return {@link #a() member A} */
    public T left() { return a(); }
    /** @return {@link #a() member A} */
    public T start() { return a(); }

    /** @return {@link #b() member B} */
    public T b() { return b; }
    /** @return {@link #b() member B} */
    public T y() { return b(); }
    /** @return {@link #b() member B} */
    public T second() { return b(); }
    /** @return {@link #b() member B} */
    public T value() { return b(); }
    /** @return {@link #b() member B} */
    public  T right() { return b(); }
    /** @return {@link #b() member B} */
    public  T end() { return b(); }


    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof Duo that)) return false;
        return this.a.equals(that.a) && this.b.equals(that.b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.a.hashCode(), this.b.hashCode());
    }
}
