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
package de.fraunhofer.iosb.app.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class ChangeSet<A, R> {

    private final Collection<R> toRemove;
    private final Collection<A> toAdd;

    private ChangeSet(Collection<R> toRemove, Collection<A> toAdd) {
        this.toRemove = toRemove;
        this.toAdd = toAdd;
    }

    public Collection<A> toAdd() {
        return toAdd;
    }

    public Collection<R> toRemove() {
        return toRemove;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeSet<?, ?> changeSet = (ChangeSet<?, ?>) o;
        return Objects.equals(toRemove, changeSet.toRemove) && Objects.equals(toAdd, changeSet.toAdd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toRemove, toAdd);
    }

    @Override
    public String toString() {
        return "ChangeSet{to remove=%s, to add=%s}".formatted(toRemove, toAdd);
    }

    public static class Builder<A, R> {
        private final Collection<R> toRemove;
        private final Collection<A> toAdd;

        public Builder() {
            toRemove = new ArrayList<>();
            toAdd = new ArrayList<>();
        }

        public Builder<A, R> remove(Collection<R> remove) {
            toRemove.addAll(remove);
            return this;
        }

        public Builder<A, R> add(Collection<A> add) {
            toAdd.addAll(add);
            return this;
        }

        public ChangeSet<A, R> build() {
            return new ChangeSet<>(toRemove, toAdd);
        }
    }

}
