package de.fraunhofer.iosb.app.stores.repository;

import de.fraunhofer.iosb.app.handler.aas.AasHandler;

import java.net.URI;
import java.util.Set;


public class AasServerStore extends AasStore<AasHandler<?>> {

    public boolean isStored(URI uri) {
        return store.containsKey(uri);
    }


    public Set<URI> keySet() {
        return store.keySet();
    }
}
