package de.fraunhofer.iosb.app.stores.repository;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AasStore<T> {
    protected final Map<URI, T> store;

    public AasStore() {
        this.store = new ConcurrentHashMap<>();
    }

    public Optional<T> get(URI uri) {
        return Optional.ofNullable(store.get(uri));
    }

    public List<T> getAll() {
        return store.values().stream().toList();
    }

    public boolean put(URI uri, T t) {
        return null == store.put(uri, t);
    }

    public T remove(URI uri) {
        return store.remove(uri);
    }
}
