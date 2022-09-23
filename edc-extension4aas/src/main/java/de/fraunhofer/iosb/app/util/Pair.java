package de.fraunhofer.iosb.app.util;

/**
 * Lightweight implementation of a tuple.
 */
public class Pair<T, U> {
    private T first;
    private U second;

    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public U getSecond() {
        return second;
    }

    public void setFirst(T t) {
        this.first = t;
    }

    public void setSecond(U u) {
        this.second = u;
    }

    public static class PairBuilder<T, U> {
        private T first;
        private U second;

        public PairBuilder<T, U> first(T t) {
            this.first = t;
            return this;
        }

        public PairBuilder<T, U> second(U u) {
            this.second = u;
            return this;
        }

        public Pair<T, U> build() {
            return new Pair<T, U>(this.first, this.second);
        }

    }
}
