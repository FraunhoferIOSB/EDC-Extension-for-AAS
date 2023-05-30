/*
 * Copyright (c) 2021 Fraunhofer IOSB, eine rechtlich nicht selbstaendige
 * Einrichtung der Fraunhofer-Gesellschaft zur Foerderung der angewandten
 * Forschung e.V.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    @Override
    public String toString() {
        return first.toString().concat(";").concat(second.toString());
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
            return new Pair<>(this.first, this.second);
        }

    }
}
