package de.fraunhofer.iosb.app.sync;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SynchronizerRepository {
    private final Set<Synchronizer> synchronizers = new HashSet<>();

    public Collection<Synchronizer> getSynchronizers() {
        return synchronizers;
    }

    public void registerSynchronizer(Synchronizer synchronizer) {
        synchronizers.add(synchronizer);
    }

    public void unregisterSynchronizer(Synchronizer synchronizer) {
        synchronizers.remove(synchronizer);
    }
}
